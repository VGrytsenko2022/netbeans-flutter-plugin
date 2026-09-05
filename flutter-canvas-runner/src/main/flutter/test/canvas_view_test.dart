import 'dart:convert';
import 'dart:math' as math;
import 'dart:ui' as ui;

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';

import 'canvas_model_test.dart' as fixture;

final Uint8List _viewPng8 = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAA'
  'AARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/'
  'j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=',
);

void main() {
  test('models the reviewed multi-slot Canvas insertion matrix', () {
    expect(canvasChildrenAppendDropSlot.insertionIndexFor(0), 0);
    expect(canvasChildrenAppendDropSlot.insertionIndexFor(7), 7);
    expect(canvasChildrenAppendDropSlot.insertionIndexFor(10000), isNull);
    expect(
      canvasChildrenAppendDropSlot.accepts(
        currentChildCount: 7,
        insertionIndex: 6,
      ),
      isFalse,
    );
    expect(canvasEmptyChildDropSlot.insertionIndexFor(0), 0);
    expect(canvasEmptyChildDropSlot.insertionIndexFor(1), isNull);
    expect(canvasDropSlotsForWidgetType('flutter.material.Scaffold'), const [
      canvasScaffoldAppBarDropSlot,
      canvasScaffoldBodyDropSlot,
      canvasScaffoldFloatingActionButtonDropSlot,
    ]);
    expect(
      canvasDropSlotForWidgetSlot('flutter.material.Scaffold', 'body'),
      same(canvasScaffoldBodyDropSlot),
    );
    expect(
      canvasDropSlotForWidgetSlot(
        'flutter.material.Scaffold',
        'floatingActionButton',
      ),
      same(canvasScaffoldFloatingActionButtonDropSlot),
    );
    expect(
      canvasDropSlotForWidgetSlot('flutter.material.Scaffold', 'appBar'),
      same(canvasScaffoldAppBarDropSlot),
    );
    expect(canvasDropSlotsForWidgetType('flutter.material.AppBar'), const [
      canvasAppBarLeadingDropSlot,
      canvasAppBarTitleDropSlot,
      canvasAppBarActionsDropSlot,
      canvasAppBarFlexibleSpaceDropSlot,
      canvasAppBarBottomDropSlot,
    ]);
    final textSource = CanvasPaletteDragSource(
      token: 'text-source',
      widgetType: 'flutter.widgets.Text',
      traits: const {},
    );
    final appBarSource = CanvasPaletteDragSource(
      token: 'appbar-source',
      widgetType: 'flutter.material.AppBar',
      traits: const {canvasPreferredSizeWidgetTrait},
    );
    expect(canvasScaffoldAppBarDropSlot.acceptsSource(textSource), isFalse);
    expect(canvasAppBarBottomDropSlot.acceptsSource(textSource), isFalse);
    expect(canvasScaffoldAppBarDropSlot.acceptsSource(appBarSource), isTrue);
    expect(canvasAppBarBottomDropSlot.acceptsSource(appBarSource), isTrue);
    expect(canvasAppBarLeadingDropSlot.acceptsSource(textSource), isTrue);
    expect(canvasAppBarTitleDropSlot.acceptsSource(textSource), isTrue);
    expect(canvasAppBarActionsDropSlot.acceptsSource(textSource), isTrue);
    expect(canvasAppBarFlexibleSpaceDropSlot.acceptsSource(textSource), isTrue);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Padding'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Align'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Center'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.ColoredBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Placeholder'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.ColoredBox',
        slotName: 'child',
        currentChildCount: 0,
        insertionIndex: 0,
        source: textSource,
      ),
      isTrue,
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.ColoredBox',
        slotName: 'child',
        currentChildCount: 1,
        insertionIndex: 0,
        source: textSource,
      ),
      isFalse,
    );
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Container'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.DecoratedBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.ExcludeSemantics'),
      const [canvasEmptyChildDropSlot],
    );
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.ConstrainedBox'),
      const [canvasEmptyChildDropSlot],
    );
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.UnconstrainedBox'),
      const [canvasEmptyChildDropSlot],
    );
    expect(canvasDropSlotsForWidgetType('flutter.widgets.LimitedBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.OverflowBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.FractionallySizedBox'),
      const [canvasEmptyChildDropSlot],
    );
    expect(canvasDropSlotsForWidgetType('flutter.widgets.FittedBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.SizedBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.SingleChildScrollView'),
      const [canvasEmptyChildDropSlot],
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.SingleChildScrollView',
        slotName: 'child',
        currentChildCount: 0,
        insertionIndex: 0,
        source: textSource,
      ),
      isTrue,
    );
    for (final flexType in const [
      canvasExpandedWidgetType,
      canvasFlexibleWidgetType,
      canvasSpacerWidgetType,
    ]) {
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.widgets.SingleChildScrollView',
          slotName: 'child',
          currentChildCount: 0,
          insertionIndex: 0,
          source: CanvasPaletteDragSource(
            token: '$flexType-single-scroll-source',
            widgetType: flexType,
            traits: const {},
          ),
        ),
        isFalse,
      );
    }
    expect(canvasDropSlotsForWidgetType('flutter.widgets.AspectRatio'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Baseline'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.IntrinsicHeight'),
      const [canvasEmptyChildDropSlot],
    );
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.IntrinsicWidth'),
      const [canvasEmptyChildDropSlot],
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.IntrinsicWidth',
        slotName: 'child',
        currentChildCount: 0,
        insertionIndex: 0,
        source: textSource,
      ),
      isTrue,
    );
    for (final flexType in const [
      canvasExpandedWidgetType,
      canvasFlexibleWidgetType,
      canvasSpacerWidgetType,
    ]) {
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.widgets.IntrinsicWidth',
          slotName: 'child',
          currentChildCount: 0,
          insertionIndex: 0,
          source: CanvasPaletteDragSource(
            token: '$flexType-source',
            widgetType: flexType,
            traits: const {},
          ),
        ),
        isFalse,
        reason: '$flexType requires a direct Row or Column children slot',
      );
    }
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Offstage'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Offstage',
        slotName: 'child',
        currentChildCount: 0,
        insertionIndex: 0,
        source: textSource,
      ),
      isTrue,
    );
    for (final flexType in const [
      canvasExpandedWidgetType,
      canvasFlexibleWidgetType,
      canvasSpacerWidgetType,
    ]) {
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.widgets.Offstage',
          slotName: 'child',
          currentChildCount: 0,
          insertionIndex: 0,
          source: CanvasPaletteDragSource(
            token: '$flexType-offstage-source',
            widgetType: flexType,
            traits: const {},
          ),
        ),
        isFalse,
        reason: '$flexType requires a direct Row or Column children slot',
      );
    }
    expect(
      canvasDropSlotsForWidgetType('flutter.widgets.SizedOverflowBox'),
      const [canvasEmptyChildDropSlot],
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.SizedOverflowBox',
        slotName: 'child',
        currentChildCount: 0,
        insertionIndex: 0,
        source: textSource,
      ),
      isTrue,
    );
    for (final flexType in const [
      canvasExpandedWidgetType,
      canvasFlexibleWidgetType,
      canvasSpacerWidgetType,
    ]) {
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.widgets.SizedOverflowBox',
          slotName: 'child',
          currentChildCount: 0,
          insertionIndex: 0,
          source: CanvasPaletteDragSource(
            token: '$flexType-sized-overflow-source',
            widgetType: flexType,
            traits: const {},
          ),
        ),
        isFalse,
        reason: '$flexType requires a direct Row or Column children slot',
      );
    }
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Opacity'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.RotatedBox'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Transform'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.ClipRect'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.ClipRect',
        slotName: 'child',
        currentChildCount: 0,
        insertionIndex: 0,
        source: textSource,
      ),
      isTrue,
    );
    for (final flexType in const [
      canvasExpandedWidgetType,
      canvasFlexibleWidgetType,
      canvasSpacerWidgetType,
    ]) {
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.widgets.ClipRect',
          slotName: 'child',
          currentChildCount: 0,
          insertionIndex: 0,
          source: CanvasPaletteDragSource(
            token: '$flexType-clip-rect-source',
            widgetType: flexType,
            traits: const {},
          ),
        ),
        isFalse,
        reason: '$flexType requires a direct Row or Column children slot',
      );
    }
    expect(canvasDropSlotsForWidgetType('flutter.widgets.ClipOval'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.ClipOval',
        slotName: 'child',
        currentChildCount: 0,
        insertionIndex: 0,
        source: textSource,
      ),
      isTrue,
    );
    for (final flexType in const [
      canvasExpandedWidgetType,
      canvasFlexibleWidgetType,
      canvasSpacerWidgetType,
    ]) {
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.widgets.ClipOval',
          slotName: 'child',
          currentChildCount: 0,
          insertionIndex: 0,
          source: CanvasPaletteDragSource(
            token: '$flexType-clip-oval-source',
            widgetType: flexType,
            traits: const {},
          ),
        ),
        isFalse,
        reason: '$flexType requires a direct Row or Column children slot',
      );
    }
    expect(canvasDropSlotsForWidgetType('flutter.widgets.ClipRRect'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.ClipRRect',
        slotName: 'child',
        currentChildCount: 0,
        insertionIndex: 0,
        source: textSource,
      ),
      isTrue,
    );
    for (final flexType in const [
      canvasExpandedWidgetType,
      canvasFlexibleWidgetType,
      canvasSpacerWidgetType,
    ]) {
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: 'flutter.widgets.ClipRRect',
          slotName: 'child',
          currentChildCount: 0,
          insertionIndex: 0,
          source: CanvasPaletteDragSource(
            token: '$flexType-clip-rrect-source',
            widgetType: flexType,
            traits: const {},
          ),
        ),
        isFalse,
        reason: '$flexType requires a direct Row or Column children slot',
      );
    }
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Stack'), const [
      canvasStackChildrenAppendDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.IndexedStack'), const [
      canvasIndexedStackChildrenAppendDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.ListView'), const [
      canvasChildrenAppendDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.GridView'), const [
      canvasChildrenAppendDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.ListBody'), const [
      canvasChildrenAppendDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.OverflowBar'), const [
      canvasChildrenAppendDropSlot,
    ]);
    expect(
      canvasStackChildrenAppendDropSlot.zonePlacement,
      CanvasDropZonePlacement.fullNode,
    );
    expect(
      canvasIndexedStackChildrenAppendDropSlot.zonePlacement,
      CanvasDropZonePlacement.fullNode,
    );
    expect(
      canvasDropSlotsForWidgetType('flutter.material.ElevatedButton'),
      const [canvasEmptyChildDropSlot],
    );
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Icon'), isEmpty);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Image'), isEmpty);
    expect(canvasDropSlotsForWidgetType('flutter.material.TextField'), isEmpty);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Text'), isEmpty);
    expect(
      canvasEmptyChildDropSlot.accepts(currentChildCount: 0, insertionIndex: 1),
      isFalse,
    );
    expect(canvasChildrenAppendDropSlot.modelSlotKind, 'list');
    expect(canvasScaffoldBodyDropSlot.modelSlotKind, 'single');
    expect(canvasExpandedWrapDropSlot.wrapsExistingChild, isTrue);
    expect(canvasFlexibleWrapDropSlot, same(canvasExpandedWrapDropSlot));
    expect(canvasExpandedWrapDropSlot.insertionIndexFor(2), isNull);
    expect(
      canvasExpandedWrapDropSlot.accepts(
        currentChildCount: 2,
        insertionIndex: 1,
      ),
      isTrue,
    );
    expect(
      canvasExpandedWrapDropSlot.accepts(
        currentChildCount: 2,
        insertionIndex: 2,
      ),
      isFalse,
    );
    expect(canvasDropSlotsForWidgetType(canvasExpandedWidgetType), isEmpty);
    expect(canvasDropSlotsForWidgetType(canvasFlexibleWidgetType), isEmpty);
    expect(canvasDropSlotsForWidgetType(canvasSpacerWidgetType), isEmpty);
    expect(canvasDropSlotsForWidgetType(canvasSafeAreaWidgetType), isEmpty);
    expect(
      canvasDropSlotsForWidgetType(canvasDirectionalityWidgetType),
      isEmpty,
    );
    expect(
      isCanvasReviewedRequiredChildWrapperWidgetType(canvasSafeAreaWidgetType),
      isTrue,
    );
    expect(
      isCanvasReviewedRequiredChildWrapperWidgetType(
        canvasDirectionalityWidgetType,
      ),
      isTrue,
    );
    expect(
      isCanvasReviewedRequiredChildWrapperWidgetType(canvasExpandedWidgetType),
      isTrue,
      reason: 'shape is generic even though Canvas placement is flex-special',
    );
    expect(
      isCanvasReviewedRequiredChildWrapperWidgetType('flutter.widgets.Center'),
      isFalse,
    );

    final safeAreaSource = CanvasPaletteDragSource(
      token: 'safe-area-source',
      widgetType: canvasSafeAreaWidgetType,
      traits: const {},
    );
    for (final target
        in <
          ({
            String parentWidgetType,
            String slotName,
            int childCount,
            int index,
          })
        >[
          (
            parentWidgetType: 'flutter.widgets.Center',
            slotName: 'child',
            childCount: 1,
            index: 0,
          ),
          (
            parentWidgetType: 'flutter.widgets.Row',
            slotName: 'children',
            childCount: 2,
            index: 1,
          ),
          (
            parentWidgetType: canvasExpandedWidgetType,
            slotName: 'child',
            childCount: 1,
            index: 0,
          ),
          (
            parentWidgetType: canvasSafeAreaWidgetType,
            slotName: 'child',
            childCount: 1,
            index: 0,
          ),
        ]) {
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: target.parentWidgetType,
          slotName: target.slotName,
          currentChildCount: target.childCount,
          insertionIndex: target.index,
          source: safeAreaSource,
        ),
        isTrue,
        reason: '${target.parentWidgetType}.${target.slotName}',
      );
    }
    expect(
      canvasExistingChildWrapTargetSlot(
        parentWidgetType: 'flutter.widgets.Center',
        slotName: 'child',
      )?.modelSlotKind,
      'single',
    );
    expect(
      canvasExistingChildWrapTargetSlot(
        parentWidgetType: 'flutter.widgets.Row',
        slotName: 'children',
      )?.modelSlotKind,
      'list',
    );
    for (final rejected
        in <
          ({
            String parentWidgetType,
            String slotName,
            int childCount,
            int index,
          })
        >[
          (
            parentWidgetType: 'flutter.widgets.Center',
            slotName: 'child',
            childCount: 0,
            index: 0,
          ),
          (
            parentWidgetType: 'flutter.widgets.Row',
            slotName: 'children',
            childCount: 2,
            index: 2,
          ),
          (
            parentWidgetType: 'flutter.material.Scaffold',
            slotName: 'appBar',
            childCount: 1,
            index: 0,
          ),
          (
            parentWidgetType: 'flutter.widgets.Center',
            slotName: 'futureSlot',
            childCount: 1,
            index: 0,
          ),
        ]) {
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: rejected.parentWidgetType,
          slotName: rejected.slotName,
          currentChildCount: rejected.childCount,
          insertionIndex: rejected.index,
          source: safeAreaSource,
        ),
        isFalse,
        reason: '${rejected.parentWidgetType}.${rejected.slotName}',
      );
    }
    expect(
      canvasWrapperAcceptsExistingChild(
        wrapperWidgetType: canvasSafeAreaWidgetType,
        childWidgetType: 'flutter.widgets.Text',
      ),
      isTrue,
    );
    for (final flexType in const [
      canvasExpandedWidgetType,
      canvasFlexibleWidgetType,
      canvasSpacerWidgetType,
    ]) {
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: canvasSafeAreaWidgetType,
          childWidgetType: flexType,
        ),
        isFalse,
      );
    }

    final directionalitySource = CanvasPaletteDragSource(
      token: 'directionality-source',
      widgetType: canvasDirectionalityWidgetType,
      traits: const {},
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Center',
        slotName: 'child',
        currentChildCount: 1,
        insertionIndex: 0,
        source: directionalitySource,
      ),
      isTrue,
      reason: 'Directionality atomically wraps an existing ordinary child',
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Row',
        slotName: 'children',
        currentChildCount: 2,
        insertionIndex: 1,
        source: directionalitySource,
      ),
      isTrue,
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Row',
        slotName: 'children',
        currentChildCount: 2,
        insertionIndex: 2,
        source: directionalitySource,
      ),
      isFalse,
      reason: 'required-child wrappers never terminal-append',
    );
    for (final flexType in const [
      canvasExpandedWidgetType,
      canvasFlexibleWidgetType,
      canvasSpacerWidgetType,
    ]) {
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: canvasDirectionalityWidgetType,
          childWidgetType: flexType,
        ),
        isFalse,
      );
    }

    final spacerSource = CanvasPaletteDragSource(
      token: 'spacer-source',
      widgetType: canvasSpacerWidgetType,
      traits: const {},
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Row',
        slotName: 'children',
        currentChildCount: 0,
        insertionIndex: 0,
        source: spacerSource,
      ),
      isTrue,
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Column',
        slotName: 'children',
        currentChildCount: 1,
        insertionIndex: 1,
        source: spacerSource,
      ),
      isTrue,
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Row',
        slotName: 'children',
        currentChildCount: 1,
        insertionIndex: 0,
        source: spacerSource,
      ),
      isFalse,
    );
    expect(
      canvasDropTargetAcceptsSource(
        parentWidgetType: 'flutter.widgets.Stack',
        slotName: 'children',
        currentChildCount: 0,
        insertionIndex: 0,
        source: spacerSource,
      ),
      isFalse,
    );
  });

  test('closes the 56-source by 53-destination compatibility matrix', () {
    const sourceTypes = {
      'flutter.material.Scaffold',
      'flutter.material.AppBar',
      'flutter.material.ElevatedButton',
      'flutter.material.TextField',
      'flutter.widgets.Align',
      'flutter.widgets.AspectRatio',
      'flutter.widgets.Baseline',
      'flutter.widgets.IntrinsicHeight',
      'flutter.widgets.IntrinsicWidth',
      'flutter.widgets.Offstage',
      'flutter.widgets.RotatedBox',
      'flutter.widgets.SafeArea',
      'flutter.widgets.Directionality',
      'flutter.widgets.SizedOverflowBox',
      'flutter.widgets.Transform',
      'flutter.widgets.Column',
      'flutter.widgets.Row',
      'flutter.widgets.Wrap',
      'flutter.widgets.Padding',
      'flutter.widgets.Center',
      'flutter.widgets.ClipRect',
      'flutter.widgets.ClipOval',
      'flutter.widgets.ClipRRect',
      'flutter.widgets.ClipPath',
      'flutter.widgets.ClipRSuperellipse',
      'flutter.widgets.PhysicalModel',
      'flutter.widgets.PhysicalShape',
      'flutter.widgets.RepaintBoundary',
      'flutter.widgets.IgnorePointer',
      'flutter.widgets.AbsorbPointer',
      'flutter.widgets.ConstrainedBox',
      'flutter.widgets.UnconstrainedBox',
      'flutter.widgets.LimitedBox',
      'flutter.widgets.OverflowBox',
      'flutter.widgets.Placeholder',
      'flutter.widgets.ColoredBox',
      'flutter.widgets.Container',
      'flutter.widgets.DecoratedBox',
      'flutter.widgets.ExcludeSemantics',
      'flutter.widgets.Expanded',
      'flutter.widgets.Flexible',
      'flutter.widgets.Spacer',
      'flutter.widgets.FittedBox',
      'flutter.widgets.FractionallySizedBox',
      'flutter.widgets.Opacity',
      'flutter.widgets.Icon',
      'flutter.widgets.Image',
      'flutter.widgets.IndexedStack',
      'flutter.widgets.ListBody',
      'flutter.widgets.ListView',
      'flutter.widgets.GridView',
      'flutter.widgets.SingleChildScrollView',
      'flutter.widgets.OverflowBar',
      'flutter.widgets.SizedBox',
      'flutter.widgets.Stack',
      'flutter.widgets.Text',
    };
    final destinations =
        <({String parentType, CanvasDropSlotSemantics slot})>[];
    for (final type in sourceTypes) {
      destinations.addAll([
        for (final slot in canvasDropSlotsForWidgetType(type))
          (parentType: type, slot: slot),
      ]);
    }
    expect(sourceTypes, hasLength(56));
    expect(destinations, hasLength(53));

    var accepted = 0;
    var rejected = 0;
    for (final widgetType in sourceTypes) {
      final source = CanvasPaletteDragSource(
        token: widgetType,
        widgetType: widgetType,
        traits: canvasWidgetTraitsForType(widgetType),
      );
      for (final destination in destinations) {
        if (canvasDropTargetAcceptsSource(
          parentWidgetType: destination.parentType,
          slotName: destination.slot.slotName,
          currentChildCount: isCanvasPaletteWrapperWidgetType(widgetType)
              ? 1
              : 0,
          insertionIndex: 0,
          source: source,
        )) {
          accepted++;
        } else {
          rejected++;
        }
      }
    }
    expect(accepted, 2711);
    expect(rejected, 257);
    expect(accepted + rejected, 2968);
  });

  testWidgets('applies every exact adaptive target to the Flutter theme', (
    tester,
  ) async {
    const expected = <String, TargetPlatform>{
      'android': TargetPlatform.android,
      'ios': TargetPlatform.iOS,
      'windows': TargetPlatform.windows,
      'macos': TargetPlatform.macOS,
      'linux': TargetPlatform.linux,
      // Web layout preview uses the browser-sized viewport while the native
      // Windows runner supplies the closest available adaptive platform.
      'web': TargetPlatform.windows,
    };

    for (final entry in expected.entries) {
      final json =
          jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
              as Map<String, Object?>;
      final profile = json['profile']! as Map<String, Object?>;
      profile['targetPlatform'] = entry.key;
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );

      final app = tester.widget<MaterialApp>(find.byType(MaterialApp));
      expect(app.theme!.platform, entry.value, reason: entry.key);
      expect(app.darkTheme!.platform, entry.value, reason: entry.key);
    }
  });

  test(
    'rejects an unknown adaptive target instead of silently falling back',
    () {
      expect(
        () => canvasAdaptiveTargetPlatform('unknown'),
        throwsArgumentError,
      );
    },
  );

  testWidgets('gates all Canvas input behind a visible exact fence barrier', (
    tester,
  ) async {
    tester.view.devicePixelRatio = 1;
    tester.view.physicalSize = const Size(800, 600);
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    var interactions = 0;
    var deletes = 0;
    final selections = <String>[];
    await tester.pumpWidget(
      CanvasModelApp(
        model: model,
        selectedWidgetId: null,
        onSelected: selections.add,
        onInteraction: () => interactions++,
        onDeleteSelected: () {
          deletes++;
          return true;
        },
        interactionInputSynchronized: false,
      ),
    );

    expect(
      find.byKey(const ValueKey('canvas-interaction-fence-overlay')),
      findsOneWidget,
    );
    expect(find.text('Synchronizing input…'), findsOneWidget);
    await tester.tapAt(const Offset(8, 300));
    await tester.tapAt(const Offset(400, 300));
    await tester.sendKeyEvent(LogicalKeyboardKey.delete);

    expect(interactions, 0);
    expect(selections, isEmpty);
    expect(deletes, 0);

    await tester.pumpWidget(
      CanvasModelApp(
        model: model,
        selectedWidgetId: null,
        onSelected: selections.add,
        onInteraction: () => interactions++,
        onDeleteSelected: () {
          deletes++;
          return true;
        },
        interactionInputSynchronized: true,
      ),
    );
    expect(
      find.byKey(const ValueKey('canvas-interaction-fence-overlay')),
      findsNothing,
    );
    await tester.tapAt(const Offset(8, 300));
    await tester.tapAt(const Offset(400, 300));
    await tester.sendKeyEvent(LogicalKeyboardKey.delete);

    expect(interactions, 2);
    expect(deletes, 1);
  });

  testWidgets(
    'keeps the logical MediaQuery fixed while manual zoom enables panning',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      final metrics = <CanvasViewportMetrics>[];
      final changes = <CanvasViewportPresentation>[];
      final presentation = CanvasViewportPresentation.fit(
        model,
      ).copyWith(mode: 'manual', zoomMicros: 2000000);
      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: SizedBox(
              width: 600,
              height: 400,
              child: CanvasDocumentView(
                model: model,
                selectedWidgetId: null,
                onSelected: (_) {},
                viewportPresentation: presentation,
                onViewportPresentationChanged: changes.add,
                onViewportMetricsChanged: metrics.add,
              ),
            ),
          ),
        ),
      );
      await tester.pump();

      final rootContext = tester.element(
        find.byKey(ValueKey('canvas-widget-${model.root.id}')),
      );
      expect(MediaQuery.sizeOf(rootContext), const Size(390, 844));
      expect(
        MediaQuery.devicePixelRatioOf(rootContext),
        model.profile.devicePixelRatio,
      );
      expect(
        find.byKey(const ValueKey('canvas-horizontal-viewport-scrollbar')),
        findsOneWidget,
      );
      expect(
        find.byKey(const ValueKey('canvas-vertical-viewport-scrollbar')),
        findsOneWidget,
      );
      expect(
        tester
            .getSize(
              find.byKey(
                const ValueKey('canvas-horizontal-viewport-scrollbar'),
              ),
            )
            .height,
        12,
        reason: 'the transparent pointer target remains comfortably usable',
      );
      expect(
        tester
            .getSize(
              find.byKey(const ValueKey('canvas-vertical-viewport-scrollbar')),
            )
            .width,
        12,
      );
      expect(
        tester
            .getSize(
              find.byKey(
                const ValueKey('canvas-horizontal-viewport-scrollbar-track'),
              ),
            )
            .height,
        6,
        reason: 'the visible horizontal track is exactly twice as thin',
      );
      expect(
        tester
            .getSize(
              find.byKey(
                const ValueKey('canvas-vertical-viewport-scrollbar-track'),
              ),
            )
            .width,
        6,
      );
      expect(
        tester
            .getSize(
              find.byKey(
                const ValueKey('canvas-horizontal-viewport-scrollbar-thumb'),
              ),
            )
            .height,
        6,
      );
      expect(
        tester
            .getSize(
              find.byKey(
                const ValueKey('canvas-vertical-viewport-scrollbar-thumb'),
              ),
            )
            .width,
        6,
      );
      expect(metrics.last.effectiveScaleMicros, 2000000);
      expect(metrics.last.horizontalScrollable, isTrue);
      expect(metrics.last.verticalScrollable, isTrue);

      final horizontalHit = tester.getRect(
        find.byKey(const ValueKey('canvas-horizontal-viewport-scrollbar')),
      );
      final horizontalTrack = tester.getRect(
        find.byKey(
          const ValueKey('canvas-horizontal-viewport-scrollbar-track'),
        ),
      );
      final horizontalGutter = Offset(
        horizontalHit.center.dx,
        horizontalHit.top + 1,
      );
      expect(horizontalTrack.contains(horizontalGutter), isFalse);
      final horizontalGesture = await tester.startGesture(
        horizontalGutter,
        kind: PointerDeviceKind.mouse,
      );
      await tester.pump();
      expect(changes.last.horizontalScrollMicros, 500000);
      await horizontalGesture.moveBy(const Offset(40, 0));
      await tester.pump();
      expect(changes.last.horizontalScrollMicros, greaterThan(500000));
      await horizontalGesture.up();

      final verticalHit = tester.getRect(
        find.byKey(const ValueKey('canvas-vertical-viewport-scrollbar')),
      );
      final verticalTrack = tester.getRect(
        find.byKey(const ValueKey('canvas-vertical-viewport-scrollbar-track')),
      );
      final verticalGutter = Offset(
        verticalHit.left + 1,
        verticalHit.center.dy,
      );
      expect(verticalTrack.contains(verticalGutter), isFalse);
      final verticalGesture = await tester.startGesture(
        verticalGutter,
        kind: PointerDeviceKind.mouse,
      );
      await tester.pump();
      expect(changes.last.verticalScrollMicros, 500000);
      await verticalGesture.up();
    },
  );

  testWidgets('native wheel pans and Ctrl-wheel changes manual zoom', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    final changes = <CanvasViewportPresentation>[];
    final presentation = CanvasViewportPresentation.fit(
      model,
    ).copyWith(mode: 'manual', zoomMicros: 1500000);
    await tester.pumpWidget(
      MaterialApp(
        home: Center(
          child: SizedBox(
            width: 600,
            height: 400,
            child: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
              viewportPresentation: presentation,
              onViewportPresentationChanged: changes.add,
            ),
          ),
        ),
      ),
    );
    await tester.pump();
    final center = tester.getCenter(find.byType(CanvasDocumentView));

    tester.binding.handlePointerEvent(
      PointerScrollEvent(
        position: center,
        kind: PointerDeviceKind.mouse,
        scrollDelta: const Offset(0, 100),
      ),
    );
    await tester.pump();
    expect(changes.last.verticalScrollMicros, greaterThan(0));
    expect(changes.last.horizontalScrollMicros, 0);

    await tester.sendKeyDownEvent(LogicalKeyboardKey.shiftLeft);
    tester.binding.handlePointerEvent(
      PointerScrollEvent(
        position: center,
        kind: PointerDeviceKind.mouse,
        scrollDelta: const Offset(0, 100),
      ),
    );
    await tester.sendKeyUpEvent(LogicalKeyboardKey.shiftLeft);
    await tester.pump();
    expect(changes.last.horizontalScrollMicros, greaterThan(0));

    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    tester.binding.handlePointerEvent(
      PointerScrollEvent(
        position: center,
        kind: PointerDeviceKind.mouse,
        scrollDelta: const Offset(0, -100),
      ),
    );
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();
    expect(changes.last.mode, 'manual');
    expect(changes.last.commandSequence, presentation.commandSequence);
    expect(changes.last.zoomMicros, 1600000);
  });

  testWidgets(
    'emits Delete only while Canvas owns focus and ignores modified Delete',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      final selected = model.widgetIds.firstWhere((id) => id != model.root.id);
      var deleteCalls = 0;
      final selections = <String>[];
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: selected,
            onSelected: selections.add,
            onDeleteSelected: () {
              deleteCalls++;
              return true;
            },
          ),
        ),
      );
      await tester.pump();

      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(
        deleteCalls,
        0,
        reason: 'rendering the Canvas must not claim keyboard focus',
      );

      await tester.sendKeyDownEvent(LogicalKeyboardKey.shiftLeft);
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      await tester.sendKeyUpEvent(LogicalKeyboardKey.shiftLeft);
      expect(deleteCalls, 0);

      FocusManager.instance.primaryFocus?.unfocus();
      await tester.pump();
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(deleteCalls, 0, reason: 'an unfocused Canvas must stay inert');

      await tester.tap(find.byKey(ValueKey('canvas-widget-$selected')));
      await tester.pump();
      expect(selections, hasLength(1));
      expect(model.widgetIds, contains(selections.single));
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(deleteCalls, 1, reason: 'widget tap gives the Canvas focus');
    },
  );

  testWidgets(
    'does not steal external focus on render and claims it on Canvas pointer down',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      final externalFocusNode = FocusNode(debugLabel: 'external-owner');
      addTearDown(externalFocusNode.dispose);
      var interactions = 0;

      Widget buildApp() {
        return MaterialApp(
          home: Column(
            children: [
              Focus(
                focusNode: externalFocusNode,
                child: const SizedBox(width: 1, height: 1),
              ),
              Expanded(
                child: CanvasDocumentView(
                  model: model,
                  selectedWidgetId: null,
                  onSelected: (_) {},
                  onInteraction: () => interactions++,
                ),
              ),
            ],
          ),
        );
      }

      await tester.pumpWidget(buildApp());
      externalFocusNode.requestFocus();
      await tester.pump();
      expect(externalFocusNode.hasFocus, isTrue);

      await tester.pumpWidget(buildApp());
      await tester.pump();
      expect(
        externalFocusNode.hasFocus,
        isTrue,
        reason: 'a Canvas rebuild must preserve the current external owner',
      );

      await tester.tap(
        find.byKey(const ValueKey('canvas-interaction-surface')),
      );
      await tester.pump();
      expect(externalFocusNode.hasFocus, isFalse);
      expect(interactions, 1);
    },
  );

  testWidgets(
    'opens inline Text.data editing only by selected Text double-click or F2',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
      const rowId = '1035b7df-df9b-442b-9af2-72b4c90f1462';

      Future<void> pump({required String? selected, required bool enabled}) =>
          tester.pumpWidget(
            MaterialApp(
              home: CanvasDocumentView(
                model: model,
                selectedWidgetId: selected,
                onSelected: (_) {},
                inlineTextEditEnabled: enabled,
              ),
            ),
          );

      await pump(selected: null, enabled: true);
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$textId')),
      );
      await tester.pump();
      expect(find.byType(TextField), findsNothing);

      await pump(selected: rowId, enabled: true);
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$rowId')),
      );
      await tester.pump();
      expect(find.byType(TextField), findsNothing);

      await pump(selected: textId, enabled: false);
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$textId')),
      );
      await tester.pump();
      expect(find.byType(TextField), findsNothing);

      await pump(selected: textId, enabled: true);
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$textId')),
      );
      await tester.pump();
      expect(
        find.byKey(const ValueKey('canvas-inline-text-editor-$textId')),
        findsOneWidget,
      );
      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pump();

      await tester.tap(find.byKey(const ValueKey('canvas-widget-$textId')));
      await tester.pump(kDoubleTapTimeout);
      await tester.sendKeyEvent(LogicalKeyboardKey.f2);
      await tester.pump();
      expect(
        find.byKey(const ValueKey('canvas-inline-text-editor-$textId')),
        findsOneWidget,
      );
    },
  );

  testWidgets('keeps IME preedit local and emits one final Unicode commit', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    final commits = <({String id, String text, bool compositionObserved})>[];
    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: textId,
          onSelected: (_) {},
          inlineTextEditEnabled: true,
          onInlineTextCommit: (id, text, compositionObserved) {
            commits.add((
              id: id,
              text: text,
              compositionObserved: compositionObserved,
            ));
            return true;
          },
        ),
      ),
    );
    await _doubleTap(
      tester,
      find.byKey(const ValueKey('canvas-widget-$textId')),
    );
    await tester.pump();

    tester.testTextInput.updateEditingValue(
      const TextEditingValue(
        text: 'に',
        selection: TextSelection.collapsed(offset: 1),
        composing: TextRange(start: 0, end: 1),
      ),
    );
    await tester.pump();
    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();
    expect(commits, isEmpty, reason: 'a composing preedit is never an intent');
    expect(find.byType(TextField), findsOneWidget);

    const committedText = '日本語 👩🏽‍💻';
    tester.testTextInput.updateEditingValue(
      const TextEditingValue(
        text: committedText,
        selection: TextSelection.collapsed(offset: committedText.length),
      ),
    );
    await tester.pump();
    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();

    expect(commits, [
      (id: textId, text: committedText, compositionObserved: true),
    ]);
    expect(find.byType(TextField), findsNothing);
  });

  testWidgets('reports direct typing without a fabricated composition', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    final compositions = <bool>[];
    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: textId,
          onSelected: (_) {},
          inlineTextEditEnabled: true,
          onInlineTextCommit: (_, _, compositionObserved) {
            compositions.add(compositionObserved);
            return true;
          },
        ),
      ),
    );
    await tester.tap(find.byKey(const ValueKey('canvas-widget-$textId')));
    await tester.pump(kDoubleTapTimeout);
    await tester.sendKeyEvent(LogicalKeyboardKey.f2);
    await tester.pump();
    await tester.enterText(find.byType(TextField), 'plain typing');
    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();

    expect(compositions, [false]);
  });

  testWidgets('preserves rejected and over-limit inline drafts in the editor', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    var commitCalls = 0;
    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: textId,
          onSelected: (_) {},
          inlineTextEditEnabled: true,
          onInlineTextCommit: (_, _, _) {
            commitCalls++;
            return false;
          },
        ),
      ),
    );
    await _doubleTap(
      tester,
      find.byKey(const ValueKey('canvas-widget-$textId')),
    );
    await tester.pump();
    final editor = find.byType(TextField);
    await tester.enterText(editor, 'unsaved draft');
    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();

    expect(commitCalls, 1);
    expect(editor, findsOneWidget);
    expect(tester.widget<TextField>(editor).controller!.text, 'unsaved draft');
    expect(
      find.text(
        'The host rejected this edit because its Canvas authority changed.',
      ),
      findsOneWidget,
    );

    final overScalarLimit = List<String>.filled(
      maximumInlineTextUnicodeScalars + 1,
      'a',
    ).join();
    tester.widget<TextField>(editor).controller!.text = overScalarLimit;
    await tester.pump();
    await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
    await tester.sendKeyEvent(LogicalKeyboardKey.enter);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
    await tester.pump();

    expect(
      commitCalls,
      1,
      reason: 'invalid local text never becomes an intent',
    );
    expect(editor, findsOneWidget);
    expect(tester.widget<TextField>(editor).controller!.text, overScalarLimit);
    expect(
      find.text(
        'Text exceeds $maximumInlineTextUnicodeScalars Unicode scalar values.',
      ),
      findsOneWidget,
    );
  });

  testWidgets(
    'Escape waits for composition and Delete never deletes the widget',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
      var deletes = 0;
      var commits = 0;
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: textId,
            onSelected: (_) {},
            inlineTextEditEnabled: true,
            onDeleteSelected: () {
              deletes++;
              return true;
            },
            onInlineTextCommit: (_, _, _) {
              commits++;
              return true;
            },
          ),
        ),
      );
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$textId')),
      );
      await tester.pump();
      tester.testTextInput.updateEditingValue(
        const TextEditingValue(
          text: '文',
          selection: TextSelection.collapsed(offset: 1),
          composing: TextRange(start: 0, end: 1),
        ),
      );
      await tester.pump();

      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      await tester.pump();
      expect(find.byType(TextField), findsOneWidget);
      expect(deletes, 0);
      expect(commits, 0);

      tester.testTextInput.updateEditingValue(
        const TextEditingValue(
          text: '文',
          selection: TextSelection.collapsed(offset: 1),
        ),
      );
      await tester.pump();
      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pump();
      expect(find.byType(TextField), findsNothing);
      expect(deletes, 0);
      expect(commits, 0);
    },
  );

  testWidgets('selection, revision, fence, and capability loss cancel drafts', (
    tester,
  ) async {
    var model = CanvasModel.decode(fixture.modelBytesForViewTest());
    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    const rowId = '1035b7df-df9b-442b-9af2-72b4c90f1462';
    var selected = textId;
    var synchronized = true;
    var enabled = true;
    var commits = 0;
    late StateSetter rebuild;
    await tester.pumpWidget(
      MaterialApp(
        home: StatefulBuilder(
          builder: (context, setState) {
            rebuild = setState;
            return CanvasDocumentView(
              model: model,
              selectedWidgetId: selected,
              onSelected: (_) {},
              interactionInputSynchronized: synchronized,
              inlineTextEditEnabled: enabled,
              onInlineTextCommit: (_, _, _) {
                commits++;
                return true;
              },
            );
          },
        ),
      ),
    );

    Future<void> openEditor() async {
      await _doubleTap(
        tester,
        find.byKey(const ValueKey('canvas-widget-$textId')),
      );
      await tester.pump();
      expect(find.byType(TextField), findsOneWidget);
    }

    await openEditor();
    rebuild(() => selected = rowId);
    await tester.pump();
    expect(find.byType(TextField), findsNothing);

    rebuild(() => selected = textId);
    await tester.pump();
    await openEditor();
    rebuild(() => synchronized = false);
    await tester.pump();
    expect(find.byType(TextField), findsNothing);

    rebuild(() => synchronized = true);
    await tester.pump();
    await openEditor();
    final json =
        jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
            as Map<String, Object?>;
    json['logicalRevisionId'] = (json['logicalRevisionId']! as int) + 1;
    rebuild(
      () => model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      ),
    );
    await tester.pump();
    expect(find.byType(TextField), findsNothing);

    await openEditor();
    rebuild(() => enabled = false);
    await tester.pump();
    expect(find.byType(TextField), findsNothing);
    expect(commits, 0);
  });

  testWidgets('renders the exact resolved light and dark project theme seed', (
    tester,
  ) async {
    Future<MaterialApp> pumpTheme({
      required String brightness,
      required String seedArgb,
    }) async {
      final json =
          jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
              as Map<String, Object?>;
      final profile = json['profile']! as Map<String, Object?>;
      final theme = profile['theme']! as Map<String, Object?>;
      theme
        ..['definitionId'] = brightness
        ..['seedArgb'] = seedArgb
        ..['brightness'] = brightness
        ..['digestIdentity'] = brightness == 'light' ? 'A' * 64 : 'B' * 64;
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      return tester.widget<MaterialApp>(find.byType(MaterialApp));
    }

    final light = await pumpTheme(brightness: 'light', seedArgb: '0xFF123456');
    expect(light.themeMode, ThemeMode.light);
    expect(
      light.theme!.colorScheme,
      ColorScheme.fromSeed(
        seedColor: const Color(0xff123456),
        brightness: Brightness.light,
      ),
    );

    final dark = await pumpTheme(brightness: 'dark', seedArgb: '0xFF654321');
    expect(dark.themeMode, ThemeMode.dark);
    expect(
      dark.darkTheme!.colorScheme,
      ColorScheme.fromSeed(
        seedColor: const Color(0xff654321),
        brightness: Brightness.dark,
      ),
    );
  });

  testWidgets(
    'applies project role overrides and keeps local Text leaves last',
    (tester) async {
      final json = _modelJsonForView();
      final theme =
          (json['profile']! as Map<String, Object?>)['theme']!
              as Map<String, Object?>;
      theme
        ..['colorScheme'] = {'primary': '0xFF102030', 'onSurface': '0xFF405060'}
        ..['textTheme'] = {
          'bodyLarge': {
            'color': {'kind': 'colorScheme', 'role': 'onSurface'},
            'fontSize': 13.0,
            'fontWeight': 'w600',
            'decoration': ['underline', 'lineThrough'],
            'decorationColor': {'kind': 'colorScheme', 'role': 'primary'},
          },
        };
      final properties = _nodeProperties(json, 'flutter.widgets.Text');
      properties
        ..['styleThemeTextStyle'] = {
          'kind': 'themeToken',
          'token': 'material.textTheme.bodyLarge',
        }
        ..['styleFontSize'] = {'kind': 'double', 'value': 19.0}
        ..['styleColor'] = {'kind': 'color', 'argb': '0xFFABCDEF'};
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );

      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );

      final app = tester.widget<MaterialApp>(find.byType(MaterialApp));
      expect(app.theme!.colorScheme.primary, const Color(0xff102030));
      final resolvedRole = app.theme!.textTheme.bodyLarge!;
      expect(resolvedRole.color, const Color(0xff405060));
      expect(resolvedRole.fontSize, 13.0);
      expect(resolvedRole.fontWeight, FontWeight.w600);
      expect(
        resolvedRole.decoration,
        TextDecoration.combine(const [
          TextDecoration.underline,
          TextDecoration.lineThrough,
        ]),
      );
      expect(resolvedRole.decorationColor, const Color(0xff102030));

      final local = tester.widget<Text>(find.text('Hello')).style!;
      expect(
        local.fontSize,
        19.0,
        reason: 'local .fd leaf wins over theme role',
      );
      expect(local.color, const Color(0xffabcdef));
      expect(
        local.fontWeight,
        FontWeight.w600,
        reason: 'unoverridden theme leaf inherits',
      );
    },
  );

  testWidgets('applies component colors and keeps local widget colors last', (
    tester,
  ) async {
    final json =
        jsonDecode(
              utf8.decode(
                fixture.elevatedButtonModelBytesForViewTest(
                  properties: {
                    'onPressed': {'kind': 'callbackPresence'},
                    'styleForegroundColor': {
                      'kind': 'color',
                      'argb': '0xFFABCDEF',
                    },
                  },
                ),
              ),
            )
            as Map<String, Object?>;
    final theme =
        (json['profile']! as Map<String, Object?>)['theme']!
            as Map<String, Object?>;
    theme['components'] = {
      'scaffold.backgroundColor': {'kind': 'argb', 'argb': '0xFF102030'},
      'appBar.backgroundColor': {'kind': 'argb', 'argb': '0xFF203040'},
      'appBar.foregroundColor': {'kind': 'colorScheme', 'role': 'onPrimary'},
      'appBar.shadowColor': {'kind': 'argb', 'argb': '0xFF304050'},
      'appBar.surfaceTintColor': {'kind': 'argb', 'argb': '0xFF405060'},
      'icon.color': {'kind': 'argb', 'argb': '0xFF506070'},
      'elevatedButton.backgroundColor.default': {
        'kind': 'argb',
        'argb': '0xFF607080',
      },
      'elevatedButton.backgroundColor.disabled': {
        'kind': 'argb',
        'argb': '0xFF708090',
      },
      'elevatedButton.backgroundColor.pressed': {
        'kind': 'argb',
        'argb': '0xFF8090A0',
      },
      'elevatedButton.foregroundColor.default': {
        'kind': 'argb',
        'argb': '0xFF90A0B0',
      },
      'elevatedButton.overlayColor.pressed': {
        'kind': 'argb',
        'argb': '0x66A0B0C0',
      },
    };
    final model = CanvasModel.decode(
      Uint8List.fromList(utf8.encode(jsonEncode(json))),
    );

    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final app = tester.widget<MaterialApp>(find.byType(MaterialApp));
    final resolvedTheme = app.theme!;
    expect(resolvedTheme.scaffoldBackgroundColor, const Color(0xff102030));
    expect(resolvedTheme.appBarTheme.backgroundColor, const Color(0xff203040));
    expect(
      resolvedTheme.appBarTheme.foregroundColor,
      resolvedTheme.colorScheme.onPrimary,
    );
    expect(resolvedTheme.appBarTheme.shadowColor, const Color(0xff304050));
    expect(resolvedTheme.appBarTheme.surfaceTintColor, const Color(0xff405060));
    expect(resolvedTheme.iconTheme.color, const Color(0xff506070));

    final componentStyle = resolvedTheme.elevatedButtonTheme.style!;
    expect(
      componentStyle.backgroundColor!.resolve(const <WidgetState>{}),
      const Color(0xff607080),
    );
    expect(
      componentStyle.backgroundColor!.resolve(const {
        WidgetState.disabled,
        WidgetState.pressed,
      }),
      const Color(0xff708090),
      reason: 'disabled has priority over pressed',
    );
    expect(
      componentStyle.overlayColor!.resolve(const {
        WidgetState.disabled,
        WidgetState.pressed,
      }),
      isNull,
      reason:
          'an absent disabled override must fall through to framework, '
          'never to the pressed component override',
    );

    expect(
      tester
          .widgetList<Scaffold>(find.byType(Scaffold))
          .map((widget) => widget.backgroundColor),
      contains(const Color(0xffffffff)),
      reason: 'local Scaffold color wins over the component theme',
    );
    expect(
      tester
          .widget<ElevatedButton>(find.byType(ElevatedButton))
          .style!
          .foregroundColor!
          .resolve(const <WidgetState>{}),
      const Color(0xffabcdef),
      reason: 'local ButtonStyle color wins over the component theme',
    );
  });

  testWidgets('renders actual Flutter widgets and reports a stable selection', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    String? selected;

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (id) => selected = id,
        ),
      ),
    );

    expect(find.text('Hello'), findsOneWidget);
    expect(find.text('World'), findsOneWidget);
    expect(find.text('Action'), findsOneWidget);
    expect(find.byType(Column), findsWidgets);
    expect(find.byType(Row), findsOneWidget);
    expect(find.byType(Padding), findsWidgets);
    expect(find.byType(Center), findsWidgets);

    final column = tester
        .widgetList<Column>(find.byType(Column))
        .singleWhere((candidate) => candidate.spacing == 12.5);
    expect(column.mainAxisSize, MainAxisSize.min);
    expect(column.crossAxisAlignment, CrossAxisAlignment.baseline);
    expect(column.textDirection, TextDirection.ltr);
    expect(column.verticalDirection, VerticalDirection.down);
    expect(column.textBaseline, TextBaseline.alphabetic);

    final row = tester.widget<Row>(find.byType(Row));
    expect(row.mainAxisAlignment, MainAxisAlignment.end);
    expect(row.mainAxisSize, MainAxisSize.min);
    expect(row.crossAxisAlignment, CrossAxisAlignment.baseline);
    expect(row.textDirection, TextDirection.rtl);
    expect(row.verticalDirection, VerticalDirection.up);
    expect(row.textBaseline, TextBaseline.ideographic);
    expect(row.spacing, 4.0);

    final hello = tester.widget<Text>(find.text('Hello'));
    expect(hello.textAlign, TextAlign.center);
    expect(hello.textDirection, TextDirection.ltr);
    expect(hello.softWrap, isFalse);
    expect(hello.overflow, TextOverflow.ellipsis);
    expect(hello.maxLines, 2);
    expect(hello.semanticsLabel, 'Primary greeting');
    expect(hello.semanticsIdentifier, 'primary-greeting');
    expect(hello.textWidthBasis, TextWidthBasis.longestLine);
    expect(hello.selectionColor, const Color(0xff336699));

    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    await tester.tap(find.byKey(const ValueKey('canvas-widget-$textId')));
    await tester.pump();

    expect(selected, textId);
  });

  testWidgets(
    'paints every widget outline dashed and transitions selection to solid',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      String? selectedWidgetId;

      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
            ),
          ),
        ),
      );

      for (final widgetId in model.widgetIds) {
        expect(
          find.byKey(ValueKey('canvas-widget-outline-$widgetId')),
          findsOneWidget,
          reason: 'every decoded Canvas node has a designer outline',
        );
      }

      const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
      const secondTextId = '64260967-f830-4e3c-bbd1-f81cb79db092';
      final unselectedTextPainter = _foregroundPainter(
        tester,
        'canvas-widget-outline-$textId',
      );
      final dashedCanvas = _recordPainter(
        unselectedTextPainter,
        const Size(96, 48),
      );
      expect(_drawRectCount(dashedCanvas), 0);
      final topDashes = _horizontalLinesAt(_drawLines(dashedCanvas), y: 0);
      expect(topDashes, hasLength(greaterThanOrEqualTo(2)));
      expect(topDashes.first.start, Offset.zero);
      expect(topDashes.first.length, greaterThan(0));
      expect(
        topDashes[1].start.dx,
        greaterThan(topDashes.first.end.dx),
        reason: 'separate drawLine commands retain a visible dash gap',
      );
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$textId')),
        findsNothing,
      );

      await tester.tap(find.byKey(const ValueKey('canvas-widget-$textId')));
      await tester.pump();

      expect(selectedWidgetId, textId);
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$textId')),
        findsOneWidget,
      );
      final selectedCanvas = _recordPainter(
        _foregroundPainter(tester, 'canvas-widget-outline-$textId'),
        const Size(96, 48),
      );
      expect(_drawRectCount(selectedCanvas), 1);
      expect(_drawLines(selectedCanvas), isEmpty);

      await tester.tap(
        find.byKey(const ValueKey('canvas-widget-$secondTextId')),
      );
      await tester.pump();

      expect(selectedWidgetId, secondTextId);
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$textId')),
        findsNothing,
      );
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$secondTextId')),
        findsOneWidget,
      );
      expect(
        _drawLines(
          _recordPainter(
            _foregroundPainter(tester, 'canvas-widget-outline-$textId'),
            const Size(96, 48),
          ),
        ),
        isNotEmpty,
        reason: 'the previously selected widget returns to a dashed outline',
      );
    },
  );

  testWidgets('keeps outline stroke and dash screen-stable across zoom', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());
    final base = CanvasViewportPresentation.fit(model);

    Future<({double stroke, double dash})> metricsAt(int zoomMicros) async {
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
            viewportPresentation: base.copyWith(
              mode: 'manual',
              zoomMicros: zoomMicros,
            ),
          ),
        ),
      );
      await tester.pump();
      final painter = _foregroundPainter(
        tester,
        'canvas-widget-outline-${model.root.id}',
      );
      final lines = _horizontalLinesAt(
        _drawLines(_recordPainter(painter, const Size(390, 844))),
        y: 0,
      );
      expect(lines, isNotEmpty);
      return (stroke: lines.first.paint.strokeWidth, dash: lines.first.length);
    }

    final half = await metricsAt(500000);
    final doubleZoom = await metricsAt(2000000);

    expect(half.stroke * 0.5, closeTo(1, 0.000001));
    expect(doubleZoom.stroke * 2, closeTo(1, 0.000001));
    expect(half.dash * 0.5, closeTo(4, 0.000001));
    expect(doubleZoom.dash * 2, closeTo(4, 0.000001));
  });

  testWidgets('renders empty required Text data without a synthetic fallback', (
    tester,
  ) async {
    const textId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
    final json = _modelJsonForView();
    final properties = _nodeProperties(json, 'flutter.widgets.Text');
    properties['data'] = {'kind': 'string', 'value': ''};
    final model = CanvasModel.decode(
      Uint8List.fromList(utf8.encode(jsonEncode(json))),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final text = tester.widget<Text>(
      find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$textId')),
            matching: find.byType(Text),
          )
          .first,
    );
    expect(text.data, isEmpty);
  });

  testWidgets('renders every reviewed Scaffold scalar with framework parity', (
    tester,
  ) async {
    const scaffoldId = '6e88bff4-8d73-48aa-92b5-87aa3344f6a7';
    const themedScrim = Color(0xff5a1234);
    final json = _modelJsonForView();
    final root = json['root']! as Map<String, Object?>;
    root['properties'] = <String, Object?>{
      'floatingActionButtonLocation': {
        'kind': 'string',
        'value': 'miniCenterDocked',
      },
      'floatingActionButtonAnimator': {
        'kind': 'string',
        'value': 'noAnimation',
      },
      'persistentFooterAlignment': {'kind': 'string', 'value': 'bottomStart'},
      'onDrawerChanged': {'kind': 'callbackPresence'},
      'onEndDrawerChanged': {'kind': 'callbackPresence'},
      'backgroundColor': {'kind': 'color', 'argb': '0xFF102030'},
      'resizeToAvoidBottomInset': {'kind': 'boolean', 'value': false},
      'primary': {'kind': 'boolean', 'value': false},
      'drawerDragStartBehavior': {
        'kind': 'enum',
        'type': 'DragStartBehavior',
        'value': 'down',
      },
      'extendBody': {'kind': 'boolean', 'value': true},
      'drawerBarrierDismissible': {'kind': 'boolean', 'value': false},
      'extendBodyBehindAppBar': {'kind': 'boolean', 'value': true},
      'drawerScrimColor': {
        'kind': 'themeToken',
        'token': 'material.colorScheme.scrim',
      },
      'drawerEdgeDragWidth': {'kind': 'double', 'value': 24.5},
      'drawerEnableOpenDragGesture': {'kind': 'boolean', 'value': false},
      'endDrawerEnableOpenDragGesture': {'kind': 'boolean', 'value': false},
      'restorationId': {'kind': 'string', 'value': 'home-scaffold'},
    };
    final model = CanvasModel.decode(
      Uint8List.fromList(utf8.encode(jsonEncode(json))),
    );

    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData.from(
          colorScheme: ColorScheme.fromSeed(
            seedColor: const Color(0xff6750a4),
            scrim: themedScrim,
          ),
        ),
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final scaffold = tester.widget<Scaffold>(
      find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$scaffoldId')),
            matching: find.byType(Scaffold),
          )
          .first,
    );
    expect(
      scaffold.floatingActionButtonLocation,
      same(FloatingActionButtonLocation.miniCenterDocked),
    );
    expect(
      scaffold.floatingActionButtonAnimator,
      same(FloatingActionButtonAnimator.noAnimation),
    );
    expect(
      scaffold.persistentFooterAlignment,
      AlignmentDirectional.bottomStart,
    );
    expect(scaffold.onDrawerChanged, isNotNull);
    expect(scaffold.onEndDrawerChanged, isNotNull);
    expect(scaffold.backgroundColor, const Color(0xff102030));
    expect(scaffold.resizeToAvoidBottomInset, isFalse);
    expect(scaffold.primary, isFalse);
    expect(scaffold.drawerDragStartBehavior, DragStartBehavior.down);
    expect(scaffold.extendBody, isTrue);
    expect(scaffold.drawerBarrierDismissible, isFalse);
    expect(scaffold.extendBodyBehindAppBar, isTrue);
    expect(scaffold.drawerScrimColor, themedScrim);
    expect(scaffold.drawerEdgeDragWidth, 24.5);
    expect(scaffold.drawerEnableOpenDragGesture, isFalse);
    expect(scaffold.endDrawerEnableOpenDragGesture, isFalse);
    expect(scaffold.restorationId, 'home-scaffold');
  });

  testWidgets('renders exact Padding sides and nullable Center factors', (
    tester,
  ) async {
    const paddingId = '0f78bed5-2fba-42cd-914e-a3abbd2c48c3';
    const centerId = '733ef462-41d7-4849-b780-5d9520666ae4';

    Future<void> pump(Map<String, Object?> json) async {
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        ),
      );
    }

    Padding renderedPadding() => tester.widget<Padding>(
      find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$paddingId')),
            matching: find.byType(Padding),
          )
          .first,
    );
    Center renderedCenter() => tester.widget<Center>(
      find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$centerId')),
            matching: find.byType(Center),
          )
          .first,
    );

    final explicit = _modelJsonForView();
    final paddingProperties = _nodeProperties(
      explicit,
      'flutter.widgets.Padding',
    );
    paddingProperties['padding'] = {
      'kind': 'edgeInsets',
      'left': 1,
      'top': 2.5,
      'right': 3,
      'bottom': 4.25,
    };
    final centerProperties = _nodeProperties(
      explicit,
      'flutter.widgets.Center',
    );
    centerProperties
      ..['widthFactor'] = {'kind': 'integer', 'value': 0}
      ..['heightFactor'] = {'kind': 'double', 'value': 2.5};

    await pump(explicit);

    expect(
      renderedPadding().padding,
      const EdgeInsets.fromLTRB(1, 2.5, 3, 4.25),
    );
    expect(renderedCenter().widthFactor, 0.0);
    expect(renderedCenter().heightFactor, 2.5);

    final omitted = _modelJsonForView();
    _nodeProperties(omitted, 'flutter.widgets.Center')
      ..remove('widthFactor')
      ..remove('heightFactor');

    await pump(omitted);

    expect(renderedCenter().widthFactor, isNull);
    expect(renderedCenter().heightFactor, isNull);
  });

  testWidgets(
    'renders real Align defaults, factors, and extrapolated physical layout',
    (tester) async {
      const alignId = 'b51246d4-4e44-4d7c-91bc-a0892df4a341';
      final fixedChild = <String, Object?>{
        'id': '2985a65c-e7ef-46b7-b942-93f72ab18930',
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{
          'width': {'kind': 'integer', 'value': 40},
          'height': {'kind': 'integer', 'value': 20},
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      };

      Finder alignFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$alignId')),
            matching: find.byType(Align),
          )
          .first;

      Future<RenderPositionedBox> pump(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredAlign(
                  properties: properties,
                  child: fixedChild,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderPositionedBox>(alignFinder());
      }

      final omitted = await pump(const {});
      final omittedWidget = tester.widget<Align>(alignFinder());
      expect(omittedWidget.alignment, Alignment.center);
      expect(omittedWidget.widthFactor, isNull);
      expect(omittedWidget.heightFactor, isNull);
      expect(omitted.child!.size, const Size(40, 20));
      expect(omitted.size.width, greaterThan(omitted.child!.size.width));
      expect(omitted.size.height, greaterThan(omitted.child!.size.height));
      expect(
        (omitted.child!.parentData! as BoxParentData).offset,
        Offset(
          (omitted.size.width - omitted.child!.size.width) / 2,
          (omitted.size.height - omitted.child!.size.height) / 2,
        ),
      );

      final extrapolated = await pump({
        'alignment': _viewAlignment(horizontal: 2, vertical: -2),
        'widthFactor': {'kind': 'integer', 'value': 2},
        'heightFactor': {'kind': 'double', 'value': 3.0},
      });
      final explicitWidget = tester.widget<Align>(alignFinder());
      expect(explicitWidget.alignment, const Alignment(2, -2));
      expect(explicitWidget.widthFactor, 2);
      expect(explicitWidget.heightFactor, 3);
      expect(extrapolated.child!.size, const Size(40, 20));
      expect(extrapolated.size, const Size(80, 60));
      expect(
        (extrapolated.child!.parentData! as BoxParentData).offset,
        const Offset(60, -20),
      );
      expect(find.byType(AnimatedAlign), findsNothing);
    },
  );

  testWidgets('resolves Align directional start in LTR and RTL', (
    tester,
  ) async {
    const alignId = 'b51246d4-4e44-4d7c-91bc-a0892df4a341';
    final child = <String, Object?>{
      'id': '2985a65c-e7ef-46b7-b942-93f72ab18930',
      'type': 'flutter.widgets.SizedBox',
      'properties': <String, Object?>{
        'width': {'kind': 'integer', 'value': 40},
        'height': {'kind': 'integer', 'value': 20},
      },
      'slots': <String, Object?>{
        'child': <String, Object?>{'kind': 'single', 'child': null},
      },
    };

    Future<({Offset offset, double remainingWidth, TextDirection direction})>
    render(String locale) async {
      final json = _modelWithCenteredAlign(
        properties: {
          'alignment': _viewAlignment(
            basis: 'directional',
            horizontal: -1,
            vertical: 0,
          ),
        },
        child: child,
      );
      (json['profile']! as Map<String, Object?>)['locale'] = locale;
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      final node = find.byKey(const ValueKey('canvas-widget-$alignId'));
      final align = tester.widget<Align>(
        find.descendant(of: node, matching: find.byType(Align)).first,
      );
      expect(align.alignment, const AlignmentDirectional(-1, 0));
      final render = tester.renderObject<RenderPositionedBox>(
        find.descendant(of: node, matching: find.byType(Align)).first,
      );
      return (
        offset: (render.child!.parentData! as BoxParentData).offset,
        remainingWidth: render.size.width - render.child!.size.width,
        direction: render.textDirection!,
      );
    }

    final ltr = await render('en-US');
    expect(ltr.direction, TextDirection.ltr);
    expect(ltr.offset.dx, 0);
    expect(ltr.offset.dy, greaterThan(0));

    final rtl = await render('ar-SA');
    expect(rtl.direction, TextDirection.rtl);
    expect(rtl.remainingWidth, greaterThan(0));
    expect(rtl.offset.dx, rtl.remainingWidth);
    expect(rtl.offset.dy, greaterThan(0));
  });

  testWidgets(
    'keeps empty and factor-zero Align selectable and exposes empty child DnD',
    (tester) async {
      const alignId = 'b51246d4-4e44-4d7c-91bc-a0892df4a341';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;

      final emptyModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredAlign(
                properties: const {
                  'widthFactor': {'kind': 'integer', 'value': 1},
                  'heightFactor': {'kind': 'integer', 'value': 1},
                },
                child: null,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: emptyModel,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$alignId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$alignId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, alignId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, alignId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);

      final occupiedChild = <String, Object?>{
        'id': '2985a65c-e7ef-46b7-b942-93f72ab18930',
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{
          'width': {'kind': 'integer', 'value': 40},
          'height': {'kind': 'integer', 'value': 20},
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      };
      final factorZeroModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredAlign(
                properties: const {
                  'widthFactor': {'kind': 'integer', 'value': 0},
                  'heightFactor': {'kind': 'integer', 'value': 1},
                },
                child: occupiedChild,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: factorZeroModel,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      expect(tester.getSize(rendered), const Size(0, 20));
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      final align = tester.widget<Align>(
        find.descendant(of: rendered, matching: find.byType(Align)).first,
      );
      expect(align.child, isNotNull);
      expect(align.widthFactor, 0);
      expect(align.heightFactor, 1);
    },
  );

  testWidgets(
    'renders real FractionallySizedBox constraints, overflow, and alignment',
    (tester) async {
      const widgetId = '403df8b2-b244-4e7c-9ff1-29ba070206b6';
      final fixedChild = <String, Object?>{
        'id': '941b5560-c22e-4c5d-8180-e2985e4123a7',
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{
          'width': {'kind': 'integer', 'value': 40},
          'height': {'kind': 'integer', 'value': 20},
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      };

      Finder widgetFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(FractionallySizedBox),
          )
          .first;

      Future<RenderFractionallySizedOverflowBox> pump(
        Map<String, Object?> properties,
      ) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredFractionallySizedBox(
                  properties: properties,
                  child: fixedChild,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderFractionallySizedOverflowBox>(
          widgetFinder(),
        );
      }

      final omitted = await pump(const {});
      final omittedWidget = tester.widget<FractionallySizedBox>(widgetFinder());
      expect(omittedWidget.alignment, Alignment.center);
      expect(omittedWidget.widthFactor, isNull);
      expect(omittedWidget.heightFactor, isNull);
      expect(omitted.size, const Size(200, 100));
      expect(omitted.child!.size, const Size(200, 100));
      expect((omitted.child!.parentData! as BoxParentData).offset, Offset.zero);

      final fractional = await pump({
        'widthFactor': {'kind': 'double', 'value': 0.5},
        'heightFactor': {'kind': 'double', 'value': 0.25},
      });
      expect(fractional.size, const Size(200, 100));
      expect(fractional.child!.size, const Size(100, 25));
      expect(
        (fractional.child!.parentData! as BoxParentData).offset,
        const Offset(50, 37.5),
      );

      final overflow = await pump({
        'widthFactor': {'kind': 'double', 'value': 1.5},
        'heightFactor': {'kind': 'integer', 'value': 2},
      });
      expect(overflow.size, const Size(200, 100));
      expect(overflow.child!.size, const Size(300, 200));
      expect(
        (overflow.child!.parentData! as BoxParentData).offset,
        const Offset(-50, -50),
      );

      final extrapolated = await pump({
        'alignment': _viewAlignment(horizontal: 2, vertical: -2),
        'widthFactor': {'kind': 'double', 'value': 0.5},
        'heightFactor': {'kind': 'double', 'value': 0.25},
      });
      final explicitWidget = tester.widget<FractionallySizedBox>(
        widgetFinder(),
      );
      expect(explicitWidget.alignment, const Alignment(2, -2));
      expect(explicitWidget.widthFactor, 0.5);
      expect(explicitWidget.heightFactor, 0.25);
      expect(
        (extrapolated.child!.parentData! as BoxParentData).offset,
        const Offset(150, -37.5),
      );
      expect(find.byType(Align), findsNothing);
    },
  );

  testWidgets(
    'resolves FractionallySizedBox directional start in LTR and RTL',
    (tester) async {
      const widgetId = '403df8b2-b244-4e7c-9ff1-29ba070206b6';
      final child = <String, Object?>{
        'id': '941b5560-c22e-4c5d-8180-e2985e4123a7',
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{},
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      };

      Future<({Offset offset, double remainingWidth, TextDirection direction})>
      render(String locale) async {
        final json = _modelWithCenteredFractionallySizedBox(
          properties: {
            'alignment': _viewAlignment(
              basis: 'directional',
              horizontal: -1,
              vertical: 0,
            ),
            'widthFactor': {'kind': 'double', 'value': 0.5},
            'heightFactor': {'kind': 'double', 'value': 0.5},
          },
          child: child,
        );
        (json['profile']! as Map<String, Object?>)['locale'] = locale;
        final model = CanvasModel.decode(
          Uint8List.fromList(utf8.encode(jsonEncode(json))),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final node = find.byKey(const ValueKey('canvas-widget-$widgetId'));
        final finder = find
            .descendant(of: node, matching: find.byType(FractionallySizedBox))
            .first;
        final widget = tester.widget<FractionallySizedBox>(finder);
        expect(widget.alignment, const AlignmentDirectional(-1, 0));
        final render = tester.renderObject<RenderFractionallySizedOverflowBox>(
          finder,
        );
        return (
          offset: (render.child!.parentData! as BoxParentData).offset,
          remainingWidth: render.size.width - render.child!.size.width,
          direction: render.textDirection!,
        );
      }

      final ltr = await render('en-US');
      expect(ltr.direction, TextDirection.ltr);
      expect(ltr.offset.dx, 0);
      expect(ltr.offset.dy, 25);

      final rtl = await render('ar-SA');
      expect(rtl.direction, TextDirection.rtl);
      expect(rtl.remainingWidth, 100);
      expect(rtl.offset.dx, rtl.remainingWidth);
      expect(rtl.offset.dy, 25);
    },
  );

  testWidgets(
    'keeps zero-size FractionallySizedBox selectable and exposes child DnD',
    (tester) async {
      const widgetId = '403df8b2-b244-4e7c-9ff1-29ba070206b6';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredFractionallySizedBox(
                properties: const {},
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
    },
  );

  testWidgets(
    'renders real FittedBox defaults and every reviewed fit and clip value',
    (tester) async {
      const widgetId = 'b587a092-9a65-420a-9d1c-e127cc752f8d';
      final child = <String, Object?>{
        'id': 'feea5045-2de2-4d34-b08d-79e5ee710f7e',
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{
          'width': {'kind': 'integer', 'value': 40},
          'height': {'kind': 'integer', 'value': 20},
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      };

      Finder fittedFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(FittedBox),
          )
          .first;

      Future<RenderFittedBox> pump(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredFittedBox(
                  properties: properties,
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderFittedBox>(fittedFinder());
      }

      final omitted = await pump(const {});
      final omittedWidget = tester.widget<FittedBox>(fittedFinder());
      expect(omittedWidget.fit, BoxFit.contain);
      expect(omittedWidget.alignment, Alignment.center);
      expect(omittedWidget.clipBehavior, Clip.none);
      expect(omitted.size, const Size(200, 100));
      expect(omitted.child!.size, const Size(40, 20));

      const fits = <String, BoxFit>{
        'fill': BoxFit.fill,
        'contain': BoxFit.contain,
        'cover': BoxFit.cover,
        'fitWidth': BoxFit.fitWidth,
        'fitHeight': BoxFit.fitHeight,
        'none': BoxFit.none,
        'scaleDown': BoxFit.scaleDown,
      };
      for (final entry in fits.entries) {
        final render = await pump({
          'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': entry.key},
        });
        expect(tester.widget<FittedBox>(fittedFinder()).fit, entry.value);
        expect(render.child!.size, const Size(40, 20), reason: entry.key);
      }

      const clips = <String, Clip>{
        'none': Clip.none,
        'hardEdge': Clip.hardEdge,
        'antiAlias': Clip.antiAlias,
        'antiAliasWithSaveLayer': Clip.antiAliasWithSaveLayer,
      };
      for (final entry in clips.entries) {
        await pump({
          'alignment': _viewAlignment(horizontal: 2, vertical: -2),
          'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': entry.key},
        });
        final widget = tester.widget<FittedBox>(fittedFinder());
        expect(widget.alignment, const Alignment(2, -2));
        expect(widget.clipBehavior, entry.value);
      }
    },
  );

  testWidgets(
    'resolves FittedBox directional alignment and applies overflow clipping',
    (tester) async {
      const widgetId = 'b587a092-9a65-420a-9d1c-e127cc752f8d';
      const childId = 'feea5045-2de2-4d34-b08d-79e5ee710f7e';

      Future<({Offset offset, TextDirection direction})> directional(
        String locale,
      ) async {
        final json = _modelWithCenteredFittedBox(
          properties: {
            'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'none'},
            'alignment': _viewAlignment(
              basis: 'directional',
              horizontal: -1,
              vertical: 0,
            ),
          },
          child: <String, Object?>{
            'id': childId,
            'type': 'flutter.widgets.SizedBox',
            'properties': <String, Object?>{
              'width': {'kind': 'integer', 'value': 40},
              'height': {'kind': 'integer', 'value': 20},
            },
            'slots': <String, Object?>{},
          },
        );
        (json['profile']! as Map<String, Object?>)['locale'] = locale;
        final model = CanvasModel.decode(
          Uint8List.fromList(utf8.encode(jsonEncode(json))),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final node = find.byKey(const ValueKey('canvas-widget-$widgetId'));
        final finder = find
            .descendant(of: node, matching: find.byType(FittedBox))
            .first;
        final render = tester.renderObject<RenderFittedBox>(finder);
        final childRect = MatrixUtils.transformRect(
          render.child!.getTransformTo(render),
          Offset.zero & render.child!.size,
        );
        return (offset: childRect.topLeft, direction: render.textDirection!);
      }

      final ltr = await directional('en-US');
      expect(ltr.direction, TextDirection.ltr);
      expect(ltr.offset, const Offset(0, 40));
      final rtl = await directional('ar-SA');
      expect(rtl.direction, TextDirection.rtl);
      expect(rtl.offset, const Offset(160, 40));

      Future<({Clip clip, Rect childRect, Size size})> overflow(
        String clip,
      ) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredFittedBox(
                  properties: {
                    'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'none'},
                    'clipBehavior': {
                      'kind': 'enum',
                      'type': 'Clip',
                      'value': clip,
                    },
                  },
                  child: <String, Object?>{
                    'id': childId,
                    'type': 'flutter.widgets.SizedBox',
                    'properties': <String, Object?>{
                      'width': {'kind': 'integer', 'value': 300},
                      'height': {'kind': 'integer', 'value': 200},
                    },
                    'slots': <String, Object?>{},
                  },
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final render = tester.renderObject<RenderFittedBox>(
          find
              .descendant(
                of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
                matching: find.byType(FittedBox),
              )
              .first,
        );
        return (
          clip: render.clipBehavior,
          childRect: MatrixUtils.transformRect(
            render.child!.getTransformTo(render),
            Offset.zero & render.child!.size,
          ),
          size: render.size,
        );
      }

      final unclipped = await overflow('none');
      expect(unclipped.clip, Clip.none);
      expect(unclipped.size, const Size(200, 100));
      expect(unclipped.childRect, const Rect.fromLTWH(-50, -50, 300, 200));
      final clipped = await overflow('hardEdge');
      expect(clipped.clip, Clip.hardEdge);
      expect(clipped.size, const Size(200, 100));
      expect(clipped.childRect, const Rect.fromLTWH(-50, -50, 300, 200));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps an empty zero-size FittedBox selectable and exposes child DnD',
    (tester) async {
      const widgetId = 'b587a092-9a65-420a-9d1c-e127cc752f8d';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredFittedBox(
                properties: const {},
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
    },
  );

  testWidgets(
    'renders real ClipOval defaults and exact paint, hit, semantics, and clip behavior on native and exact-Web profiles',
    (tester) async {
      const clipOvalId = '91684e03-f63a-4cde-b899-7a50c888f9e7';
      const childId = '9f5350ee-598c-4664-9cdd-11e344d0f4e0';
      final semantics = tester.ensureSemantics();

      Finder clipOvalFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$clipOvalId')),
            matching: find.byType(ClipOval),
          )
          .first;

      Future<({ClipOval widget, RenderClipOval render})> pump({
        required Map<String, Object?> properties,
        required Map<String, Object?> child,
        String targetPlatform = 'windows',
      }) async {
        final json = _modelWithCenteredClipOval(
          properties: properties,
          child: child,
        );
        (json['profile']! as Map<String, Object?>)['targetPlatform'] =
            targetPlatform;
        final model = CanvasModel.decode(
          Uint8List.fromList(utf8.encode(jsonEncode(json))),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: clipOvalId,
            onSelected: (_) {},
            dropHoverTarget: const CanvasDropTarget(
              parentWidgetId: clipOvalId,
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
        final finder = clipOvalFinder();
        return (
          widget: tester.widget<ClipOval>(finder),
          render: tester.renderObject<RenderClipOval>(finder),
        );
      }

      final visibleChild = _viewSizedBoxNode(childId, width: 80, height: 60);
      final omitted = await pump(properties: const {}, child: visibleChild);
      expect(omitted.widget.clipper, isNull);
      expect(omitted.widget.clipBehavior, Clip.antiAlias);
      expect(omitted.render.clipper, isNull);
      expect(omitted.render.clipBehavior, Clip.antiAlias);
      expect(omitted.render.size, const Size(80, 60));
      expect(
        omitted.render.describeApproximatePaintClip(omitted.render.child!),
        Offset.zero & omitted.render.size,
      );
      final expectedOval = Path()..addOval(Offset.zero & omitted.render.size);
      expect(
        omitted.render,
        paints..clipPath(
          pathMatcher: coversSameAreaAs(
            expectedOval,
            areaToCompare: const Rect.fromLTRB(-1, -1, 81, 61),
          ),
        ),
      );
      expect(
        omitted.render.hitTest(
          BoxHitTestResult(),
          position: omitted.render.size.center(Offset.zero),
        ),
        isTrue,
      );
      expect(
        omitted.render.hitTest(
          BoxHitTestResult(),
          position: const Offset(1, 1),
        ),
        isFalse,
        reason: 'the real RenderClipOval owns elliptical hit testing',
      );
      expect(
        find.bySemanticsLabel(RegExp('SizedBox ${RegExp.escape(childId)}')),
        findsOneWidget,
      );

      final webOmitted = await pump(
        properties: const {},
        child: visibleChild,
        targetPlatform: 'web',
      );
      expect(webOmitted.widget.clipBehavior, Clip.antiAlias);
      expect(webOmitted.render.clipBehavior, Clip.antiAlias);
      expect(webOmitted.render.size, const Size(80, 60));

      final selectionOutline = find.byKey(
        const ValueKey('canvas-selection-outline-$clipOvalId'),
      );
      final dropOverlay = find.byKey(
        const ValueKey('canvas-widget-insert-drop-zone'),
      );
      expect(selectionOutline, findsOneWidget);
      expect(dropOverlay, findsOneWidget);
      expect(
        find.ancestor(of: clipOvalFinder(), matching: selectionOutline),
        findsOneWidget,
        reason: 'the Designer selection wrapper must remain outside ClipOval',
      );
      expect(
        find.ancestor(of: selectionOutline, matching: find.byType(ClipOval)),
        findsNothing,
      );
      expect(
        find.ancestor(of: dropOverlay, matching: find.byType(ClipOval)),
        findsNothing,
        reason: 'the surface DnD overlay must not be clipped by ClipOval',
      );

      const clips = <String, Clip>{
        'none': Clip.none,
        'hardEdge': Clip.hardEdge,
        'antiAlias': Clip.antiAlias,
        'antiAliasWithSaveLayer': Clip.antiAliasWithSaveLayer,
      };
      for (final entry in clips.entries) {
        final result = await pump(
          properties: {
            'clipBehavior': {
              'kind': 'enum',
              'type': 'Clip',
              'value': entry.key,
            },
          },
          child: visibleChild,
        );
        expect(result.widget.clipper, isNull, reason: entry.key);
        expect(result.widget.clipBehavior, entry.value, reason: entry.key);
        expect(result.render.clipper, isNull, reason: entry.key);
        expect(result.render.clipBehavior, entry.value, reason: entry.key);
        expect(
          result.render.describeApproximatePaintClip(result.render.child!),
          entry.value == Clip.none ? isNull : Offset.zero & result.render.size,
          reason: entry.key,
        );
        if (entry.value == Clip.none) {
          expect(result.render, isNot(paints..clipPath()), reason: entry.key);
        } else {
          final oval = Path()..addOval(Offset.zero & result.render.size);
          expect(
            result.render,
            paints..clipPath(
              pathMatcher: coversSameAreaAs(
                oval,
                areaToCompare: const Rect.fromLTRB(-1, -1, 81, 61),
              ),
            ),
            reason: entry.key,
          );
        }
        expect(
          result.render.hitTest(
            BoxHitTestResult(),
            position: const Offset(1, 1),
          ),
          isFalse,
          reason: '${entry.key} retains ClipOval hit testing',
        );
        expect(
          find.bySemanticsLabel(RegExp('SizedBox ${RegExp.escape(childId)}')),
          findsOneWidget,
          reason: entry.key,
        );
      }
      expect(tester.takeException(), isNull);
      semantics.dispose();
    },
  );

  testWidgets(
    'keeps an empty zero-size ClipOval selectable and exposes child DnD',
    (tester) async {
      const clipOvalId = '91684e03-f63a-4cde-b899-7a50c888f9e7';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredClipOval(
                properties: const {},
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$clipOvalId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$clipOvalId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size.square(36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, clipOvalId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, clipOvalId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real ClipRRect defaults, radii, clips, hit tests, semantics, and external overlays',
    (tester) async {
      const clipRRectId = '5582922d-9044-4e78-bba8-bb740884a49d';
      const childId = '68d29542-ab18-41fb-a509-8db0451aed8e';
      final semantics = tester.ensureSemantics();

      Finder clipRRectFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$clipRRectId')),
            matching: find.byType(ClipRRect),
          )
          .first;

      Future<({ClipRRect widget, RenderClipRRect render})> pump({
        required Map<String, Object?> properties,
        String textDirection = 'ltr',
        String targetPlatform = 'windows',
      }) async {
        final json = _modelWithCenteredClipRRect(
          properties: properties,
          child: _viewSizedBoxNode(childId, width: 80, height: 60),
          textDirection: textDirection,
        );
        (json['profile']! as Map<String, Object?>)['targetPlatform'] =
            targetPlatform;
        final model = CanvasModel.decode(
          Uint8List.fromList(utf8.encode(jsonEncode(json))),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: clipRRectId,
            onSelected: (_) {},
            dropHoverTarget: const CanvasDropTarget(
              parentWidgetId: clipRRectId,
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
        final finder = clipRRectFinder();
        return (
          widget: tester.widget<ClipRRect>(finder),
          render: tester.renderObject<RenderClipRRect>(finder),
        );
      }

      final omitted = await pump(properties: const {});
      expect(omitted.widget.borderRadius, BorderRadius.zero);
      expect(omitted.widget.clipper, isNull);
      expect(omitted.widget.clipBehavior, Clip.antiAlias);
      expect(omitted.render.borderRadius, BorderRadius.zero);
      expect(omitted.render.clipper, isNull);
      expect(omitted.render.clipBehavior, Clip.antiAlias);
      expect(omitted.render.textDirection, TextDirection.ltr);
      expect(omitted.render.size, const Size(80, 60));
      expect(
        omitted.render.describeApproximatePaintClip(omitted.render.child!),
        Offset.zero & omitted.render.size,
      );
      expect(
        omitted.render,
        paints..clipRRect(
          rrect: BorderRadius.zero.toRRect(Offset.zero & omitted.render.size),
        ),
      );
      expect(
        omitted.render.hitTest(
          BoxHitTestResult(),
          position: omitted.render.size.center(Offset.zero),
        ),
        isTrue,
      );
      expect(
        omitted.render.hitTest(
          BoxHitTestResult(),
          position: Offset(omitted.render.size.width + 1, 30),
        ),
        isFalse,
      );
      expect(
        find.bySemanticsLabel(RegExp('SizedBox ${RegExp.escape(childId)}')),
        findsOneWidget,
      );

      final webOmitted = await pump(
        properties: const {},
        targetPlatform: 'web',
      );
      expect(webOmitted.widget.borderRadius, BorderRadius.zero);
      expect(webOmitted.widget.clipBehavior, Clip.antiAlias);
      expect(webOmitted.render.borderRadius, BorderRadius.zero);
      expect(webOmitted.render.clipBehavior, Clip.antiAlias);

      const physicalProperties = <String, Object?>{
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {
            'kind': 'physical',
            'topLeft': {'x': 20.0, 'y': 18.0},
            'topRight': {'x': 12.0, 'y': 10.0},
            'bottomRight': {'x': 8.0, 'y': 6.0},
            'bottomLeft': {'x': 4.0, 'y': 2.0},
          },
        },
      };
      const expectedPhysical = BorderRadius.only(
        topLeft: Radius.elliptical(20, 18),
        topRight: Radius.elliptical(12, 10),
        bottomRight: Radius.elliptical(8, 6),
        bottomLeft: Radius.elliptical(4, 2),
      );
      final physical = await pump(properties: physicalProperties);
      expect(physical.widget.borderRadius, expectedPhysical);
      expect(physical.render.borderRadius, expectedPhysical);
      expect(
        physical.render,
        paints..clipRRect(
          rrect: expectedPhysical.toRRect(Offset.zero & physical.render.size),
        ),
      );
      expect(
        physical.render.hitTest(
          BoxHitTestResult(),
          position: const Offset(1, 1),
        ),
        isTrue,
        reason:
            'Flutter ClipRRect uses box hit testing without a custom clipper',
      );

      const directionalProperties = <String, Object?>{
        'borderRadius': {
          'kind': 'borderRadius',
          'geometry': {
            'kind': 'directional',
            'topStart': {'x': 20.0, 'y': 18.0},
            'topEnd': {'x': 12.0, 'y': 10.0},
            'bottomEnd': {'x': 8.0, 'y': 6.0},
            'bottomStart': {'x': 4.0, 'y': 2.0},
          },
        },
      };
      final directionalLtr = await pump(properties: directionalProperties);
      expect(directionalLtr.widget.borderRadius, expectedPhysical);
      expect(directionalLtr.render.borderRadius, expectedPhysical);
      expect(directionalLtr.render.textDirection, TextDirection.ltr);
      expect(
        directionalLtr.render,
        paints..clipRRect(
          rrect: expectedPhysical.toRRect(
            Offset.zero & directionalLtr.render.size,
          ),
        ),
      );

      const expectedRtl = BorderRadius.only(
        topLeft: Radius.elliptical(12, 10),
        topRight: Radius.elliptical(20, 18),
        bottomRight: Radius.elliptical(4, 2),
        bottomLeft: Radius.elliptical(8, 6),
      );
      final directionalRtl = await pump(
        properties: directionalProperties,
        textDirection: 'rtl',
      );
      expect(directionalRtl.widget.borderRadius, expectedRtl);
      expect(directionalRtl.render.borderRadius, expectedRtl);
      expect(directionalRtl.render.textDirection, TextDirection.rtl);
      expect(
        directionalRtl.render,
        paints..clipRRect(
          rrect: expectedRtl.toRRect(Offset.zero & directionalRtl.render.size),
        ),
      );

      final selectionOutline = find.byKey(
        const ValueKey('canvas-selection-outline-$clipRRectId'),
      );
      final dropOverlay = find.byKey(
        const ValueKey('canvas-widget-insert-drop-zone'),
      );
      expect(selectionOutline, findsOneWidget);
      expect(dropOverlay, findsOneWidget);
      expect(
        find.ancestor(of: clipRRectFinder(), matching: selectionOutline),
        findsOneWidget,
        reason: 'the Designer selection wrapper must remain outside ClipRRect',
      );
      expect(
        find.ancestor(of: selectionOutline, matching: find.byType(ClipRRect)),
        findsNothing,
      );
      expect(
        find.ancestor(of: dropOverlay, matching: find.byType(ClipRRect)),
        findsNothing,
        reason: 'the surface DnD overlay must not be clipped by ClipRRect',
      );

      const clips = <String, Clip>{
        'none': Clip.none,
        'hardEdge': Clip.hardEdge,
        'antiAlias': Clip.antiAlias,
        'antiAliasWithSaveLayer': Clip.antiAliasWithSaveLayer,
      };
      for (final entry in clips.entries) {
        final result = await pump(
          properties: <String, Object?>{
            ...physicalProperties,
            'clipBehavior': {
              'kind': 'enum',
              'type': 'Clip',
              'value': entry.key,
            },
          },
        );
        expect(result.widget.clipper, isNull, reason: entry.key);
        expect(result.widget.borderRadius, expectedPhysical, reason: entry.key);
        expect(result.widget.clipBehavior, entry.value, reason: entry.key);
        expect(result.render.clipper, isNull, reason: entry.key);
        expect(result.render.borderRadius, expectedPhysical, reason: entry.key);
        expect(result.render.clipBehavior, entry.value, reason: entry.key);
        expect(
          result.render.describeApproximatePaintClip(result.render.child!),
          entry.value == Clip.none ? isNull : Offset.zero & result.render.size,
          reason: entry.key,
        );
        if (entry.value == Clip.none) {
          expect(result.render, isNot(paints..clipRRect()), reason: entry.key);
        } else {
          expect(
            result.render,
            paints..clipRRect(
              rrect: expectedPhysical.toRRect(Offset.zero & result.render.size),
            ),
            reason: entry.key,
          );
        }
        expect(
          result.render.hitTest(
            BoxHitTestResult(),
            position: const Offset(1, 1),
          ),
          isTrue,
          reason: '${entry.key} keeps Flutter default box hit testing',
        );
        expect(
          find.bySemanticsLabel(RegExp('SizedBox ${RegExp.escape(childId)}')),
          findsOneWidget,
          reason: entry.key,
        );
      }
      expect(tester.takeException(), isNull);
      semantics.dispose();
    },
  );

  testWidgets(
    'shows an explicit fail-closed preview for project rectangular clippers',
    (tester) async {
      final semantics = tester.ensureSemantics();
      const childId = '63d6e621-f581-4404-91c1-f7a74bdb7a0d';
      const cases = <({String name, String id, Type renderedType})>[
        (
          name: 'ClipRect',
          id: '4fcae2d8-2c48-4c9d-b3f0-62bf0d89ef67',
          renderedType: ClipRect,
        ),
        (
          name: 'ClipOval',
          id: '91684e03-f63a-4cde-b899-7a50c888f9e7',
          renderedType: ClipOval,
        ),
      ];

      for (final entry in cases) {
        for (final targetPlatform in const ['windows', 'web']) {
          final properties = <String, Object?>{
            'clipper': <String, Object?>{'kind': 'dartObjectReferencePresence'},
            'clipBehavior': <String, Object?>{
              'kind': 'enum',
              'type': 'Clip',
              'value': 'none',
            },
          };
          final child = _viewSizedBoxNode(childId, width: 80, height: 60);
          final json = entry.name == 'ClipRect'
              ? _modelWithCenteredClipRect(properties: properties, child: child)
              : _modelWithCenteredClipOval(
                  properties: properties,
                  child: child,
                );
          (json['profile']! as Map<String, Object?>)['targetPlatform'] =
              targetPlatform;
          final model = CanvasModel.decode(
            Uint8List.fromList(utf8.encode(jsonEncode(json))),
          );
          await tester.pumpWidget(
            CanvasModelApp(
              model: model,
              selectedWidgetId: entry.id,
              onSelected: (_) {},
            ),
          );
          await tester.pump();

          final widget = find.byKey(ValueKey('canvas-widget-${entry.id}'));
          final warning = find.byKey(
            ValueKey('canvas-custom-clipper-preview-${entry.id}'),
          );
          final message =
              'Custom ${entry.name} preview unavailable. Generated Dart uses '
              'the configured CustomClipper<Rect>; isolated Canvas does not '
              'execute project or dependency Dart.';
          expect(tester.getSize(widget), const Size(80, 60));
          expect(warning, findsOneWidget, reason: '$entry $targetPlatform');
          expect(
            find.bySemanticsLabel(message),
            findsOneWidget,
            reason: '$entry $targetPlatform',
          );
          expect(
            find.descendant(
              of: widget,
              matching: find.byType(entry.renderedType),
            ),
            findsNothing,
            reason:
                'Canvas must not pretend to execute the project clipper '
                'for ${entry.name} on $targetPlatform',
          );
          expect(
            find.bySemanticsLabel(RegExp('SizedBox ${RegExp.escape(childId)}')),
            findsOneWidget,
            reason: '$entry $targetPlatform',
          );
          expect(
            find.byKey(ValueKey('canvas-selection-outline-${entry.id}')),
            findsOneWidget,
            reason: '$entry $targetPlatform',
          );
          expect(
            tester.takeException(),
            isNull,
            reason: '$entry $targetPlatform',
          );
        }
      }
      semantics.dispose();
    },
  );

  testWidgets(
    'shows an explicit fail-closed preview for a project CustomClipper<RRect>',
    (tester) async {
      const clipRRectId = '5582922d-9044-4e78-bba8-bb740884a49d';
      const childId = '68d29542-ab18-41fb-a509-8db0451aed8e';
      const message =
          'Custom ClipRRect preview unavailable. Generated Dart uses the '
          'configured CustomClipper<RRect>; isolated Canvas does not execute '
          'project or dependency Dart.';
      final semantics = tester.ensureSemantics();
      final selections = <String>[];

      for (final targetPlatform in const ['windows', 'web']) {
        final json = _modelWithCenteredClipRRect(
          properties: const {
            'borderRadius': {
              'kind': 'borderRadius',
              'geometry': {
                'kind': 'physical',
                'topLeft': {'x': 24.0, 'y': 24.0},
                'topRight': {'x': 24.0, 'y': 24.0},
                'bottomRight': {'x': 24.0, 'y': 24.0},
                'bottomLeft': {'x': 24.0, 'y': 24.0},
              },
            },
            'clipper': {'kind': 'dartObjectReferencePresence'},
            'clipBehavior': {
              'kind': 'enum',
              'type': 'Clip',
              'value': 'hardEdge',
            },
          },
          child: _viewSizedBoxNode(childId, width: 80, height: 60),
        );
        (json['profile']! as Map<String, Object?>)['targetPlatform'] =
            targetPlatform;
        final model = CanvasModel.decode(
          Uint8List.fromList(utf8.encode(jsonEncode(json))),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: clipRRectId,
            onSelected: selections.add,
            dropHoverTarget: const CanvasDropTarget(
              parentWidgetId: clipRRectId,
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

        final widget = find.byKey(const ValueKey('canvas-widget-$clipRRectId'));
        final warning = find.byKey(
          const ValueKey('canvas-custom-clipper-preview-$clipRRectId'),
        );
        expect(tester.getSize(widget), const Size(80, 60));
        expect(warning, findsOneWidget, reason: targetPlatform);
        expect(
          find.text('Custom clipper\npreview unavailable'),
          findsOneWidget,
          reason: targetPlatform,
        );
        final warningText = tester.widget<Text>(
          find.text('Custom clipper\npreview unavailable'),
        );
        expect(
          warningText.style?.color,
          Colors.black87,
          reason: 'warning text must retain a high-contrast foreground',
        );
        final warningLabel = tester.widget<DecoratedBox>(
          find
              .ancestor(
                of: find.text('Custom clipper\npreview unavailable'),
                matching: find.byType(DecoratedBox),
              )
              .first,
        );
        expect(
          (warningLabel.decoration as BoxDecoration).color,
          Colors.amber.shade100,
          reason: 'warning text must retain an opaque contrast surface',
        );
        expect(
          find.bySemanticsLabel(message),
          findsOneWidget,
          reason: targetPlatform,
        );
        expect(find.text(message), findsNothing, reason: targetPlatform);
        final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
        await mouse.addPointer(location: Offset.zero);
        await mouse.moveTo(tester.getCenter(warning));
        await tester.pump(const Duration(milliseconds: 500));
        expect(
          find.text(message),
          findsOneWidget,
          reason: 'the full reason must be visible on hover: $targetPlatform',
        );
        await tester.tapAt(tester.getCenter(warning));
        await tester.pump();
        expect(
          selections.last,
          clipRRectId,
          reason: 'the warning visual must not consume Canvas selection taps',
        );
        await mouse.removePointer();
        expect(
          find.descendant(of: widget, matching: find.byType(ClipRRect)),
          findsNothing,
          reason:
              'borderRadius must not be rendered when Flutter will ignore it '
              'in favor of the project clipper ($targetPlatform)',
        );
        expect(
          find.bySemanticsLabel(RegExp('SizedBox ${RegExp.escape(childId)}')),
          findsOneWidget,
          reason: targetPlatform,
        );
        expect(
          find.byKey(const ValueKey('canvas-selection-outline-$clipRRectId')),
          findsOneWidget,
          reason: targetPlatform,
        );
        expect(
          find.byKey(const ValueKey('canvas-widget-insert-drop-zone')),
          findsOneWidget,
          reason: targetPlatform,
        );
        expect(tester.takeException(), isNull, reason: targetPlatform);
      }
      semantics.dispose();
    },
  );

  testWidgets(
    'keeps empty custom-clipper nodes zero-size and puts the reason on the external target',
    (tester) async {
      const cases =
          <({String name, String id, String expectedType, Type renderedType})>[
            (
              name: 'ClipRect',
              id: '4fcae2d8-2c48-4c9d-b3f0-62bf0d89ef67',
              expectedType: 'CustomClipper<Rect>',
              renderedType: ClipRect,
            ),
            (
              name: 'ClipOval',
              id: '91684e03-f63a-4cde-b899-7a50c888f9e7',
              expectedType: 'CustomClipper<Rect>',
              renderedType: ClipOval,
            ),
            (
              name: 'ClipRRect',
              id: '5582922d-9044-4e78-bba8-bb740884a49d',
              expectedType: 'CustomClipper<RRect>',
              renderedType: ClipRRect,
            ),
          ];
      final semantics = tester.ensureSemantics();

      for (final entry in cases) {
        for (final targetPlatform in const ['windows', 'web']) {
          final properties = <String, Object?>{
            'clipper': <String, Object?>{'kind': 'dartObjectReferencePresence'},
          };
          final json = switch (entry.name) {
            'ClipRect' => _modelWithCenteredClipRect(
              properties: properties,
              child: null,
              bounded: false,
            ),
            'ClipOval' => _modelWithCenteredClipOval(
              properties: properties,
              child: null,
              bounded: false,
            ),
            'ClipRRect' => _modelWithCenteredClipRRect(
              properties: properties,
              child: null,
              bounded: false,
              explicitDirectionality: false,
            ),
            _ => throw AssertionError('Unexpected clip widget ${entry.name}'),
          };
          (json['profile']! as Map<String, Object?>)['targetPlatform'] =
              targetPlatform;
          final model = CanvasModel.decode(
            Uint8List.fromList(utf8.encode(jsonEncode(json))),
          );
          final selections = <String>[];
          CanvasDropResolver? resolver;
          await tester.pumpWidget(
            CanvasModelApp(
              model: model,
              selectedWidgetId: null,
              onSelected: selections.add,
              onDropResolverChanged: (value) => resolver = value,
            ),
          );
          await tester.pump();

          final node = find.byKey(ValueKey('canvas-widget-${entry.id}'));
          final target = find.byKey(
            ValueKey('canvas-zero-size-widget-target-${entry.id}'),
          );
          final badge = find.byKey(
            ValueKey('canvas-zero-size-custom-clipper-warning-${entry.id}'),
          );
          final message =
              'Custom ${entry.name} preview unavailable. Generated Dart uses '
              'the configured ${entry.expectedType}; isolated Canvas does not '
              'execute project or dependency Dart.';

          expect(
            tester.getSize(node),
            Size.zero,
            reason:
                'the external warning must not fabricate ${entry.name} layout '
                'on $targetPlatform',
          );
          expect(target, findsOneWidget, reason: '$entry $targetPlatform');
          expect(tester.getSize(target), const Size.square(36));
          expect(badge, findsOneWidget, reason: '$entry $targetPlatform');
          expect(tester.getSize(badge), const Size.square(20));
          final badgeDecoration =
              tester.widget<DecoratedBox>(badge).decoration as BoxDecoration;
          expect(badgeDecoration.color, Colors.amber.shade100);
          expect(badgeDecoration.border!.top.color, Colors.amber.shade800);

          final externalSemantics = tester.widget<Semantics>(
            find.byKey(
              ValueKey('canvas-zero-size-widget-semantics-${entry.id}'),
            ),
          );
          expect(externalSemantics.properties.label, contains(message));
          final tooltip = tester.widget<Tooltip>(
            find.ancestor(of: badge, matching: find.byType(Tooltip)),
          );
          expect(tooltip.message, message);

          final mouse = await tester.createGesture(
            kind: PointerDeviceKind.mouse,
          );
          await mouse.addPointer(location: Offset.zero);
          await mouse.moveTo(tester.getCenter(target));
          await tester.pump(const Duration(milliseconds: 500));
          expect(
            find.text(message),
            findsOneWidget,
            reason: 'the full reason must be visible on hover',
          );
          await tester.tap(target);
          await tester.pump();
          expect(selections.last, entry.id);
          expect(
            find.descendant(
              of: node,
              matching: find.byType(entry.renderedType),
            ),
            findsNothing,
            reason: 'Canvas must not fake the configured project clipper',
          );

          final surface = tester.getRect(find.byType(CanvasDocumentView));
          final point = tester.getRect(target).center;
          final drop = resolver!(
            ((point.dx - surface.left) / surface.width * 1000000).round(),
            ((point.dy - surface.top) / surface.height * 1000000).round(),
          );
          expect(drop?.parentWidgetId, entry.id);
          expect(drop?.slotName, 'child');
          expect(drop?.insertionIndex, 0);
          expect(drop?.zone?.isEmpty, isFalse);
          expect(tester.takeException(), isNull);

          await mouse.removePointer();
          await tester.pumpAndSettle();
        }
      }
      semantics.dispose();
    },
  );

  testWidgets(
    'keeps a custom-clipper warning visible while a coincident plain node is selected',
    (tester) async {
      const plainId = '38f49912-8e51-4e62-bd4c-2517ecad4962';
      const clipRectId = '4fcae2d8-2c48-4c9d-b3f0-62bf0d89ef67';
      const clipRRectId = '5582922d-9044-4e78-bba8-bb740884a49d';
      const cyclingMessage =
          '3 overlapping zero-size widgets. '
          'Activate repeatedly to cycle selection.';
      const clipRectMessage =
          'Custom ClipRect preview unavailable. Generated Dart uses the '
          'configured CustomClipper<Rect>; isolated Canvas does not execute '
          'project or dependency Dart.';
      const clipRRectMessage =
          'Custom ClipRRect preview unavailable. Generated Dart uses the '
          'configured CustomClipper<RRect>; isolated Canvas does not execute '
          'project or dependency Dart.';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(jsonEncode(_modelWithOverlappingCustomClippers())),
        ),
      );
      var selectedWidgetId = clipRRectId;

      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
            ),
          ),
        ),
      );
      await tester.pump();

      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-group-$plainId'),
      );
      final badge = find.byKey(
        const ValueKey(
          'canvas-zero-size-custom-clipper-warning-group-$plainId',
        ),
      );
      final semantics = find.byKey(
        const ValueKey('canvas-zero-size-widget-semantics-group-$plainId'),
      );
      expect(target, findsOneWidget);
      expect(badge, findsOneWidget);
      expect(
        tester
            .widget<Tooltip>(
              find.ancestor(of: badge, matching: find.byType(Tooltip)),
            )
            .message,
        '$cyclingMessage $clipRRectMessage',
        reason: 'the selected warning-bearing node must own the group reason',
      );
      expect(
        tester.widget<Semantics>(semantics).properties.label,
        '$cyclingMessage. $clipRRectMessage',
      );

      await tester.tap(target);
      await tester.pump();

      expect(selectedWidgetId, plainId);
      expect(badge, findsOneWidget);
      expect(
        tester
            .widget<Tooltip>(
              find.ancestor(of: badge, matching: find.byType(Tooltip)),
            )
            .message,
        '$cyclingMessage $clipRectMessage',
        reason:
            'a selected plain node must not hide another coincident warning',
      );
      expect(
        tester.widget<Semantics>(semantics).properties.label,
        '$cyclingMessage. $clipRectMessage',
      );

      await tester.tap(target);
      await tester.pump();

      expect(selectedWidgetId, clipRectId);
      expect(
        tester
            .widget<Tooltip>(
              find.ancestor(of: badge, matching: find.byType(Tooltip)),
            )
            .message,
        '$cyclingMessage $clipRectMessage',
        reason: 'the selected ClipRect must keep its own warning reason',
      );
    },
  );

  testWidgets(
    'keeps an empty zero-size ClipRRect selectable and exposes child DnD',
    (tester) async {
      const clipRRectId = '5582922d-9044-4e78-bba8-bb740884a49d';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredClipRRect(
                properties: const {},
                child: null,
                bounded: false,
                explicitDirectionality: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$clipRRectId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$clipRRectId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size.square(36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, clipRRectId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, clipRRectId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real ClipRect defaults and exact paint, hit, semantics, and clip behavior',
    (tester) async {
      const clipRectId = '4fcae2d8-2c48-4c9d-b3f0-62bf0d89ef67';
      const childId = '3bb165aa-21d8-497c-8e95-0c5c960588b0';
      final semantics = tester.ensureSemantics();

      Finder clipRectFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$clipRectId')),
            matching: find.byType(ClipRect),
          )
          .first;

      Future<({ClipRect widget, RenderClipRect render})> pump({
        required Map<String, Object?> properties,
        required Map<String, Object?> child,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredClipRect(
                  properties: properties,
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final finder = clipRectFinder();
        return (
          widget: tester.widget<ClipRect>(finder),
          render: tester.renderObject<RenderClipRect>(finder),
        );
      }

      final visibleChild = _viewSizedBoxNode(childId, width: 80, height: 60);
      final omitted = await pump(properties: const {}, child: visibleChild);
      expect(omitted.widget.clipper, isNull);
      expect(omitted.widget.clipBehavior, Clip.hardEdge);
      expect(omitted.render.clipper, isNull);
      expect(omitted.render.clipBehavior, Clip.hardEdge);
      expect(omitted.render.size, const Size(80, 60));
      expect(
        omitted.render.describeApproximatePaintClip(omitted.render.child!),
        Offset.zero & omitted.render.size,
      );
      expect(
        omitted.render,
        paints..clipRect(rect: Offset.zero & omitted.render.size),
      );
      expect(
        omitted.render.hitTest(
          BoxHitTestResult(),
          position: omitted.render.size.center(Offset.zero),
        ),
        isTrue,
      );
      expect(
        omitted.render.hitTest(
          BoxHitTestResult(),
          position: Offset(omitted.render.size.width + 1, 30),
        ),
        isFalse,
      );
      expect(
        find.bySemanticsLabel(RegExp('SizedBox ${RegExp.escape(childId)}')),
        findsOneWidget,
      );

      const clips = <String, Clip>{
        'none': Clip.none,
        'hardEdge': Clip.hardEdge,
        'antiAlias': Clip.antiAlias,
        'antiAliasWithSaveLayer': Clip.antiAliasWithSaveLayer,
      };
      for (final entry in clips.entries) {
        final result = await pump(
          properties: {
            'clipBehavior': {
              'kind': 'enum',
              'type': 'Clip',
              'value': entry.key,
            },
          },
          child: visibleChild,
        );
        expect(result.widget.clipper, isNull, reason: entry.key);
        expect(result.widget.clipBehavior, entry.value, reason: entry.key);
        expect(result.render.clipper, isNull, reason: entry.key);
        expect(result.render.clipBehavior, entry.value, reason: entry.key);
        expect(
          result.render.describeApproximatePaintClip(result.render.child!),
          entry.value == Clip.none ? isNull : Offset.zero & result.render.size,
          reason: entry.key,
        );
        if (entry.value == Clip.none) {
          expect(result.render, isNot(paints..clipRect()), reason: entry.key);
        } else {
          expect(
            result.render,
            paints..clipRect(rect: Offset.zero & result.render.size),
            reason: entry.key,
          );
        }
        expect(
          find.bySemanticsLabel(RegExp('SizedBox ${RegExp.escape(childId)}')),
          findsOneWidget,
          reason: entry.key,
        );
      }
      expect(tester.takeException(), isNull);
      semantics.dispose();
    },
  );

  testWidgets(
    'keeps an empty zero-size ClipRect selectable and exposes child DnD',
    (tester) async {
      const clipRectId = '4fcae2d8-2c48-4c9d-b3f0-62bf0d89ef67';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredClipRect(
                properties: const {},
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$clipRectId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$clipRectId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size.square(36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, clipRectId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, clipRectId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders finite, unbounded, expanding, and mixed ConstrainedBox axes',
    (tester) async {
      const widgetId = '93d89766-af04-4fee-af57-a56c4ed5e37c';
      final child = _viewSizedBoxNode(
        'bacaf0a6-b27a-4c98-9f88-8c8eb91597c1',
        width: 50,
        height: 30,
      );

      Finder constrainedFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(ConstrainedBox),
          )
          .first;

      Future<({BoxConstraints constraints, Size size, Size? childSize})> pump({
        required Map<String, Object?> constraints,
        required Map<String, Object?>? child,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredConstrainedBox(
                  constraints: constraints,
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final widget = tester.widget<ConstrainedBox>(constrainedFinder());
        final render = tester.renderObject<RenderConstrainedBox>(
          constrainedFinder(),
        );
        expect(tester.takeException(), isNull);
        return (
          constraints: widget.constraints,
          size: render.size,
          childSize: render.child?.size,
        );
      }

      final finite = await pump(
        constraints: _viewBoxConstraints(10, 80, 20, 90),
        child: child,
      );
      expect(
        finite.constraints,
        const BoxConstraints(
          minWidth: 10,
          maxWidth: 80,
          minHeight: 20,
          maxHeight: 90,
        ),
      );
      expect(finite.size, const Size(50, 30));
      expect(finite.childSize, const Size(50, 30));

      final finiteEmpty = await pump(
        constraints: _viewBoxConstraints(10, 80, 20, 90),
        child: null,
      );
      expect(finiteEmpty.size, const Size(10, 20));
      expect(finiteEmpty.childSize, isNull);

      final unbounded = await pump(
        constraints: _viewBoxConstraints(0, null, 0, null),
        child: child,
      );
      expect(unbounded.constraints.minWidth, 0);
      expect(unbounded.constraints.maxWidth, double.infinity);
      expect(unbounded.constraints.minHeight, 0);
      expect(unbounded.constraints.maxHeight, double.infinity);
      expect(unbounded.size, const Size(50, 30));

      final unboundedEmpty = await pump(
        constraints: _viewBoxConstraints(0, null, 0, null),
        child: null,
      );
      expect(unboundedEmpty.size, Size.zero);

      final expanding = await pump(
        constraints: _viewBoxConstraints(null, null, null, null),
        child: child,
      );
      expect(expanding.constraints.minWidth, double.infinity);
      expect(expanding.constraints.maxWidth, double.infinity);
      expect(expanding.constraints.minHeight, double.infinity);
      expect(expanding.constraints.maxHeight, double.infinity);
      expect(expanding.size, const Size(200, 100));
      expect(expanding.childSize, const Size(200, 100));

      final expandingWidth = await pump(
        constraints: _viewBoxConstraints(null, null, 10, 40),
        child: child,
      );
      expect(expandingWidth.size, const Size(200, 30));
      expect(expandingWidth.childSize, const Size(200, 30));

      final expandingHeight = await pump(
        constraints: _viewBoxConstraints(25, 70, null, null),
        child: child,
      );
      expect(expandingHeight.size, const Size(50, 100));
      expect(expandingHeight.childSize, const Size(50, 100));

      final mixedEmpty = await pump(
        constraints: _viewBoxConstraints(0, null, 12, 40),
        child: null,
      );
      expect(mixedEmpty.size, const Size(0, 12));
    },
  );

  testWidgets(
    'lets finite parent constraints enforce ConstrainedBox invalid boundary',
    (tester) async {
      const widgetId = '93d89766-af04-4fee-af57-a56c4ed5e37c';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredConstrainedBox(
                constraints: _viewBoxConstraints(300, 400, 150, 200),
                child: _viewSizedBoxNode(
                  'bacaf0a6-b27a-4c98-9f88-8c8eb91597c1',
                  width: 500,
                  height: 500,
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final finder = find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(ConstrainedBox),
          )
          .first;
      final render = tester.renderObject<RenderConstrainedBox>(finder);
      expect(
        render.additionalConstraints,
        const BoxConstraints(
          minWidth: 300,
          maxWidth: 400,
          minHeight: 150,
          maxHeight: 200,
        ),
      );
      expect(
        render.constraints,
        const BoxConstraints(
          minWidth: 0,
          maxWidth: 200,
          minHeight: 0,
          maxHeight: 100,
        ),
      );
      expect(render.size, const Size(200, 100));
      expect(render.child!.size, const Size(200, 100));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps empty default ConstrainedBox layout zero while exposing bounded DnD',
    (tester) async {
      const widgetId = '93d89766-af04-4fee-af57-a56c4ed5e37c';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredConstrainedBox(
                constraints: _viewBoxConstraints(0, null, 0, null),
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real UnconstrainedBox defaults, both retained axes, and every clip',
    (tester) async {
      const widgetId = '67247867-79f8-470f-b109-59971aa7392c';
      final child = _viewSizedBoxNode(
        'ff6ed199-2782-4be8-873b-e61ed6422664',
        width: 300,
        height: 200,
      );
      var overflowObserved = false;

      Finder widgetFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(UnconstrainedBox),
          )
          .first;
      Finder renderFinder() => find
          .descendant(
            of: widgetFinder(),
            matching: find.byType(ConstraintsTransformBox),
          )
          .first;

      Future<({UnconstrainedBox widget, RenderConstraintsTransformBox render})>
      pump(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredUnconstrainedBox(
                  properties: properties,
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final overflow = tester.takeException();
        if (overflow != null) {
          overflowObserved = true;
          expect(overflow, isA<FlutterError>());
          expect(
            overflow.toString(),
            contains('A RenderConstraintsTransformBox overflowed'),
          );
        }
        return (
          widget: tester.widget<UnconstrainedBox>(widgetFinder()),
          render: tester.renderObject<RenderConstraintsTransformBox>(
            renderFinder(),
          ),
        );
      }

      final omitted = await pump(const {});
      expect(overflowObserved, isTrue);
      expect(omitted.widget.textDirection, isNull);
      expect(omitted.widget.alignment, Alignment.center);
      expect(omitted.widget.constrainedAxis, isNull);
      expect(omitted.widget.clipBehavior, Clip.none);
      expect(omitted.render.size, const Size(200, 100));
      expect(omitted.render.child!.size, const Size(300, 200));
      expect(
        (omitted.render.child!.parentData! as BoxParentData).offset,
        const Offset(-50, -50),
      );

      final horizontal = await pump(const {
        'constrainedAxis': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
      });
      expect(horizontal.widget.constrainedAxis, Axis.horizontal);
      expect(horizontal.render.size, const Size(200, 100));
      expect(horizontal.render.child!.size, const Size(200, 200));
      expect(
        (horizontal.render.child!.parentData! as BoxParentData).offset,
        const Offset(0, -50),
      );

      final vertical = await pump(const {
        'constrainedAxis': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'vertical',
        },
      });
      expect(vertical.widget.constrainedAxis, Axis.vertical);
      expect(vertical.render.size, const Size(200, 100));
      expect(vertical.render.child!.size, const Size(300, 100));
      expect(
        (vertical.render.child!.parentData! as BoxParentData).offset,
        const Offset(-50, 0),
      );

      const clips = <String, Clip>{
        'none': Clip.none,
        'hardEdge': Clip.hardEdge,
        'antiAlias': Clip.antiAlias,
        'antiAliasWithSaveLayer': Clip.antiAliasWithSaveLayer,
      };
      for (final entry in clips.entries) {
        final result = await pump({
          'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': entry.key},
        });
        expect(result.widget.clipBehavior, entry.value, reason: entry.key);
        expect(result.render.clipBehavior, entry.value, reason: entry.key);
      }
    },
  );

  testWidgets(
    'resolves UnconstrainedBox physical and directional alignment exactly',
    (tester) async {
      const widgetId = '67247867-79f8-470f-b109-59971aa7392c';
      final child = _viewSizedBoxNode(
        'ff6ed199-2782-4be8-873b-e61ed6422664',
        width: 40,
        height: 20,
      );

      Future<({Offset offset, TextDirection direction})> pump({
        required String locale,
        required String basis,
        required double horizontal,
        String? explicitDirection,
      }) async {
        final properties = <String, Object?>{
          'alignment': _viewAlignment(
            basis: basis,
            horizontal: horizontal,
            vertical: 0,
          ),
        };
        if (explicitDirection != null) {
          properties['textDirection'] = {
            'kind': 'enum',
            'type': 'TextDirection',
            'value': explicitDirection,
          };
        }
        final json = _modelWithCenteredUnconstrainedBox(
          properties: properties,
          child: child,
        );
        (json['profile']! as Map<String, Object?>)['locale'] = locale;
        final model = CanvasModel.decode(
          Uint8List.fromList(utf8.encode(jsonEncode(json))),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final render = tester.renderObject<RenderConstraintsTransformBox>(
          find
              .descendant(
                of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
                matching: find.byType(ConstraintsTransformBox),
              )
              .first,
        );
        expect(tester.takeException(), isNull);
        return (
          offset: (render.child!.parentData! as BoxParentData).offset,
          direction: render.textDirection!,
        );
      }

      final physicalLtr = await pump(
        locale: 'en-US',
        basis: 'physical',
        horizontal: 1,
      );
      final physicalRtl = await pump(
        locale: 'ar-SA',
        basis: 'physical',
        horizontal: 1,
      );
      expect(physicalLtr.offset, const Offset(160, 40));
      expect(physicalRtl.offset, const Offset(160, 40));

      final ambientLtr = await pump(
        locale: 'en-US',
        basis: 'directional',
        horizontal: -1,
      );
      final ambientRtl = await pump(
        locale: 'ar-SA',
        basis: 'directional',
        horizontal: -1,
      );
      expect(ambientLtr.direction, TextDirection.ltr);
      expect(ambientLtr.offset, const Offset(0, 40));
      expect(ambientRtl.direction, TextDirection.rtl);
      expect(ambientRtl.offset, const Offset(160, 40));

      final explicitLtr = await pump(
        locale: 'ar-SA',
        basis: 'directional',
        horizontal: -1,
        explicitDirection: 'ltr',
      );
      final explicitRtl = await pump(
        locale: 'en-US',
        basis: 'directional',
        horizontal: -1,
        explicitDirection: 'rtl',
      );
      expect(explicitLtr.direction, TextDirection.ltr);
      expect(explicitLtr.offset, const Offset(0, 40));
      expect(explicitRtl.direction, TextDirection.rtl);
      expect(explicitRtl.offset, const Offset(160, 40));
    },
  );

  testWidgets(
    'keeps empty UnconstrainedBox selectable and exposes only its empty child slot',
    (tester) async {
      const widgetId = '67247867-79f8-470f-b109-59971aa7392c';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredUnconstrainedBox(
                properties: const {},
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real LimitedBox limits only on unbounded incoming axes',
    (tester) async {
      const widgetId = '57b8ce90-edde-4988-a4b7-bbf2eec66922';
      final child = _viewSizedBoxNode(
        'f28acc9b-e654-4b3a-ad07-59d75742b433',
        width: 150,
        height: 75,
      );

      Finder limitedFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(LimitedBox),
          )
          .first;

      Future<({LimitedBox widget, RenderLimitedBox render})> pump({
        required Map<String, Object?> properties,
        required String incoming,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredLimitedBox(
                  properties: properties,
                  child: child,
                  incoming: incoming,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        expect(tester.takeException(), isNull);
        return (
          widget: tester.widget<LimitedBox>(limitedFinder()),
          render: tester.renderObject<RenderLimitedBox>(limitedFinder()),
        );
      }

      const limits = <String, Object?>{
        'maxWidth': {'kind': 'double', 'value': 80.0},
        'maxHeight': {'kind': 'double', 'value': 40.0},
      };

      final bounded = await pump(properties: limits, incoming: 'bounded');
      expect(bounded.widget.maxWidth, 80);
      expect(bounded.widget.maxHeight, 40);
      expect(bounded.render.maxWidth, 80);
      expect(bounded.render.maxHeight, 40);
      expect(
        bounded.render.constraints,
        BoxConstraints.tight(const Size(200, 100)),
      );
      expect(bounded.render.size, const Size(200, 100));
      expect(bounded.render.child!.size, const Size(200, 100));

      final widthUnbounded = await pump(
        properties: limits,
        incoming: 'widthUnbounded',
      );
      expect(widthUnbounded.render.constraints.hasBoundedWidth, isFalse);
      expect(widthUnbounded.render.constraints.hasBoundedHeight, isTrue);
      expect(widthUnbounded.render.size, const Size(80, 100));
      expect(widthUnbounded.render.child!.size, const Size(80, 100));

      final heightUnbounded = await pump(
        properties: limits,
        incoming: 'heightUnbounded',
      );
      expect(heightUnbounded.render.constraints.hasBoundedWidth, isTrue);
      expect(heightUnbounded.render.constraints.hasBoundedHeight, isFalse);
      expect(heightUnbounded.render.size, const Size(200, 40));
      expect(heightUnbounded.render.child!.size, const Size(200, 40));

      final bothUnbounded = await pump(
        properties: limits,
        incoming: 'bothUnbounded',
      );
      expect(bothUnbounded.render.constraints.hasBoundedWidth, isFalse);
      expect(bothUnbounded.render.constraints.hasBoundedHeight, isFalse);
      expect(bothUnbounded.render.size, const Size(80, 40));
      expect(bothUnbounded.render.child!.size, const Size(80, 40));

      final omitted = await pump(
        properties: const {},
        incoming: 'bothUnbounded',
      );
      expect(omitted.widget.maxWidth, double.infinity);
      expect(omitted.widget.maxHeight, double.infinity);
      expect(omitted.render.maxWidth, double.infinity);
      expect(omitted.render.maxHeight, double.infinity);
      expect(omitted.render.size, const Size(150, 75));
      expect(omitted.render.child!.size, const Size(150, 75));
    },
  );

  testWidgets(
    'keeps empty LimitedBox selectable with bounded child-drop geometry',
    (tester) async {
      const widgetId = '57b8ce90-edde-4988-a4b7-bbf2eec66922';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredLimitedBox(
                properties: const {},
                child: null,
                incoming: 'bothUnbounded',
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real OverflowBox bound overrides, overflow, omission, and both fits',
    (tester) async {
      const widgetId = '7a59d693-7fd5-4c80-a4e2-3d0f908a41f4';

      Finder overflowFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(OverflowBox),
          )
          .first;

      Future<({OverflowBox widget, RenderConstrainedOverflowBox render})> pump({
        required Map<String, Object?> properties,
        required Map<String, Object?>? child,
        bool bounded = true,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredOverflowBox(
                  properties: properties,
                  child: child,
                  bounded: bounded,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        expect(tester.takeException(), isNull);
        return (
          widget: tester.widget<OverflowBox>(overflowFinder()),
          render: tester.renderObject<RenderConstrainedOverflowBox>(
            overflowFinder(),
          ),
        );
      }

      final overflow = await pump(
        properties: const {
          'minWidth': {'kind': 'double', 'value': 300.0},
          'maxWidth': {'kind': 'double', 'value': 300.0},
          'minHeight': {'kind': 'double', 'value': 160.0},
          'maxHeight': {'kind': 'double', 'value': 160.0},
        },
        child: _viewSizedBoxNode(
          'f72283ea-320c-4733-9072-963bc8281e7b',
          width: 10,
          height: 10,
        ),
      );
      expect(overflow.widget.minWidth, 300);
      expect(overflow.widget.maxWidth, 300);
      expect(overflow.widget.minHeight, 160);
      expect(overflow.widget.maxHeight, 160);
      expect(overflow.widget.fit, OverflowBoxFit.max);
      expect(
        overflow.render.constraints,
        BoxConstraints.tight(const Size(200, 100)),
      );
      expect(overflow.render.size, const Size(200, 100));
      expect(overflow.render.child!.size, const Size(300, 160));

      final maxFit = await pump(
        properties: const {
          'fit': {'kind': 'enum', 'type': 'OverflowBoxFit', 'value': 'max'},
        },
        child: _viewSizedBoxNode(
          'f72283ea-320c-4733-9072-963bc8281e7b',
          width: 60,
          height: 30,
        ),
        bounded: false,
      );
      expect(maxFit.widget.alignment, Alignment.center);
      expect(maxFit.widget.minWidth, isNull);
      expect(maxFit.widget.maxWidth, isNull);
      expect(maxFit.widget.minHeight, isNull);
      expect(maxFit.widget.maxHeight, isNull);
      expect(maxFit.widget.fit, OverflowBoxFit.max);
      expect(maxFit.render.child!.size, const Size(60, 30));
      expect(maxFit.render.size.width, greaterThan(60));
      expect(maxFit.render.size.height, greaterThan(30));

      final deferToChild = await pump(
        properties: const {
          'fit': {
            'kind': 'enum',
            'type': 'OverflowBoxFit',
            'value': 'deferToChild',
          },
        },
        child: _viewSizedBoxNode(
          'f72283ea-320c-4733-9072-963bc8281e7b',
          width: 60,
          height: 30,
        ),
        bounded: false,
      );
      expect(deferToChild.widget.fit, OverflowBoxFit.deferToChild);
      expect(deferToChild.render.size, const Size(60, 30));
      expect(deferToChild.render.child!.size, const Size(60, 30));
    },
  );

  testWidgets(
    'resolves OverflowBox physical and directional alignment in LTR and RTL',
    (tester) async {
      const widgetId = '7a59d693-7fd5-4c80-a4e2-3d0f908a41f4';
      final child = _viewSizedBoxNode(
        'f72283ea-320c-4733-9072-963bc8281e7b',
        width: 10,
        height: 10,
      );

      Future<({Offset offset, TextDirection direction})> pump({
        required String locale,
        required String basis,
        required double horizontal,
      }) async {
        final json = _modelWithCenteredOverflowBox(
          properties: {
            'alignment': _viewAlignment(
              basis: basis,
              horizontal: horizontal,
              vertical: 0,
            ),
            'minWidth': {'kind': 'double', 'value': 300.0},
            'maxWidth': {'kind': 'double', 'value': 300.0},
            'minHeight': {'kind': 'double', 'value': 160.0},
            'maxHeight': {'kind': 'double', 'value': 160.0},
          },
          child: child,
        );
        (json['profile']! as Map<String, Object?>)['locale'] = locale;
        final model = CanvasModel.decode(
          Uint8List.fromList(utf8.encode(jsonEncode(json))),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final render = tester.renderObject<RenderConstrainedOverflowBox>(
          find
              .descendant(
                of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
                matching: find.byType(OverflowBox),
              )
              .first,
        );
        expect(tester.takeException(), isNull);
        return (
          offset: (render.child!.parentData! as BoxParentData).offset,
          direction: render.textDirection!,
        );
      }

      final physicalLtr = await pump(
        locale: 'en-US',
        basis: 'physical',
        horizontal: 1,
      );
      final physicalRtl = await pump(
        locale: 'ar-SA',
        basis: 'physical',
        horizontal: 1,
      );
      expect(physicalLtr.offset, const Offset(-100, -30));
      expect(physicalRtl.offset, const Offset(-100, -30));

      final directionalLtr = await pump(
        locale: 'en-US',
        basis: 'directional',
        horizontal: -1,
      );
      final directionalRtl = await pump(
        locale: 'ar-SA',
        basis: 'directional',
        horizontal: -1,
      );
      expect(directionalLtr.direction, TextDirection.ltr);
      expect(directionalLtr.offset, const Offset(0, -30));
      expect(directionalRtl.direction, TextDirection.rtl);
      expect(directionalRtl.offset, const Offset(-100, -30));
    },
  );

  testWidgets(
    'keeps empty zero-size OverflowBox selectable with child-drop geometry',
    (tester) async {
      const widgetId = '7a59d693-7fd5-4c80-a4e2-3d0f908a41f4';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredOverflowBox(
                properties: const {
                  'fit': {
                    'kind': 'enum',
                    'type': 'OverflowBoxFit',
                    'value': 'deferToChild',
                  },
                },
                child: null,
                bounded: false,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$widgetId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('renders every Wrap argument and flows children into runs', (
    tester,
  ) async {
    const wrapId = '0cdde885-b68c-4b21-bd47-246721c4e8b2';
    const firstId = 'f5d1751b-136a-4db0-9e22-2ecb498eb425';
    const secondId = '0bdf25b7-ce2c-4cb7-9f04-270e53ad3157';
    const thirdId = '26829bde-f9d3-4bbc-b557-51693708e35e';
    CanvasDropResolver? resolver;
    CanvasMovePreviewResolver? moveResolver;
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithConstrainedWrap(
              properties: {
                'direction': {
                  'kind': 'enum',
                  'type': 'Axis',
                  'value': 'horizontal',
                },
                'alignment': {
                  'kind': 'enum',
                  'type': 'WrapAlignment',
                  'value': 'center',
                },
                'spacing': {'kind': 'double', 'value': 10.0},
                'runAlignment': {
                  'kind': 'enum',
                  'type': 'WrapAlignment',
                  'value': 'end',
                },
                'runSpacing': {'kind': 'double', 'value': 5.0},
                'crossAxisAlignment': {
                  'kind': 'enum',
                  'type': 'WrapCrossAlignment',
                  'value': 'end',
                },
                'textDirection': {
                  'kind': 'enum',
                  'type': 'TextDirection',
                  'value': 'ltr',
                },
                'verticalDirection': {
                  'kind': 'enum',
                  'type': 'VerticalDirection',
                  'value': 'down',
                },
                'clipBehavior': {
                  'kind': 'enum',
                  'type': 'Clip',
                  'value': 'antiAlias',
                },
              },
              children: [
                _viewSizedBoxNode(firstId, width: 40, height: 20),
                _viewSizedBoxNode(secondId, width: 40, height: 30),
                _viewSizedBoxNode(thirdId, width: 40, height: 10),
              ],
            ),
          ),
        ),
      ),
    );

    await tester.pumpWidget(
      CanvasModelApp(
        model: model,
        selectedWidgetId: null,
        onSelected: (_) {},
        onDropResolverChanged: (value) => resolver = value,
        onMovePreviewResolverChanged: (value) => moveResolver = value,
      ),
    );
    await tester.pump();

    final node = find.byKey(const ValueKey('canvas-widget-$wrapId'));
    final finder = find.descendant(of: node, matching: find.byType(Wrap)).first;
    final widget = tester.widget<Wrap>(finder);
    expect(widget.direction, Axis.horizontal);
    expect(widget.alignment, WrapAlignment.center);
    expect(widget.spacing, 10);
    expect(widget.runAlignment, WrapAlignment.end);
    expect(widget.runSpacing, 5);
    expect(widget.crossAxisAlignment, WrapCrossAlignment.end);
    expect(widget.textDirection, TextDirection.ltr);
    expect(widget.verticalDirection, VerticalDirection.down);
    expect(widget.clipBehavior, Clip.antiAlias);
    expect(widget.children, hasLength(3));

    final render = tester.renderObject<RenderWrap>(finder);
    expect(render.size, const Size(90, 45));
    final offsets = <Offset>[];
    RenderBox? child = render.firstChild;
    while (child != null) {
      offsets.add((child.parentData! as WrapParentData).offset);
      child = render.childAfter(child);
    }
    expect(offsets, const [Offset(0, 10), Offset(50, 0), Offset(25, 35)]);

    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final wrapRect = tester.getRect(node);
    final point = wrapRect.bottomRight - const Offset(1, 1);
    final drop = resolver!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
    );
    expect(drop?.parentWidgetId, wrapId);
    expect(drop?.slotName, 'children');
    expect(drop?.insertionIndex, 3);
    expect(
      drop!.zone!.rightMicros - drop.zone!.leftMicros,
      closeTo((wrapRect.width / surface.width * 1000000).round(), 2),
      reason: 'multi-run Wrap exposes its complete rendered node',
    );
    final move = moveResolver!(firstId, wrapId, 'children', 0);
    expect(move?.parentWidgetId, wrapId);
    expect(move?.slotName, 'children');
    expect(move?.insertionIndex, 0);
    expect(move?.zone?.leftMicros, drop.zone!.leftMicros);
    expect(move?.zone?.topMicros, drop.zone!.topMicros);
    expect(move?.zone?.rightMicros, drop.zone!.rightMicros);
    expect(move?.zone?.bottomMicros, drop.zone!.bottomMicros);
    expect(tester.takeException(), isNull);
  });

  testWidgets('keeps an empty Wrap selectable and exposes its append slot', (
    tester,
  ) async {
    const wrapId = '0cdde885-b68c-4b21-bd47-246721c4e8b2';
    String? selectedWidgetId;
    CanvasDropResolver? resolver;
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithConstrainedWrap(
              properties: {
                'spacing': {'kind': 'double', 'value': -4.0},
                'runSpacing': {'kind': 'double', 'value': -2.0},
              },
              children: const [],
              unboundedMainAxis: true,
            ),
          ),
        ),
      ),
    );

    await tester.pumpWidget(
      StatefulBuilder(
        builder: (context, setState) => CanvasModelApp(
          model: model,
          selectedWidgetId: selectedWidgetId,
          onSelected: (id) => setState(() => selectedWidgetId = id),
          onDropResolverChanged: (value) => resolver = value,
        ),
      ),
    );
    await tester.pump();

    final node = find.byKey(const ValueKey('canvas-widget-$wrapId'));
    final widget = tester.widget<Wrap>(
      find.descendant(of: node, matching: find.byType(Wrap)).first,
    );
    expect(widget.spacing, -4);
    expect(widget.runSpacing, -2);
    expect(tester.getSize(node), Size.zero);
    final target = find.byKey(
      const ValueKey('canvas-zero-size-widget-target-$wrapId'),
    );
    expect(target, findsOneWidget);
    expect(tester.getSize(target), const Size(36, 36));
    await tester.tap(target);
    await tester.pump();
    expect(selectedWidgetId, wrapId);

    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final point = tester.getRect(target).center;
    final drop = resolver!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
    );
    expect(drop?.parentWidgetId, wrapId);
    expect(drop?.slotName, 'children');
    expect(drop?.insertionIndex, 0);
    expect(drop?.zone?.isEmpty, isFalse);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'renders real Stack defaults, exact fits, alignment, and z-order',
    (tester) async {
      const stackId = '5a809127-5a50-4dbf-b5fd-464619344ac4';
      const bottomId = 'f5d1751b-136a-4db0-9e22-2ecb498eb425';
      const topId = '0bdf25b7-ce2c-4cb7-9f04-270e53ad3157';
      final children = <Map<String, Object?>>[
        _viewSizedBoxNode(bottomId, width: 40, height: 20),
        _viewSizedBoxNode(topId, width: 80, height: 30),
      ];
      String? selectedWidgetId;

      Finder stackFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$stackId')),
            matching: find.byType(Stack),
          )
          .first;

      Future<RenderStack> pump(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithConstrainedStack(
                  properties: properties,
                  children: children,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => selectedWidgetId = id,
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderStack>(stackFinder());
      }

      List<RenderBox> renderChildren(RenderStack stack) {
        final result = <RenderBox>[];
        RenderBox? child = stack.firstChild;
        while (child != null) {
          result.add(child);
          child = stack.childAfter(child);
        }
        return result;
      }

      final defaults = await pump(const {});
      final defaultWidget = tester.widget<Stack>(stackFinder());
      expect(defaultWidget.alignment, AlignmentDirectional.topStart);
      expect(defaultWidget.textDirection, isNull);
      expect(defaultWidget.fit, StackFit.loose);
      expect(defaultWidget.clipBehavior, Clip.hardEdge);
      expect(defaults.size, const Size(100, 50));
      var renderedChildren = renderChildren(defaults);
      expect(renderedChildren.map((child) => child.size), const [
        Size(40, 20),
        Size(80, 30),
      ]);
      expect(
        renderedChildren.map(
          (child) => (child.parentData! as StackParentData).offset,
        ),
        const [Offset.zero, Offset.zero],
      );

      final stackRect = tester.getRect(stackFinder());
      await tester.tapAt(stackRect.topLeft + const Offset(10, 10));
      await tester.pump();
      expect(
        selectedWidgetId,
        topId,
        reason: 'the last Stack child paints and hit-tests on top',
      );

      final expanded = await pump({
        'fit': {'kind': 'enum', 'type': 'StackFit', 'value': 'expand'},
      });
      expect(expanded.size, const Size(200, 100));
      expect(renderChildren(expanded).map((child) => child.size), const [
        Size(200, 100),
        Size(200, 100),
      ]);

      final passthrough = await pump({
        'fit': {'kind': 'enum', 'type': 'StackFit', 'value': 'passthrough'},
      });
      expect(passthrough.size, const Size(100, 50));
      expect(renderChildren(passthrough).map((child) => child.size), const [
        Size(100, 50),
        Size(100, 50),
      ]);

      final extrapolated = await pump({
        'alignment': _viewAlignment(horizontal: 2, vertical: -2),
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'none'},
      });
      final explicitWidget = tester.widget<Stack>(stackFinder());
      expect(explicitWidget.alignment, const Alignment(2, -2));
      expect(explicitWidget.clipBehavior, Clip.none);
      renderedChildren = renderChildren(extrapolated);
      expect(
        (renderedChildren.first.parentData! as StackParentData).offset,
        const Offset(90, -15),
      );
      expect(
        (renderedChildren.last.parentData! as StackParentData).offset,
        const Offset(30, -10),
      );
    },
  );

  testWidgets('resolves Stack directional defaults and explicit override', (
    tester,
  ) async {
    const stackId = '5a809127-5a50-4dbf-b5fd-464619344ac4';
    final child = _viewSizedBoxNode(
      'f5d1751b-136a-4db0-9e22-2ecb498eb425',
      width: 40,
      height: 20,
    );

    Future<({Offset offset, TextDirection direction})> render({
      required String locale,
      String? explicitDirection,
    }) async {
      final properties = <String, Object?>{};
      if (explicitDirection != null) {
        properties['textDirection'] = {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': explicitDirection,
        };
      }
      final json = _modelWithConstrainedStack(
        properties: properties,
        children: [child],
      );
      (json['profile']! as Map<String, Object?>)['locale'] = locale;
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      final node = find.byKey(const ValueKey('canvas-widget-$stackId'));
      final finder = find
          .descendant(of: node, matching: find.byType(Stack))
          .first;
      final stack = tester.renderObject<RenderStack>(finder);
      final first = stack.firstChild!;
      return (
        offset: (first.parentData! as StackParentData).offset,
        direction: stack.textDirection!,
      );
    }

    final ambientLtr = await render(locale: 'en-US');
    expect(ambientLtr.direction, TextDirection.ltr);
    expect(ambientLtr.offset, Offset.zero);

    final ambientRtl = await render(locale: 'ar-SA');
    expect(ambientRtl.direction, TextDirection.rtl);
    expect(ambientRtl.offset, const Offset(60, 0));

    final overridden = await render(locale: 'ar-SA', explicitDirection: 'ltr');
    expect(overridden.direction, TextDirection.ltr);
    expect(overridden.offset, Offset.zero);
  });

  testWidgets(
    'uses full-node Stack append and move zones and preserves zero selection',
    (tester) async {
      const stackId = '5a809127-5a50-4dbf-b5fd-464619344ac4';
      const sourceId = 'f5d1751b-136a-4db0-9e22-2ecb498eb425';
      CanvasDropResolver? dropResolver;
      CanvasMovePreviewResolver? moveResolver;

      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithConstrainedStack(
                properties: const {},
                children: [_viewSizedBoxNode(sourceId, width: 40, height: 20)],
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
          onDropResolverChanged: (value) => dropResolver = value,
          onMovePreviewResolverChanged: (value) => moveResolver = value,
        ),
      );
      await tester.pump();

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final stack = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$stackId')),
      );
      final point = stack.bottomRight - const Offset(2, 2);
      final drop = dropResolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, stackId);
      expect(drop?.slotName, 'children');
      expect(drop?.insertionIndex, 1);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(
        drop!.zone!.rightMicros - drop.zone!.leftMicros,
        closeTo((stack.width / surface.width * 1000000).round(), 2),
        reason: 'Stack exposes its complete rendered node, not a terminal band',
      );

      final move = moveResolver!(sourceId, stackId, 'children', 0);
      expect(move?.parentWidgetId, stackId);
      expect(move?.slotName, 'children');
      expect(move?.insertionIndex, 0);
      expect(move?.zone?.leftMicros, drop.zone!.leftMicros);
      expect(move?.zone?.topMicros, drop.zone!.topMicros);
      expect(move?.zone?.rightMicros, drop.zone!.rightMicros);
      expect(move?.zone?.bottomMicros, drop.zone!.bottomMicros);

      final zeroModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithConstrainedStack(
                properties: const {},
                children: const [],
                unboundedMainAxis: true,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: zeroModel,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(
        tester.getSize(find.byKey(const ValueKey('canvas-widget-$stackId'))),
        Size.zero,
      );
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$stackId'),
      );
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
    },
  );

  testWidgets(
    'renders IndexedStack largest-child layout and only the selected child paints, hits, and has semantics',
    (tester) async {
      const indexedStackId = '910fd547-b4aa-4da2-8bd1-bde08cae3944';
      const firstId = 'd63a0643-b74f-4ce7-a2f9-751936f45868';
      const secondId = '21f27093-ed37-4d8d-b12c-2077a8d88c84';
      final semantics = tester.ensureSemantics();
      String? selectedWidgetId;
      late CanvasModel model;

      Finder indexedStackFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$indexedStackId')),
            matching: find.byType(IndexedStack),
          )
          .first;

      List<RenderBox> renderChildren(RenderIndexedStack stack) {
        final result = <RenderBox>[];
        RenderBox? child = stack.firstChild;
        while (child != null) {
          result.add(child);
          child = stack.childAfter(child);
        }
        return result;
      }

      Future<RenderIndexedStack> pump(Map<String, Object?> properties) async {
        selectedWidgetId = null;
        model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithConstrainedIndexedStack(
                  properties: properties,
                  children: [
                    _viewSizedBoxNode(firstId, width: 120, height: 70),
                    _viewSizedBoxNode(secondId, width: 80, height: 30),
                  ],
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (id) => selectedWidgetId = id,
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderIndexedStack>(indexedStackFinder());
      }

      Future<void> expectSelection({
        required RenderIndexedStack render,
        required int? index,
        required String? selectedChildId,
      }) async {
        expect(render.index, index);
        expect(render.size, const Size(120, 70));
        expect(renderChildren(render).map((child) => child.size), const [
          Size(120, 70),
          Size(80, 30),
        ]);
        expect(model.widgetIds, containsAll([firstId, secondId]));
        expect(
          find.byKey(
            const ValueKey('canvas-widget-$firstId'),
            skipOffstage: false,
          ),
          findsOneWidget,
        );
        expect(
          find.byKey(
            const ValueKey('canvas-widget-$secondId'),
            skipOffstage: false,
          ),
          findsOneWidget,
        );

        final described = render.debugDescribeChildren();
        expect(described, hasLength(2));
        for (var childIndex = 0; childIndex < described.length; childIndex++) {
          expect(
            described[childIndex].style == DiagnosticsTreeStyle.offstage,
            childIndex != index,
            reason: 'only child $index is painted',
          );
        }
        expect(
          find.bySemanticsLabel(
            RegExp('^SizedBox ${RegExp.escape(firstId)}\$'),
          ),
          selectedChildId == firstId ? findsOneWidget : findsNothing,
        );
        expect(
          find.bySemanticsLabel(
            RegExp('^SizedBox ${RegExp.escape(secondId)}\$'),
          ),
          selectedChildId == secondId ? findsOneWidget : findsNothing,
        );

        await tester.tapAt(
          tester.getRect(indexedStackFinder()).topLeft + const Offset(10, 10),
        );
        await tester.pump();
        expect(selectedWidgetId, selectedChildId ?? indexedStackId);
      }

      final omitted = await pump(const {});
      expect(tester.widget<IndexedStack>(indexedStackFinder()).index, 0);
      await expectSelection(
        render: omitted,
        index: 0,
        selectedChildId: firstId,
      );

      final explicit = await pump(const {
        'index': {'kind': 'integer', 'value': 1},
      });
      await expectSelection(
        render: explicit,
        index: 1,
        selectedChildId: secondId,
      );

      final explicitNull = await pump(const {
        'index': {'kind': 'null'},
      });
      expect(tester.widget<IndexedStack>(indexedStackFinder()).index, isNull);
      await expectSelection(
        render: explicitNull,
        index: null,
        selectedChildId: null,
      );
      expect(
        find.bySemanticsLabel(
          RegExp('^IndexedStack ${RegExp.escape(indexedStackId)}\$'),
        ),
        findsOneWidget,
        reason: 'the Designer wrapper remains selectable when index is null',
      );
      expect(tester.takeException(), isNull);
      semantics.dispose();
    },
  );

  testWidgets(
    'applies IndexedStack direction, alignment, sizing, and clip exactly',
    (tester) async {
      const indexedStackId = '910fd547-b4aa-4da2-8bd1-bde08cae3944';
      final children = <Map<String, Object?>>[
        _viewSizedBoxNode(
          'd63a0643-b74f-4ce7-a2f9-751936f45868',
          width: 40,
          height: 20,
        ),
        _viewSizedBoxNode(
          '21f27093-ed37-4d8d-b12c-2077a8d88c84',
          width: 80,
          height: 30,
        ),
      ];

      Finder indexedStackFinder() => find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$indexedStackId')),
            matching: find.byType(IndexedStack),
          )
          .first;

      List<RenderBox> renderChildren(RenderIndexedStack stack) {
        final result = <RenderBox>[];
        RenderBox? child = stack.firstChild;
        while (child != null) {
          result.add(child);
          child = stack.childAfter(child);
        }
        return result;
      }

      Future<RenderIndexedStack> pump(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithConstrainedIndexedStack(
                  properties: properties,
                  children: children,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderIndexedStack>(indexedStackFinder());
      }

      final directional = await pump({
        'alignment': _viewAlignment(
          basis: 'directional',
          horizontal: -1,
          vertical: 1,
        ),
        'textDirection': {
          'kind': 'enum',
          'type': 'TextDirection',
          'value': 'rtl',
        },
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'none'},
        'sizing': {'kind': 'enum', 'type': 'StackFit', 'value': 'loose'},
        'index': {'kind': 'integer', 'value': 1},
      });
      final directionalWidget = tester.widget<IndexedStack>(
        indexedStackFinder(),
      );
      expect(directionalWidget.alignment, AlignmentDirectional.bottomStart);
      expect(directionalWidget.textDirection, TextDirection.rtl);
      expect(directionalWidget.clipBehavior, Clip.none);
      expect(directionalWidget.sizing, StackFit.loose);
      expect(directional.alignment, AlignmentDirectional.bottomStart);
      expect(directional.textDirection, TextDirection.rtl);
      expect(directional.clipBehavior, Clip.none);
      expect(directional.fit, StackFit.loose);
      expect(directional.size, const Size(100, 50));
      var renderedChildren = renderChildren(directional);
      expect(
        renderedChildren.map(
          (child) => (child.parentData! as StackParentData).offset,
        ),
        const [Offset(60, 30), Offset(20, 20)],
      );

      final expanded = await pump(const {
        'sizing': {'kind': 'enum', 'type': 'StackFit', 'value': 'expand'},
      });
      expect(expanded.fit, StackFit.expand);
      expect(expanded.size, const Size(200, 100));
      expect(renderChildren(expanded).map((child) => child.size), const [
        Size(200, 100),
        Size(200, 100),
      ]);

      final passthrough = await pump(const {
        'sizing': {'kind': 'enum', 'type': 'StackFit', 'value': 'passthrough'},
      });
      expect(passthrough.fit, StackFit.passthrough);
      expect(passthrough.size, const Size(100, 50));
      renderedChildren = renderChildren(passthrough);
      expect(renderedChildren.map((child) => child.size), const [
        Size(100, 50),
        Size(100, 50),
      ]);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'uses full-node IndexedStack append and move geometry with an empty transient target',
    (tester) async {
      const indexedStackId = '910fd547-b4aa-4da2-8bd1-bde08cae3944';
      const sourceId = 'd63a0643-b74f-4ce7-a2f9-751936f45868';
      CanvasDropResolver? dropResolver;
      CanvasMovePreviewResolver? moveResolver;
      String? selectedWidgetId;

      Future<void> pump({
        required List<Map<String, Object?>> children,
        bool unboundedMainAxis = false,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithConstrainedIndexedStack(
                  properties: const {},
                  children: children,
                  unboundedMainAxis: unboundedMainAxis,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => selectedWidgetId = id,
            onDropResolverChanged: (value) => dropResolver = value,
            onMovePreviewResolverChanged: (value) => moveResolver = value,
          ),
        );
        await tester.pump();
      }

      await pump(
        children: [_viewSizedBoxNode(sourceId, width: 40, height: 20)],
      );
      var surface = tester.getRect(find.byType(CanvasDocumentView));
      var stack = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$indexedStackId')),
      );
      var point = stack.bottomRight - const Offset(2, 2);
      var drop = dropResolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, indexedStackId);
      expect(drop?.slotName, 'children');
      expect(drop?.insertionIndex, 1);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(
        drop!.zone!.rightMicros - drop.zone!.leftMicros,
        closeTo((stack.width / surface.width * 1000000).round(), 2),
      );
      expect(
        drop.zone!.bottomMicros - drop.zone!.topMicros,
        closeTo(
          (math.max(stack.height, 36) / surface.height * 1000000).round(),
          2,
        ),
      );

      final move = moveResolver!(sourceId, indexedStackId, 'children', 0);
      expect(move?.parentWidgetId, indexedStackId);
      expect(move?.slotName, 'children');
      expect(move?.insertionIndex, 0);
      expect(move?.zone?.leftMicros, drop.zone!.leftMicros);
      expect(move?.zone?.topMicros, drop.zone!.topMicros);
      expect(move?.zone?.rightMicros, drop.zone!.rightMicros);
      expect(move?.zone?.bottomMicros, drop.zone!.bottomMicros);

      await pump(children: const [], unboundedMainAxis: true);
      final rendered = find.byKey(
        const ValueKey('canvas-widget-$indexedStackId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$indexedStackId'),
      );
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size.square(36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, indexedStackId);

      surface = tester.getRect(find.byType(CanvasDocumentView));
      stack = tester.getRect(target);
      point = stack.center;
      drop = dropResolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, indexedStackId);
      expect(drop?.slotName, 'children');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('IndexedStack drop traversal ignores hidden child destinations', (
    tester,
  ) async {
    const indexedStackId = '910fd547-b4aa-4da2-8bd1-bde08cae3944';
    const visibleId = 'd63a0643-b74f-4ce7-a2f9-751936f45868';
    const hiddenId = '21f27093-ed37-4d8d-b12c-2077a8d88c84';
    CanvasDropResolver? resolver;
    final hiddenContainer = <String, Object?>{
      'id': hiddenId,
      'type': 'flutter.widgets.Container',
      'properties': <String, Object?>{
        'width': {'kind': 'integer', 'value': 40},
        'height': {'kind': 'integer', 'value': 20},
      },
      'slots': <String, Object?>{
        'child': <String, Object?>{'kind': 'single', 'child': null},
      },
    };
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithConstrainedIndexedStack(
              properties: const {},
              children: [
                _viewSizedBoxNode(visibleId, width: 80, height: 30),
                hiddenContainer,
              ],
            ),
          ),
        ),
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(
        model: model,
        selectedWidgetId: null,
        onSelected: (_) {},
        onDropResolverChanged: (value) => resolver = value,
      ),
    );
    await tester.pump();

    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final stack = tester.getRect(
      find.byKey(const ValueKey('canvas-widget-$indexedStackId')),
    );
    final point = stack.topLeft + const Offset(10, 10);
    final drop = resolver!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
    );
    expect(drop?.parentWidgetId, visibleId);
    expect(drop?.slotName, 'child');
    expect(drop?.insertionIndex, 0);
    expect(
      drop?.parentWidgetId,
      isNot(hiddenId),
      reason: 'a retained but unpainted child must not capture Canvas DnD',
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'renders real Expanded as the direct ParentDataWidget in Row and Column',
    (tester) async {
      const firstId = '6ab52421-f234-4443-978f-bb57b9926f2e';
      const secondId = 'a17cb29f-3784-4ab8-a590-1233dd3e5731';
      final first = _viewExpandedNode(
        firstId,
        child: _viewSizedBoxNode(
          '0e991cf4-2481-4527-b608-c5c6244376f2',
          width: 30,
          height: 20,
        ),
      );
      final second = _viewExpandedNode(
        secondId,
        flex: 2,
        child: _viewSizedBoxNode(
          '1809f65f-03a7-4aba-8c23-10fe3020b364',
          width: 30,
          height: 20,
        ),
      );

      Future<void> pump(String parentType) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithFixedFlex(
                  parentType: parentType,
                  children: [first, second],
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        expect(tester.takeException(), isNull);
      }

      await pump('flutter.widgets.Row');
      var expanded = tester
          .widgetList<Expanded>(find.byType(Expanded))
          .toList();
      expect(expanded.map((widget) => widget.flex), [1, 2]);
      final firstRowSize = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$firstId')),
      );
      final secondRowSize = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$secondId')),
      );
      expect(firstRowSize.width, closeTo(100, 0.01));
      expect(secondRowSize.width, closeTo(200, 0.01));

      final zeroModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithFixedFlex(
                parentType: 'flutter.widgets.Row',
                children: [
                  _viewExpandedNode(
                    firstId,
                    flex: 0,
                    child: _viewSizedBoxNode(
                      '0e991cf4-2481-4527-b608-c5c6244376f2',
                      width: 30,
                      height: 20,
                    ),
                  ),
                  _viewExpandedNode(
                    secondId,
                    child: _viewSizedBoxNode(
                      '1809f65f-03a7-4aba-8c23-10fe3020b364',
                      width: 30,
                      height: 20,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: zeroModel,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$firstId')))
            .width,
        closeTo(30, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$secondId')))
            .width,
        closeTo(270, 0.01),
      );
      expect(tester.takeException(), isNull);

      await pump('flutter.widgets.Column');
      expanded = tester.widgetList<Expanded>(find.byType(Expanded)).toList();
      expect(expanded.map((widget) => widget.flex), [1, 2]);
      final firstColumnSize = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$firstId')),
      );
      final secondColumnSize = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$secondId')),
      );
      expect(firstColumnSize.height, closeTo(40, 0.01));
      expect(secondColumnSize.height, closeTo(80, 0.01));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real Flexible with exact flex and loose or tight fit in Row and Column',
    (tester) async {
      const zeroId = '70517708-fd15-4900-a5a1-3fb659f3fd60';
      const defaultId = '913206a7-d96d-488a-a6a6-261e0c14a9c2';
      const maximumId = '63e57fd7-7541-49bc-a81c-ae521b5f1fb9';
      final zero = _viewFlexibleNode(
        zeroId,
        flex: 0,
        child: _viewSizedBoxNode(
          'd66779a8-ac37-41a4-b77a-e4b47a2314b9',
          width: 30,
          height: 20,
        ),
      );
      final defaults = _viewFlexibleNode(
        defaultId,
        child: _viewSizedBoxNode(
          '2564005f-f3df-444a-8ed2-1718e4d9468e',
          width: 30,
          height: 20,
        ),
      );
      final maximum = _viewFlexibleNode(
        maximumId,
        flex: maxCanvasSequence,
        fit: 'tight',
        child: _viewSizedBoxNode(
          '052e72cd-569d-48fd-8ec7-d733c2a6f45b',
          width: 30,
          height: 20,
        ),
      );

      Future<void> pump(String parentType) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithFixedFlex(
                  parentType: parentType,
                  children: [zero, defaults, maximum],
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        expect(tester.takeException(), isNull);
      }

      void expectDirectFlexRenderChildren(Finder parent) {
        final renderFlex = tester.renderObject<RenderFlex>(parent);
        for (final id in <String>[zeroId, defaultId, maximumId]) {
          var directChild = tester.renderObject<RenderObject>(
            find.byKey(ValueKey('canvas-widget-$id')),
          );
          while (directChild.parent != null &&
              directChild.parent != renderFlex) {
            directChild = directChild.parent!;
          }
          expect(directChild.parent, same(renderFlex));
          expect(directChild.parentData, isA<FlexParentData>());
        }
      }

      await pump('flutter.widgets.Row');
      var flexible = tester
          .widgetList<Flexible>(find.byType(Flexible))
          .toList();
      expect(flexible.map((widget) => widget.flex), [0, 1, maxCanvasSequence]);
      expect(flexible.map((widget) => widget.fit), [
        FlexFit.loose,
        FlexFit.loose,
        FlexFit.tight,
      ]);
      expectDirectFlexRenderChildren(find.byType(Row));
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$zeroId')))
            .width,
        closeTo(30, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$defaultId')))
            .width,
        closeTo(0, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$maximumId')))
            .width,
        closeTo(270, 0.01),
      );

      await pump('flutter.widgets.Column');
      flexible = tester.widgetList<Flexible>(find.byType(Flexible)).toList();
      expect(flexible.map((widget) => widget.flex), [0, 1, maxCanvasSequence]);
      expect(flexible.map((widget) => widget.fit), [
        FlexFit.loose,
        FlexFit.loose,
        FlexFit.tight,
      ]);
      expectDirectFlexRenderChildren(find.byType(Column));
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$zeroId')))
            .height,
        closeTo(20, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$defaultId')))
            .height,
        closeTo(0, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$maximumId')))
            .height,
        closeTo(100, 0.01),
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real Spacer directly in Row and Column with selectable overlays',
    (tester) async {
      const defaultId = '18f6cf8c-4bb7-4d00-a159-a2fa5abc92ea';
      const weightedId = 'ee23441e-0993-4f6e-951e-fcc38ac2fe69';
      const leadingId = '185940b7-436d-4671-9215-f62d29d6754a';
      const trailingId = '8607bb96-03a2-4d85-a8f6-91192394cf6e';
      String? selected;

      Future<void> pump(
        String parentType, {
        bool stretchCrossAxis = false,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithFixedFlex(
                  parentType: parentType,
                  parentProperties: stretchCrossAxis
                      ? const {
                          'crossAxisAlignment': {
                            'kind': 'enum',
                            'type': 'CrossAxisAlignment',
                            'value': 'stretch',
                          },
                        }
                      : const {},
                  children: [
                    _viewSizedBoxNode(leadingId, width: 30, height: 20),
                    _viewSpacerNode(defaultId),
                    _viewSpacerNode(weightedId, flex: 2),
                    _viewSizedBoxNode(trailingId, width: 30, height: 20),
                  ],
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: selected,
            onSelected: (id) => selected = id,
          ),
        );
        await tester.pump();
        await tester.pump();
        expect(tester.takeException(), isNull);
      }

      void expectDirectSpacerRenderChildren(Finder parent) {
        final renderFlex = tester.renderObject<RenderFlex>(parent);
        for (var index = 0; index < 2; index++) {
          final renderObject = tester.renderObject<RenderObject>(
            find.byType(Spacer).at(index),
          );
          expect(renderObject.parent, same(renderFlex));
          expect(renderObject.parentData, isA<FlexParentData>());
        }
      }

      await pump('flutter.widgets.Row');
      var spacers = tester.widgetList<Spacer>(find.byType(Spacer)).toList();
      expect(spacers.map((widget) => widget.flex), [1, 2]);
      expectDirectSpacerRenderChildren(find.byType(Row));
      expect(
        tester.getSize(find.byKey(const ValueKey('canvas-widget-$defaultId'))),
        const Size(80, 0),
      );
      expect(
        tester.getSize(find.byKey(const ValueKey('canvas-widget-$weightedId'))),
        const Size(160, 0),
      );
      final defaultTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$defaultId'),
      );
      expect(defaultTarget, findsOneWidget);
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$weightedId'),
        ),
        findsOneWidget,
      );
      await tester.tap(defaultTarget);
      expect(selected, defaultId);

      await pump('flutter.widgets.Column');
      spacers = tester.widgetList<Spacer>(find.byType(Spacer)).toList();
      expect(spacers.map((widget) => widget.flex), [1, 2]);
      expectDirectSpacerRenderChildren(find.byType(Column));
      final defaultColumn = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$defaultId')),
      );
      final weightedColumn = tester.getSize(
        find.byKey(const ValueKey('canvas-widget-$weightedId')),
      );
      expect(defaultColumn.width, 0);
      expect(defaultColumn.height, closeTo(80 / 3, 0.01));
      expect(weightedColumn.width, 0);
      expect(weightedColumn.height, closeTo(160 / 3, 0.01));

      await pump('flutter.widgets.Row', stretchCrossAxis: true);
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$defaultId')))
            .height,
        120,
      );
      expect(
        find.byKey(const ValueKey('canvas-zero-size-widget-target-$defaultId')),
        findsOneWidget,
        reason: 'stretched Spacer still needs external safe instrumentation',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('offers flex wrapper zones on existing Row children only', (
    tester,
  ) async {
    const rowId = 'be4b446d-12b9-42c3-a427-03fb2fd472bd';
    const firstId = '2c65ab83-02e9-4100-b116-2485762440d9';
    const secondId = '294cdd5d-c142-4b93-9856-1b8497cc6bc7';
    const spacerId = '40ab557c-e837-420f-b66a-25ae9ebec96f';
    CanvasDropResolver? resolver;
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithFixedFlex(
              parentType: 'flutter.widgets.Row',
              parentId: rowId,
              children: [
                _viewSizedBoxNode(firstId, width: 80, height: 30),
                _viewSizedBoxNode(secondId, width: 60, height: 30),
                _viewSpacerNode(spacerId),
              ],
            ),
          ),
        ),
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(
        model: model,
        selectedWidgetId: null,
        onSelected: (_) {},
        onDropResolverChanged: (value) => resolver = value,
      ),
    );
    await tester.pump();

    final surface = tester.getRect(find.byType(CanvasDocumentView));
    CanvasDropTarget? resolveAt(
      String widgetId,
      CanvasPaletteDragSource source,
    ) {
      final point = tester
          .getRect(find.byKey(ValueKey('canvas-widget-$widgetId')))
          .center;
      return resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        source,
      );
    }

    for (final widgetType in <String>[
      canvasExpandedWidgetType,
      canvasFlexibleWidgetType,
    ]) {
      final source = CanvasPaletteDragSource(
        token: '$widgetType-source',
        widgetType: widgetType,
        traits: const {},
      );
      final first = resolveAt(firstId, source);
      expect(first?.parentWidgetId, rowId);
      expect(first?.slotName, 'children');
      expect(first?.insertionIndex, 0);
      expect(first?.zone?.isEmpty, isFalse);

      final second = resolveAt(secondId, source);
      expect(second?.parentWidgetId, rowId);
      expect(second?.slotName, 'children');
      expect(second?.insertionIndex, 1);
      expect(second?.zone?.isEmpty, isFalse);

      expect(
        resolveAt(spacerId, source),
        isNull,
        reason: '$widgetType cannot wrap Spacer',
      );
    }

    final normalSource = CanvasPaletteDragSource(
      token: 'text-source',
      widgetType: 'flutter.widgets.Text',
      traits: const {},
    );
    final normal = resolver!(
      ((tester
                      .getRect(
                        find.byKey(const ValueKey('canvas-widget-$rowId')),
                      )
                      .right -
                  2 -
                  surface.left) /
              surface.width *
              1000000)
          .round(),
      ((tester
                      .getRect(
                        find.byKey(const ValueKey('canvas-widget-$rowId')),
                      )
                      .center
                      .dy -
                  surface.top) /
              surface.height *
              1000000)
          .round(),
      normalSource,
    );
    expect(normal?.parentWidgetId, rowId);
    expect(normal?.insertionIndex, 3);
  });

  testWidgets(
    'offers required-child wrapper zones for occupied reviewed non-root slots only',
    (tester) async {
      const centerId = '4e7b1056-bc85-48b4-bbc1-aa27b8671508';
      const rowId = 'af1561d7-ee2e-4c02-9fb9-7af2eac80bd2';
      const expandedId = 'f2128b64-9006-47ce-b08b-ff4878274c43';
      const targetId = '56826883-284c-40d5-99ee-63bf85feca79';
      CanvasDropResolver? resolver;
      Future<CanvasDropTarget?> pumpAndResolve(
        Map<String, Object?> root,
        String renderedTargetId, {
        String wrapperWidgetType = canvasSafeAreaWidgetType,
      }) async {
        final json = _modelJsonForView();
        json['root'] = root;
        resolver = null;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
          ),
        );
        await tester.pump();
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final point = tester
            .getRect(find.byKey(ValueKey('canvas-widget-$renderedTargetId')))
            .center;
        return resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          CanvasPaletteDragSource(
            token: '$wrapperWidgetType-source',
            widgetType: wrapperWidgetType,
            traits: const {},
          ),
        );
      }

      final target = _viewSizedBoxNode(targetId, width: 80, height: 40);
      final singleRoot = <String, Object?>{
        'id': centerId,
        'type': 'flutter.widgets.Center',
        'properties': <String, Object?>{},
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': target},
        },
      };
      final single = await pumpAndResolve(singleRoot, targetId);
      expect(single?.parentWidgetId, centerId);
      expect(single?.slotName, 'child');
      expect(single?.insertionIndex, 0);
      final directionalSingle = await pumpAndResolve(
        singleRoot,
        targetId,
        wrapperWidgetType: canvasDirectionalityWidgetType,
      );
      expect(directionalSingle?.parentWidgetId, centerId);
      expect(directionalSingle?.slotName, 'child');
      expect(directionalSingle?.insertionIndex, 0);

      final list = await pumpAndResolve(<String, Object?>{
        'id': rowId,
        'type': 'flutter.widgets.Row',
        'properties': <String, Object?>{
          'mainAxisSize': {
            'kind': 'enum',
            'type': 'MainAxisSize',
            'value': 'min',
          },
        },
        'slots': <String, Object?>{
          'children': <String, Object?>{
            'kind': 'list',
            'children': <Map<String, Object?>>[
              target,
              _viewSizedBoxNode(
                '05fc09ef-4d8c-4587-a55f-f1fc0513a7bd',
                width: 60,
                height: 40,
              ),
            ],
          },
        },
      }, targetId);
      expect(list?.parentWidgetId, rowId);
      expect(list?.slotName, 'children');
      expect(list?.insertionIndex, 0);

      final required = await pumpAndResolve(<String, Object?>{
        'id': '2b310aec-90fc-4286-a248-2b0903ff2731',
        'type': 'flutter.widgets.Column',
        'properties': <String, Object?>{},
        'slots': <String, Object?>{
          'children': <String, Object?>{
            'kind': 'list',
            'children': <Map<String, Object?>>[
              <String, Object?>{
                'id': expandedId,
                'type': canvasExpandedWidgetType,
                'properties': <String, Object?>{},
                'slots': <String, Object?>{
                  'child': <String, Object?>{'kind': 'single', 'child': target},
                },
              },
            ],
          },
        },
      }, targetId);
      expect(required?.parentWidgetId, expandedId);
      expect(required?.slotName, 'child');
      expect(required?.insertionIndex, 0);

      final rootOnly = await pumpAndResolve(
        _viewTextNode(targetId, 'Root cannot be wrapped on this wire'),
        targetId,
      );
      expect(
        rootOnly,
        isNull,
        reason: 'CanvasDropTarget has no root target identity field',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders every reviewed direct Image argument with real Flutter',
    (tester) async {
      const imageId = '93e0b32e-cefa-43ea-8b73-209f9434980c';
      final resourceId = sha256Hex(_viewPng8);
      final resources = CanvasImageResourceBundle.fromResources([
        CanvasImageResource(
          resourceId: resourceId,
          mediaType: 'image/png',
          pixelWidth: 8,
          pixelHeight: 8,
          encodedBytes: _viewPng8,
        ),
      ]);
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredImage(
                id: imageId,
                properties: {
                  'image': _viewDirectImageProvider(
                    provider: _viewImageProvider(
                      kind: 'exactAsset',
                      assetName: 'assets/images/hero.png',
                      exactScale: 2,
                      resize: {
                        'width': 8,
                        'height': 8,
                        'policy': 'exact',
                        'allowUpscaling': false,
                      },
                      resolution: {
                        'kind': 'resolved',
                        'resourceId': resourceId,
                        'resolvedScale': 2,
                      },
                    ),
                  ),
                  'frameBuilder': {'kind': 'callbackPresence'},
                  'loadingBuilder': {'kind': 'callbackPresence'},
                  'errorBuilder': {'kind': 'callbackPresence'},
                  'semanticLabel': {'kind': 'string', 'value': 'Hero image'},
                  'excludeFromSemantics': {'kind': 'boolean', 'value': true},
                  'width': {'kind': 'integer', 'value': 120},
                  'height': {'kind': 'double', 'value': 80.0},
                  'color': {
                    'kind': 'themeToken',
                    'token': 'material.colorScheme.primary',
                  },
                  'opacity': {'kind': 'double', 'value': 0.6},
                  'colorBlendMode': {
                    'kind': 'enum',
                    'type': 'BlendMode',
                    'value': 'multiply',
                  },
                  'fit': {'kind': 'enum', 'type': 'BoxFit', 'value': 'fill'},
                  'alignment': _viewAlignment(
                    basis: 'directional',
                    horizontal: -1,
                    vertical: 0.25,
                  ),
                  'repeat': {
                    'kind': 'enum',
                    'type': 'ImageRepeat',
                    'value': 'repeatX',
                  },
                  'centerSliceLeft': {'kind': 'double', 'value': 1.0},
                  'centerSliceTop': {'kind': 'double', 'value': 1.0},
                  'centerSliceRight': {'kind': 'double', 'value': 3.0},
                  'centerSliceBottom': {'kind': 'double', 'value': 3.0},
                  'matchTextDirection': {'kind': 'boolean', 'value': true},
                  'gaplessPlayback': {'kind': 'boolean', 'value': true},
                  'isAntiAlias': {'kind': 'boolean', 'value': true},
                  'filterQuality': {
                    'kind': 'enum',
                    'type': 'FilterQuality',
                    'value': 'high',
                  },
                },
              ),
            ),
          ),
        ),
      );

      String? selected;
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          imageResources: resources,
          selectedWidgetId: imageId,
          onSelected: (value) => selected = value,
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$imageId'));
      final image = tester.widget<Image>(
        find.descendant(of: node, matching: find.byType(Image)).first,
      );
      expect(image.image, isA<ResizeImage>());
      final resized = image.image as ResizeImage;
      expect(resized.width, 8);
      expect(resized.height, 8);
      expect(resized.policy, ResizeImagePolicy.exact);
      expect(resized.allowUpscaling, isFalse);
      expect((resized.imageProvider as MemoryImage).scale, 2);
      expect(image.frameBuilder, isNotNull);
      expect(image.loadingBuilder, isNotNull);
      expect(image.errorBuilder, isNotNull);
      expect(image.semanticLabel, 'Hero image');
      expect(image.excludeFromSemantics, isTrue);
      expect(image.width, 120);
      expect(image.height, 80);
      expect(image.color, Theme.of(tester.element(node)).colorScheme.primary);
      expect(image.opacity!.value, 0.6);
      expect(image.colorBlendMode, BlendMode.multiply);
      expect(image.fit, BoxFit.fill);
      expect(image.alignment, const AlignmentDirectional(-1, 0.25));
      expect(image.repeat, ImageRepeat.repeatX);
      expect(image.centerSlice, const Rect.fromLTRB(1, 1, 3, 3));
      expect(image.matchTextDirection, isTrue);
      expect(image.gaplessPlayback, isTrue);
      expect(image.isAntiAlias, isTrue);
      expect(image.filterQuality, FilterQuality.high);
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$imageId')),
        findsOneWidget,
      );
      await tester.tap(node);
      expect(selected, imageId);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders direct Image unavailable placeholder without an unsafe centerSlice',
    (tester) async {
      const imageId = '93e0b32e-cefa-43ea-8b73-209f9434980c';
      const reason = 'The declared project image does not exist';
      final semantics = tester.ensureSemantics();
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredImage(
                id: imageId,
                properties: {
                  'image': _viewDirectImageProvider(
                    provider: _viewImageProvider(
                      assetName: 'assets/images/missing.png',
                      packageName: 'image_pack',
                      resolution: {
                        'kind': 'unavailable',
                        'code': 'missing',
                        'reason': reason,
                      },
                    ),
                  ),
                  'width': {'kind': 'integer', 'value': 120},
                  'height': {'kind': 'integer', 'value': 80},
                  'centerSliceLeft': {'kind': 'double', 'value': 1.0},
                  'centerSliceTop': {'kind': 'double', 'value': 1.0},
                  'centerSliceRight': {'kind': 'double', 'value': 3.0},
                  'centerSliceBottom': {'kind': 'double', 'value': 3.0},
                },
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$imageId'));
      final image = tester.widget<Image>(
        find.descendant(of: node, matching: find.byType(Image)).first,
      );
      expect(image.image, isA<MemoryImage>());
      expect(image.centerSlice, isNull);
      expect(
        find.bySemanticsLabel(
          RegExp(
            'Image preview unavailable for '
            'package:image_pack:assets/images/missing.png.*'
            'Status missing.*Reason: ${RegExp.escape(reason)}',
          ),
        ),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
      semantics.dispose();
    },
  );

  testWidgets(
    'renders all reviewed TextField arguments without editable state',
    (tester) async {
      const textFieldId = '73d9ec43-3d37-4304-8998-71fe51804284';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredTextField(
                id: textFieldId,
                properties: {
                  'keyboardType': {
                    'kind': 'string',
                    'value': 'numberSignedDecimal',
                  },
                  'textInputAction': {
                    'kind': 'enum',
                    'type': 'TextInputAction',
                    'value': 'done',
                  },
                  'textCapitalization': {
                    'kind': 'enum',
                    'type': 'TextCapitalization',
                    'value': 'words',
                  },
                  'textAlign': {
                    'kind': 'enum',
                    'type': 'TextAlign',
                    'value': 'center',
                  },
                  'textAlignVertical': {'kind': 'string', 'value': 'bottom'},
                  'textDirection': {
                    'kind': 'enum',
                    'type': 'TextDirection',
                    'value': 'ltr',
                  },
                  'readOnly': {'kind': 'boolean', 'value': true},
                  'showCursor': {'kind': 'boolean', 'value': false},
                  'autofocus': {'kind': 'boolean', 'value': true},
                  'obscuringCharacter': {'kind': 'string', 'value': '#'},
                  'obscureText': {'kind': 'boolean', 'value': false},
                  'autocorrect': {'kind': 'boolean', 'value': false},
                  'smartDashesType': {
                    'kind': 'enum',
                    'type': 'SmartDashesType',
                    'value': 'disabled',
                  },
                  'smartQuotesType': {
                    'kind': 'enum',
                    'type': 'SmartQuotesType',
                    'value': 'enabled',
                  },
                  'enableSuggestions': {'kind': 'boolean', 'value': false},
                  'maxLines': {'kind': 'integer', 'value': 4},
                  'minLines': {'kind': 'integer', 'value': 2},
                  'expands': {'kind': 'boolean', 'value': false},
                  'maxLength': {'kind': 'integer', 'value': -1},
                  'maxLengthEnforcement': {
                    'kind': 'enum',
                    'type': 'MaxLengthEnforcement',
                    'value': 'truncateAfterCompositionEnds',
                  },
                  'onChanged': {'kind': 'callbackPresence'},
                  'onEditingComplete': {'kind': 'callbackPresence'},
                  'onSubmitted': {'kind': 'callbackPresence'},
                  'onAppPrivateCommand': {'kind': 'callbackPresence'},
                  'enabled': {'kind': 'boolean', 'value': true},
                  'ignorePointers': {'kind': 'boolean', 'value': false},
                  'cursorWidth': {'kind': 'integer', 'value': 3},
                  'cursorHeight': {'kind': 'double', 'value': 22.5},
                  'cursorRadiusX': {'kind': 'double', 'value': 4.0},
                  'cursorRadiusY': {'kind': 'double', 'value': 6.0},
                  'cursorOpacityAnimates': {'kind': 'boolean', 'value': true},
                  'cursorColor': {
                    'kind': 'themeToken',
                    'token': 'material.colorScheme.primary',
                  },
                  'cursorErrorColor': {'kind': 'color', 'argb': '0xFFAA1122'},
                  'selectionHeightStyle': {
                    'kind': 'enum',
                    'type': 'BoxHeightStyle',
                    'value': 'includeLineSpacingBottom',
                  },
                  'selectionWidthStyle': {
                    'kind': 'enum',
                    'type': 'BoxWidthStyle',
                    'value': 'max',
                  },
                  'keyboardAppearance': {
                    'kind': 'enum',
                    'type': 'Brightness',
                    'value': 'dark',
                  },
                  'scrollPaddingLeft': {'kind': 'double', 'value': 1.0},
                  'scrollPaddingTop': {'kind': 'double', 'value': 2.0},
                  'scrollPaddingRight': {'kind': 'double', 'value': 3.0},
                  'scrollPaddingBottom': {'kind': 'double', 'value': 4.0},
                  'dragStartBehavior': {
                    'kind': 'enum',
                    'type': 'DragStartBehavior',
                    'value': 'down',
                  },
                  'enableInteractiveSelection': {
                    'kind': 'boolean',
                    'value': false,
                  },
                  'selectAllOnFocus': {'kind': 'boolean', 'value': true},
                  'onTap': {'kind': 'callbackPresence'},
                  'onTapAlwaysCalled': {'kind': 'boolean', 'value': true},
                  'onTapOutside': {'kind': 'callbackPresence'},
                  'onTapUpOutside': {'kind': 'callbackPresence'},
                  'mouseCursor': {'kind': 'string', 'value': 'resizeColumn'},
                  'clipBehavior': {
                    'kind': 'enum',
                    'type': 'Clip',
                    'value': 'antiAlias',
                  },
                  'restorationId': {'kind': 'string', 'value': 'profile-name'},
                  'stylusHandwritingEnabled': {
                    'kind': 'boolean',
                    'value': false,
                  },
                  'enableIMEPersonalizedLearning': {
                    'kind': 'boolean',
                    'value': false,
                  },
                  'enableInlinePrediction': {'kind': 'boolean', 'value': true},
                  'canRequestFocus': {'kind': 'boolean', 'value': true},
                },
              ),
            ),
          ),
        ),
      );

      String? selected;
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (value) => selected = value,
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$textFieldId'));
      final finder = find.descendant(
        of: node,
        matching: find.byType(TextField),
      );
      final field = tester.widget<TextField>(finder);
      expect(
        field.keyboardType,
        const TextInputType.numberWithOptions(signed: true, decimal: true),
      );
      expect(field.textInputAction, TextInputAction.done);
      expect(field.textCapitalization, TextCapitalization.words);
      expect(field.textAlign, TextAlign.center);
      expect(field.textAlignVertical, TextAlignVertical.bottom);
      expect(field.textDirection, TextDirection.ltr);
      expect(field.readOnly, isTrue);
      expect(field.showCursor, isFalse);
      expect(field.autofocus, isTrue);
      expect(field.obscuringCharacter, '#');
      expect(field.obscureText, isFalse);
      expect(field.autocorrect, isFalse);
      expect(field.smartDashesType, SmartDashesType.disabled);
      expect(field.smartQuotesType, SmartQuotesType.enabled);
      expect(field.enableSuggestions, isFalse);
      expect(field.maxLines, 4);
      expect(field.minLines, 2);
      expect(field.expands, isFalse);
      expect(field.maxLength, TextField.noMaxLength);
      expect(
        field.maxLengthEnforcement,
        MaxLengthEnforcement.truncateAfterCompositionEnds,
      );
      expect(field.onChanged, isNotNull);
      expect(field.onEditingComplete, isNotNull);
      expect(field.onSubmitted, isNotNull);
      expect(field.onAppPrivateCommand, isNotNull);
      expect(field.enabled, isTrue);
      expect(field.ignorePointers, isFalse);
      expect(field.cursorWidth, 3);
      expect(field.cursorHeight, 22.5);
      expect(field.cursorRadius, const Radius.elliptical(4, 6));
      expect(field.cursorOpacityAnimates, isTrue);
      expect(
        field.cursorColor,
        Theme.of(tester.element(finder)).colorScheme.primary,
      );
      expect(field.cursorErrorColor, const Color(0xffaa1122));
      expect(
        field.selectionHeightStyle,
        ui.BoxHeightStyle.includeLineSpacingBottom,
      );
      expect(field.selectionWidthStyle, ui.BoxWidthStyle.max);
      expect(field.keyboardAppearance, Brightness.dark);
      expect(field.scrollPadding, const EdgeInsets.fromLTRB(1, 2, 3, 4));
      expect(field.dragStartBehavior, DragStartBehavior.down);
      expect(field.enableInteractiveSelection, isFalse);
      expect(field.selectAllOnFocus, isTrue);
      expect(field.onTap, isNotNull);
      expect(field.onTapAlwaysCalled, isTrue);
      expect(field.onTapOutside, isNotNull);
      expect(field.onTapUpOutside, isNotNull);
      expect(field.mouseCursor, SystemMouseCursors.resizeColumn);
      expect(field.clipBehavior, Clip.antiAlias);
      expect(field.restorationId, 'profile-name');
      expect(field.stylusHandwritingEnabled, isFalse);
      expect(field.enableIMEPersonalizedLearning, isFalse);
      expect(field.enableInlinePrediction, isTrue);
      expect(field.canRequestFocus, isTrue);
      expect(field.controller, isNull);
      expect(field.focusNode, isNull);

      final ignorePointers = find.ancestor(
        of: finder,
        matching: find.byType(IgnorePointer),
      );
      expect(ignorePointers, findsWidgets);
      expect(
        ignorePointers
            .evaluate()
            .map((element) => element.widget)
            .whereType<IgnorePointer>(),
        contains(predicate<IgnorePointer>((widget) => widget.ignoring)),
      );
      final excludeFocus = find.ancestor(
        of: finder,
        matching: find.byType(ExcludeFocus),
      );
      expect(excludeFocus, findsOneWidget);
      expect(tester.widget<ExcludeFocus>(excludeFocus).excluding, isTrue);
      final focusGuards = find
          .ancestor(of: finder, matching: find.byType(Focus))
          .evaluate()
          .map((element) => element.widget)
          .whereType<Focus>();
      expect(
        focusGuards,
        contains(
          predicate<Focus>(
            (widget) =>
                !widget.canRequestFocus &&
                widget.skipTraversal &&
                !widget.descendantsAreFocusable &&
                !widget.descendantsAreTraversable,
          ),
        ),
      );
      final editable = tester.widget<EditableText>(
        find.descendant(of: finder, matching: find.byType(EditableText)),
      );
      expect(editable.focusNode.hasFocus, isFalse);
      await tester.tap(node);
      await tester.pump();
      expect(selected, textFieldId);
      expect(editable.focusNode.hasFocus, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('bounds TextField behind unbounded flex intermediaries', (
    tester,
  ) async {
    Future<Size> render({
      required String parentType,
      required Map<String, Object?> properties,
    }) async {
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithTextFieldInFlex(
                parentType: parentType,
                properties: properties,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      return tester.getSize(find.byType(TextField));
    }

    final rowSize = await render(
      parentType: 'flutter.widgets.Row',
      properties: {},
    );
    expect(rowSize.width, 240);
    expect(rowSize.height, greaterThan(0));

    final columnSize = await render(
      parentType: 'flutter.widgets.Column',
      properties: {
        'expands': {'kind': 'boolean', 'value': true},
      },
    );
    expect(columnSize.height, 120);
    expect(columnSize.width, greaterThan(0));
    final expanded = tester.widget<TextField>(find.byType(TextField));
    expect(expanded.expands, isTrue);
    expect(expanded.maxLines, isNull);
    expect(expanded.minLines, isNull);
    expect(tester.takeException(), isNull);
  });

  testWidgets('preserves pinned TextField defaults when leaves are omitted', (
    tester,
  ) async {
    const textFieldId = '73d9ec43-3d37-4304-8998-71fe51804284';
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithCenteredTextField(id: textFieldId, properties: {}),
          ),
        ),
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );
    await tester.pump();

    final field = tester.widget<TextField>(find.byType(TextField));
    expect(field.keyboardType, TextInputType.text);
    expect(field.textInputAction, isNull);
    expect(field.textCapitalization, TextCapitalization.none);
    expect(field.textAlign, TextAlign.start);
    expect(field.textAlignVertical, isNull);
    expect(field.textDirection, isNull);
    expect(field.readOnly, isFalse);
    expect(field.showCursor, isNull);
    expect(field.autofocus, isFalse);
    expect(field.obscuringCharacter, '•');
    expect(field.obscureText, isFalse);
    expect(field.autocorrect, isNull);
    expect(field.smartDashesType, SmartDashesType.enabled);
    expect(field.smartQuotesType, SmartQuotesType.enabled);
    expect(field.enableSuggestions, isTrue);
    expect(field.maxLines, 1);
    expect(field.minLines, isNull);
    expect(field.expands, isFalse);
    expect(field.maxLength, isNull);
    expect(field.maxLengthEnforcement, isNull);
    expect(field.enabled, isNull);
    expect(field.ignorePointers, isNull);
    expect(field.cursorWidth, 2);
    expect(field.cursorHeight, isNull);
    expect(field.cursorRadius, isNull);
    expect(field.cursorOpacityAnimates, isNull);
    expect(field.cursorColor, isNull);
    expect(field.cursorErrorColor, isNull);
    expect(field.keyboardAppearance, isNull);
    expect(field.scrollPadding, const EdgeInsets.all(20));
    expect(field.dragStartBehavior, DragStartBehavior.start);
    expect(field.enableInteractiveSelection, isTrue);
    expect(field.selectAllOnFocus, isNull);
    expect(field.onTapAlwaysCalled, isFalse);
    expect(field.mouseCursor, isNull);
    expect(field.clipBehavior, Clip.hardEdge);
    expect(field.restorationId, isNull);
    expect(
      field.stylusHandwritingEnabled,
      EditableText.defaultStylusHandwritingEnabled,
    );
    expect(field.enableIMEPersonalizedLearning, isTrue);
    expect(field.enableInlinePrediction, isNull);
    expect(field.canRequestFocus, isTrue);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'renders real ListBody defaults and exact horizontal reverse arguments',
    (tester) async {
      const listBodyId = 'd79aaf47-73ef-43ee-943b-c2d44b92391d';
      const firstId = '1b7b8bc1-f72e-4059-8d60-a32add0a62ef';
      const secondId = '493448dd-35a8-42ed-96b5-e14dff0764a2';
      final children = [
        _viewSizedBoxNode(firstId, width: 40, height: 30),
        _viewSizedBoxNode(secondId, width: 40, height: 30),
      ];

      Future<void> pump(Map<String, Object?> properties) async {
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(
                utf8.encode(
                  jsonEncode(
                    _modelWithListBody(
                      properties: properties,
                      children: children,
                    ),
                  ),
                ),
              ),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
      }

      await pump(const {});
      final node = find.byKey(const ValueKey('canvas-widget-$listBodyId'));
      ListBody listBody() => tester.widget<ListBody>(
        find.descendant(of: node, matching: find.byType(ListBody)).first,
      );
      SingleChildScrollView viewport() => tester.widget<SingleChildScrollView>(
        find
            .descendant(of: node, matching: find.byType(SingleChildScrollView))
            .first,
      );

      expect(listBody().mainAxis, Axis.vertical);
      expect(listBody().reverse, isFalse);
      expect(listBody().children, hasLength(2));
      expect(viewport().scrollDirection, Axis.vertical);
      expect(viewport().reverse, isFalse);
      expect(viewport().primary, isFalse);
      final defaultFirst = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$firstId')),
      );
      final defaultSecond = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$secondId')),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$firstId')))
            .width,
        closeTo(300, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$secondId')))
            .width,
        closeTo(300, 0.01),
      );
      expect(defaultFirst.top, lessThan(defaultSecond.top));
      expect(tester.takeException(), isNull);

      await pump(const {
        'mainAxis': {'kind': 'enum', 'type': 'Axis', 'value': 'horizontal'},
        'reverse': {'kind': 'boolean', 'value': true},
      });
      expect(listBody().mainAxis, Axis.horizontal);
      expect(listBody().reverse, isTrue);
      expect(viewport().scrollDirection, Axis.horizontal);
      expect(viewport().reverse, isTrue);
      final render = tester.renderObject<RenderListBody>(
        find.descendant(of: node, matching: find.byType(ListBody)).first,
      );
      expect(render.axisDirection, AxisDirection.left);
      final reversedFirst = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$firstId')),
      );
      final reversedSecond = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$secondId')),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$firstId')))
            .height,
        closeTo(160, 0.01),
      );
      expect(
        tester
            .getSize(find.byKey(const ValueKey('canvas-widget-$secondId')))
            .height,
        closeTo(160, 0.01),
      );
      expect(reversedFirst.left, greaterThan(reversedSecond.left));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'gives ListBody an unbounded main axis and a finite cross axis in every flex placement',
    (tester) async {
      const cases = <({String parentType, Axis axis})>[
        (parentType: 'flutter.widgets.Column', axis: Axis.vertical),
        (parentType: 'flutter.widgets.Row', axis: Axis.vertical),
        (parentType: 'flutter.widgets.Row', axis: Axis.horizontal),
        (parentType: 'flutter.widgets.Column', axis: Axis.horizontal),
      ];

      for (final entry in cases) {
        final properties = <String, Object?>{
          if (entry.axis == Axis.horizontal)
            'mainAxis': {'kind': 'enum', 'type': 'Axis', 'value': 'horizontal'},
        };
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(
                utf8.encode(
                  jsonEncode(
                    _modelWithListBody(
                      properties: properties,
                      children: [
                        _viewSizedBoxNode(
                          '1b7b8bc1-f72e-4059-8d60-a32add0a62ef',
                          width: 40,
                          height: 30,
                        ),
                        _viewSizedBoxNode(
                          '493448dd-35a8-42ed-96b5-e14dff0764a2',
                          width: 40,
                          height: 30,
                        ),
                      ],
                      unboundedParentType: entry.parentType,
                    ),
                  ),
                ),
              ),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();

        final finder = find.byType(ListBody);
        final render = tester.renderObject<RenderListBody>(finder);
        final size = tester.getSize(finder);
        expect(size.width.isFinite, isTrue, reason: entry.toString());
        expect(size.height.isFinite, isTrue, reason: entry.toString());
        if (entry.axis == Axis.vertical) {
          expect(
            render.constraints.hasBoundedHeight,
            isFalse,
            reason: entry.toString(),
          );
          expect(
            render.constraints.hasBoundedWidth,
            isTrue,
            reason: entry.toString(),
          );
        } else {
          expect(
            render.constraints.hasBoundedWidth,
            isFalse,
            reason: entry.toString(),
          );
          expect(
            render.constraints.hasBoundedHeight,
            isTrue,
            reason: entry.toString(),
          );
        }
        if (entry.parentType == 'flutter.widgets.Row' &&
            entry.axis == Axis.vertical) {
          expect(size.width, closeTo(240, 0.01));
        }
        if (entry.parentType == 'flutter.widgets.Column' &&
            entry.axis == Axis.horizontal) {
          expect(size.height, closeTo(120, 0.01));
        }
        expect(tester.takeException(), isNull, reason: entry.toString());
      }
    },
  );

  testWidgets(
    'keeps empty ListBody nodes concrete, selectable, and appendable in Column and Row',
    (tester) async {
      const listBodyId = 'd79aaf47-73ef-43ee-943b-c2d44b92391d';
      const cases = <({String parentType, Axis axis})>[
        (parentType: 'flutter.widgets.Column', axis: Axis.vertical),
        (parentType: 'flutter.widgets.Row', axis: Axis.horizontal),
      ];

      for (final entry in cases) {
        String? selectedWidgetId;
        CanvasDropResolver? resolver;
        final properties = <String, Object?>{
          if (entry.axis == Axis.horizontal)
            'mainAxis': {'kind': 'enum', 'type': 'Axis', 'value': 'horizontal'},
        };
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithListBody(
                  properties: properties,
                  children: const [],
                  unboundedParentType: entry.parentType,
                ),
              ),
            ),
          ),
        );

        await tester.pumpWidget(
          StatefulBuilder(
            builder: (context, setState) => CanvasModelApp(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();

        final node = find.byKey(const ValueKey('canvas-widget-$listBodyId'));
        final size = tester.getSize(node);
        expect(size.isEmpty, isFalse, reason: entry.toString());
        expect(
          entry.axis == Axis.vertical ? size.height : size.width,
          greaterThanOrEqualTo(36),
          reason: entry.toString(),
        );
        await tester.tap(node);
        await tester.pump();
        expect(selectedWidgetId, listBodyId, reason: entry.toString());

        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final point = tester.getRect(node).center;
        final drop = resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
        expect(drop?.parentWidgetId, listBodyId, reason: entry.toString());
        expect(drop?.slotName, 'children', reason: entry.toString());
        expect(drop?.insertionIndex, 0, reason: entry.toString());
        expect(drop?.zone?.isEmpty, isFalse, reason: entry.toString());
        expect(tester.takeException(), isNull, reason: entry.toString());
      }
    },
  );

  testWidgets(
    'resolves ListBody append and move edges for both axes, reverse, and RTL',
    (tester) async {
      const listBodyId = 'd79aaf47-73ef-43ee-943b-c2d44b92391d';
      const firstId = '1b7b8bc1-f72e-4059-8d60-a32add0a62ef';
      const secondId = '493448dd-35a8-42ed-96b5-e14dff0764a2';
      const cases = <({Axis axis, bool reverse, TextDirection direction})>[
        (axis: Axis.vertical, reverse: false, direction: TextDirection.ltr),
        (axis: Axis.vertical, reverse: true, direction: TextDirection.ltr),
        (axis: Axis.horizontal, reverse: false, direction: TextDirection.ltr),
        (axis: Axis.horizontal, reverse: true, direction: TextDirection.ltr),
        (axis: Axis.horizontal, reverse: false, direction: TextDirection.rtl),
        (axis: Axis.horizontal, reverse: true, direction: TextDirection.rtl),
      ];

      for (final entry in cases) {
        final properties = <String, Object?>{
          if (entry.axis == Axis.horizontal)
            'mainAxis': {'kind': 'enum', 'type': 'Axis', 'value': 'horizontal'},
          'reverse': {'kind': 'boolean', 'value': entry.reverse},
        };
        final json = _modelWithListBody(
          properties: properties,
          children: [
            _viewSizedBoxNode(firstId, width: 40, height: 30),
            _viewSizedBoxNode(secondId, width: 40, height: 30),
          ],
        );
        (json['profile']! as Map<String, Object?>)['locale'] =
            entry.direction == TextDirection.rtl ? 'ar-SA' : 'en-US';
        CanvasDropResolver? dropResolver;
        CanvasMovePreviewResolver? moveResolver;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => dropResolver = value,
            onMovePreviewResolverChanged: (value) => moveResolver = value,
          ),
        );
        await tester.pump();

        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final listRect = tester.getRect(
          find.byKey(const ValueKey('canvas-widget-$listBodyId')),
        );
        final retainedRect = tester.getRect(
          find.byKey(const ValueKey('canvas-widget-$secondId')),
        );
        final visuallyReversed = entry.axis == Axis.vertical
            ? entry.reverse
            : (entry.direction == TextDirection.rtl) != entry.reverse;
        final terminalPoint = entry.axis == Axis.vertical
            ? Offset(
                listRect.center.dx,
                visuallyReversed ? listRect.top + 1 : listRect.bottom - 1,
              )
            : Offset(
                visuallyReversed ? listRect.left + 1 : listRect.right - 1,
                listRect.center.dy,
              );
        int micros(double value, double origin, double extent) =>
            ((value - origin) / extent * 1000000).round();
        final terminal = dropResolver!(
          micros(terminalPoint.dx, surface.left, surface.width),
          micros(terminalPoint.dy, surface.top, surface.height),
        );
        expect(terminal?.parentWidgetId, listBodyId, reason: entry.toString());
        expect(terminal?.slotName, 'children', reason: entry.toString());
        expect(terminal?.insertionIndex, 2, reason: entry.toString());
        expect(terminal?.zone?.isEmpty, isFalse, reason: entry.toString());

        final move = moveResolver!(firstId, listBodyId, 'children', 1);
        expect(move?.parentWidgetId, listBodyId, reason: entry.toString());
        expect(move?.slotName, 'children', reason: entry.toString());
        expect(move?.insertionIndex, 1, reason: entry.toString());
        expect(move?.zone?.isEmpty, isFalse, reason: entry.toString());
        final zone = move!.zone!;
        final actualMoveEdge = entry.axis == Axis.vertical
            ? (zone.topMicros + zone.bottomMicros) ~/ 2
            : (zone.leftMicros + zone.rightMicros) ~/ 2;
        final physicalEdge = entry.axis == Axis.vertical
            ? (visuallyReversed ? retainedRect.top : retainedRect.bottom)
            : (visuallyReversed ? retainedRect.left : retainedRect.right);
        final expectedMoveEdge = entry.axis == Axis.vertical
            ? micros(physicalEdge, surface.top, surface.height)
            : micros(physicalEdge, surface.left, surface.width);
        expect(
          actualMoveEdge,
          closeTo(expectedMoveEdge, 2),
          reason: entry.toString(),
        );
        expect(tester.takeException(), isNull, reason: entry.toString());
      }
    },
  );

  testWidgets('renders real OverflowBar defaults and every exact argument', (
    tester,
  ) async {
    const overflowBarId = '765940f9-e63f-46d8-8837-07c52904c9b9';
    final children = [
      _viewTextNode('1120bc37-90cf-4980-8ee5-611c0ca6b876', 'First action'),
      _viewTextNode('e02c34d9-1ca6-4373-b074-79b3a8052ce8', 'OK'),
    ];

    Future<void> pump(Map<String, Object?> properties) async {
      await tester.pumpWidget(
        CanvasModelApp(
          model: CanvasModel.decode(
            Uint8List.fromList(
              utf8.encode(
                jsonEncode(
                  _modelWithOverflowBar(
                    width: 300,
                    properties: properties,
                    children: children,
                  ),
                ),
              ),
            ),
          ),
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
    }

    await pump(const {});
    final node = find.byKey(const ValueKey('canvas-widget-$overflowBarId'));
    OverflowBar overflowBar() => tester.widget<OverflowBar>(
      find.descendant(of: node, matching: find.byType(OverflowBar)).first,
    );
    expect(overflowBar().spacing, 0);
    expect(overflowBar().alignment, isNull);
    expect(overflowBar().overflowSpacing, 0);
    expect(overflowBar().overflowAlignment, OverflowBarAlignment.start);
    expect(overflowBar().overflowDirection, VerticalDirection.down);
    expect(overflowBar().textDirection, isNull);
    expect(overflowBar().children, hasLength(2));
    expect(tester.takeException(), isNull);

    await pump(const {
      'spacing': {'kind': 'double', 'value': -4.5},
      'alignment': {
        'kind': 'enum',
        'type': 'MainAxisAlignment',
        'value': 'spaceEvenly',
      },
      'overflowSpacing': {'kind': 'double', 'value': -2.25},
      'overflowAlignment': {
        'kind': 'enum',
        'type': 'OverflowBarAlignment',
        'value': 'center',
      },
      'overflowDirection': {
        'kind': 'enum',
        'type': 'VerticalDirection',
        'value': 'up',
      },
      'textDirection': {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': 'rtl',
      },
    });
    expect(overflowBar().spacing, -4.5);
    expect(overflowBar().alignment, MainAxisAlignment.spaceEvenly);
    expect(overflowBar().overflowSpacing, -2.25);
    expect(overflowBar().overflowAlignment, OverflowBarAlignment.center);
    expect(overflowBar().overflowDirection, VerticalDirection.up);
    expect(overflowBar().textDirection, TextDirection.rtl);
    expect(overflowBar().children, hasLength(2));
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'keeps OverflowBar finite in bounded, Row, and Column placements',
    (tester) async {
      const overflowBarId = '765940f9-e63f-46d8-8837-07c52904c9b9';
      const cases = <String?>[
        null,
        'flutter.widgets.Row',
        'flutter.widgets.Column',
      ];
      for (final parentType in cases) {
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(
                utf8.encode(
                  jsonEncode(
                    _modelWithOverflowBar(
                      width: 300,
                      parentType: parentType,
                      properties: const {
                        'alignment': {
                          'kind': 'enum',
                          'type': 'MainAxisAlignment',
                          'value': 'spaceBetween',
                        },
                      },
                      children: [
                        _viewTextNode(
                          '1120bc37-90cf-4980-8ee5-611c0ca6b876',
                          'A deliberately wide first action',
                        ),
                        _viewTextNode(
                          'e02c34d9-1ca6-4373-b074-79b3a8052ce8',
                          'Second action',
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();

        final finder = find.descendant(
          of: find.byKey(const ValueKey('canvas-widget-$overflowBarId')),
          matching: find.byType(OverflowBar),
        );
        final render = tester.renderObject<RenderBox>(finder.first);
        final size = tester.getSize(finder.first);
        expect(render.constraints.hasBoundedWidth, isTrue, reason: parentType);
        expect(size.width.isFinite, isTrue, reason: parentType);
        expect(size.height.isFinite, isTrue, reason: parentType);
        if (parentType == 'flutter.widgets.Row') {
          expect(render.constraints.maxWidth, lessThanOrEqualTo(240));
        }
        expect(tester.takeException(), isNull, reason: parentType);
      }
    },
  );

  testWidgets(
    'keeps null-aligned OverflowBar natural and horizontal in an unbounded Row',
    (tester) async {
      const overflowBarId = '765940f9-e63f-46d8-8837-07c52904c9b9';
      const firstId = '1120bc37-90cf-4980-8ee5-611c0ca6b876';
      const secondId = 'e02c34d9-1ca6-4373-b074-79b3a8052ce8';
      CanvasDropResolver? resolver;
      await tester.pumpWidget(
        CanvasModelApp(
          model: CanvasModel.decode(
            Uint8List.fromList(
              utf8.encode(
                jsonEncode(
                  _modelWithOverflowBar(
                    width: 300,
                    parentType: 'flutter.widgets.Row',
                    properties: const {},
                    children: [
                      _viewTextNode(firstId, 'Wide action'),
                      _viewTextNode(secondId, 'Continue'),
                    ],
                  ),
                ),
              ),
            ),
          ),
          selectedWidgetId: null,
          onSelected: (_) {},
          onDropResolverChanged: (value) => resolver = value,
        ),
      );
      await tester.pump();

      final bar = find.byKey(const ValueKey('canvas-widget-$overflowBarId'));
      final overflowBar = find.descendant(
        of: bar,
        matching: find.byType(OverflowBar),
      );
      final render = tester.renderObject<RenderBox>(overflowBar.first);
      final barRect = tester.getRect(bar);
      final firstRect = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$firstId')),
      );
      final secondRect = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$secondId')),
      );
      expect(render.constraints.hasBoundedWidth, isFalse);
      expect(render.size.width, greaterThan(240));
      expect(firstRect.left, closeTo(barRect.left, 0.01));
      expect(secondRect.left, closeTo(firstRect.right, 0.01));

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      int micros(double value, double origin, double extent) =>
          ((value - origin) / extent * 1000000).round();
      final drop = resolver!(
        micros(barRect.right - 1, surface.left, surface.width),
        micros(barRect.center.dy, surface.top, surface.height),
      );
      expect(drop?.parentWidgetId, overflowBarId);
      expect(drop?.slotName, 'children');
      expect(drop?.insertionIndex, 2);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps empty OverflowBar concrete, selectable, and appendable in Row and Column',
    (tester) async {
      const overflowBarId = '765940f9-e63f-46d8-8837-07c52904c9b9';
      for (final parentType in const [
        'flutter.widgets.Row',
        'flutter.widgets.Column',
      ]) {
        String? selectedWidgetId;
        CanvasDropResolver? resolver;
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithOverflowBar(
                  width: 300,
                  parentType: parentType,
                  properties: const {
                    'alignment': {
                      'kind': 'enum',
                      'type': 'MainAxisAlignment',
                      'value': 'spaceAround',
                    },
                  },
                  children: const [],
                ),
              ),
            ),
          ),
        );

        await tester.pumpWidget(
          StatefulBuilder(
            builder: (context, setState) => CanvasModelApp(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();

        final node = find.byKey(const ValueKey('canvas-widget-$overflowBarId'));
        final size = tester.getSize(node);
        expect(size, const Size(36, 36), reason: parentType);
        await tester.tap(node);
        await tester.pump();
        expect(selectedWidgetId, overflowBarId, reason: parentType);

        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final point = tester.getRect(node).center;
        final drop = resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
        expect(drop?.parentWidgetId, overflowBarId, reason: parentType);
        expect(drop?.slotName, 'children', reason: parentType);
        expect(drop?.insertionIndex, 0, reason: parentType);
        expect(drop?.zone?.isEmpty, isFalse, reason: parentType);
        expect(tester.takeException(), isNull, reason: parentType);
      }
    },
  );

  testWidgets(
    'resolves OverflowBar fit and overflow append and move edges for LTR RTL up and down',
    (tester) async {
      const overflowBarId = '765940f9-e63f-46d8-8837-07c52904c9b9';
      const firstId = '1120bc37-90cf-4980-8ee5-611c0ca6b876';
      const secondId = 'e02c34d9-1ca6-4373-b074-79b3a8052ce8';
      const cases =
          <
            ({
              bool overflow,
              TextDirection direction,
              VerticalDirection vertical,
            })
          >[
            (
              overflow: false,
              direction: TextDirection.ltr,
              vertical: VerticalDirection.down,
            ),
            (
              overflow: false,
              direction: TextDirection.rtl,
              vertical: VerticalDirection.up,
            ),
            (
              overflow: true,
              direction: TextDirection.ltr,
              vertical: VerticalDirection.down,
            ),
            (
              overflow: true,
              direction: TextDirection.rtl,
              vertical: VerticalDirection.down,
            ),
            (
              overflow: true,
              direction: TextDirection.ltr,
              vertical: VerticalDirection.up,
            ),
            (
              overflow: true,
              direction: TextDirection.rtl,
              vertical: VerticalDirection.up,
            ),
          ];

      for (final entry in cases) {
        final properties = <String, Object?>{
          // Keep the overflow cases deterministic under the Canvas preview's
          // fit-to-surface scale: the real Flutter criterion must overflow
          // even when the surrounding preview can offer the full test width.
          'spacing': {'kind': 'double', 'value': entry.overflow ? 400.0 : 8.0},
          'overflowSpacing': {'kind': 'double', 'value': 6.0},
          'overflowAlignment': {
            'kind': 'enum',
            'type': 'OverflowBarAlignment',
            'value': 'start',
          },
          'overflowDirection': {
            'kind': 'enum',
            'type': 'VerticalDirection',
            'value': entry.vertical == VerticalDirection.up ? 'up' : 'down',
          },
          'textDirection': {
            'kind': 'enum',
            'type': 'TextDirection',
            'value': entry.direction == TextDirection.rtl ? 'rtl' : 'ltr',
          },
        };
        CanvasDropResolver? dropResolver;
        CanvasMovePreviewResolver? moveResolver;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(
                utf8.encode(
                  jsonEncode(
                    _modelWithOverflowBar(
                      width: entry.overflow ? 50 : 240,
                      properties: properties,
                      children: [
                        _viewTextNode(firstId, 'Wide action'),
                        _viewTextNode(secondId, 'OK'),
                      ],
                    ),
                  ),
                ),
              ),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => dropResolver = value,
            onMovePreviewResolverChanged: (value) => moveResolver = value,
          ),
        );
        await tester.pump();

        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final barRect = tester.getRect(
          find.byKey(const ValueKey('canvas-widget-$overflowBarId')),
        );
        final firstRect = tester.getRect(
          find.byKey(const ValueKey('canvas-widget-$firstId')),
        );
        final secondRect = tester.getRect(
          find.byKey(const ValueKey('canvas-widget-$secondId')),
        );
        if (entry.overflow) {
          expect(
            entry.vertical == VerticalDirection.up
                ? firstRect.top > secondRect.top
                : firstRect.top < secondRect.top,
            isTrue,
            reason:
                '$entry; bar=$barRect; first=$firstRect; second=$secondRect',
          );
          if (entry.direction == TextDirection.rtl) {
            expect(firstRect.right, closeTo(barRect.right, 0.01));
            expect(secondRect.right, closeTo(barRect.right, 0.01));
          } else {
            expect(firstRect.left, closeTo(barRect.left, 0.01));
            expect(secondRect.left, closeTo(barRect.left, 0.01));
          }
        } else {
          expect(
            entry.direction == TextDirection.rtl
                ? firstRect.left > secondRect.left
                : firstRect.left < secondRect.left,
            isTrue,
            reason: entry.toString(),
          );
        }

        final terminalPoint = entry.overflow
            ? Offset(
                barRect.center.dx,
                entry.vertical == VerticalDirection.up
                    ? barRect.top + 1
                    : barRect.bottom - 1,
              )
            : Offset(
                entry.direction == TextDirection.rtl
                    ? barRect.left + 1
                    : barRect.right - 1,
                barRect.center.dy,
              );
        int micros(double value, double origin, double extent) =>
            ((value - origin) / extent * 1000000).round();
        final terminal = dropResolver!(
          micros(terminalPoint.dx, surface.left, surface.width),
          micros(terminalPoint.dy, surface.top, surface.height),
        );
        expect(
          terminal?.parentWidgetId,
          overflowBarId,
          reason: entry.toString(),
        );
        expect(terminal?.slotName, 'children', reason: entry.toString());
        expect(terminal?.insertionIndex, 2, reason: entry.toString());
        expect(terminal?.zone?.isEmpty, isFalse, reason: entry.toString());

        final move = moveResolver!(firstId, overflowBarId, 'children', 1);
        expect(move?.parentWidgetId, overflowBarId, reason: entry.toString());
        expect(move?.slotName, 'children', reason: entry.toString());
        expect(move?.insertionIndex, 1, reason: entry.toString());
        expect(move?.zone?.isEmpty, isFalse, reason: entry.toString());
        final zone = move!.zone!;
        final actualMoveEdge = entry.overflow
            ? (zone.topMicros + zone.bottomMicros) ~/ 2
            : (zone.leftMicros + zone.rightMicros) ~/ 2;
        final physicalEdge = entry.overflow
            ? (entry.vertical == VerticalDirection.up
                  ? secondRect.top
                  : secondRect.bottom)
            : (entry.direction == TextDirection.rtl
                  ? secondRect.left
                  : secondRect.right);
        final clampedEdge = entry.overflow
            ? physicalEdge.clamp(barRect.top + 6, barRect.bottom - 6)
            : physicalEdge.clamp(barRect.left + 6, barRect.right - 6);
        final expectedMoveEdge = entry.overflow
            ? micros(clampedEdge, surface.top, surface.height)
            : micros(clampedEdge, surface.left, surface.width);
        expect(
          actualMoveEdge,
          // The Canvas surface may scale its preview. At a clamped outer edge
          // the resolver's six logical pixels therefore become a few global
          // pixels before normalization; keep the assertion tighter than the
          // twelve-pixel insertion marker while allowing that transform.
          closeTo(expectedMoveEdge, 4000),
          reason: entry.toString(),
        );
        expect(tester.takeException(), isNull, reason: entry.toString());
      }
    },
  );

  testWidgets('renders every reviewed ListView argument with real Flutter', (
    tester,
  ) async {
    const listViewId = '4febd2a9-b2ef-4f4d-8d67-2df913b46da2';
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithListView(
              properties: {
                'scrollDirection': {
                  'kind': 'enum',
                  'type': 'Axis',
                  'value': 'horizontal',
                },
                'reverse': {'kind': 'boolean', 'value': true},
                'primary': {'kind': 'boolean', 'value': false},
                'physics': {'kind': 'string', 'value': 'rangeMaintaining'},
                'shrinkWrap': {'kind': 'boolean', 'value': false},
                'padding': {
                  'kind': 'edgeInsetsDirectional',
                  'start': 1,
                  'top': 2,
                  'end': 3,
                  'bottom': 4,
                },
                'itemExtent': {'kind': 'double', 'value': 48.5},
                'addAutomaticKeepAlives': {'kind': 'boolean', 'value': false},
                'addRepaintBoundaries': {'kind': 'boolean', 'value': false},
                'addSemanticIndexes': {'kind': 'boolean', 'value': false},
                'scrollCacheExtent': {'kind': 'integer', 'value': 240},
                'semanticChildCount': {'kind': 'integer', 'value': 2},
                'dragStartBehavior': {
                  'kind': 'enum',
                  'type': 'DragStartBehavior',
                  'value': 'down',
                },
                'keyboardDismissBehavior': {
                  'kind': 'enum',
                  'type': 'ScrollViewKeyboardDismissBehavior',
                  'value': 'onDrag',
                },
                'restorationId': {'kind': 'string', 'value': 'primary-list'},
                'clipBehavior': {
                  'kind': 'enum',
                  'type': 'Clip',
                  'value': 'antiAlias',
                },
                'hitTestBehavior': {
                  'kind': 'enum',
                  'type': 'HitTestBehavior',
                  'value': 'translucent',
                },
              },
              children: [
                <String, Object?>{
                  'id': '7a6d767e-9d7b-4cf1-9bfd-83f75383a08f',
                  'type': 'flutter.widgets.Text',
                  'properties': <String, Object?>{
                    'data': {'kind': 'string', 'value': 'First'},
                  },
                  'slots': <String, Object?>{},
                },
                <String, Object?>{
                  'id': '99de11d2-8f49-4efc-bb79-c6d0e89fcae6',
                  'type': 'flutter.widgets.Text',
                  'properties': <String, Object?>{
                    'data': {'kind': 'string', 'value': 'Second'},
                  },
                  'slots': <String, Object?>{},
                },
              ],
            ),
          ),
        ),
      ),
    );

    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );
    await tester.pump();

    final node = find.byKey(const ValueKey('canvas-widget-$listViewId'));
    final finder = find.descendant(of: node, matching: find.byType(ListView));
    final listView = tester.widget<ListView>(finder);
    expect(listView.scrollDirection, Axis.horizontal);
    expect(listView.reverse, isTrue);
    expect(listView.primary, isFalse);
    expect(listView.physics, isA<RangeMaintainingScrollPhysics>());
    expect(listView.shrinkWrap, isFalse);
    expect(listView.padding, const EdgeInsetsDirectional.fromSTEB(1, 2, 3, 4));
    expect(listView.itemExtent, 48.5);
    final delegate = listView.childrenDelegate as SliverChildListDelegate;
    expect(delegate.addAutomaticKeepAlives, isFalse);
    expect(delegate.addRepaintBoundaries, isFalse);
    expect(delegate.addSemanticIndexes, isFalse);
    expect(listView.scrollCacheExtent?.value, 240);
    expect(listView.semanticChildCount, 2);
    expect(listView.dragStartBehavior, DragStartBehavior.down);
    expect(
      listView.keyboardDismissBehavior,
      ScrollViewKeyboardDismissBehavior.onDrag,
    );
    expect(listView.restorationId, 'primary-list');
    expect(listView.clipBehavior, Clip.antiAlias);
    expect(listView.hitTestBehavior, HitTestBehavior.translucent);
    expect(find.text('First'), findsOneWidget);
    expect(find.text('Second'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('bounds every required ListView axis under flex constraints', (
    tester,
  ) async {
    Future<Size> render({
      required String parentType,
      required Map<String, Object?> properties,
    }) async {
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithListView(
                properties: properties,
                children: const [],
                unboundedParentType: parentType,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      return tester.getSize(find.byType(ListView));
    }

    final vertical = await render(
      parentType: 'flutter.widgets.Column',
      properties: {},
    );
    expect(vertical.height, 120);

    final horizontal = await render(
      parentType: 'flutter.widgets.Row',
      properties: {
        'scrollDirection': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
      },
    );
    expect(horizontal.width, 240);

    final verticalCrossAxis = await render(
      parentType: 'flutter.widgets.Row',
      properties: {
        'shrinkWrap': {'kind': 'boolean', 'value': true},
      },
    );
    expect(verticalCrossAxis.width, 240);

    final horizontalCrossAxis = await render(
      parentType: 'flutter.widgets.Column',
      properties: {
        'scrollDirection': {
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
        'shrinkWrap': {'kind': 'boolean', 'value': true},
      },
    );
    expect(horizontalCrossAxis.height, 120);
  });

  testWidgets('exposes an empty ListView as a children insertion target', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(_modelWithListView(properties: {}, children: const [])),
        ),
      ),
    );
    CanvasDropResolver? resolver;
    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
          onDropResolverChanged: (value) => resolver = value,
        ),
      ),
    );
    await tester.pump();

    const listViewId = '4febd2a9-b2ef-4f4d-8d67-2df913b46da2';
    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final rect = tester.getRect(
      find.byKey(const ValueKey('canvas-widget-$listViewId')),
    );
    final target = resolver!(
      ((rect.center.dx - surface.left) / surface.width * 1000000).round(),
      ((rect.center.dy - surface.top) / surface.height * 1000000).round(),
    );
    expect(target?.parentWidgetId, listViewId);
    expect(target?.slotName, 'children');
    expect(target?.insertionIndex, 0);
    expect(target?.zone?.isEmpty, isFalse);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'resolves populated ListView terminal and move edges for reverse and RTL',
    (tester) async {
      const listViewId = '4febd2a9-b2ef-4f4d-8d67-2df913b46da2';
      const firstId = '1b7b8bc1-f72e-4059-8d60-a32add0a62ef';
      const secondId = '493448dd-35a8-42ed-96b5-e14dff0764a2';

      Future<({int actualMoveEdge, int expectedMoveEdge})> resolve({
        required Axis axis,
        required bool reverse,
        required TextDirection direction,
      }) async {
        final properties = <String, Object?>{
          'reverse': {'kind': 'boolean', 'value': reverse},
          if (axis == Axis.horizontal)
            'scrollDirection': {
              'kind': 'enum',
              'type': 'Axis',
              'value': 'horizontal',
            },
        };
        final json = _modelWithListView(
          properties: properties,
          children: [
            _viewSizedBoxNode(firstId, width: 40, height: 30),
            _viewSizedBoxNode(secondId, width: 40, height: 30),
          ],
        );
        (json['profile']! as Map<String, Object?>)['locale'] =
            direction == TextDirection.rtl ? 'ar-SA' : 'en-US';
        CanvasDropResolver? dropResolver;
        CanvasMovePreviewResolver? moveResolver;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => dropResolver = value,
            onMovePreviewResolverChanged: (value) => moveResolver = value,
          ),
        );
        await tester.pump();

        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final listRect = tester.getRect(
          find.byKey(const ValueKey('canvas-widget-$listViewId')),
        );
        final retainedRect = tester.getRect(
          find.byKey(const ValueKey('canvas-widget-$secondId')),
        );
        final visuallyReversed = axis == Axis.vertical
            ? reverse
            : (direction == TextDirection.rtl) != reverse;
        final terminalPoint = axis == Axis.vertical
            ? Offset(
                listRect.center.dx,
                visuallyReversed ? listRect.top + 1 : listRect.bottom - 1,
              )
            : Offset(
                visuallyReversed ? listRect.left + 1 : listRect.right - 1,
                listRect.center.dy,
              );
        int micros(double value, double origin, double extent) =>
            ((value - origin) / extent * 1000000).round();
        final terminal = dropResolver!(
          micros(terminalPoint.dx, surface.left, surface.width),
          micros(terminalPoint.dy, surface.top, surface.height),
        );
        expect(terminal?.parentWidgetId, listViewId);
        expect(terminal?.slotName, 'children');
        expect(terminal?.insertionIndex, 2);
        expect(terminal?.zone?.isEmpty, isFalse);

        final move = moveResolver!(firstId, listViewId, 'children', 1);
        expect(move?.parentWidgetId, listViewId);
        expect(move?.slotName, 'children');
        expect(move?.insertionIndex, 1);
        expect(move?.zone?.isEmpty, isFalse);
        final zone = move!.zone!;
        final actualMoveEdge = axis == Axis.vertical
            ? (zone.topMicros + zone.bottomMicros) ~/ 2
            : (zone.leftMicros + zone.rightMicros) ~/ 2;
        final physicalEdge = axis == Axis.vertical
            ? (visuallyReversed ? retainedRect.top : retainedRect.bottom)
            : (visuallyReversed ? retainedRect.left : retainedRect.right);
        final expectedMoveEdge = axis == Axis.vertical
            ? micros(physicalEdge, surface.top, surface.height)
            : micros(physicalEdge, surface.left, surface.width);
        expect(tester.takeException(), isNull);
        return (
          actualMoveEdge: actualMoveEdge,
          expectedMoveEdge: expectedMoveEdge,
        );
      }

      final verticalReverse = await resolve(
        axis: Axis.vertical,
        reverse: true,
        direction: TextDirection.ltr,
      );
      expect(
        verticalReverse.actualMoveEdge,
        closeTo(verticalReverse.expectedMoveEdge, 2),
      );

      final rtlForward = await resolve(
        axis: Axis.horizontal,
        reverse: false,
        direction: TextDirection.rtl,
      );
      expect(
        rtlForward.actualMoveEdge,
        closeTo(rtlForward.expectedMoveEdge, 2),
      );

      final rtlReverse = await resolve(
        axis: Axis.horizontal,
        reverse: true,
        direction: TextDirection.rtl,
      );
      expect(
        rtlReverse.actualMoveEdge,
        closeTo(rtlReverse.expectedMoveEdge, 2),
      );
    },
  );

  testWidgets('renders every GridView.count argument with real Flutter', (
    tester,
  ) async {
    const gridId = '7c5646ab-89dc-45b0-b147-f46604fc411f';
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithGridView(
              properties: {
                'scrollDirection': {
                  'kind': 'enum',
                  'type': 'Axis',
                  'value': 'horizontal',
                },
                'reverse': {'kind': 'boolean', 'value': true},
                'primary': {'kind': 'boolean', 'value': false},
                'physics': {'kind': 'string', 'value': 'bouncing'},
                'shrinkWrap': {'kind': 'boolean', 'value': false},
                'padding': {
                  'kind': 'edgeInsetsDirectional',
                  'start': 1.0,
                  'top': 2.0,
                  'end': 3.0,
                  'bottom': 4.0,
                },
                'crossAxisCount': {'kind': 'integer', 'value': 2},
                'mainAxisSpacing': {'kind': 'double', 'value': 5.0},
                'crossAxisSpacing': {'kind': 'double', 'value': 6.0},
                'childAspectRatio': {'kind': 'double', 'value': 1.5},
                'mainAxisExtent': {'kind': 'double', 'value': 48.0},
                'addAutomaticKeepAlives': {'kind': 'boolean', 'value': false},
                'addRepaintBoundaries': {'kind': 'boolean', 'value': false},
                'addSemanticIndexes': {'kind': 'boolean', 'value': false},
                'scrollCacheExtent': {'kind': 'integer', 'value': 240},
                'semanticChildCount': {'kind': 'integer', 'value': 2},
                'dragStartBehavior': {
                  'kind': 'enum',
                  'type': 'DragStartBehavior',
                  'value': 'down',
                },
                'keyboardDismissBehavior': {
                  'kind': 'enum',
                  'type': 'ScrollViewKeyboardDismissBehavior',
                  'value': 'onDrag',
                },
                'restorationId': {'kind': 'string', 'value': 'primary-grid'},
                'clipBehavior': {
                  'kind': 'enum',
                  'type': 'Clip',
                  'value': 'antiAlias',
                },
                'hitTestBehavior': {
                  'kind': 'enum',
                  'type': 'HitTestBehavior',
                  'value': 'translucent',
                },
              },
              children: [
                _viewTextNode('0e15ae86-0c58-4298-b80e-2ab6b3a55371', 'First'),
                _viewTextNode('c2fab615-373d-475e-81ca-76cc1d28460c', 'Second'),
              ],
            ),
          ),
        ),
      ),
    );

    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );
    await tester.pump();

    final node = find.byKey(const ValueKey('canvas-widget-$gridId'));
    final finder = find.descendant(of: node, matching: find.byType(GridView));
    final grid = tester.widget<GridView>(finder);
    expect(grid.scrollDirection, Axis.horizontal);
    expect(grid.reverse, isTrue);
    expect(grid.primary, isFalse);
    expect(grid.physics, isA<BouncingScrollPhysics>());
    expect(grid.shrinkWrap, isFalse);
    expect(grid.padding, const EdgeInsetsDirectional.fromSTEB(1, 2, 3, 4));
    final gridDelegate =
        grid.gridDelegate as SliverGridDelegateWithFixedCrossAxisCount;
    expect(gridDelegate.crossAxisCount, 2);
    expect(gridDelegate.mainAxisSpacing, 5);
    expect(gridDelegate.crossAxisSpacing, 6);
    expect(gridDelegate.childAspectRatio, 1.5);
    expect(gridDelegate.mainAxisExtent, 48);
    final childDelegate = grid.childrenDelegate as SliverChildListDelegate;
    expect(childDelegate.addAutomaticKeepAlives, isFalse);
    expect(childDelegate.addRepaintBoundaries, isFalse);
    expect(childDelegate.addSemanticIndexes, isFalse);
    expect(grid.scrollCacheExtent?.value, 240);
    expect(grid.semanticChildCount, 2);
    expect(grid.dragStartBehavior, DragStartBehavior.down);
    expect(
      grid.keyboardDismissBehavior,
      ScrollViewKeyboardDismissBehavior.onDrag,
    );
    expect(grid.restorationId, 'primary-grid');
    expect(grid.clipBehavior, Clip.antiAlias);
    expect(grid.hitTestBehavior, HitTestBehavior.translucent);
    expect(find.text('First'), findsOneWidget);
    expect(find.text('Second'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('honors GridView.count tile geometry in both axes', (
    tester,
  ) async {
    const firstId = '0e15ae86-0c58-4298-b80e-2ab6b3a55371';
    const secondId = 'c2fab615-373d-475e-81ca-76cc1d28460c';
    const thirdId = '83854346-c458-48c5-a650-912667302538';

    Future<({List<Rect> children, double scale})> render(
      Map<String, Object?> properties,
    ) async {
      await tester.pumpWidget(
        CanvasModelApp(
          model: CanvasModel.decode(
            Uint8List.fromList(
              utf8.encode(
                jsonEncode(
                  _modelWithGridView(
                    properties: properties,
                    children: [
                      _viewTextNode(firstId, 'First'),
                      _viewTextNode(secondId, 'Second'),
                      _viewTextNode(thirdId, 'Third'),
                    ],
                  ),
                ),
              ),
            ),
          ),
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      final grid = find.byType(GridView);
      final scale = tester.getRect(grid).width / tester.getSize(grid).width;
      return (
        children: [
          tester.getRect(find.byKey(const ValueKey('canvas-widget-$firstId'))),
          tester.getRect(find.byKey(const ValueKey('canvas-widget-$secondId'))),
          tester.getRect(find.byKey(const ValueKey('canvas-widget-$thirdId'))),
        ],
        scale: scale,
      );
    }

    final vertical = await render({
      'crossAxisCount': {'kind': 'integer', 'value': 2},
      'mainAxisSpacing': {'kind': 'double', 'value': 8.0},
      'crossAxisSpacing': {'kind': 'double', 'value': 10.0},
      'childAspectRatio': {'kind': 'double', 'value': 2.0},
      'padding': {
        'kind': 'edgeInsets',
        'left': 10.0,
        'top': 10.0,
        'right': 10.0,
        'bottom': 10.0,
      },
    });
    expect(vertical.children[0].width, closeTo(135 * vertical.scale, 0.01));
    expect(vertical.children[0].height, closeTo(67.5 * vertical.scale, 0.01));
    expect(
      vertical.children[1].left - vertical.children[0].right,
      closeTo(10 * vertical.scale, 0.01),
    );
    expect(
      vertical.children[2].top - vertical.children[0].bottom,
      closeTo(8 * vertical.scale, 0.01),
    );

    final fixedExtent = await render({
      'crossAxisCount': {'kind': 'integer', 'value': 2},
      'childAspectRatio': {'kind': 'double', 'value': 0.25},
      'mainAxisExtent': {'kind': 'double', 'value': 40.0},
    });
    expect(
      fixedExtent.children[0].height,
      closeTo(40 * fixedExtent.scale, 0.01),
    );

    final horizontal = await render({
      'scrollDirection': {
        'kind': 'enum',
        'type': 'Axis',
        'value': 'horizontal',
      },
      'crossAxisCount': {'kind': 'integer', 'value': 2},
      'mainAxisSpacing': {'kind': 'double', 'value': 8.0},
      'crossAxisSpacing': {'kind': 'double', 'value': 10.0},
      'mainAxisExtent': {'kind': 'double', 'value': 50.0},
    });
    expect(
      horizontal.children[1].top - horizontal.children[0].bottom,
      closeTo(10 * horizontal.scale, 0.01),
    );
    expect(
      horizontal.children[2].left - horizontal.children[0].right,
      closeTo(8 * horizontal.scale, 0.01),
    );
    expect(horizontal.children[0].width, closeTo(50 * horizontal.scale, 0.01));
  });

  testWidgets('bounds every required GridView axis under flex constraints', (
    tester,
  ) async {
    Future<Size> render({
      required String parentType,
      required Map<String, Object?> properties,
    }) async {
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithGridView(
                properties: properties,
                children: const [],
                unboundedParentType: parentType,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      return tester.getSize(find.byType(GridView));
    }

    expect(
      (await render(
        parentType: 'flutter.widgets.Column',
        properties: {},
      )).height,
      120,
    );
    expect(
      (await render(
        parentType: 'flutter.widgets.Row',
        properties: {
          'scrollDirection': {
            'kind': 'enum',
            'type': 'Axis',
            'value': 'horizontal',
          },
        },
      )).width,
      240,
    );
    expect(
      (await render(
        parentType: 'flutter.widgets.Row',
        properties: {
          'shrinkWrap': {'kind': 'boolean', 'value': true},
        },
      )).width,
      240,
    );
    expect(
      (await render(
        parentType: 'flutter.widgets.Column',
        properties: {
          'scrollDirection': {
            'kind': 'enum',
            'type': 'Axis',
            'value': 'horizontal',
          },
          'shrinkWrap': {'kind': 'boolean', 'value': true},
        },
      )).height,
      120,
    );
  });

  testWidgets('exposes an empty GridView as insertion index zero', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(_modelWithGridView(properties: {}, children: const [])),
        ),
      ),
    );
    CanvasDropResolver? resolver;
    await tester.pumpWidget(
      CanvasModelApp(
        model: model,
        selectedWidgetId: null,
        onSelected: (_) {},
        onDropResolverChanged: (value) => resolver = value,
      ),
    );
    await tester.pump();

    const gridId = '7c5646ab-89dc-45b0-b147-f46604fc411f';
    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final gridRect = tester.getRect(
      find.byKey(const ValueKey('canvas-widget-$gridId')),
    );
    int micros(double value, double origin, double extent) =>
        ((value - origin) / extent * 1000000).round();
    final target = resolver!(
      micros(gridRect.center.dx, surface.left, surface.width),
      micros(gridRect.center.dy, surface.top, surface.height),
    );
    expect(target?.parentWidgetId, gridId);
    expect(target?.slotName, 'children');
    expect(target?.insertionIndex, 0);
    expect(target?.zone?.isEmpty, isFalse);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'resolves row-major GridView append and move markers across directions',
    (tester) async {
      const gridId = '7c5646ab-89dc-45b0-b147-f46604fc411f';
      const firstId = '0e15ae86-0c58-4298-b80e-2ab6b3a55371';
      const secondId = 'c2fab615-373d-475e-81ca-76cc1d28460c';
      const thirdId = '83854346-c458-48c5-a650-912667302538';
      final cases =
          <
            ({
              Axis axis,
              bool reverse,
              TextDirection direction,
              int childCount,
              int moveIndex,
            })
          >[
            (
              axis: Axis.vertical,
              reverse: false,
              direction: TextDirection.rtl,
              childCount: 3,
              moveIndex: 1,
            ),
            (
              axis: Axis.vertical,
              reverse: true,
              direction: TextDirection.ltr,
              childCount: 2,
              moveIndex: 0,
            ),
            (
              axis: Axis.horizontal,
              reverse: true,
              direction: TextDirection.rtl,
              childCount: 2,
              moveIndex: 0,
            ),
            (
              axis: Axis.horizontal,
              reverse: false,
              direction: TextDirection.ltr,
              childCount: 3,
              moveIndex: 1,
            ),
          ];

      for (final entry in cases) {
        final allChildren = [
          _viewTextNode(firstId, 'First'),
          _viewTextNode(secondId, 'Second'),
          _viewTextNode(thirdId, 'Third'),
        ];
        final json = _modelWithGridView(
          properties: {
            if (entry.axis == Axis.horizontal)
              'scrollDirection': {
                'kind': 'enum',
                'type': 'Axis',
                'value': 'horizontal',
              },
            'reverse': {'kind': 'boolean', 'value': entry.reverse},
            'crossAxisCount': {'kind': 'integer', 'value': 2},
            'mainAxisExtent': {'kind': 'double', 'value': 50.0},
            'mainAxisSpacing': {'kind': 'double', 'value': 8.0},
            'crossAxisSpacing': {'kind': 'double', 'value': 10.0},
          },
          children: allChildren.take(entry.childCount).toList(),
        );
        (json['profile']! as Map<String, Object?>)['locale'] =
            entry.direction == TextDirection.rtl ? 'ar-SA' : 'en-US';
        CanvasDropResolver? dropResolver;
        CanvasMovePreviewResolver? moveResolver;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => dropResolver = value,
            onMovePreviewResolverChanged: (value) => moveResolver = value,
          ),
        );
        await tester.pump();

        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final gridRect = tester.getRect(
          find.byKey(const ValueKey('canvas-widget-$gridId')),
        );
        final grid = find.byType(GridView);
        final visualScale =
            tester.getRect(grid).width / tester.getSize(grid).width;
        final lastId = entry.childCount == 2 ? secondId : thirdId;
        final lastRect = tester.getRect(
          find.byKey(ValueKey('canvas-widget-$lastId')),
        );
        final groupBoundary = entry.childCount % 2 == 0;
        final horizontalMain = entry.axis == Axis.horizontal;
        final mainForwardPositive = horizontalMain
            ? (entry.direction == TextDirection.ltr) != entry.reverse
            : !entry.reverse;
        final crossForwardPositive = horizontalMain
            ? true
            : entry.direction == TextDirection.ltr;
        final forwardPositive = groupBoundary
            ? mainForwardPositive
            : crossForwardPositive;
        final verticalEdge = groupBoundary == horizontalMain;
        final appendEdge = verticalEdge
            ? (forwardPositive ? lastRect.right : lastRect.left)
            : (forwardPositive ? lastRect.bottom : lastRect.top);
        final appendPoint = verticalEdge
            ? Offset(
                appendEdge.clamp(
                  gridRect.left + 18 * visualScale,
                  gridRect.right - 18 * visualScale,
                ),
                lastRect.center.dy,
              )
            : Offset(
                lastRect.center.dx,
                appendEdge.clamp(
                  gridRect.top + 18 * visualScale,
                  gridRect.bottom - 18 * visualScale,
                ),
              );
        int micros(double value, double origin, double extent) =>
            ((value - origin) / extent * 1000000).round();
        final append = dropResolver!(
          micros(appendPoint.dx, surface.left, surface.width),
          micros(appendPoint.dy, surface.top, surface.height),
        );
        expect(append?.parentWidgetId, gridId, reason: entry.toString());
        expect(append?.slotName, 'children', reason: entry.toString());
        expect(
          append?.insertionIndex,
          entry.childCount,
          reason: entry.toString(),
        );

        final retained = entry.moveIndex == 0 ? secondId : thirdId;
        final retainedRect = tester.getRect(
          find.byKey(ValueKey('canvas-widget-$retained')),
        );
        final moveGroupBoundary = entry.moveIndex % 2 == 0;
        final moveForwardPositive = moveGroupBoundary
            ? mainForwardPositive
            : crossForwardPositive;
        final moveVerticalEdge = moveGroupBoundary == horizontalMain;
        final leadingUsesMinimum = moveForwardPositive;
        final moveEdge = moveVerticalEdge
            ? (leadingUsesMinimum ? retainedRect.left : retainedRect.right)
            : (leadingUsesMinimum ? retainedRect.top : retainedRect.bottom);
        final move = moveResolver!(
          firstId,
          gridId,
          'children',
          entry.moveIndex,
        );
        expect(move?.parentWidgetId, gridId, reason: entry.toString());
        expect(move?.insertionIndex, entry.moveIndex, reason: entry.toString());
        final zone = move!.zone!;
        final actualEdge = moveVerticalEdge
            ? (zone.leftMicros + zone.rightMicros) ~/ 2
            : (zone.topMicros + zone.bottomMicros) ~/ 2;
        final clampedMoveEdge = moveVerticalEdge
            ? moveEdge.clamp(
                gridRect.left + 6 * visualScale,
                gridRect.right - 6 * visualScale,
              )
            : moveEdge.clamp(
                gridRect.top + 6 * visualScale,
                gridRect.bottom - 6 * visualScale,
              );
        final expectedEdge = moveVerticalEdge
            ? micros(clampedMoveEdge, surface.left, surface.width)
            : micros(clampedMoveEdge, surface.top, surface.height);
        expect(actualEdge, closeTo(expectedEdge, 2), reason: entry.toString());
        expect(tester.takeException(), isNull, reason: entry.toString());
      }
    },
  );

  testWidgets('renders every reviewed SingleChildScrollView argument', (
    tester,
  ) async {
    const scrollId = 'cd068a37-bb45-49a3-a622-6ba944878d58';
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithSingleChildScrollView(
              properties: const {
                'scrollDirection': {
                  'kind': 'enum',
                  'type': 'Axis',
                  'value': 'horizontal',
                },
                'reverse': {'kind': 'boolean', 'value': true},
                'padding': {
                  'kind': 'edgeInsetsDirectional',
                  'start': 1.0,
                  'top': 2.0,
                  'end': 3.0,
                  'bottom': 4.0,
                },
                'primary': {'kind': 'boolean', 'value': false},
                'physics': {'kind': 'string', 'value': 'bouncing'},
                'dragStartBehavior': {
                  'kind': 'enum',
                  'type': 'DragStartBehavior',
                  'value': 'down',
                },
                'clipBehavior': {
                  'kind': 'enum',
                  'type': 'Clip',
                  'value': 'antiAlias',
                },
                'hitTestBehavior': {
                  'kind': 'enum',
                  'type': 'HitTestBehavior',
                  'value': 'translucent',
                },
                'restorationId': {'kind': 'string', 'value': 'single-scroll'},
                'keyboardDismissBehavior': {
                  'kind': 'enum',
                  'type': 'ScrollViewKeyboardDismissBehavior',
                  'value': 'onDrag',
                },
              },
              child: _viewSizedBoxNode(
                'f1009a48-969b-4cee-bcf7-8fe208ae8c9f',
                width: 500,
                height: 40,
              ),
            ),
          ),
        ),
      ),
    );

    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );
    await tester.pump();

    final node = find.byKey(const ValueKey('canvas-widget-$scrollId'));
    final finder = find.descendant(
      of: node,
      matching: find.byType(SingleChildScrollView),
    );
    final scrollView = tester.widget<SingleChildScrollView>(finder);
    expect(scrollView.scrollDirection, Axis.horizontal);
    expect(scrollView.reverse, isTrue);
    expect(
      scrollView.padding,
      const EdgeInsetsDirectional.fromSTEB(1, 2, 3, 4),
    );
    expect(scrollView.primary, isFalse);
    expect(scrollView.physics, isA<BouncingScrollPhysics>());
    expect(scrollView.dragStartBehavior, DragStartBehavior.down);
    expect(scrollView.clipBehavior, Clip.antiAlias);
    expect(scrollView.hitTestBehavior, HitTestBehavior.translucent);
    expect(scrollView.restorationId, 'single-scroll');
    expect(
      scrollView.keyboardDismissBehavior,
      ScrollViewKeyboardDismissBehavior.onDrag,
    );
    expect(scrollView.child, isNotNull);
    expect(
      find.descendant(of: node, matching: find.byType(LayoutBuilder)),
      findsNothing,
      reason: 'SingleChildScrollView keeps its native shrink-wrap semantics',
    );
    expect(tester.takeException(), isNull);
  });

  testWidgets('maps every reviewed SingleChildScrollView physics preset', (
    tester,
  ) async {
    const scrollId = 'cd068a37-bb45-49a3-a622-6ba944878d58';
    final cases = <String, Type>{
      'alwaysScrollable': AlwaysScrollableScrollPhysics,
      'bouncing': BouncingScrollPhysics,
      'clamping': ClampingScrollPhysics,
      'neverScrollable': NeverScrollableScrollPhysics,
      'page': PageScrollPhysics,
      'rangeMaintaining': RangeMaintainingScrollPhysics,
    };
    for (final entry in cases.entries) {
      await tester.pumpWidget(
        CanvasModelApp(
          model: CanvasModel.decode(
            Uint8List.fromList(
              utf8.encode(
                jsonEncode(
                  _modelWithSingleChildScrollView(
                    properties: {
                      'primary': {'kind': 'boolean', 'value': false},
                      'physics': {'kind': 'string', 'value': entry.key},
                    },
                    child: _viewTextNode(
                      'f1009a48-969b-4cee-bcf7-8fe208ae8c9f',
                      entry.key,
                    ),
                  ),
                ),
              ),
            ),
          ),
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();
      final scrollView = tester.widget<SingleChildScrollView>(
        find.descendant(
          of: find.byKey(const ValueKey('canvas-widget-$scrollId')),
          matching: find.byType(SingleChildScrollView),
        ),
      );
      expect(scrollView.physics.runtimeType, entry.value, reason: entry.key);
      expect(tester.takeException(), isNull, reason: entry.key);
    }
  });

  testWidgets(
    'resolves SingleChildScrollView axis, reverse, and directionality',
    (tester) async {
      const scrollId = 'cd068a37-bb45-49a3-a622-6ba944878d58';
      final cases =
          <({String locale, Axis axis, bool reverse, AxisDirection expected})>[
            (
              locale: 'en-US',
              axis: Axis.vertical,
              reverse: false,
              expected: AxisDirection.down,
            ),
            (
              locale: 'en-US',
              axis: Axis.vertical,
              reverse: true,
              expected: AxisDirection.up,
            ),
            (
              locale: 'en-US',
              axis: Axis.horizontal,
              reverse: false,
              expected: AxisDirection.right,
            ),
            (
              locale: 'ar-SA',
              axis: Axis.horizontal,
              reverse: false,
              expected: AxisDirection.left,
            ),
            (
              locale: 'ar-SA',
              axis: Axis.horizontal,
              reverse: true,
              expected: AxisDirection.right,
            ),
          ];

      for (final entry in cases) {
        final json = _modelWithSingleChildScrollView(
          properties: {
            if (entry.axis == Axis.horizontal)
              'scrollDirection': {
                'kind': 'enum',
                'type': 'Axis',
                'value': 'horizontal',
              },
            'reverse': {'kind': 'boolean', 'value': entry.reverse},
            'primary': {'kind': 'boolean', 'value': false},
          },
          child: _viewSizedBoxNode(
            'f1009a48-969b-4cee-bcf7-8fe208ae8c9f',
            width: 500,
            height: 500,
          ),
        );
        (json['profile']! as Map<String, Object?>)['locale'] = entry.locale;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();

        final node = find.byKey(const ValueKey('canvas-widget-$scrollId'));
        final scrollable = tester.widget<Scrollable>(
          find.descendant(of: node, matching: find.byType(Scrollable)),
        );
        expect(
          scrollable.axisDirection,
          entry.expected,
          reason: entry.toString(),
        );
        expect(tester.takeException(), isNull, reason: entry.toString());
      }
    },
  );

  testWidgets(
    'keeps an empty SingleChildScrollView selectable and supports child reparent',
    (tester) async {
      const scrollId = 'cd068a37-bb45-49a3-a622-6ba944878d58';
      const sourceId = '0f91484c-46d1-439c-a8cf-786009b58842';
      CanvasDropResolver? dropResolver;
      CanvasMovePreviewResolver? moveResolver;
      String? selectedWidgetId;
      void captureMoveResolver(CanvasMovePreviewResolver? value) {
        moveResolver = value;
      }

      final empty = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithSingleChildScrollView(
                properties: const {},
                child: null,
                unbounded: true,
                sibling: _viewTextNode(sourceId, 'Move me'),
              ),
            ),
          ),
        ),
      );
      var currentModel = empty;
      StateSetter? rebuild;
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) {
            rebuild = setState;
            return CanvasModelApp(
              model: currentModel,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => dropResolver = value,
              onMovePreviewResolverChanged: captureMoveResolver,
            );
          },
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$scrollId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$scrollId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, scrollId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      int micros(double value, double origin, double extent) =>
          ((value - origin) / extent * 1000000).round();
      final drop = dropResolver!(
        micros(point.dx, surface.left, surface.width),
        micros(point.dy, surface.top, surface.height),
      );
      expect(drop?.parentWidgetId, scrollId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);

      final move = moveResolver!(sourceId, scrollId, 'child', 0);
      expect(move?.parentWidgetId, scrollId);
      expect(move?.slotName, 'child');
      expect(move?.insertionIndex, 0);
      expect(move?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);

      final occupied = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithSingleChildScrollView(
                properties: const {},
                child: _viewTextNode(
                  'f1009a48-969b-4cee-bcf7-8fe208ae8c9f',
                  'Occupied',
                ),
                unbounded: true,
                sibling: _viewTextNode(sourceId, 'Move me'),
              ),
            ),
          ),
        ),
      );
      rebuild!(() {
        currentModel = occupied;
        selectedWidgetId = null;
      });
      await tester.pump();
      expect(moveResolver!(sourceId, scrollId, 'child', 0), isNull);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('renders exact nullable SizedBox dimensions and child', (
    tester,
  ) async {
    const sizedBoxId = '38f49912-8e51-4e62-bd4c-2517ecad4962';

    Map<String, Object?> withSizedBox({
      required Map<String, Object?> properties,
      required Map<String, Object?>? child,
    }) {
      final json = _modelJsonForView();
      final root = json['root']! as Map<String, Object?>;
      final body =
          (root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>;
      body['child'] = <String, Object?>{
        'id': sizedBoxId,
        'type': 'flutter.widgets.SizedBox',
        'properties': properties,
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': child},
        },
      };
      return json;
    }

    Future<SizedBox> pump(Map<String, Object?> json) async {
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        ),
      );
      return tester.widget<SizedBox>(
        find
            .descendant(
              of: find.byKey(const ValueKey('canvas-widget-$sizedBoxId')),
              matching: find.byType(SizedBox),
            )
            .first,
      );
    }

    final source = _modelJsonForView();
    final sourceRoot = source['root']! as Map<String, Object?>;
    final text = _findNode(sourceRoot, 'flutter.widgets.Text');
    final explicit = await pump(
      withSizedBox(
        properties: {
          'width': {'kind': 'integer', 'value': 120},
          'height': {'kind': 'double', 'value': 48.5},
        },
        child: text,
      ),
    );
    expect(explicit.width, 120.0);
    expect(explicit.height, 48.5);
    expect(explicit.child, isNotNull);
    expect(find.text('Hello'), findsOneWidget);

    final omitted = await pump(withSizedBox(properties: const {}, child: null));
    expect(omitted.width, isNull);
    expect(omitted.height, isNull);
    expect(omitted.child, isNull);
  });

  testWidgets(
    'renders AspectRatio with an optional child, outlines it, and exposes only its empty slot',
    (tester) async {
      const aspectRatioId = '4a07880d-54a7-44c6-97e4-a88df4e44e67';
      CanvasDropResolver? resolver;

      Future<void> pump({
        required Map<String, Object?>? child,
        String? selectedWidgetId,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredAspectRatio(aspectRatio: 2.0, child: child),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (_) {},
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
      }

      Finder nodeFinder() =>
          find.byKey(const ValueKey('canvas-widget-$aspectRatioId'));
      Finder ratioFinder() => find
          .descendant(of: nodeFinder(), matching: find.byType(AspectRatio))
          .first;

      await pump(child: null);

      final emptyRatio = tester.widget<AspectRatio>(ratioFinder());
      expect(emptyRatio.aspectRatio, 2.0);
      expect(emptyRatio.child, isNull);
      final emptySize = tester.getSize(nodeFinder());
      expect(emptySize.width, greaterThan(0));
      expect(emptySize.height, greaterThan(0));
      expect(emptySize.width / emptySize.height, closeTo(2.0, 0.001));
      expect(
        find.byKey(const ValueKey('canvas-widget-outline-$aspectRatioId')),
        findsOneWidget,
      );
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$aspectRatioId')),
        findsNothing,
      );

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final rect = tester.getRect(nodeFinder());
      final point = rect.center;
      final target = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(target?.parentWidgetId, aspectRatioId);
      expect(target?.slotName, 'child');
      expect(target?.insertionIndex, 0);
      expect(target?.zone?.isEmpty, isFalse);

      await pump(child: null, selectedWidgetId: aspectRatioId);
      expect(
        find.byKey(const ValueKey('canvas-selection-outline-$aspectRatioId')),
        findsOneWidget,
      );
      expect(tester.widget<AspectRatio>(ratioFinder()).child, isNull);

      final source = _modelJsonForView();
      final sourceRoot = source['root']! as Map<String, Object?>;
      final text = _findNode(sourceRoot, 'flutter.widgets.Text');
      await pump(child: text);
      expect(tester.widget<AspectRatio>(ratioFinder()).child, isNotNull);
      expect(find.text('Hello'), findsOneWidget);

      final occupiedSurface = tester.getRect(find.byType(CanvasDocumentView));
      final occupiedRect = tester.getRect(nodeFinder());
      final occupiedPoint = occupiedRect.center;
      expect(
        resolver!(
          ((occupiedPoint.dx - occupiedSurface.left) /
                  occupiedSurface.width *
                  1000000)
              .round(),
          ((occupiedPoint.dy - occupiedSurface.top) /
                  occupiedSurface.height *
                  1000000)
              .round(),
        ),
        isNull,
      );
    },
  );

  testWidgets(
    'renders real Baseline values and both TextBaseline modes with Flutter layout',
    (tester) async {
      const baselineId = '0197b4c0-11f0-45b1-bfe8-cc84ef5a6fae';
      const childId = '0197b4c0-11f0-45b2-a191-d9b1b00a21ce';
      final child = _viewSizedBoxNode(childId, width: 40, height: 10);

      Future<({Baseline widget, RenderBaseline render, Rect childRect})> pump({
        required double baseline,
        required String baselineType,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredBaseline(
                  baseline: baseline,
                  baselineType: baselineType,
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        await tester.pump();
        final finder = find
            .descendant(
              of: find.byKey(const ValueKey('canvas-widget-$baselineId')),
              matching: find.byType(Baseline),
            )
            .first;
        final widget = tester.widget<Baseline>(finder);
        final render = tester.renderObject<RenderBaseline>(finder);
        final renderChild = render.child!;
        return (
          widget: widget,
          render: render,
          childRect: MatrixUtils.transformRect(
            renderChild.getTransformTo(render),
            Offset.zero & renderChild.size,
          ),
        );
      }

      final alphabetic = await pump(baseline: 24, baselineType: 'alphabetic');
      expect(alphabetic.widget.baseline, 24);
      expect(alphabetic.widget.baselineType, TextBaseline.alphabetic);
      expect(alphabetic.render.size, const Size(40, 24));
      expect(alphabetic.childRect, const Rect.fromLTWH(0, 14, 40, 10));

      final ideographic = await pump(
        baseline: 36.5,
        baselineType: 'ideographic',
      );
      expect(ideographic.widget.baseline, 36.5);
      expect(ideographic.widget.baselineType, TextBaseline.ideographic);
      expect(ideographic.render.size, const Size(40, 36.5));
      expect(ideographic.childRect, const Rect.fromLTWH(0, 26.5, 40, 10));

      final negative = await pump(baseline: -6, baselineType: 'alphabetic');
      expect(negative.widget.baseline, -6);
      expect(negative.render.size, const Size(40, 0));
      expect(negative.childRect, const Rect.fromLTWH(0, -16, 40, 10));
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$baselineId'),
        ),
        findsOneWidget,
        reason: 'a legal finite baseline can collapse the real RenderBaseline',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps empty and child-bearing Baseline selectable with exact child DnD',
    (tester) async {
      const baselineId = '0197b4c0-11f0-45b1-bfe8-cc84ef5a6fae';
      const childId = '0197b4c0-11f0-45b2-a191-d9b1b00a21ce';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;

      Future<void> pump({
        required double baseline,
        required Map<String, Object?>? child,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredBaseline(
                  baseline: baseline,
                  baselineType: 'alphabetic',
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          StatefulBuilder(
            builder: (context, setState) => CanvasModelApp(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
        await tester.pump();
      }

      CanvasDropTarget? resolveAt(Offset point) {
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        return resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
      }

      await pump(baseline: 24, child: null);
      final emptyNode = find.byKey(const ValueKey('canvas-widget-$baselineId'));
      final emptyTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$baselineId'),
      );
      expect(tester.getSize(emptyNode), Size.zero);
      expect(emptyTarget, findsOneWidget);
      expect(tester.getSize(emptyTarget), const Size(36, 36));
      await tester.tap(emptyTarget);
      await tester.pump();
      expect(selectedWidgetId, baselineId);
      final emptyDrop = resolveAt(tester.getRect(emptyTarget).center);
      expect(emptyDrop?.parentWidgetId, baselineId);
      expect(emptyDrop?.slotName, 'child');
      expect(emptyDrop?.insertionIndex, 0);
      expect(emptyDrop?.zone?.isEmpty, isFalse);

      selectedWidgetId = null;
      await pump(
        baseline: 24,
        child: _viewSizedBoxNode(childId, width: 40, height: 10),
      );
      final occupiedNode = find.byKey(
        const ValueKey('canvas-widget-$baselineId'),
      );
      final occupiedRect = tester.getRect(occupiedNode);
      final occupiedRender = tester.renderObject<RenderBaseline>(
        find
            .descendant(of: occupiedNode, matching: find.byType(Baseline))
            .first,
      );
      expect(occupiedRender.size, const Size(40, 24));
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$baselineId'),
        ),
        findsNothing,
      );
      await tester.tapAt(Offset(occupiedRect.center.dx, occupiedRect.top + 4));
      await tester.pump();
      expect(selectedWidgetId, baselineId);
      expect(resolveAt(occupiedRect.center)?.parentWidgetId, isNot(baselineId));

      selectedWidgetId = null;
      await pump(
        baseline: -6,
        child: _viewSizedBoxNode(childId, width: 40, height: 10),
      );
      final collapsedTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$baselineId'),
      );
      final collapsedRender = tester.renderObject<RenderBaseline>(
        find
            .descendant(of: occupiedNode, matching: find.byType(Baseline))
            .first,
      );
      expect(collapsedRender.size, const Size(40, 0));
      expect(collapsedTarget, findsOneWidget);
      await tester.tap(collapsedTarget);
      await tester.pump();
      expect(selectedWidgetId, baselineId);
      expect(
        resolveAt(tester.getRect(collapsedTarget).center)?.parentWidgetId,
        isNot(baselineId),
        reason: 'an occupied Baseline child slot cannot accept another child',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders the real IntrinsicHeight and preserves its measured child size',
    (tester) async {
      const intrinsicId = '15aa2055-201d-4f3e-bd50-24cc60fa50c0';
      const childId = 'cf781f2a-8d2e-4a88-a05a-9758e5e911d8';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredIntrinsicHeight(
                child: _viewSizedBoxNode(childId, width: 40, height: 10),
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$intrinsicId'));
      final intrinsicFinder = find
          .descendant(of: node, matching: find.byType(IntrinsicHeight))
          .first;
      final widget = tester.widget<IntrinsicHeight>(intrinsicFinder);
      final render = tester.renderObject<RenderIntrinsicHeight>(
        intrinsicFinder,
      );
      expect(widget.child, isNotNull);
      expect(render.size, const Size(40, 10));
      expect(render.child!.size, const Size(40, 10));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps empty and collapsed IntrinsicHeight selectable without changing layout',
    (tester) async {
      const intrinsicId = '15aa2055-201d-4f3e-bd50-24cc60fa50c0';
      const childId = 'cf781f2a-8d2e-4a88-a05a-9758e5e911d8';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;

      Future<void> pump(Map<String, Object?>? child) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(_modelWithCenteredIntrinsicHeight(child: child)),
            ),
          ),
        );
        await tester.pumpWidget(
          StatefulBuilder(
            builder: (context, setState) => CanvasModelApp(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
        await tester.pump();
      }

      CanvasDropTarget? resolveAt(Offset point) {
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        return resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
      }

      RenderIntrinsicHeight intrinsicRender() =>
          tester.renderObject<RenderIntrinsicHeight>(
            find
                .descendant(
                  of: find.byKey(const ValueKey('canvas-widget-$intrinsicId')),
                  matching: find.byType(IntrinsicHeight),
                )
                .first,
          );

      await pump(null);
      final emptyTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$intrinsicId'),
      );
      expect(intrinsicRender().size, Size.zero);
      expect(emptyTarget, findsOneWidget);
      expect(tester.getSize(emptyTarget), const Size(36, 36));
      final surfaceRect = tester.getRect(find.byType(CanvasDocumentView));
      final emptyTargetRect = tester.getRect(emptyTarget);
      expect(surfaceRect.intersect(emptyTargetRect), emptyTargetRect);
      await tester.tap(emptyTarget);
      await tester.pump();
      expect(selectedWidgetId, intrinsicId);
      final emptyDrop = resolveAt(emptyTargetRect.center);
      expect(emptyDrop?.parentWidgetId, intrinsicId);
      expect(emptyDrop?.slotName, 'child');
      expect(emptyDrop?.insertionIndex, 0);
      expect(emptyDrop?.zone?.isEmpty, isFalse);
      expect(intrinsicRender().size, Size.zero);

      selectedWidgetId = null;
      await pump(_viewSizedBoxNode(childId, width: 40, height: 10));
      final occupiedNode = find.byKey(
        const ValueKey('canvas-widget-$intrinsicId'),
      );
      expect(intrinsicRender().size, const Size(40, 10));
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$intrinsicId'),
        ),
        findsNothing,
      );
      final occupiedRect = tester.getRect(occupiedNode);
      await tester.tapAt(occupiedRect.center);
      await tester.pump();
      expect(selectedWidgetId, childId);
      expect(
        resolveAt(occupiedRect.center)?.parentWidgetId,
        isNot(intrinsicId),
      );

      selectedWidgetId = null;
      await pump(<String, Object?>{
        'id': childId,
        'type': 'flutter.widgets.Padding',
        'properties': <String, Object?>{
          'padding': {
            'kind': 'edgeInsets',
            'left': 0,
            'top': 0,
            'right': 0,
            'bottom': 0,
          },
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      });
      final collapsedTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$intrinsicId'),
      );
      expect(intrinsicRender().size, Size.zero);
      expect(collapsedTarget, findsOneWidget);
      expect(tester.getSize(collapsedTarget), const Size(36, 36));
      final collapsedTargetRect = tester.getRect(collapsedTarget);
      expect(surfaceRect.intersect(collapsedTargetRect), collapsedTargetRect);
      await tester.tap(collapsedTarget);
      await tester.pump();
      expect(selectedWidgetId, intrinsicId);
      expect(
        resolveAt(collapsedTargetRect.center)?.parentWidgetId,
        isNot(intrinsicId),
        reason: 'an occupied IntrinsicHeight child slot cannot accept another',
      );
      expect(intrinsicRender().size, Size.zero);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real IntrinsicWidth with null, zero, and snapped step values',
    (tester) async {
      const intrinsicId = 'bbce8cc2-8ff1-4337-83e2-46f70a579075';
      const childId = '4eaf2797-e975-402c-8e3a-827e40ee9c83';

      Future<({IntrinsicWidth widget, RenderIntrinsicWidth render})> pump(
        Map<String, Object?> properties,
      ) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredIntrinsicWidth(
                  properties: properties,
                  child: _viewSizedBoxNode(childId, width: 40, height: 10),
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final finder = find
            .descendant(
              of: find.byKey(const ValueKey('canvas-widget-$intrinsicId')),
              matching: find.byType(IntrinsicWidth),
            )
            .first;
        return (
          widget: tester.widget<IntrinsicWidth>(finder),
          render: tester.renderObject<RenderIntrinsicWidth>(finder),
        );
      }

      final omitted = await pump(const {});
      expect(omitted.widget.stepWidth, isNull);
      expect(omitted.widget.stepHeight, isNull);
      expect(omitted.render.stepWidth, isNull);
      expect(omitted.render.stepHeight, isNull);
      expect(omitted.render.size, const Size(40, 10));
      expect(omitted.render.child!.size, const Size(40, 10));

      final zero = await pump(const {
        'stepWidth': {'kind': 'double', 'value': 0.0},
        'stepHeight': {'kind': 'double', 'value': 0.0},
      });
      expect(zero.widget.stepWidth, 0.0);
      expect(zero.widget.stepHeight, 0.0);
      expect(
        zero.render.stepWidth,
        isNull,
        reason: 'Flutter defines public zero as unsnapped intrinsic width',
      );
      expect(
        zero.render.stepHeight,
        isNull,
        reason: 'Flutter defines public zero as an unconstrained height',
      );
      expect(zero.render.size, const Size(40, 10));
      expect(zero.render.child!.size, const Size(40, 10));

      final snapped = await pump(const {
        'stepWidth': {'kind': 'double', 'value': 24.0},
        'stepHeight': {'kind': 'double', 'value': 6.0},
      });
      expect(snapped.widget.stepWidth, 24.0);
      expect(snapped.widget.stepHeight, 6.0);
      expect(snapped.render.stepWidth, 24.0);
      expect(snapped.render.stepHeight, 6.0);
      expect(snapped.render.size, const Size(48, 12));
      expect(snapped.render.child!.size, const Size(48, 12));
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps empty and collapsed IntrinsicWidth selectable without changing layout',
    (tester) async {
      const intrinsicId = 'bbce8cc2-8ff1-4337-83e2-46f70a579075';
      const childId = '4eaf2797-e975-402c-8e3a-827e40ee9c83';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;

      Future<void> pump(Map<String, Object?>? child) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredIntrinsicWidth(
                  properties: const {},
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          StatefulBuilder(
            builder: (context, setState) => CanvasModelApp(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
        await tester.pump();
      }

      CanvasDropTarget? resolveAt(Offset point) {
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        return resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
      }

      RenderIntrinsicWidth intrinsicRender() =>
          tester.renderObject<RenderIntrinsicWidth>(
            find
                .descendant(
                  of: find.byKey(const ValueKey('canvas-widget-$intrinsicId')),
                  matching: find.byType(IntrinsicWidth),
                )
                .first,
          );

      await pump(null);
      final emptyTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$intrinsicId'),
      );
      expect(intrinsicRender().size, Size.zero);
      expect(emptyTarget, findsOneWidget);
      expect(tester.getSize(emptyTarget), const Size.square(36));
      final surfaceRect = tester.getRect(find.byType(CanvasDocumentView));
      final emptyTargetRect = tester.getRect(emptyTarget);
      expect(surfaceRect.intersect(emptyTargetRect), emptyTargetRect);
      await tester.tap(emptyTarget);
      await tester.pump();
      expect(selectedWidgetId, intrinsicId);
      final emptyDrop = resolveAt(emptyTargetRect.center);
      expect(emptyDrop?.parentWidgetId, intrinsicId);
      expect(emptyDrop?.slotName, 'child');
      expect(emptyDrop?.insertionIndex, 0);
      expect(emptyDrop?.zone?.isEmpty, isFalse);
      expect(intrinsicRender().size, Size.zero);

      selectedWidgetId = null;
      await pump(_viewSizedBoxNode(childId, width: 40, height: 10));
      final occupiedNode = find.byKey(
        const ValueKey('canvas-widget-$intrinsicId'),
      );
      expect(intrinsicRender().size, const Size(40, 10));
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$intrinsicId'),
        ),
        findsNothing,
      );
      final occupiedRect = tester.getRect(occupiedNode);
      await tester.tapAt(occupiedRect.center);
      await tester.pump();
      expect(selectedWidgetId, childId);
      expect(
        resolveAt(occupiedRect.center)?.parentWidgetId,
        isNot(intrinsicId),
      );

      selectedWidgetId = null;
      await pump(<String, Object?>{
        'id': childId,
        'type': 'flutter.widgets.Padding',
        'properties': <String, Object?>{
          'padding': {
            'kind': 'edgeInsets',
            'left': 0,
            'top': 0,
            'right': 0,
            'bottom': 0,
          },
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': null},
        },
      });
      final collapsedTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$intrinsicId'),
      );
      expect(intrinsicRender().size, Size.zero);
      expect(collapsedTarget, findsOneWidget);
      expect(tester.getSize(collapsedTarget), const Size.square(36));
      final collapsedTargetRect = tester.getRect(collapsedTarget);
      expect(surfaceRect.intersect(collapsedTargetRect), collapsedTargetRect);
      await tester.tap(collapsedTarget);
      await tester.pump();
      expect(selectedWidgetId, intrinsicId);
      expect(
        resolveAt(collapsedTargetRect.center)?.parentWidgetId,
        isNot(intrinsicId),
        reason: 'an occupied IntrinsicWidth child slot cannot accept another',
      );
      expect(intrinsicRender().size, Size.zero);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real Offstage defaults and exact hidden or normal child behavior',
    (tester) async {
      const offstageId = 'aa0bc346-b863-471f-bf3a-bcaa9bbf5090';
      const childId = 'eea084e6-c139-421b-8a62-4e82a877b72c';

      Future<({Offstage widget, RenderOffstage render})> pump(
        Map<String, Object?> properties,
      ) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredOffstage(
                  properties: properties,
                  child: _viewSizedBoxNode(childId, width: 40, height: 10),
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        await tester.pump();
        final finder = find
            .descendant(
              of: find.byKey(const ValueKey('canvas-widget-$offstageId')),
              matching: find.byType(Offstage),
            )
            .first;
        return (
          widget: tester.widget<Offstage>(finder),
          render: tester.renderObject<RenderOffstage>(finder),
        );
      }

      final omitted = await pump(const {});
      expect(omitted.widget.offstage, isTrue);
      expect(omitted.render.offstage, isTrue);
      expect(omitted.render.size, Size.zero);
      expect(omitted.render.child!.size, const Size(40, 10));
      expect(omitted.render.paintsChild(omitted.render.child!), isFalse);
      expect(
        omitted.render.hitTest(BoxHitTestResult(), position: Offset.zero),
        isFalse,
      );

      final explicitTrue = await pump(const {
        'offstage': {'kind': 'boolean', 'value': true},
      });
      expect(explicitTrue.widget.offstage, isTrue);
      expect(explicitTrue.render.offstage, isTrue);
      expect(explicitTrue.render.size, Size.zero);
      expect(explicitTrue.render.child!.size, const Size(40, 10));
      expect(
        explicitTrue.render.paintsChild(explicitTrue.render.child!),
        isFalse,
      );

      final visible = await pump(const {
        'offstage': {'kind': 'boolean', 'value': false},
      });
      expect(visible.widget.offstage, isFalse);
      expect(visible.render.offstage, isFalse);
      expect(visible.render.size, const Size(40, 10));
      expect(visible.render.child!.size, const Size(40, 10));
      expect(visible.render.paintsChild(visible.render.child!), isTrue);
      expect(
        visible.render.hitTest(
          BoxHitTestResult(),
          position: visible.render.size.center(Offset.zero),
        ),
        isTrue,
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps zero-size Offstage selectable and exposes only an empty child drop',
    (tester) async {
      const offstageId = 'aa0bc346-b863-471f-bf3a-bcaa9bbf5090';
      const childId = 'eea084e6-c139-421b-8a62-4e82a877b72c';
      CanvasDropResolver? resolver;
      String? selectedWidgetId;

      Future<void> pump({
        required Map<String, Object?> properties,
        required Map<String, Object?>? child,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredOffstage(
                  properties: properties,
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          StatefulBuilder(
            builder: (context, setState) => CanvasModelApp(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
        await tester.pump();
      }

      CanvasDropTarget? resolveAt(Offset point) {
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        return resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
      }

      RenderOffstage offstageRender() => tester.renderObject<RenderOffstage>(
        find
            .descendant(
              of: find.byKey(const ValueKey('canvas-widget-$offstageId')),
              matching: find.byType(Offstage),
            )
            .first,
      );

      await pump(
        properties: const {},
        child: _viewSizedBoxNode(childId, width: 40, height: 10),
      );
      final hiddenTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$offstageId'),
      );
      expect(offstageRender().size, Size.zero);
      expect(hiddenTarget, findsOneWidget);
      expect(tester.getSize(hiddenTarget), const Size.square(36));
      final hiddenRect = tester.getRect(hiddenTarget);
      await tester.tap(hiddenTarget);
      await tester.pump();
      expect(selectedWidgetId, offstageId);
      expect(
        resolveAt(hiddenRect.center)?.parentWidgetId,
        isNot(offstageId),
        reason: 'the hidden but active child still occupies the model slot',
      );

      selectedWidgetId = null;
      await pump(
        properties: const {
          'offstage': {'kind': 'boolean', 'value': false},
        },
        child: _viewSizedBoxNode(childId, width: 40, height: 10),
      );
      expect(offstageRender().size, const Size(40, 10));
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$offstageId'),
        ),
        findsNothing,
      );
      final visibleNode = find.byKey(
        const ValueKey('canvas-widget-$offstageId'),
      );
      await tester.tapAt(tester.getRect(visibleNode).center);
      await tester.pump();
      expect(selectedWidgetId, childId);

      selectedWidgetId = null;
      await pump(
        properties: const {
          'offstage': {'kind': 'boolean', 'value': false},
        },
        child: null,
      );
      final emptyTarget = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$offstageId'),
      );
      expect(offstageRender().size, Size.zero);
      expect(emptyTarget, findsOneWidget);
      expect(tester.getSize(emptyTarget), const Size.square(36));
      final emptyRect = tester.getRect(emptyTarget);
      final surfaceRect = tester.getRect(find.byType(CanvasDocumentView));
      expect(surfaceRect.intersect(emptyRect), emptyRect);
      final emptyDrop = resolveAt(emptyRect.center);
      expect(emptyDrop?.parentWidgetId, offstageId);
      expect(emptyDrop?.slotName, 'child');
      expect(emptyDrop?.insertionIndex, 0);
      expect(emptyDrop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real RotatedBox layout for signed and large quarter turns',
    (tester) async {
      const widgetId = '81cdfd65-c958-40cf-ab25-3494d1a9e1fc';
      const childId = '908cd88b-2ffd-449a-9af8-a7e31dcc0055';

      Future<RenderRotatedBox> pump(int quarterTurns) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredRotatedBox(
                  quarterTurns: quarterTurns,
                  child: _viewSizedBoxNode(childId, width: 80, height: 40),
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: widgetId,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final finder = find
            .descendant(
              of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
              matching: find.byType(RotatedBox),
            )
            .first;
        final widget = tester.widget<RotatedBox>(finder);
        final render = tester.renderObject<RenderRotatedBox>(finder);
        expect(widget.quarterTurns, quarterTurns);
        expect(render.quarterTurns, quarterTurns);
        expect(render.child!.size, const Size(80, 40));
        return render;
      }

      const cases = <(int, Size)>[
        (1, Size(40, 80)),
        (-1, Size(40, 80)),
        (maxCanvasSequence, Size(40, 80)),
        (-maxCanvasSequence, Size(40, 80)),
        (4, Size(80, 40)),
        (-4, Size(80, 40)),
      ];
      for (final (quarterTurns, expectedSize) in cases) {
        final render = await pump(quarterTurns);
        expect(
          render.size,
          expectedSize,
          reason: '$quarterTurns quarter turns',
        );
        expect(
          MatrixUtils.transformRect(
            render.child!.getTransformTo(render),
            Offset.zero & render.child!.size,
          ),
          rectMoreOrLessEquals(Offset.zero & expectedSize),
          reason: 'the real RenderRotatedBox must own the layout rotation',
        );
        expect(
          tester.getSize(
            find.byKey(const ValueKey('canvas-selection-outline-$widgetId')),
          ),
          expectedSize,
          reason: 'selection must follow the rotated layout bounds',
        );
      }
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets('keeps an empty large-turn RotatedBox selectable and droppable', (
    tester,
  ) async {
    const widgetId = '81cdfd65-c958-40cf-ab25-3494d1a9e1fc';
    final model = CanvasModel.decode(
      Uint8List.fromList(
        utf8.encode(
          jsonEncode(
            _modelWithCenteredRotatedBox(
              quarterTurns: maxCanvasSequence,
              child: null,
            ),
          ),
        ),
      ),
    );
    String? selectedWidgetId;
    CanvasDropResolver? resolver;
    await tester.pumpWidget(
      StatefulBuilder(
        builder: (context, setState) => CanvasModelApp(
          model: model,
          selectedWidgetId: selectedWidgetId,
          onSelected: (id) => setState(() => selectedWidgetId = id),
          onDropResolverChanged: (value) => resolver = value,
        ),
      ),
    );
    await tester.pump();
    await tester.pump();

    final finder = find.byType(RotatedBox);
    final widget = tester.widget<RotatedBox>(finder);
    final render = tester.renderObject<RenderRotatedBox>(finder);
    final target = find.byKey(
      const ValueKey('canvas-zero-size-widget-target-$widgetId'),
    );
    expect(widget.quarterTurns, maxCanvasSequence);
    expect(render.quarterTurns, maxCanvasSequence);
    expect(render.size, Size.zero);
    expect(render.child, isNull);
    expect(target, findsOneWidget);
    expect(tester.getSize(target), const Size.square(36));

    await tester.tap(target);
    await tester.pump();
    expect(selectedWidgetId, widgetId);

    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final point = tester.getRect(target).center;
    final drop = resolver!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
    );
    expect(drop?.parentWidgetId, widgetId);
    expect(drop?.slotName, 'child');
    expect(drop?.insertionIndex, 0);
    expect(drop?.zone?.isEmpty, isFalse);
    expect(tester.takeException(), isNull);
  });

  testWidgets(
    'renders real SizedOverflowBox size, overflow constraints, and alignment',
    (tester) async {
      const widgetId = 'd01c13f5-c20d-48bf-a360-5da7b6b36c67';
      const childId = '04bf06da-4419-4d88-b504-25c93ba7a76a';

      Future<RenderSizedOverflowBox> pump({
        required Map<String, Object?> properties,
        required double childWidth,
        required double childHeight,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredSizedOverflowBox(
                  properties: properties,
                  child: _viewSizedBoxNode(
                    childId,
                    width: childWidth,
                    height: childHeight,
                  ),
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();
        final finder = find
            .descendant(
              of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
              matching: find.byType(SizedOverflowBox),
            )
            .first;
        final widget = tester.widget<SizedOverflowBox>(finder);
        final render = tester.renderObject<RenderSizedOverflowBox>(finder);
        expect(widget.size, render.requestedSize);
        return render;
      }

      final centered = await pump(
        properties: const {
          'size': {'kind': 'size', 'width': 100.0, 'height': 80.0},
        },
        childWidth: 160,
        childHeight: 30,
      );
      expect(centered.requestedSize, const Size(100, 80));
      expect(centered.alignment, Alignment.center);
      expect(centered.size, const Size(100, 80));
      expect(
        centered.child!.size,
        const Size(160, 30),
        reason: 'the original incoming constraints must reach the child',
      );
      expect(
        (centered.child!.parentData! as BoxParentData).offset,
        const Offset(-30, 25),
      );
      expect(
        centered.hitTest(BoxHitTestResult(), position: const Offset(50, 40)),
        isTrue,
      );
      expect(
        centered.hitTest(BoxHitTestResult(), position: const Offset(-10, 40)),
        isFalse,
        reason: 'Flutter clips hit testing to the parent bounds, not painting',
      );

      final aligned = await pump(
        properties: const {
          'size': {'kind': 'size', 'width': 100.0, 'height': 80.0},
          'alignment': {
            'kind': 'alignmentGeometry',
            'basis': 'directional',
            'horizontal': -1.0,
            'vertical': 1.0,
          },
        },
        childWidth: 40,
        childHeight: 20,
      );
      expect(aligned.alignment, AlignmentDirectional.bottomStart);
      expect(aligned.size, const Size(100, 80));
      expect(aligned.child!.size, const Size(40, 20));
      expect(
        (aligned.child!.parentData! as BoxParentData).offset,
        const Offset(0, 60),
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps zero SizedOverflowBox selectable and exposes its empty child slot',
    (tester) async {
      const widgetId = 'd01c13f5-c20d-48bf-a360-5da7b6b36c67';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredSizedOverflowBox(
                properties: const {
                  'size': {'kind': 'size', 'width': 0.0, 'height': 0.0},
                },
                child: null,
              ),
            ),
          ),
        ),
      );
      String? selectedWidgetId;
      CanvasDropResolver? resolver;
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();
      await tester.pump();

      final finder = find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$widgetId')),
            matching: find.byType(SizedOverflowBox),
          )
          .first;
      final render = tester.renderObject<RenderSizedOverflowBox>(finder);
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$widgetId'),
      );
      expect(render.requestedSize, Size.zero);
      expect(render.size, Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size.square(36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, widgetId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, widgetId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real Transform.new properties outside instrumentation without changing layout',
    (tester) async {
      const transformId = '6408cfe9-e227-43de-916c-bb9d66224de9';
      const childId = 'd571f630-fb8d-4c0f-99e7-9cd7eb85337f';
      const storage = <Object?>[
        1.5,
        0,
        0,
        0,
        0,
        0.75,
        0,
        0,
        0,
        0,
        1,
        0,
        12,
        -4,
        0,
        1,
      ];
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredTransform(
                properties: const {
                  'transform': {'kind': 'matrix4', 'storage': storage},
                  'origin': {'kind': 'offset', 'dx': -5.5, 'dy': 7.25},
                  'alignment': {
                    'kind': 'alignmentGeometry',
                    'basis': 'directional',
                    'horizontal': -1.0,
                    'vertical': 1.0,
                  },
                  'transformHitTests': {'kind': 'boolean', 'value': false},
                  'filterQuality': {
                    'kind': 'enum',
                    'type': 'FilterQuality',
                    'value': 'low',
                  },
                },
                child: _viewSizedBoxNode(childId, width: 80, height: 40),
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: transformId,
            onSelected: (_) {},
          ),
        ),
      );
      await tester.pump();

      final transformFinder = find.byKey(
        const ValueKey('canvas-transform-$transformId'),
      );
      final transform = tester.widget<Transform>(transformFinder);
      final render = tester.renderObject<RenderTransform>(transformFinder);
      expect(transform.transform.storage, storage);
      expect(transform.origin, const Offset(-5.5, 7.25));
      expect(transform.alignment, AlignmentDirectional.bottomStart);
      expect(transform.transformHitTests, isFalse);
      expect(transform.filterQuality, FilterQuality.low);
      expect(render.origin, const Offset(-5.5, 7.25));
      expect(render.alignment, AlignmentDirectional.bottomStart);
      expect(render.textDirection, TextDirection.ltr);
      expect(render.transformHitTests, isFalse);
      expect(render.filterQuality, FilterQuality.low);
      expect(render.size, const Size(80, 40));
      expect(
        tester.getSize(find.byKey(const ValueKey('canvas-widget-$childId'))),
        const Size(80, 40),
      );
      expect(
        find.descendant(
          of: transformFinder,
          matching: find.byKey(const ValueKey('canvas-widget-$transformId')),
        ),
        findsOneWidget,
        reason: 'the real Transform must own Designer hit testing and geometry',
      );
      expect(
        find.descendant(
          of: transformFinder,
          matching: find.byKey(
            const ValueKey('canvas-selection-outline-$transformId'),
          ),
        ),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'uses transformed selection and drop geometry and exact transformHitTests behavior',
    (tester) async {
      const transformId = '6408cfe9-e227-43de-916c-bb9d66224de9';
      CanvasDropResolver? resolver;

      Future<RenderTransform> pump({required bool transformHitTests}) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredTransform(
                  properties: {
                    'transform': const {
                      'kind': 'matrix4',
                      'storage': <Object?>[
                        1,
                        0,
                        0,
                        0,
                        0,
                        1,
                        0,
                        0,
                        0,
                        0,
                        1,
                        0,
                        20,
                        0,
                        0,
                        1,
                      ],
                    },
                    'transformHitTests': {
                      'kind': 'boolean',
                      'value': transformHitTests,
                    },
                  },
                  child: null,
                  tightSize: const Size(80, 40),
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: transformId,
              onSelected: (_) {},
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderTransform>(
          find.byKey(const ValueKey('canvas-transform-$transformId')),
        );
      }

      final transformedHitTests = await pump(transformHitTests: true);
      final transformFinder = find.byKey(
        const ValueKey('canvas-transform-$transformId'),
      );
      final outlineFinder = find.byKey(
        const ValueKey('canvas-selection-outline-$transformId'),
      );
      final layoutRect = tester.getRect(transformFinder);
      final outlineRect = tester.getRect(outlineFinder);
      final visualScale = layoutRect.width / 80;
      expect(transformedHitTests.size, const Size(80, 40));
      expect(outlineRect.size, layoutRect.size);
      expect(
        outlineRect.left - layoutRect.left,
        closeTo(20 * visualScale, 0.01),
        reason:
            'the node key and selection outline must follow RenderTransform',
      );
      expect(outlineRect.top, closeTo(layoutRect.top, 0.01));
      expect(
        transformedHitTests.hitTest(
          BoxHitTestResult(),
          position: const Offset(5, 20),
        ),
        isFalse,
        reason: 'the inverse translation places this point outside the child',
      );
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$transformId'),
        ),
        findsNothing,
        reason: 'a real non-zero layout must not receive an IDE-only target',
      );

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = outlineRect.center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, transformId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);

      final untransformedHitTests = await pump(transformHitTests: false);
      expect(
        untransformedHitTests.hitTest(
          BoxHitTestResult(),
          position: const Offset(5, 20),
        ),
        isTrue,
        reason:
            'transformHitTests false must test the child before translation',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps only an empty zero-layout Transform target and Flutter defaults',
    (tester) async {
      const transformId = '6408cfe9-e227-43de-916c-bb9d66224de9';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredTransform(
                properties: const {
                  'transform': {
                    'kind': 'matrix4',
                    'storage': <Object?>[
                      1,
                      0,
                      0,
                      0,
                      0,
                      1,
                      0,
                      0,
                      0,
                      0,
                      1,
                      0,
                      0,
                      0,
                      0,
                      1,
                    ],
                  },
                },
                child: null,
              ),
            ),
          ),
        ),
      );
      String? selectedWidgetId;
      CanvasDropResolver? resolver;
      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        ),
      );
      await tester.pump();
      await tester.pump();

      final transformFinder = find.byKey(
        const ValueKey('canvas-transform-$transformId'),
      );
      final transform = tester.widget<Transform>(transformFinder);
      final render = tester.renderObject<RenderTransform>(transformFinder);
      final nodeFinder = find.byKey(
        const ValueKey('canvas-widget-$transformId'),
      );
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$transformId'),
      );
      expect(transform.origin, isNull);
      expect(transform.alignment, isNull);
      expect(transform.transformHitTests, isTrue);
      expect(transform.filterQuality, isNull);
      expect(render.size, Size.zero);
      expect(tester.getSize(nodeFinder), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size.square(36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, transformId);
      expect(tester.getSize(nodeFinder), Size.zero);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, transformId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'fails closed for non-finite singular and perspective Transform geometry',
    (tester) async {
      const transformId = '6408cfe9-e227-43de-916c-bb9d66224de9';
      const childId = 'd571f630-fb8d-4c0f-99e7-9cd7eb85337f';
      const zero = <Object?>[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
      const perspectiveAtInfinity = <Object?>[
        0,
        0,
        0,
        1,
        0,
        1,
        0,
        0,
        0,
        0,
        1,
        0,
        1,
        0,
        0,
        0,
      ];
      final cases = <(String, List<Object?>, Map<String, Object?>?)>[
        ('all-zero singular empty Transform', zero, null),
        (
          'perspective empty Transform at infinity',
          perspectiveAtInfinity,
          null,
        ),
        (
          'perspective Transform with a laid-out child at infinity',
          perspectiveAtInfinity,
          _viewSizedBoxNode(childId, width: 80, height: 40),
        ),
      ];
      CanvasDropResolver? resolver;

      for (final (description, storage, child) in cases) {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredTransform(
                  properties: {
                    'transform': {'kind': 'matrix4', 'storage': storage},
                  },
                  child: child,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
        await tester.pump();

        final transform = tester.widget<Transform>(
          find.byKey(const ValueKey('canvas-transform-$transformId')),
        );
        final render = tester.renderObject<RenderTransform>(
          find.byKey(const ValueKey('canvas-transform-$transformId')),
        );
        expect(transform.transform.storage, storage, reason: description);
        expect(
          render.size,
          child == null ? Size.zero : const Size(80, 40),
          reason: description,
        );
        expect(
          find.byKey(
            const ValueKey('canvas-zero-size-widget-target-$transformId'),
          ),
          findsNothing,
          reason: '$description has no finite painted geometry to target',
        );

        final drop = resolver!(500000, 500000);
        expect(
          drop?.parentWidgetId,
          isNot(transformId),
          reason: '$description must not invent a Transform drop zone',
        );
        expect(
          drop?.parentWidgetId,
          isNot(childId),
          reason: '$description must not invent a transformed child drop zone',
        );
        expect(tester.takeException(), isNull, reason: description);
      }
    },
  );

  testWidgets(
    'rejects a projective horizon but keeps same-sign perspective geometry',
    (tester) async {
      const transformId = '6408cfe9-e227-43de-916c-bb9d66224de9';
      CanvasDropResolver? resolver;

      Future<Rect> pump(List<Object?> storage) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredTransform(
                  properties: {
                    'transform': {'kind': 'matrix4', 'storage': storage},
                  },
                  child: null,
                  tightSize: const Size(80, 40),
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
        final finder = find.byKey(
          const ValueKey('canvas-transform-$transformId'),
        );
        expect(tester.getSize(finder), const Size(80, 40));
        return tester.getRect(finder);
      }

      CanvasDropTarget? resolveAt(Rect rendered) {
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final point = rendered.center;
        return resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
      }

      final horizonRect = await pump(const <Object?>[
        1,
        0,
        0,
        -0.025,
        0,
        1,
        0,
        0,
        0,
        0,
        1,
        0,
        0,
        0,
        0,
        1,
      ]);
      expect(horizonRect.isFinite, isTrue);
      expect(
        resolveAt(horizonRect)?.parentWidgetId,
        isNot(transformId),
        reason:
            'w changes sign across the box and its true bounds are unbounded',
      );

      final boundedRect = await pump(const <Object?>[
        1,
        0,
        0,
        0.0025,
        0,
        1,
        0,
        0,
        0,
        0,
        1,
        0,
        0,
        0,
        0,
        1,
      ]);
      expect(boundedRect.isFinite, isTrue);
      final boundedDrop = resolveAt(boundedRect);
      expect(boundedDrop?.parentWidgetId, transformId);
      expect(boundedDrop?.slotName, 'child');
      expect(boundedDrop?.insertionIndex, 0);
      expect(boundedDrop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'uses exact local containment and rejects a degenerate Transform inverse',
    (tester) async {
      const transformId = '6408cfe9-e227-43de-916c-bb9d66224de9';
      CanvasDropResolver? resolver;

      Future<RenderBox> pump(
        List<Object?> storage, {
        Map<String, Object?>? alignment,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredTransform(
                  properties: {
                    'transform': {'kind': 'matrix4', 'storage': storage},
                    'alignment': ?alignment,
                  },
                  child: null,
                  tightSize: const Size(80, 40),
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
        return tester.renderObject<RenderBox>(
          find.byKey(const ValueKey('canvas-widget-$transformId')),
        );
      }

      CanvasDropTarget? resolveAt(Offset point) {
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        return resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
      }

      final rotated = await pump(
        const <Object?>[
          0.7071067811865476,
          0.7071067811865476,
          0,
          0,
          -0.7071067811865476,
          0.7071067811865476,
          0,
          0,
          0,
          0,
          1,
          0,
          0,
          0,
          0,
          1,
        ],
        alignment: const {
          'kind': 'alignmentGeometry',
          'basis': 'physical',
          'horizontal': 0.0,
          'vertical': 0.0,
        },
      );
      final rotatedBounds = MatrixUtils.transformRect(
        rotated.getTransformTo(null),
        Offset.zero & rotated.size,
      );
      final outsidePaintedQuad = rotatedBounds.topLeft + const Offset(1, 1);
      expect(rotatedBounds.isFinite, isTrue);
      expect(rotatedBounds.contains(outsidePaintedQuad), isTrue);
      expect(
        (Offset.zero & rotated.size).contains(
          rotated.globalToLocal(outsidePaintedQuad),
        ),
        isFalse,
        reason: 'the AABB corner lies outside the rotated local rectangle',
      );
      expect(
        resolveAt(outsidePaintedQuad)?.parentWidgetId,
        isNot(transformId),
        reason: 'an AABB corner triangle is not a painted Transform hit',
      );
      expect(
        resolveAt(rotatedBounds.center)?.parentWidgetId,
        transformId,
        reason: 'the center remains inside the rotated local rectangle',
      );

      final degenerate = await pump(const <Object?>[
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        1,
        0,
        0,
        0,
        0,
        1,
      ]);
      final collapsedBounds = MatrixUtils.transformRect(
        degenerate.getTransformTo(null),
        Offset.zero & degenerate.size,
      );
      expect(collapsedBounds.isFinite, isTrue);
      expect(collapsedBounds.isEmpty, isTrue);
      expect(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-$transformId'),
        ),
        findsNothing,
        reason: 'the real Transform layout is 80x40, not zero-sized',
      );
      expect(
        resolveAt(collapsedBounds.center)?.parentWidgetId,
        isNot(transformId),
        reason: 'globalToLocal Offset.zero must not mask a degenerate inverse',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'resolves a 90-degree Row terminal zone and move marker in local axes',
    (tester) async {
      const rowId = '66b26d19-5b49-4f32-9ee2-0f1aefcda249';
      const firstId = '2f02ed09-67b6-47f3-a7b6-d285d79e1d4f';
      const secondId = 'd3a5e444-f78f-4d4e-931a-65141cf849e4';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredTransform(
                properties: const {
                  'transform': {
                    'kind': 'matrix4',
                    'storage': <Object?>[
                      0,
                      1,
                      0,
                      0,
                      -1,
                      0,
                      0,
                      0,
                      0,
                      0,
                      1,
                      0,
                      0,
                      0,
                      0,
                      1,
                    ],
                  },
                  'alignment': {
                    'kind': 'alignmentGeometry',
                    'basis': 'physical',
                    'horizontal': 0.0,
                    'vertical': 0.0,
                  },
                },
                child: <String, Object?>{
                  'id': rowId,
                  'type': 'flutter.widgets.Row',
                  'properties': <String, Object?>{},
                  'slots': <String, Object?>{
                    'children': <String, Object?>{
                      'kind': 'list',
                      'children': <Object?>[
                        _viewSizedBoxNode(firstId, width: 50, height: 40),
                        _viewSizedBoxNode(secondId, width: 50, height: 40),
                      ],
                    },
                  },
                },
                tightSize: const Size(160, 80),
              ),
            ),
          ),
        ),
      );
      CanvasDropResolver? resolver;
      CanvasMovePreviewResolver? moveResolver;
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
            onMovePreviewResolverChanged: (value) => moveResolver = value,
          ),
        ),
      );
      await tester.pump();

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final row = tester.renderObject<RenderBox>(
        find.byKey(const ValueKey('canvas-widget-$rowId')),
      );
      final last = tester.renderObject<RenderBox>(
        find.byKey(const ValueKey('canvas-widget-$secondId')),
      );
      final lastLocalRect = MatrixUtils.transformRect(
        last.getTransformTo(row),
        Offset.zero & last.size,
      );
      final localTerminal = Rect.fromLTRB(
        (lastLocalRect.right - 36).clamp(0.0, row.size.width),
        0,
        row.size.width,
        row.size.height,
      );
      expect(localTerminal.left, greaterThan(0));

      CanvasDropTarget? resolveLocal(Offset local) {
        final point = row.localToGlobal(local);
        return resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
      }

      final terminal = resolveLocal(
        Offset(row.size.width - 1, row.size.height / 2),
      );
      expect(terminal?.parentWidgetId, rowId);
      expect(terminal?.slotName, 'children');
      expect(terminal?.insertionIndex, 2);
      expect(
        resolveLocal(const Offset(1, 1))?.parentWidgetId,
        isNot(rowId),
        reason:
            'the old global-right AABB strip maps to the local Row start edge',
      );

      final zone = terminal!.zone!;
      final globalZone = Rect.fromLTRB(
        surface.left + surface.width * zone.leftMicros / 1000000,
        surface.top + surface.height * zone.topMicros / 1000000,
        surface.left + surface.width * zone.rightMicros / 1000000,
        surface.top + surface.height * zone.bottomMicros / 1000000,
      );
      final expectedZone = MatrixUtils.transformRect(
        row.getTransformTo(null),
        localTerminal,
      );
      expect(globalZone.left, closeTo(expectedZone.left, 0.01));
      expect(globalZone.top, closeTo(expectedZone.top, 0.01));
      expect(globalZone.right, closeTo(expectedZone.right, 0.01));
      expect(globalZone.bottom, closeTo(expectedZone.bottom, 0.01));

      final move = moveResolver!(firstId, rowId, 'children', 1);
      expect(move?.parentWidgetId, rowId);
      expect(move?.slotName, 'children');
      expect(move?.insertionIndex, 1);
      final moveZone = move!.zone!;
      final moveWidth =
          surface.width *
          (moveZone.rightMicros - moveZone.leftMicros) /
          1000000;
      final moveHeight =
          surface.height *
          (moveZone.bottomMicros - moveZone.topMicros) /
          1000000;
      expect(
        moveWidth,
        greaterThan(moveHeight),
        reason:
            'the local vertical Row insertion marker rotates to a horizontal AABB',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'resolves rotated AppBar leading and actions zones in AppBar local axes',
    (tester) async {
      final appBarModel =
          jsonDecode(utf8.decode(fixture.appBarModelBytesForViewTest()))
              as Map<String, Object?>;
      final nestedScaffold = appBarModel['root']! as Map<String, Object?>;
      final nestedSlots = nestedScaffold['slots']! as Map<String, Object?>;
      final appBar =
          (nestedSlots['appBar']! as Map<String, Object?>)['child']!
              as Map<String, Object?>;
      appBar['properties'] = <String, Object?>{};
      final appBarSlots = appBar['slots']! as Map<String, Object?>;
      for (final slotName in const [
        'leading',
        'title',
        'flexibleSpace',
        'bottom',
      ]) {
        (appBarSlots[slotName]! as Map<String, Object?>)['child'] = null;
      }
      (appBarSlots['actions']! as Map<String, Object?>)['children'] =
          <Object?>[];

      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredTransform(
                properties: const {
                  'transform': {
                    'kind': 'matrix4',
                    'storage': <Object?>[
                      0,
                      1,
                      0,
                      0,
                      -1,
                      0,
                      0,
                      0,
                      0,
                      0,
                      1,
                      0,
                      0,
                      0,
                      0,
                      1,
                    ],
                  },
                  'alignment': {
                    'kind': 'alignmentGeometry',
                    'basis': 'physical',
                    'horizontal': 0.0,
                    'vertical': 0.0,
                  },
                },
                child: nestedScaffold,
                tightSize: const Size(300, 240),
              ),
            ),
          ),
        ),
      );
      CanvasDropResolver? resolver;
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final appBarBox = tester.renderObject<RenderBox>(
        find.byKey(
          const ValueKey('canvas-widget-${fixture.appBarWidgetIdForViewTest}'),
        ),
      );
      final fallbackBottom = math.min(36.0, appBarBox.size.height / 3);
      final toolbarHeight = appBarBox.size.height - fallbackBottom;

      CanvasDropTarget? resolveLocal(Offset local) {
        final point = appBarBox.localToGlobal(local);
        return resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
      }

      final actions = resolveLocal(
        Offset(appBarBox.size.width - 1, toolbarHeight / 2),
      );
      expect(actions?.parentWidgetId, fixture.appBarWidgetIdForViewTest);
      expect(actions?.slotName, 'actions');
      expect(actions?.insertionIndex, 0);

      final leading = resolveLocal(Offset(1, toolbarHeight / 2));
      expect(leading?.parentWidgetId, fixture.appBarWidgetIdForViewTest);
      expect(
        leading?.slotName,
        'leading',
        reason:
            'the global-top position must not be mistaken for global-axis actions',
      );

      final actionsZone = actions!.zone!;
      final actionsWidth =
          surface.width *
          (actionsZone.rightMicros - actionsZone.leftMicros) /
          1000000;
      final actionsHeight =
          surface.height *
          (actionsZone.bottomMicros - actionsZone.topMicros) /
          1000000;
      expect(
        actionsHeight,
        greaterThan(actionsWidth),
        reason:
            'the rectangular feedback is the AABB of the rotated local trailing zone',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real Opacity at exact alpha endpoints while preserving hit testing and outer Designer control',
    (tester) async {
      const opacityId = '47f754c8-9fcb-480c-9578-87cda83bd4d5';
      final source = _modelJsonForView();
      final sourceRoot = source['root']! as Map<String, Object?>;
      final text = _findNode(sourceRoot, 'flutter.widgets.Text');
      final textId = text['id']! as String;
      Future<RenderOpacity> pump({
        required double opacity,
        required bool? alwaysIncludeSemantics,
        String? selected,
      }) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredOpacity(
                  opacity: opacity,
                  alwaysIncludeSemantics: alwaysIncludeSemantics,
                  child: text,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: selected,
              onSelected: (_) {},
            ),
          ),
        );
        await tester.pump();
        final opacityFinder = find
            .descendant(
              of: find.byKey(const ValueKey('canvas-widget-$opacityId')),
              matching: find.byType(Opacity),
            )
            .first;
        return tester.renderObject<RenderOpacity>(opacityFinder);
      }

      final hidden = await pump(
        opacity: 0,
        alwaysIncludeSemantics: null,
        selected: opacityId,
      );
      expect(hidden.opacity, 0);
      expect(hidden.alwaysIncludeSemantics, isFalse);
      expect(
        (hidden.updateCompositedLayer(oldLayer: null) as OpacityLayer).alpha,
        0,
      );
      final selectionOutline = find.byKey(
        const ValueKey('canvas-selection-outline-$opacityId'),
      );
      expect(selectionOutline, findsOneWidget);
      final renderedOpacity = find
          .descendant(
            of: find.byKey(const ValueKey('canvas-widget-$opacityId')),
            matching: find.byType(Opacity),
          )
          .first;
      expect(
        find.ancestor(of: renderedOpacity, matching: selectionOutline),
        findsOneWidget,
        reason: 'the Designer outline wrapper must remain outside Opacity',
      );
      expect(
        find.ancestor(of: selectionOutline, matching: find.byType(Opacity)),
        findsNothing,
      );
      expect(
        find.bySemanticsLabel(RegExp('Opacity $opacityId')),
        findsOneWidget,
      );
      expect(find.bySemanticsLabel(RegExp('Text $textId')), findsNothing);
      final hitTest = BoxHitTestResult();
      expect(
        hidden.hitTest(hitTest, position: hidden.size.center(Offset.zero)),
        isTrue,
        reason: 'opacity zero must not disable Flutter hit testing',
      );
      expect(
        hitTest.path.any((entry) => identical(entry.target, hidden)),
        isTrue,
      );

      final semantic = await pump(opacity: 0, alwaysIncludeSemantics: true);
      expect(semantic.alwaysIncludeSemantics, isTrue);
      expect(find.bySemanticsLabel(RegExp('Text $textId')), findsOneWidget);

      final fractional = await pump(
        opacity: 0.5,
        alwaysIncludeSemantics: false,
      );
      expect(fractional.opacity, 0.5);
      expect(
        (fractional.updateCompositedLayer(oldLayer: null) as OpacityLayer)
            .alpha,
        128,
      );
      expect(fractional.isRepaintBoundary, isTrue);

      final opaque = await pump(opacity: 1, alwaysIncludeSemantics: null);
      expect(opaque.opacity, 1);
      expect(
        (opaque.updateCompositedLayer(oldLayer: null) as OpacityLayer).alpha,
        255,
      );
      expect(find.byType(AnimatedOpacity), findsNothing);
    },
  );

  testWidgets(
    'keeps empty Opacity at zero layout with a selectable child drop target',
    (tester) async {
      const opacityId = '47f754c8-9fcb-480c-9578-87cda83bd4d5';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredOpacity(
                opacity: 0,
                alwaysIncludeSemantics: null,
                child: null,
              ),
            ),
          ),
        ),
      );
      String? selectedWidgetId;
      CanvasDropResolver? resolver;
      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$opacityId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$opacityId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, opacityId);
      expect(tester.getSize(rendered), Size.zero);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, opacityId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
    },
  );

  testWidgets(
    'renders real Placeholder defaults and exact values on native and Web profiles',
    (tester) async {
      const placeholderId = 'b4d88c9a-9eca-4a53-92a6-71f1631ef235';
      const childId = '5cdf8725-c754-4590-873e-c2e60e8c8872';
      final cases =
          <
            ({
              String platform,
              Map<String, Object?> properties,
              double strokeWidth,
              double fallbackWidth,
              double fallbackHeight,
              Color? literalColor,
            })
          >[
            (
              platform: 'windows',
              properties: const {},
              strokeWidth: 2,
              fallbackWidth: 400,
              fallbackHeight: 400,
              literalColor: const Color(0xFF455A64),
            ),
            (
              platform: 'web',
              properties: const {
                'color': {
                  'kind': 'themeToken',
                  'token': 'material.colorScheme.secondaryContainer',
                },
                'strokeWidth': {'kind': 'double', 'value': 3.5},
                'fallbackWidth': {'kind': 'integer', 'value': 160},
                'fallbackHeight': {'kind': 'double', 'value': 90.5},
              },
              strokeWidth: 3.5,
              fallbackWidth: 160,
              fallbackHeight: 90.5,
              literalColor: null,
            ),
          ];

      for (final entry in cases) {
        final json = _modelWithPlaceholder(
          properties: entry.properties,
          child: _viewSizedBoxNode(childId, width: 80, height: 40),
        );
        (json['profile']! as Map<String, Object?>)['targetPlatform'] =
            entry.platform;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();

        final placeholderFinder = find.descendant(
          of: find.byKey(const ValueKey('canvas-widget-$placeholderId')),
          matching: find.byType(Placeholder),
        );
        expect(placeholderFinder, findsOneWidget, reason: entry.platform);
        final placeholder = tester.widget<Placeholder>(placeholderFinder);
        final expectedColor =
            entry.literalColor ??
            Theme.of(
              tester.element(placeholderFinder),
            ).colorScheme.secondaryContainer;
        expect(placeholder.color, expectedColor, reason: entry.platform);
        expect(
          placeholder.strokeWidth,
          entry.strokeWidth,
          reason: entry.platform,
        );
        expect(
          placeholder.fallbackWidth,
          entry.fallbackWidth,
          reason: entry.platform,
        );
        expect(
          placeholder.fallbackHeight,
          entry.fallbackHeight,
          reason: entry.platform,
        );
        expect(placeholder.child, isNotNull, reason: entry.platform);
        expect(
          find.descendant(
            of: placeholderFinder,
            matching: find.byKey(const ValueKey('canvas-widget-$childId')),
          ),
          findsOneWidget,
          reason: entry.platform,
        );
        expect(tester.takeException(), isNull, reason: entry.platform);
      }
    },
  );

  testWidgets(
    'selects empty Placeholder, resolves its child drop, and blocks occupied replacement',
    (tester) async {
      const placeholderId = 'b4d88c9a-9eca-4a53-92a6-71f1631ef235';
      const sourceId = 'f23a2095-d86c-4be0-96f0-08f34dd771a7';
      CanvasDropResolver? dropResolver;
      CanvasMovePreviewResolver? moveResolver;
      String? selectedWidgetId;
      var currentModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithPlaceholder(
                properties: const {
                  'color': {'kind': 'color', 'argb': '0xFF123456'},
                  'strokeWidth': {'kind': 'integer', 'value': 1},
                  'fallbackWidth': {'kind': 'integer', 'value': 120},
                  'fallbackHeight': {'kind': 'integer', 'value': 80},
                },
                child: null,
                sibling: _viewTextNode(sourceId, 'Move me'),
              ),
            ),
          ),
        ),
      );
      StateSetter? rebuild;
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) {
            rebuild = setState;
            return CanvasModelApp(
              model: currentModel,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => dropResolver = value,
              onMovePreviewResolverChanged: (value) => moveResolver = value,
            );
          },
        ),
      );
      await tester.pump();

      final rendered = find.byKey(
        const ValueKey('canvas-widget-$placeholderId'),
      );
      final placeholderFinder = find.descendant(
        of: rendered,
        matching: find.byType(Placeholder),
      );
      expect(placeholderFinder, findsOneWidget);
      expect(tester.widget<Placeholder>(placeholderFinder).child, isNull);
      expect(tester.getSize(rendered).height, 80);

      await tester.tapAt(tester.getRect(rendered).center);
      await tester.pump();
      expect(selectedWidgetId, placeholderId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(rendered).center;
      int micros(double value, double origin, double extent) =>
          ((value - origin) / extent * 1000000).round();
      final drop = dropResolver!(
        micros(point.dx, surface.left, surface.width),
        micros(point.dy, surface.top, surface.height),
      );
      expect(drop?.parentWidgetId, placeholderId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);

      final move = moveResolver!(sourceId, placeholderId, 'child', 0);
      expect(move?.parentWidgetId, placeholderId);
      expect(move?.slotName, 'child');
      expect(move?.insertionIndex, 0);

      final occupied = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithPlaceholder(
                properties: const {},
                child: _viewTextNode(
                  '1e58d148-1c48-499f-9a93-c8ad21759f6d',
                  'Occupied',
                ),
                sibling: _viewTextNode(sourceId, 'Move me'),
              ),
            ),
          ),
        ),
      );
      rebuild!(() {
        currentModel = occupied;
        selectedWidgetId = null;
      });
      await tester.pump();
      expect(moveResolver!(sourceId, placeholderId, 'child', 0), isNull);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'keeps a zero-fallback Placeholder real and adds only a transient target',
    (tester) async {
      const placeholderId = 'b4d88c9a-9eca-4a53-92a6-71f1631ef235';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithPlaceholder(
                properties: const {
                  'fallbackHeight': {'kind': 'integer', 'value': 0},
                },
                child: null,
                sibling: _viewTextNode(
                  'f23a2095-d86c-4be0-96f0-08f34dd771a7',
                  'Below',
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final rendered = find.byKey(
        const ValueKey('canvas-widget-$placeholderId'),
      );
      final placeholderFinder = find.descendant(
        of: rendered,
        matching: find.byType(Placeholder),
      );
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$placeholderId'),
      );
      expect(tester.getSize(rendered).height, 0);
      expect(tester.getSize(placeholderFinder).height, 0);
      expect(target, findsOneWidget);
      expect(tester.getSize(target).height, 36);
      expect(tester.getSize(target).width, greaterThanOrEqualTo(36));
      expect(tester.getSize(target).width.isFinite, isTrue);
      expect(tester.getSize(rendered).height, 0);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real ColoredBox with exact literal or theme color on native and Web profiles',
    (tester) async {
      const coloredBoxId = 'ec949ebe-9c66-48b7-8901-c691d7356e07';
      const childId = '01d263fe-dc50-4ba9-823d-fd5271db0590';
      final cases = <({String platform, bool literal, bool antiAlias})>[
        (platform: 'windows', literal: true, antiAlias: false),
        (platform: 'web', literal: false, antiAlias: true),
      ];

      for (final entry in cases) {
        final properties = <String, Object?>{
          'color': entry.literal
              ? <String, Object?>{'kind': 'color', 'argb': '0xFF2196F3'}
              : <String, Object?>{
                  'kind': 'themeToken',
                  'token': 'material.colorScheme.primaryContainer',
                },
          if (!entry.antiAlias)
            'isAntiAlias': {'kind': 'boolean', 'value': false},
        };
        final json = _modelWithColoredBox(
          properties: properties,
          child: _viewSizedBoxNode(childId, width: 80, height: 40),
        );
        (json['profile']! as Map<String, Object?>)['targetPlatform'] =
            entry.platform;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();

        final coloredBoxFinder = find.descendant(
          of: find.byKey(const ValueKey('canvas-widget-$coloredBoxId')),
          matching: find.byType(ColoredBox),
        );
        expect(coloredBoxFinder, findsOneWidget, reason: entry.platform);
        final coloredBox = tester.widget<ColoredBox>(coloredBoxFinder);
        final expectedColor = entry.literal
            ? const Color(0xff2196f3)
            : Theme.of(
                tester.element(coloredBoxFinder),
              ).colorScheme.primaryContainer;
        expect(coloredBox.color, expectedColor, reason: entry.platform);
        expect(coloredBox.isAntiAlias, entry.antiAlias, reason: entry.platform);
        expect(coloredBox.child, isNotNull, reason: entry.platform);
        expect(
          tester.getSize(
            find.byKey(const ValueKey('canvas-widget-$coloredBoxId')),
          ),
          const Size(80, 40),
          reason: entry.platform,
        );
        expect(
          find.descendant(
            of: coloredBoxFinder,
            matching: find.byKey(const ValueKey('canvas-widget-$childId')),
          ),
          findsOneWidget,
          reason: entry.platform,
        );
        expect(tester.takeException(), isNull, reason: entry.platform);
      }
    },
  );

  testWidgets(
    'keeps empty ColoredBox at zero layout with a 36px child target and move parity',
    (tester) async {
      const coloredBoxId = 'ec949ebe-9c66-48b7-8901-c691d7356e07';
      const sourceId = '0f91484c-46d1-439c-a8cf-786009b58842';
      CanvasDropResolver? dropResolver;
      CanvasMovePreviewResolver? moveResolver;
      String? selectedWidgetId;
      var currentModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithColoredBox(
                properties: const {
                  'color': {'kind': 'color', 'argb': '0x00000000'},
                },
                child: null,
                sibling: _viewTextNode(sourceId, 'Move me'),
              ),
            ),
          ),
        ),
      );
      StateSetter? rebuild;
      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) {
            rebuild = setState;
            return CanvasModelApp(
              model: currentModel,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => dropResolver = value,
              onMovePreviewResolverChanged: (value) => moveResolver = value,
            );
          },
        ),
      );
      await tester.pump();

      final rendered = find.byKey(
        const ValueKey('canvas-widget-$coloredBoxId'),
      );
      final coloredBoxFinder = find.descendant(
        of: rendered,
        matching: find.byType(ColoredBox),
      );
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$coloredBoxId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(tester.getSize(coloredBoxFinder), Size.zero);
      expect(tester.widget<ColoredBox>(coloredBoxFinder).child, isNull);
      expect(tester.widget<ColoredBox>(coloredBoxFinder).isAntiAlias, isTrue);
      expect(
        find.descendant(of: rendered, matching: find.byType(SizedBox)),
        findsNothing,
        reason: 'the 36px Designer target must not affect real widget layout',
      );
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size.square(36));

      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, coloredBoxId);
      expect(tester.getSize(rendered), Size.zero);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      int micros(double value, double origin, double extent) =>
          ((value - origin) / extent * 1000000).round();
      final drop = dropResolver!(
        micros(point.dx, surface.left, surface.width),
        micros(point.dy, surface.top, surface.height),
      );
      expect(drop?.parentWidgetId, coloredBoxId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);

      final move = moveResolver!(sourceId, coloredBoxId, 'child', 0);
      expect(move?.parentWidgetId, coloredBoxId);
      expect(move?.slotName, 'child');
      expect(move?.insertionIndex, 0);
      expect(move?.zone?.isEmpty, isFalse);

      final occupied = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithColoredBox(
                properties: const {
                  'color': {
                    'kind': 'themeToken',
                    'token': 'material.colorScheme.primary',
                  },
                },
                child: _viewTextNode(
                  'd1017247-3711-4402-b039-3c72307ba7d7',
                  'Occupied',
                ),
                sibling: _viewTextNode(sourceId, 'Move me'),
              ),
            ),
          ),
        ),
      );
      rebuild!(() {
        currentModel = occupied;
        selectedWidgetId = null;
      });
      await tester.pump();
      expect(moveResolver!(sourceId, coloredBoxId, 'child', 0), isNull);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real Directionality LTR and RTL on native and exact-Web profiles',
    (tester) async {
      const directionalityId = '1dd83790-acde-4aa4-8d58-4ab79f08042d';
      const childId = 'd2156ed9-c715-4bb4-a245-70326cafba93';
      for (final entry in const [
        (
          platform: 'windows',
          wireDirection: 'ltr',
          expected: TextDirection.ltr,
        ),
        (platform: 'web', wireDirection: 'rtl', expected: TextDirection.rtl),
      ]) {
        final json = _modelWithDirectionality(
          direction: entry.wireDirection,
          child: _viewSizedBoxNode(childId, width: 80, height: 40),
        );
        (json['profile']! as Map<String, Object?>)['targetPlatform'] =
            entry.platform;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();

        final rendered = find.byKey(
          const ValueKey('canvas-widget-$directionalityId'),
        );
        final directionalityFinder = find.descendant(
          of: rendered,
          matching: find.byType(Directionality),
        );
        expect(directionalityFinder, findsOneWidget, reason: entry.platform);
        final directionality = tester.widget<Directionality>(
          directionalityFinder,
        );
        expect(
          directionality.textDirection,
          entry.expected,
          reason: entry.platform,
        );
        final childFinder = find.byKey(
          const ValueKey('canvas-widget-$childId'),
        );
        expect(childFinder, findsOneWidget, reason: entry.platform);
        expect(
          Directionality.of(tester.element(childFinder)),
          entry.expected,
          reason: 'the real wrapper must own descendant directionality',
        );
        expect(tester.getSize(rendered), const Size(80, 40));
        expect(tester.takeException(), isNull, reason: entry.platform);
      }
    },
  );

  testWidgets(
    'selects zero-layout Directionality and previews only legal same-tree moves',
    (tester) async {
      const directionalityId = '1dd83790-acde-4aa4-8d58-4ab79f08042d';
      const childId = 'd2156ed9-c715-4bb4-a245-70326cafba93';
      const targetId = '80f095f4-f05a-45a7-8a89-917723a211f3';
      CanvasMovePreviewResolver? moveResolver;
      String? selectedWidgetId;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithDirectionality(
                direction: 'rtl',
                child: _viewSizedBoxNode(childId, width: 0, height: 0),
                sibling: <String, Object?>{
                  'id': targetId,
                  'type': 'flutter.widgets.Center',
                  'properties': <String, Object?>{},
                  'slots': <String, Object?>{
                    'child': <String, Object?>{'kind': 'single', 'child': null},
                  },
                },
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (id) => selectedWidgetId = id,
          onMovePreviewResolverChanged: (value) => moveResolver = value,
        ),
      );
      await tester.pump();

      final rendered = find.byKey(
        const ValueKey('canvas-widget-$directionalityId'),
      );
      final target = find.byKey(
        const ValueKey(
          'canvas-zero-size-widget-target-group-$directionalityId',
        ),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size.square(36));
      expect(
        find.descendant(of: rendered, matching: find.byType(SizedBox)),
        findsOneWidget,
        reason: 'only the persisted zero-size child belongs in real layout',
      );

      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, directionalityId);
      expect(tester.getSize(rendered), Size.zero);

      final move = moveResolver!(directionalityId, targetId, 'child', 0);
      expect(move?.parentWidgetId, targetId);
      expect(move?.slotName, 'child');
      expect(move?.insertionIndex, 0);
      expect(move?.zone?.isEmpty, isFalse);
      expect(
        moveResolver!(childId, directionalityId, 'child', 0),
        isNotNull,
        reason:
            'moving the retained child to its current required slot is stable',
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real SafeArea with exact defaults and physical minimum on native and Web profiles',
    (tester) async {
      const safeAreaId = 'df5babd2-16cf-44c4-b497-24375532ec68';
      const childId = 'b84ced1f-049e-479a-a5c1-6940a09ca2b3';
      final cases =
          <
            ({
              String platform,
              Map<String, Object?> properties,
              EdgeInsets minimum,
            })
          >[
            (
              platform: 'windows',
              properties: const {},
              minimum: EdgeInsets.zero,
            ),
            (
              platform: 'web',
              properties: const {
                'left': {'kind': 'boolean', 'value': false},
                'top': {'kind': 'boolean', 'value': true},
                'right': {'kind': 'boolean', 'value': false},
                'bottom': {'kind': 'boolean', 'value': true},
                'minimum': {
                  'kind': 'edgeInsets',
                  'left': 3,
                  'top': 4,
                  'right': 5,
                  'bottom': 6,
                },
                'maintainBottomViewPadding': {'kind': 'boolean', 'value': true},
              },
              minimum: EdgeInsets.fromLTRB(3, 4, 5, 6),
            ),
          ];

      for (final entry in cases) {
        final json = _modelWithSafeArea(
          properties: entry.properties,
          child: _viewSizedBoxNode(childId, width: 80, height: 40),
        );
        (json['profile']! as Map<String, Object?>)['targetPlatform'] =
            entry.platform;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();

        final safeAreaFinder = find.descendant(
          of: find.byKey(const ValueKey('canvas-widget-$safeAreaId')),
          matching: find.byType(SafeArea),
        );
        expect(safeAreaFinder, findsOneWidget, reason: entry.platform);
        final safeArea = tester.widget<SafeArea>(safeAreaFinder);
        expect(safeArea.left, entry.platform == 'windows');
        expect(safeArea.top, isTrue);
        expect(safeArea.right, entry.platform == 'windows');
        expect(safeArea.bottom, isTrue);
        expect(safeArea.minimum, entry.minimum);
        expect(safeArea.maintainBottomViewPadding, entry.platform == 'web');
        expect(safeArea.child, isNotNull);
        expect(
          find.descendant(
            of: safeAreaFinder,
            matching: find.byKey(const ValueKey('canvas-widget-$childId')),
          ),
          findsOneWidget,
        );
        expect(
          tester.getSize(safeAreaFinder),
          Size(80 + entry.minimum.horizontal, 40 + entry.minimum.vertical),
          reason: entry.platform,
        );
        expect(tester.takeException(), isNull, reason: entry.platform);
      }
    },
  );

  testWidgets(
    'keeps zero-layout SafeArea real and exposes only a transient 36px target',
    (tester) async {
      const safeAreaId = 'df5babd2-16cf-44c4-b497-24375532ec68';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithSafeArea(
                properties: const {},
                child: _viewSizedBoxNode(
                  'b84ced1f-049e-479a-a5c1-6940a09ca2b3',
                  width: 0,
                  height: 0,
                ),
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$safeAreaId'));
      final safeAreaFinder = find.descendant(
        of: rendered,
        matching: find.byType(SafeArea),
      );
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-group-$safeAreaId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(tester.getSize(safeAreaFinder), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size.square(36));
      expect(tester.getSize(rendered), Size.zero);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real DecoratedBox position and BoxDecoration on native and exact-Web profiles',
    (tester) async {
      const decoratedBoxId = '607a1c0f-5fd3-438c-ae36-0a054ce05949';
      const childId = '2042d2d2-013a-4366-9483-71dcc7d1a711';
      final semantics = tester.ensureSemantics();
      final cases = <({String platform, bool foreground, bool themed})>[
        (platform: 'windows', foreground: false, themed: false),
        (platform: 'web', foreground: true, themed: true),
      ];

      for (final entry in cases) {
        final json = _modelWithDecoratedBox(
          properties: {
            'decoration': _viewBoxDecoration(
              color: entry.themed
                  ? _viewThemeColor('material.colorScheme.primaryContainer')
                  : _viewLiteralColor('0xFF123456'),
              borderRadius: _viewPhysicalRadius(),
              boxShadow: [_viewBoxShadow()],
            ),
            if (entry.foreground)
              'position': {
                'kind': 'enum',
                'type': 'DecorationPosition',
                'value': 'foreground',
              },
          },
          child: _viewSizedBoxNode(childId, width: 80, height: 40),
        );
        (json['profile']! as Map<String, Object?>)['targetPlatform'] =
            entry.platform;
        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();

        final rendered = find.byKey(
          const ValueKey('canvas-widget-$decoratedBoxId'),
        );
        final decoratedBoxFinder = find.descendant(
          of: rendered,
          matching: find.byType(DecoratedBox),
        );
        expect(decoratedBoxFinder, findsOneWidget, reason: entry.platform);
        final decoratedBox = tester.widget<DecoratedBox>(decoratedBoxFinder);
        expect(
          decoratedBox.position,
          entry.foreground
              ? DecorationPosition.foreground
              : DecorationPosition.background,
          reason: entry.platform,
        );
        final decoration = decoratedBox.decoration as BoxDecoration;
        final expectedColor = entry.themed
            ? Theme.of(
                tester.element(decoratedBoxFinder),
              ).colorScheme.primaryContainer
            : const Color(0xff123456);
        expect(decoration.color, expectedColor, reason: entry.platform);
        expect(decoration.borderRadius, isNotNull, reason: entry.platform);
        expect(decoration.boxShadow, hasLength(1), reason: entry.platform);
        expect(decoratedBox.child, isNotNull, reason: entry.platform);
        expect(tester.getSize(rendered), const Size(80, 40));
        expect(
          find.descendant(
            of: decoratedBoxFinder,
            matching: find.byKey(const ValueKey('canvas-widget-$childId')),
          ),
          findsOneWidget,
          reason: entry.platform,
        );
        expect(
          find.bySemanticsLabel(
            RegExp('DecoratedBox ${RegExp.escape(decoratedBoxId)}'),
          ),
          findsOneWidget,
          reason: 'Designer accessibility identity on ${entry.platform}',
        );
        expect(tester.takeException(), isNull, reason: entry.platform);
      }
      semantics.dispose();
    },
  );

  testWidgets(
    'keeps empty DecoratedBox at zero layout with a transient selectable child target',
    (tester) async {
      const decoratedBoxId = '607a1c0f-5fd3-438c-ae36-0a054ce05949';
      String? selectedWidgetId;
      CanvasDropResolver? resolver;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithDecoratedBox(
                properties: {'decoration': _viewBoxDecoration()},
                child: null,
              ),
            ),
          ),
        ),
      );

      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(
        const ValueKey('canvas-widget-$decoratedBoxId'),
      );
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$decoratedBoxId'),
      );
      final decoratedBoxFinder = find.descendant(
        of: rendered,
        matching: find.byType(DecoratedBox),
      );
      expect(decoratedBoxFinder, findsOneWidget);
      expect(tester.widget<DecoratedBox>(decoratedBoxFinder).child, isNull);
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size.square(36));

      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, decoratedBoxId);
      expect(tester.getSize(rendered), Size.zero);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, decoratedBoxId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders real ExcludeSemantics and keeps Designer semantics, layout, paint, and hits exact',
    (tester) async {
      const excludeId = '2a082f33-7251-4a63-9f41-845aa0aa7a80';
      const sizedId = '3b193044-8362-4b74-a052-956bb1bb8b91';
      const textId = '4cded0e1-23b0-43f2-b310-ab6ca68e99dc';
      const applicationLabel = 'Application child semantics';
      final semantics = tester.ensureSemantics();
      final cases = <({String platform, bool? stored, bool effective})>[
        (platform: 'windows', stored: null, effective: true),
        (platform: 'web', stored: null, effective: true),
        (platform: 'windows', stored: true, effective: true),
        (platform: 'web', stored: true, effective: true),
        (platform: 'windows', stored: false, effective: false),
        (platform: 'web', stored: false, effective: false),
      ];

      for (final entry in cases) {
        final text = _viewTextNode(textId, 'Painted child');
        (text['properties']! as Map<String, Object?>)['semanticsLabel'] = {
          'kind': 'string',
          'value': applicationLabel,
        };
        final sized = _viewSizedBoxNode(sizedId, width: 80, height: 40);
        ((sized['slots']! as Map<String, Object?>)['child']!
                as Map<String, Object?>)['child'] =
            text;
        final json = _modelWithExcludeSemantics(
          properties: {
            if (entry.stored != null)
              'excluding': {'kind': 'boolean', 'value': entry.stored},
          },
          child: sized,
        );
        (json['profile']! as Map<String, Object?>)['targetPlatform'] =
            entry.platform;
        String? selectedWidgetId;

        await tester.pumpWidget(
          CanvasModelApp(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (id) => selectedWidgetId = id,
          ),
        );
        await tester.pump();

        final rendered = find.byKey(const ValueKey('canvas-widget-$excludeId'));
        final excludeFinder = find.ancestor(
          of: find.byKey(const ValueKey('canvas-widget-$sizedId')),
          matching: find.byType(ExcludeSemantics),
        );
        expect(excludeFinder, findsOneWidget, reason: entry.toString());
        final exclude = tester.widget<ExcludeSemantics>(excludeFinder);
        expect(exclude.excluding, entry.effective, reason: entry.toString());
        expect(exclude.child, isNotNull, reason: entry.toString());
        expect(tester.getSize(rendered), const Size(80, 40));
        expect(tester.getSize(excludeFinder), const Size(80, 40));

        final textFinder = find.text('Painted child');
        expect(textFinder, findsOneWidget, reason: entry.toString());
        final richTextFinder = find.descendant(
          of: find.byKey(const ValueKey('canvas-widget-$textId')),
          matching: find.byType(RichText),
        );
        expect(richTextFinder, findsOneWidget, reason: entry.toString());
        final paragraph = tester.renderObject<RenderParagraph>(richTextFinder);
        expect(paragraph.size.isEmpty, isFalse, reason: entry.toString());
        expect(
          paragraph.paintBounds.isEmpty,
          isFalse,
          reason: entry.toString(),
        );
        expect(paragraph.debugNeedsPaint, isFalse, reason: entry.toString());

        expect(
          find.bySemanticsLabel(
            RegExp('ExcludeSemantics ${RegExp.escape(excludeId)}'),
          ),
          findsOneWidget,
          reason: 'Designer node remains accessible on ${entry.platform}',
        );
        expect(
          find.bySemanticsLabel(RegExp(applicationLabel)),
          entry.effective ? findsNothing : findsOneWidget,
          reason: entry.toString(),
        );
        expect(
          find.bySemanticsLabel(RegExp('Text ${RegExp.escape(textId)}')),
          entry.effective ? findsNothing : findsOneWidget,
          reason: entry.toString(),
        );

        await tester.tap(find.byKey(const ValueKey('canvas-widget-$textId')));
        await tester.pump();
        expect(
          selectedWidgetId,
          textId,
          reason: 'ExcludeSemantics must not block hits on ${entry.platform}',
        );
        expect(tester.takeException(), isNull, reason: entry.toString());
      }
      semantics.dispose();
    },
  );

  testWidgets(
    'keeps empty ExcludeSemantics zero-sized with a selectable child target',
    (tester) async {
      const excludeId = '2a082f33-7251-4a63-9f41-845aa0aa7a80';
      String? selectedWidgetId;
      CanvasDropResolver? resolver;
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithExcludeSemantics(properties: const {}, child: null),
            ),
          ),
        ),
      );

      await tester.pumpWidget(
        StatefulBuilder(
          builder: (context, setState) => CanvasModelApp(
            model: model,
            selectedWidgetId: selectedWidgetId,
            onSelected: (id) => setState(() => selectedWidgetId = id),
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$excludeId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$excludeId'),
      );
      final excludeFinder = find.descendant(
        of: rendered,
        matching: find.byType(ExcludeSemantics),
      );
      expect(excludeFinder, findsOneWidget);
      final exclude = tester.widget<ExcludeSemantics>(excludeFinder);
      expect(exclude.excluding, isTrue);
      expect(exclude.child, isNull);
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size.square(36));

      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, excludeId);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, excludeId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
      expect(drop?.zone?.isEmpty, isFalse);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'renders every Container argument through real Flutter objects and keeps its outline outside transform',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';
      final json = _modelJsonForView();
      final sourceRoot = json['root']! as Map<String, Object?>;
      final text = _findNode(sourceRoot, 'flutter.widgets.Text');
      final properties = <String, Object?>{
        'alignment': _viewAlignment(
          basis: 'directional',
          horizontal: 0.5,
          vertical: -0.25,
        ),
        'padding': {
          'kind': 'edgeInsetsDirectional',
          'start': 5,
          'top': 6,
          'end': 7,
          'bottom': 8,
        },
        'isAntiAlias': {'kind': 'boolean', 'value': false},
        'decoration': _viewBoxDecoration(
          color: _viewThemeColor('material.colorScheme.primaryContainer'),
          border: _viewPhysicalBorder(),
          borderRadius: _viewDirectionalRadius(),
          boxShadow: [_viewBoxShadow()],
          gradient: _viewLinearGradient(),
          backgroundBlendMode: 'multiply',
        ),
        'foregroundDecoration': _viewBoxDecoration(
          color: _viewLiteralColor('0x22112233'),
          shape: 'circle',
        ),
        'width': {'kind': 'integer', 'value': 180},
        'height': {'kind': 'double', 'value': 100.5},
        'constraints': {
          'kind': 'boxConstraints',
          'minWidth': 120,
          'maxWidth': 240,
          'minHeight': 80,
          'maxHeight': null,
        },
        'margin': {
          'kind': 'edgeInsets',
          'left': 10,
          'top': 12,
          'right': 14,
          'bottom': 16,
        },
        'transform': {
          'kind': 'matrix4',
          'storage': <Object?>[
            1,
            0,
            0,
            0,
            0,
            1,
            0,
            0,
            0,
            0,
            1,
            0,
            24,
            -12,
            0,
            1,
          ],
        },
        'transformAlignment': _viewAlignment(horizontal: 1, vertical: -1),
        'clipBehavior': {'kind': 'enum', 'type': 'Clip', 'value': 'hardEdge'},
      };
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredContainer(properties: properties, child: text),
            ),
          ),
        ),
      );

      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: containerId,
            onSelected: (_) {},
          ),
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$containerId'));
      final containerFinder = find
          .descendant(of: node, matching: find.byType(Container))
          .first;
      final container = tester.widget<Container>(containerFinder);
      expect(container.alignment, const AlignmentDirectional(0.5, -0.25));
      expect(
        container.padding,
        const EdgeInsetsDirectional.fromSTEB(5, 6, 7, 8),
      );
      expect(container.color, isNull);
      expect(container.isAntiAlias, isFalse);
      expect(
        container.constraints,
        const BoxConstraints(
          minWidth: 180,
          maxWidth: 180,
          minHeight: 100.5,
          maxHeight: 100.5,
        ),
        reason: 'Container folds width and height into constraints via tighten',
      );
      expect(container.margin, const EdgeInsets.fromLTRB(10, 12, 14, 16));
      expect(container.transform!.storage[12], 24);
      expect(container.transform!.storage[13], -12);
      expect(container.transformAlignment, Alignment.topRight);
      expect(container.clipBehavior, Clip.hardEdge);
      expect(container.child, isNotNull);
      expect(find.text('Hello'), findsOneWidget);

      final context = tester.element(containerFinder);
      final decoration = container.decoration! as BoxDecoration;
      expect(decoration.color, Theme.of(context).colorScheme.primaryContainer);
      expect(decoration.border, isA<Border>());
      expect(decoration.borderRadius, isA<BorderRadiusDirectional>());
      expect(decoration.boxShadow, hasLength(1));
      expect(decoration.boxShadow!.single.blurStyle, BlurStyle.outer);
      expect(decoration.gradient, isA<LinearGradient>());
      final gradient = decoration.gradient! as LinearGradient;
      expect(gradient.begin, Alignment.topLeft);
      expect(gradient.end, AlignmentDirectional.bottomEnd);
      expect(gradient.tileMode, TileMode.mirror);
      expect(gradient.transform, isA<GradientRotation>());
      expect(decoration.backgroundBlendMode, BlendMode.multiply);
      final foreground = container.foregroundDecoration! as BoxDecoration;
      expect(foreground.color, const Color(0x22112233));
      expect(foreground.shape, BoxShape.circle);

      final outline = find.byKey(
        const ValueKey('canvas-widget-outline-$containerId'),
      );
      final guides = find.byKey(
        const ValueKey('canvas-container-insets-guides-$containerId'),
      );
      expect(outline, findsOneWidget);
      expect(guides, findsOneWidget);
      expect(
        find.ancestor(of: containerFinder, matching: outline),
        findsOneWidget,
        reason: 'the layout outline must stay outside Container.transform',
      );
      expect(
        find.descendant(of: containerFinder, matching: find.byType(Transform)),
        findsOneWidget,
        reason: 'the actual Container keeps its framework paint transform',
      );
    },
  );

  testWidgets(
    'renders every reviewed Container decoration union and constructor default',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';

      Future<Container> render(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredContainer(
                  properties: properties,
                  child: null,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          ),
        );
        await tester.pump();
        return tester.widget<Container>(
          find
              .descendant(
                of: find.byKey(const ValueKey('canvas-widget-$containerId')),
                matching: find.byType(Container),
              )
              .first,
        );
      }

      final radialContainer = await render({
        'decoration': _viewBoxDecoration(
          border: _viewDirectionalBorder(),
          borderRadius: _viewPhysicalRadius(),
          gradient: _viewRadialGradient(),
        ),
      });
      final radialDecoration = radialContainer.decoration! as BoxDecoration;
      expect(radialDecoration.border, isA<BorderDirectional>());
      expect(radialDecoration.borderRadius, isA<BorderRadius>());
      final radial = radialDecoration.gradient! as RadialGradient;
      expect(radial.center, const Alignment(0.1, -0.2));
      expect(radial.radius, 0.75);
      expect(radial.focal, const AlignmentDirectional(0.4, 0.3));
      expect(radial.focalRadius, 0.15);
      expect(radial.tileMode, TileMode.decal);
      expect(radial.transform, isNull);
      expect(radial.colors.first, const Color(0xff102030));
      expect(radial.stops, const [0.0, 1.0]);

      final sweepContainer = await render({
        'decoration': _viewBoxDecoration(gradient: _viewSweepGradient()),
      });
      final sweep =
          (sweepContainer.decoration! as BoxDecoration).gradient!
              as SweepGradient;
      expect(sweep.center, const AlignmentDirectional(-0.2, 0.4));
      expect(sweep.startAngle, 0.25);
      expect(sweep.endAngle, 5.75);
      expect(sweep.tileMode, TileMode.repeated);
      expect(sweep.transform, isA<GradientRotation>());

      final plain = await render({
        'color': {'kind': 'color', 'argb': '0x7F123456'},
      });
      expect(plain.color, const Color(0x7f123456));
      expect(plain.decoration, isNull);
      expect(plain.isAntiAlias, isTrue);
      expect(plain.clipBehavior, Clip.none);
    },
  );

  testWidgets(
    'renders resolved DecorationImage arguments in RTL without disturbing selection or DnD overlays',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';
      final resourceId = sha256Hex(_viewPng8);
      final resources = CanvasImageResourceBundle.fromResources([
        CanvasImageResource(
          resourceId: resourceId,
          mediaType: 'image/png',
          pixelWidth: 8,
          pixelHeight: 8,
          encodedBytes: _viewPng8,
        ),
      ]);
      final colorFilters = <Map<String, Object?>>[
        {
          'kind': 'mode',
          'color': _viewLiteralColor('0xFF336699'),
          'blendMode': 'srcATop',
        },
        {
          'kind': 'matrix',
          'values': <Object?>[
            1,
            0,
            0,
            0,
            0,
            0,
            1,
            0,
            0,
            0,
            0,
            0,
            1,
            0,
            0,
            0,
            0,
            0,
            1,
            0,
          ],
        },
        {'kind': 'linearToSrgbGamma'},
        {'kind': 'srgbToLinearGamma'},
        {'kind': 'saturation', 'value': 0.4},
      ];
      String? selected;

      for (final colorFilter in colorFilters) {
        final json = _modelWithCenteredContainer(
          properties: {
            'width': {'kind': 'integer', 'value': 120},
            'height': {'kind': 'integer', 'value': 80},
            'decoration': _viewBoxDecoration(
              image: _viewDecorationImage(
                image: _viewImageProvider(
                  kind: 'exactAsset',
                  exactScale: 2,
                  resize: {
                    'width': 8,
                    'height': 8,
                    'policy': 'exact',
                    'allowUpscaling': false,
                  },
                  resolution: {
                    'kind': 'resolved',
                    'resourceId': resourceId,
                    'resolvedScale': 2,
                  },
                ),
                onError: true,
                colorFilter: colorFilter,
                fit: 'fill',
                alignment: _viewNestedAlignment(
                  basis: 'directional',
                  horizontal: -1,
                  vertical: 0.25,
                ),
                centerSlice: {'left': 1, 'top': 1, 'right': 3, 'bottom': 3},
                repeat: 'repeatX',
                matchTextDirection: true,
                scale: 1,
                opacity: 0.65,
                filterQuality: 'high',
                invertColors: true,
                isAntiAlias: true,
              ),
            ),
          },
          child: null,
        );
        (json['profile']! as Map<String, Object?>)['locale'] = 'ar-SA';
        final model = CanvasModel.decode(
          Uint8List.fromList(utf8.encode(jsonEncode(json))),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            imageResources: resources,
            selectedWidgetId: containerId,
            onSelected: (value) => selected = value,
            dropHoverTarget: const CanvasDropTarget(
              parentWidgetId: containerId,
              slotName: 'child',
              insertionIndex: 0,
              zone: CanvasDropZone(
                leftMicros: 0,
                topMicros: 0,
                rightMicros: 1000000,
                bottomMicros: 1000000,
              ),
            ),
            dropIndicatorKind: CanvasDropIndicatorKind.widgetMove,
          ),
        );
        await tester.pump();

        final node = find.byKey(const ValueKey('canvas-widget-$containerId'));
        final container = tester.widget<Container>(
          find.descendant(of: node, matching: find.byType(Container)).first,
        );
        final image = (container.decoration! as BoxDecoration).image!;
        expect(image.image, isA<ResizeImage>());
        final resized = image.image as ResizeImage;
        expect(resized.width, 8);
        expect(resized.height, 8);
        expect(resized.policy, ResizeImagePolicy.exact);
        expect(resized.allowUpscaling, isFalse);
        final memory = resized.imageProvider as MemoryImage;
        expect(memory.scale, 2);
        expect(image.colorFilter, isNotNull);
        expect(image.fit, BoxFit.fill);
        expect(image.alignment, const AlignmentDirectional(-1, 0.25));
        expect(image.centerSlice, const Rect.fromLTRB(1, 1, 3, 3));
        expect(image.repeat, ImageRepeat.repeatX);
        expect(image.matchTextDirection, isTrue);
        expect(image.scale, 1);
        expect(image.opacity, 0.65);
        expect(image.filterQuality, FilterQuality.high);
        expect(image.invertColors, isTrue);
        expect(image.isAntiAlias, isTrue);
        expect(Directionality.of(tester.element(node)), TextDirection.rtl);
        expect(
          find.byKey(const ValueKey('canvas-selection-outline-$containerId')),
          findsOneWidget,
        );
        expect(
          find.byKey(const ValueKey('canvas-widget-move-preview-zone')),
          findsOneWidget,
        );
        await tester.tap(node);
        expect(selected, containerId);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets('preserves serialized DecorationImage onError presence', (
    tester,
  ) async {
    const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';
    final resourceId = sha256Hex(_viewPng8);
    final resources = CanvasImageResourceBundle.fromResources([
      CanvasImageResource(
        resourceId: resourceId,
        mediaType: 'image/png',
        pixelWidth: 8,
        pixelHeight: 8,
        encodedBytes: _viewPng8,
      ),
    ]);

    for (final configured in const [false, true]) {
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredContainer(
                properties: {
                  'decoration': _viewBoxDecoration(
                    image: _viewDecorationImage(
                      image: _viewImageProvider(
                        resolution: {
                          'kind': 'resolved',
                          'resourceId': resourceId,
                          'resolvedScale': 1,
                        },
                      ),
                      onError: configured,
                    ),
                  ),
                },
                child: null,
              ),
            ),
          ),
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          imageResources: resources,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final node = find.byKey(const ValueKey('canvas-widget-$containerId'));
      final container = tester.widget<Container>(
        find.descendant(of: node, matching: find.byType(Container)).first,
      );
      final image = (container.decoration! as BoxDecoration).image!;
      expect(
        image.onError,
        configured ? isNotNull : isNull,
        reason: 'Canvas must preserve callback presence without an identifier',
      );
      expect(tester.takeException(), isNull);
    }
  });

  testWidgets(
    'paints unavailable and runner-rejected placeholders with concrete accessible status',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';
      const packageName = 'image_pack';
      const assetName = 'assets/images/panel.png';
      const assetIdentity = 'package:image_pack:assets/images/panel.png';
      final semantics = tester.ensureSemantics();

      for (final issue in const <(String, String)>[
        ('missing', 'The declared project image does not exist'),
        ('unreadable', 'The project image could not be read'),
        ('corrupt', 'The PNG payload could not be decoded'),
      ]) {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredContainer(
                  properties: {
                    'width': {'kind': 'integer', 'value': 120},
                    'height': {'kind': 'integer', 'value': 80},
                    'decoration': _viewBoxDecoration(
                      image: _viewDecorationImage(
                        image: _viewImageProvider(
                          assetName: assetName,
                          packageName: packageName,
                          resolution: {
                            'kind': 'unavailable',
                            'code': issue.$1,
                            'reason': issue.$2,
                          },
                        ),
                        centerSlice: {
                          'left': 40,
                          'top': 40,
                          'right': 80,
                          'bottom': 80,
                        },
                      ),
                    ),
                  },
                  child: null,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        );
        await tester.pump();

        final node = find.byKey(const ValueKey('canvas-widget-$containerId'));
        final container = tester.widget<Container>(
          find.descendant(of: node, matching: find.byType(Container)).first,
        );
        final image = (container.decoration! as BoxDecoration).image!;
        expect(image.image, isA<MemoryImage>());
        expect(
          image.centerSlice,
          isNull,
          reason: 'placeholder dimensions are intentionally untrusted',
        );
        expect(
          find.bySemanticsLabel(
            RegExp(
              'Image preview unavailable for ${RegExp.escape(assetIdentity)}.*'
              'Status ${issue.$1}.*Reason: ${RegExp.escape(issue.$2)}',
            ),
          ),
          findsOneWidget,
        );
        expect(tester.takeException(), isNull);
      }

      final rejectedResourceId = sha256Hex(_viewPng8);
      final rejectedModel = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredContainer(
                properties: {
                  'width': {'kind': 'integer', 'value': 120},
                  'height': {'kind': 'integer', 'value': 80},
                  'decoration': _viewBoxDecoration(
                    image: _viewDecorationImage(
                      image: _viewImageProvider(
                        assetName: assetName,
                        packageName: packageName,
                        resolution: {
                          'kind': 'resolved',
                          'resourceId': rejectedResourceId,
                          'resolvedScale': 1,
                        },
                      ),
                      centerSlice: {
                        'left': 40,
                        'top': 40,
                        'right': 80,
                        'bottom': 80,
                      },
                    ),
                  ),
                },
                child: null,
              ),
            ),
          ),
        ),
      );
      final rejectedResources = CanvasImageResourceBundle.fromResources(
        const [],
        rejections: [
          CanvasImageResourceRejection.encodedContent(rejectedResourceId),
        ],
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: rejectedModel,
          imageResources: rejectedResources,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      await tester.pump();

      final rejectedNode = find.byKey(
        const ValueKey('canvas-widget-$containerId'),
      );
      final rejectedContainer = tester.widget<Container>(
        find
            .descendant(of: rejectedNode, matching: find.byType(Container))
            .first,
      );
      final rejectedImage =
          (rejectedContainer.decoration! as BoxDecoration).image!;
      expect(rejectedImage.image, isA<MemoryImage>());
      expect(rejectedImage.centerSlice, isNull);
      expect(
        find.bySemanticsLabel(
          RegExp(
            'Image preview unavailable for ${RegExp.escape(assetIdentity)}.*'
            'Status corrupt.*Reason: '
            '${RegExp.escape(CanvasImageResourceRejectionKind.encodedContent.reason)}',
          ),
        ),
        findsOneWidget,
      );
      expect(tester.takeException(), isNull);
      semantics.dispose();
    },
  );

  testWidgets(
    'resolves Container padding and margin guides with distinct stable styles',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';

      Future<List<_RecordedLine>> lines(TextDirection direction) async {
        final model = CanvasModel.decode(
          Uint8List.fromList(
            utf8.encode(
              jsonEncode(
                _modelWithCenteredContainer(
                  properties: {
                    'width': {'kind': 'integer', 'value': 120},
                    'height': {'kind': 'integer', 'value': 100},
                    'margin': {
                      'kind': 'edgeInsets',
                      'left': 10,
                      'top': 20,
                      'right': 30,
                      'bottom': 40,
                    },
                    'padding': {
                      'kind': 'edgeInsetsDirectional',
                      'start': 5,
                      'top': 6,
                      'end': 7,
                      'bottom': 8,
                    },
                  },
                  child: null,
                ),
              ),
            ),
          ),
        );
        await tester.pumpWidget(
          MaterialApp(
            home: Directionality(
              textDirection: direction,
              child: CanvasDocumentView(
                model: model,
                selectedWidgetId: null,
                onSelected: (_) {},
              ),
            ),
          ),
        );
        await tester.pump();
        final guide = find.byKey(
          const ValueKey('canvas-container-insets-guides-$containerId'),
        );
        expect(tester.getSize(guide), const Size(160, 160));
        return _drawLines(
          _recordPainter(
            _foregroundPainter(
              tester,
              'canvas-container-insets-guides-$containerId',
            ),
            const Size(160, 160),
          ),
        );
      }

      final ltr = await lines(TextDirection.ltr);
      final rtl = await lines(TextDirection.rtl);
      const paddingArgb = 0xffd97706;
      const marginArgb = 0xff00897b;
      List<_RecordedLine> byColor(List<_RecordedLine> all, int argb) => [
        for (final line in all)
          if (line.paint.color.toARGB32() == argb) line,
      ];

      final ltrPadding = byColor(ltr, paddingArgb);
      final rtlPadding = byColor(rtl, paddingArgb);
      final ltrMargin = byColor(ltr, marginArgb);
      expect(ltrPadding, hasLength(12));
      expect(rtlPadding, hasLength(12));
      expect(ltrMargin.length, greaterThan(12));
      expect(
        _lineEndpoints([
          for (var index = 0; index < ltrPadding.length; index += 3)
            ltrPadding[index],
        ]),
        const [
          (Offset(10, 69), Offset(15, 69)),
          (Offset(123, 69), Offset(130, 69)),
          (Offset(69, 20), Offset(69, 26)),
          (Offset(69, 112), Offset(69, 120)),
        ],
      );
      expect(
        _lineEndpoints([
          for (var index = 0; index < rtlPadding.length; index += 3)
            rtlPadding[index],
        ]),
        const [
          (Offset(10, 69), Offset(17, 69)),
          (Offset(125, 69), Offset(130, 69)),
          (Offset(71, 20), Offset(71, 26)),
          (Offset(71, 112), Offset(71, 120)),
        ],
      );
    },
  );

  testWidgets(
    'keeps an empty zero-size Container at zero layout while exposing its optional child target',
    (tester) async {
      const containerId = 'd9e278fa-32f8-4ef7-a92f-4aef0867435c';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(jsonEncode(_modelWithEmptyContainerInRow())),
        ),
      );
      String? selectedWidgetId;
      CanvasDropResolver? resolver;

      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$containerId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$containerId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));

      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, containerId);
      expect(tester.getSize(rendered), Size.zero);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final point = tester.getRect(target).center;
      final drop = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(drop?.parentWidgetId, containerId);
      expect(drop?.slotName, 'child');
      expect(drop?.insertionIndex, 0);
    },
  );

  testWidgets(
    'keeps an empty zero-size SizedBox at zero layout while exposing a selectable target',
    (tester) async {
      const sizedBoxId = '38f49912-8e51-4e62-bd4c-2517ecad4962';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(
              _modelWithCenteredSizedBox(properties: const {}, child: null),
            ),
          ),
        ),
      );
      String? selectedWidgetId;

      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
            ),
          ),
        ),
      );
      await tester.pump();

      final rendered = find.byKey(const ValueKey('canvas-widget-$sizedBoxId'));
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$sizedBoxId'),
      );
      expect(tester.getSize(rendered), Size.zero);
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      final dashed = _recordPainter(
        _foregroundPainter(
          tester,
          'canvas-zero-size-widget-outline-$sizedBoxId',
        ),
        const Size(36, 36),
      );
      expect(_drawRectCount(dashed), 0);
      expect(_drawLines(dashed), isNotEmpty);

      await tester.tap(target);
      await tester.pump();

      expect(selectedWidgetId, sizedBoxId);
      expect(tester.getSize(rendered), Size.zero);
      final selected = _recordPainter(
        _foregroundPainter(
          tester,
          'canvas-zero-size-widget-outline-$sizedBoxId',
        ),
        const Size(36, 36),
      );
      expect(_drawRectCount(selected), 1);
      expect(_drawLines(selected), isEmpty);
    },
  );

  testWidgets(
    'cycles every exactly coincident empty SizedBox through one target',
    (tester) async {
      const sizedBoxIds = [
        '38f49912-8e51-4e62-bd4c-2517ecad4962',
        'f195f817-cf8e-455c-befc-31dd30f874df',
        '7a112f9e-8599-4bca-b37c-02e33268b48c',
      ];
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(jsonEncode(_modelWithEmptySizedBoxSiblings(sizedBoxIds))),
        ),
      );
      String? selectedWidgetId;

      await tester.pumpWidget(
        MaterialApp(
          home: StatefulBuilder(
            builder: (context, setState) => CanvasDocumentView(
              model: model,
              selectedWidgetId: selectedWidgetId,
              onSelected: (id) => setState(() => selectedWidgetId = id),
            ),
          ),
        ),
      );
      await tester.pump();

      for (final id in sizedBoxIds) {
        expect(
          tester.getSize(find.byKey(ValueKey('canvas-widget-$id'))),
          Size.zero,
        );
        expect(
          find.byKey(ValueKey('canvas-zero-size-widget-target-$id')),
          findsNothing,
        );
      }

      final target = find.byKey(
        const ValueKey(
          'canvas-zero-size-widget-target-group-'
          '38f49912-8e51-4e62-bd4c-2517ecad4962',
        ),
      );
      const cyclingMessage =
          '3 overlapping empty SizedBox widgets. '
          'Activate repeatedly to cycle selection.';
      expect(target, findsOneWidget);
      expect(tester.getSize(target), const Size(36, 36));
      expect(find.byTooltip(cyclingMessage), findsOneWidget);
      expect(find.bySemanticsLabel(cyclingMessage), findsOneWidget);

      for (final id in sizedBoxIds) {
        await tester.tap(target);
        await tester.pump();
        expect(selectedWidgetId, id);
      }
      await tester.tap(target);
      await tester.pump();
      expect(selectedWidgetId, sizedBoxIds.first);

      for (final id in sizedBoxIds) {
        expect(
          tester.getSize(find.byKey(ValueKey('canvas-widget-$id'))),
          Size.zero,
        );
      }
    },
  );

  testWidgets(
    'does not add a synthetic target to sized or non-empty SizedBox layout',
    (tester) async {
      const sizedBoxId = '38f49912-8e51-4e62-bd4c-2517ecad4962';
      final target = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$sizedBoxId'),
      );

      Future<Size> pump(Map<String, Object?> json) async {
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: CanvasModel.decode(
                Uint8List.fromList(utf8.encode(jsonEncode(json))),
              ),
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          ),
        );
        await tester.pump();
        return tester.getSize(
          find.byKey(const ValueKey('canvas-widget-$sizedBoxId')),
        );
      }

      final explicitSize = await pump(
        _modelWithCenteredSizedBox(
          properties: const {
            'width': {'kind': 'integer', 'value': 120},
            'height': {'kind': 'double', 'value': 48.5},
          },
          child: null,
        ),
      );
      expect(explicitSize, const Size(120, 48.5));
      expect(target, findsNothing);

      final child = <String, Object?>{
        'id': 'f195f817-cf8e-455c-befc-31dd30f874df',
        'type': 'flutter.widgets.Text',
        'properties': <String, Object?>{
          'data': {'kind': 'string', 'value': 'Non-empty'},
        },
        'slots': <String, Object?>{},
      };
      final childSized = await pump(
        _modelWithCenteredSizedBox(properties: const {}, child: child),
      );
      expect(childSized.width, greaterThan(0));
      expect(childSized.height, greaterThan(0));
      expect(target, findsNothing);
    },
  );

  testWidgets('resolves physical and directional Padding in LTR and RTL', (
    tester,
  ) async {
    const paddingId = '0f78bed5-2fba-42cd-914e-a3abbd2c48c3';

    Finder renderedPadding() => find
        .descendant(
          of: find.byKey(const ValueKey('canvas-widget-$paddingId')),
          matching: find.byType(Padding),
        )
        .first;

    Future<void> expectResolvedOffset({
      required Map<String, Object?> padding,
      required TextDirection direction,
      required double expectedLeft,
    }) async {
      final json = _modelJsonForView();
      _nodeProperties(json, 'flutter.widgets.Padding')['padding'] = padding;
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      await tester.pumpWidget(
        MaterialApp(
          home: Directionality(
            textDirection: direction,
            child: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          ),
        ),
      );

      final renderPadding = tester.renderObject<RenderPadding>(
        renderedPadding(),
      );
      expect(renderPadding.textDirection, direction);
      final childOffset =
          (renderPadding.child!.parentData! as BoxParentData).offset;
      expect(childOffset, Offset(expectedLeft, 2));
    }

    const physical = <String, Object?>{
      'kind': 'edgeInsets',
      'left': 1,
      'top': 2,
      'right': 3,
      'bottom': 4,
    };
    await expectResolvedOffset(
      padding: physical,
      direction: TextDirection.ltr,
      expectedLeft: 1,
    );
    await expectResolvedOffset(
      padding: physical,
      direction: TextDirection.rtl,
      expectedLeft: 1,
    );

    const directional = <String, Object?>{
      'kind': 'edgeInsetsDirectional',
      'start': 1,
      'top': 2,
      'end': 3,
      'bottom': 4,
    };
    await expectResolvedOffset(
      padding: directional,
      direction: TextDirection.ltr,
      expectedLeft: 1,
    );
    await expectResolvedOffset(
      padding: directional,
      direction: TextDirection.rtl,
      expectedLeft: 3,
    );
  });

  testWidgets('keeps an empty Padding child nullable in the real widget', (
    tester,
  ) async {
    const paddingId = '0f78bed5-2fba-42cd-914e-a3abbd2c48c3';
    final json = _modelJsonForView();
    final root = json['root']! as Map<String, Object?>;
    final padding = _findNode(root, 'flutter.widgets.Padding');
    ((padding['slots']! as Map<String, Object?>)['child']!
            as Map<String, Object?>)['child'] =
        null;

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: CanvasModel.decode(
            Uint8List.fromList(utf8.encode(jsonEncode(json))),
          ),
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final paddingFinder = find
        .descendant(
          of: find.byKey(const ValueKey('canvas-widget-$paddingId')),
          matching: find.byType(Padding),
        )
        .first;
    expect(tester.widget<Padding>(paddingFinder).child, isNull);
    expect(tester.renderObject<RenderPadding>(paddingFinder).child, isNull);
  });

  testWidgets(
    'resolves physical and directional Padding guides in LTR and RTL',
    (tester) async {
      const paddingId = '0f78bed5-2fba-42cd-914e-a3abbd2c48c3';

      Future<List<_RecordedLine>> guideMeasurements({
        required Map<String, Object?> padding,
        required TextDirection direction,
      }) async {
        final json = _modelJsonForView();
        _nodeProperties(json, 'flutter.widgets.Padding')['padding'] = padding;
        await tester.pumpWidget(
          MaterialApp(
            home: Directionality(
              textDirection: direction,
              child: CanvasDocumentView(
                model: CanvasModel.decode(
                  Uint8List.fromList(utf8.encode(jsonEncode(json))),
                ),
                selectedWidgetId: null,
                onSelected: (_) {},
              ),
            ),
          ),
        );
        final painter = _foregroundPainter(
          tester,
          'canvas-padding-guides-$paddingId',
        );
        final lines = _drawLines(_recordPainter(painter, const Size(100, 120)));
        expect(lines, hasLength(12));
        expect(lines.first.paint.color.toARGB32(), 0xffd97706);
        return [
          for (var index = 0; index < lines.length; index += 3) lines[index],
        ];
      }

      const physical = <String, Object?>{
        'kind': 'edgeInsets',
        'left': 10,
        'top': 20,
        'right': 30,
        'bottom': 40,
      };
      final physicalLtr = await guideMeasurements(
        padding: physical,
        direction: TextDirection.ltr,
      );
      final physicalRtl = await guideMeasurements(
        padding: physical,
        direction: TextDirection.rtl,
      );
      expect(_lineEndpoints(physicalLtr), _expectedPaddingGuideEndpoints());
      expect(_lineEndpoints(physicalRtl), _expectedPaddingGuideEndpoints());

      const directional = <String, Object?>{
        'kind': 'edgeInsetsDirectional',
        'start': 10,
        'top': 20,
        'end': 30,
        'bottom': 40,
      };
      final directionalLtr = await guideMeasurements(
        padding: directional,
        direction: TextDirection.ltr,
      );
      final directionalRtl = await guideMeasurements(
        padding: directional,
        direction: TextDirection.rtl,
      );
      expect(_lineEndpoints(directionalLtr), _expectedPaddingGuideEndpoints());
      expect(
        _lineEndpoints(directionalRtl),
        _expectedPaddingGuideEndpoints(left: 30, right: 10),
      );
    },
  );

  testWidgets('paints an empty Padding all-40 guide cross', (tester) async {
    const paddingId = '0f78bed5-2fba-42cd-914e-a3abbd2c48c3';
    final json = _modelJsonForView();
    final padding = _findNode(
      json['root']! as Map<String, Object?>,
      'flutter.widgets.Padding',
    );
    (padding['properties']! as Map<String, Object?>)['padding'] = {
      'kind': 'edgeInsets',
      'left': 40,
      'top': 40,
      'right': 40,
      'bottom': 40,
    };
    ((padding['slots']! as Map<String, Object?>)['child']!
            as Map<String, Object?>)['child'] =
        null;

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: CanvasModel.decode(
            Uint8List.fromList(utf8.encode(jsonEncode(json))),
          ),
          selectedWidgetId: paddingId,
          onSelected: (_) {},
        ),
      ),
    );

    final guideFinder = find.byKey(
      const ValueKey('canvas-padding-guides-$paddingId'),
    );
    expect(guideFinder, findsOneWidget);
    expect(tester.getSize(guideFinder), const Size(80, 80));
    final lines = _drawLines(
      _recordPainter(
        _foregroundPainter(tester, 'canvas-padding-guides-$paddingId'),
        const Size(80, 80),
      ),
    );
    expect(lines, hasLength(12));
    expect(
      _lineEndpoints([
        for (var index = 0; index < lines.length; index += 3) lines[index],
      ]),
      const [
        (Offset(0, 40), Offset(40, 40)),
        (Offset(40, 40), Offset(80, 40)),
        (Offset(40, 0), Offset(40, 40)),
        (Offset(40, 40), Offset(40, 80)),
      ],
    );
  });

  testWidgets('renders expanded Text properties through real Flutter objects', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.expandedTextModelBytesForViewTest(),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final hello = tester.widget<Text>(find.text('Hello'));
    expect(
      hello.locale,
      const Locale.fromSubtags(
        languageCode: 'zh',
        scriptCode: 'Hans',
        countryCode: 'CN',
      ),
    );
    expect(hello.textScaler, isNotNull);
    expect(hello.textScaler!.scale(10), 12.5);
    expect(hello.textHeightBehavior, isNotNull);
    expect(hello.textHeightBehavior!.applyHeightToFirstAscent, isFalse);
    expect(hello.textHeightBehavior!.applyHeightToLastDescent, isFalse);
    expect(
      hello.textHeightBehavior!.leadingDistribution,
      TextLeadingDistribution.even,
    );

    final style = hello.style!;
    expect(style.inherit, isFalse);
    expect(style.color, const Color(0xff102030));
    expect(style.backgroundColor, const Color(0xffe0d0c0));
    expect(style.fontSize, 18.5);
    expect(style.fontWeight, FontWeight.w600);
    expect(style.fontStyle, FontStyle.italic);
    expect(style.letterSpacing, 1.25);
    expect(style.wordSpacing, 2.5);
    expect(style.textBaseline, TextBaseline.ideographic);
    expect(style.height, 1.4);
    expect(style.leadingDistribution, TextLeadingDistribution.proportional);
    expect(
      style.locale,
      const Locale.fromSubtags(
        languageCode: 'en',
        scriptCode: 'Latn',
        countryCode: 'GB',
      ),
    );
    expect(
      style.decoration,
      TextDecoration.combine(const [
        TextDecoration.underline,
        TextDecoration.overline,
        TextDecoration.lineThrough,
      ]),
    );
    expect(style.decorationColor, const Color(0xff112233));
    expect(style.decorationStyle, TextDecorationStyle.wavy);
    expect(style.decorationThickness, 2.25);
    expect(style.debugLabel, 'designer text');
    expect(style.fontFamily, 'packages/design_fonts/Inter');
    expect(style.fontFamilyFallback, const [
      'packages/design_fonts/Noto Sans',
      'packages/design_fonts/Noto Color Emoji',
    ]);
    expect(style.overflow, TextOverflow.fade);

    final strut = hello.strutStyle!;
    expect(strut.fontFamily, 'packages/metric_fonts/Roboto');
    expect(strut.fontFamilyFallback, const [
      'packages/metric_fonts/Noto Sans',
      'packages/metric_fonts/Noto Serif',
    ]);
    expect(strut.fontSize, 16.0);
    expect(strut.height, 1.2);
    expect(strut.leadingDistribution, TextLeadingDistribution.even);
    expect(strut.leading, 0.3);
    expect(strut.fontWeight, FontWeight.w500);
    expect(strut.fontStyle, FontStyle.normal);
    expect(strut.forceStrutHeight, isTrue);
    expect(strut.debugLabel, 'designer strut');
  });

  testWidgets(
    'resolves project theme roles and complex TextStyle values in Flutter',
    (tester) async {
      final model = CanvasModel.decode(
        fixture.complexTextModelBytesForViewTest(),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );

      final context = tester.element(find.text('Hello'));
      final scheme = Theme.of(context).colorScheme;
      final hello = tester.widget<Text>(find.text('Hello'));
      expect(hello.selectionColor, scheme.primary);
      final style = hello.style!;
      expect(
        style.fontSize,
        21.0,
        reason: 'local override wins over bodyLarge',
      );
      expect(style.foreground, isNotNull);
      expect(style.foreground!.color.toARGB32(), scheme.secondary.toARGB32());
      expect(style.foreground!.style, PaintingStyle.stroke);
      expect(style.foreground!.strokeWidth, 2.5);
      expect(style.foreground!.strokeCap, StrokeCap.round);
      expect(style.foreground!.strokeJoin, StrokeJoin.bevel);
      expect(style.foreground!.filterQuality, FilterQuality.medium);
      expect(style.foreground!.maskFilter, isNotNull);
      expect(style.background!.color.toARGB32(), 0x22112233);
      expect(style.decorationColor, scheme.error);
      expect(style.shadows, hasLength(2));
      expect(style.shadows!.first.color.toARGB32(), scheme.shadow.toARGB32());
      expect(style.shadows!.first.offset, const Offset(-1.25, 2.5));
      expect(style.shadows!.first.blurRadius, 4.0);
      expect(style.fontFeatures, hasLength(2));
      expect(style.fontFeatures!.map((value) => value.feature), [
        'liga',
        'kern',
      ]);
      expect(style.fontFeatures!.map((value) => value.value), [1, 0]);
      expect(style.fontVariations, hasLength(2));
      expect(style.fontVariations!.map((value) => value.axis), [
        'wght',
        'wdth',
      ]);
      expect(style.fontVariations!.map((value) => value.value), [700.0, 100.0]);
    },
  );

  testWidgets(
    'preserves theme TextStyle inherit when the local leaf is omitted',
    (tester) async {
      final json = _modelJsonForView();
      final properties = _nodeProperties(json, 'flutter.widgets.Text');
      properties
        ..['styleThemeTextStyle'] = {
          'kind': 'themeToken',
          'token': 'material.textTheme.bodyLarge',
        }
        ..['styleFontSize'] = {'kind': 'double', 'value': 19.0}
        ..remove('styleInherit');
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );

      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData(
            textTheme: const TextTheme(
              bodyLarge: TextStyle(inherit: false, fontSize: 11),
            ),
          ),
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        ),
      );

      final style = tester.widget<Text>(find.text('Hello')).style!;
      expect(style.inherit, isFalse);
      expect(style.fontSize, 19.0);
    },
  );

  testWidgets('preserves explicitly empty complex TextStyle lists', (
    tester,
  ) async {
    final json = _modelJsonForView();
    final properties = _nodeProperties(json, 'flutter.widgets.Text');
    properties
      ..['styleShadows'] = {'kind': 'shadowList', 'items': <Object?>[]}
      ..['styleFontFeatures'] = {
        'kind': 'fontFeatureList',
        'items': <Object?>[],
      }
      ..['styleFontVariations'] = {
        'kind': 'fontVariationList',
        'items': <Object?>[],
      };
    final model = CanvasModel.decode(
      Uint8List.fromList(utf8.encode(jsonEncode(json))),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final style = tester.widget<Text>(find.text('Hello')).style!;
    expect(style.shadows, isEmpty);
    expect(style.fontFeatures, isEmpty);
    expect(style.fontVariations, isEmpty);
  });

  testWidgets('omits Text composite objects when no composite field exists', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.modelBytesForViewTest());

    await tester.pumpWidget(
      MaterialApp(
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final world = tester.widget<Text>(find.text('World'));
    expect(world.style, isNull);
    expect(world.strutStyle, isNull);
    expect(world.locale, isNull);
    expect(world.textScaler, isNull);
    expect(world.textHeightBehavior, isNull);
  });

  testWidgets(
    'resolves only a terminal Row or Column append zone from native coordinates',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      CanvasDropResolver? resolver;

      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );

      expect(resolver, isNotNull);
      const rowId = '1035b7df-df9b-442b-9af2-72b4c90f1462';
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final row = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$rowId')),
      );
      // The fixture Row is RTL, so its semantic append edge is the left edge.
      final point = Offset(row.left + 1, row.center.dy);
      final xMicros = ((point.dx - surface.left) / surface.width * 1000000)
          .round();
      final yMicros = ((point.dy - surface.top) / surface.height * 1000000)
          .round();

      final target = resolver!(xMicros, yMicros);
      expect(target, isNotNull);
      expect(target!.parentWidgetId, rowId);
      expect(target.slotName, 'children');
      expect(target.insertionIndex, 1);
      expect(target.zone, isNotNull);
      expect(target.zone!.isEmpty, isFalse);
      expect(
        target.zone!.rightMicros - target.zone!.leftMicros,
        lessThan(250000),
        reason: 'the RTL Row exposes only its concise terminal append band',
      );
      final spacerTarget = resolver!(
        xMicros,
        yMicros,
        CanvasPaletteDragSource(
          token: 'spacer-source',
          widgetType: canvasSpacerWidgetType,
          traits: const {},
        ),
      );
      expect(spacerTarget?.parentWidgetId, rowId);
      expect(spacerTarget?.slotName, 'children');
      expect(spacerTarget?.insertionIndex, 1);
      expect(resolver!(-1, yMicros), isNull);
      expect(resolver!(xMicros, 1000001), isNull);

      const columnId = '4c04dc44-7381-4ca8-826f-8c23bc2351d1';
      final column = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$columnId')),
      );
      expect(column.width, lessThan(model.profile.logicalWidth));
      final outsideScaledColumn = Offset(column.right + 8, column.bottom - 1);
      final outsideX =
          ((outsideScaledColumn.dx - surface.left) / surface.width * 1000000)
              .round();
      final outsideY =
          ((outsideScaledColumn.dy - surface.top) / surface.height * 1000000)
              .round();
      expect(
        resolver!(outsideX, outsideY)?.parentWidgetId,
        isNot(columnId),
        reason: 'FittedBox scaling must not enlarge the Column hit rectangle',
      );
    },
  );

  testWidgets(
    'keeps newly inserted empty Row and Column children targets hittable',
    (tester) async {
      CanvasDropResolver? resolver;

      for (final type in const [
        'flutter.widgets.Row',
        'flutter.widgets.Column',
      ]) {
        final json = _modelJsonForView();
        final root = json['root']! as Map<String, Object?>;
        final container = _findNode(root, type);
        final containerId = container['id']! as String;
        (container['properties']! as Map<String, Object?>)
          ..remove('textDirection')
          ..remove('verticalDirection');
        final children =
            (container['slots']! as Map<String, Object?>)['children']!
                as Map<String, Object?>;
        children['children'] = <Object?>[];
        ((root['slots']! as Map<String, Object?>)['body']!
                as Map<String, Object?>)['child'] =
            container;

        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: CanvasModel.decode(
                Uint8List.fromList(utf8.encode(jsonEncode(json))),
              ),
              selectedWidgetId: null,
              onSelected: (_) {},
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();

        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final rect = tester.getRect(
          find.byKey(ValueKey('canvas-widget-$containerId')),
        );
        final point = type == 'flutter.widgets.Row'
            ? Offset(rect.right - 1, rect.center.dy)
            : Offset(rect.center.dx, rect.bottom - 1);
        final target = resolver!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
        );
        expect(target?.parentWidgetId, containerId, reason: type);
        expect(target?.slotName, 'children', reason: type);
        expect(target?.insertionIndex, 0, reason: type);
        expect(target?.zone?.isEmpty, isFalse, reason: type);
        expect(target!.zone!.leftMicros, greaterThanOrEqualTo(0));
        expect(target.zone!.topMicros, greaterThanOrEqualTo(0));
        expect(target.zone!.rightMicros, lessThanOrEqualTo(1000000));
        expect(target.zone!.bottomMicros, lessThanOrEqualTo(1000000));
      }
    },
  );

  testWidgets(
    'resolves native DnD coordinates after manual zoom and scroll transforms',
    (tester) async {
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      final presentation = CanvasViewportPresentation.fit(model).copyWith(
        mode: 'manual',
        zoomMicros: 1500000,
        horizontalScrollMicros: 250000,
        verticalScrollMicros: 100000,
      );
      CanvasDropResolver? resolver;
      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: SizedBox(
              width: 500,
              height: 400,
              child: CanvasDocumentView(
                model: model,
                selectedWidgetId: null,
                onSelected: (_) {},
                viewportPresentation: presentation,
                onDropResolverChanged: (value) => resolver = value,
              ),
            ),
          ),
        ),
      );
      await tester.pump();

      const rowId = '1035b7df-df9b-442b-9af2-72b4c90f1462';
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final root = tester.getRect(
        find.byKey(ValueKey('canvas-widget-${model.root.id}')),
      );
      final row = tester.getRect(
        find.byKey(const ValueKey('canvas-widget-$rowId')),
      );
      expect(
        root.height,
        allOf(
          greaterThan(model.profile.logicalHeight * 1.4),
          lessThan(model.profile.logicalHeight * 1.6),
        ),
        reason: 'the transformed Flutter viewport must use the manual zoom',
      );
      expect(
        root.top,
        lessThan(surface.top),
        reason: 'the normalized vertical scroll must translate the Canvas',
      );

      final visibleRow = row.intersect(surface);
      expect(visibleRow.isEmpty, isFalse);
      final point = Offset(visibleRow.left + 1, visibleRow.center.dy);
      final target = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(target, isNotNull);
      expect(target!.parentWidgetId, rowId);
      expect(target.slotName, 'children');
      expect(target.insertionIndex, 1);
      expect(target.zone?.isEmpty, isFalse);
    },
  );

  testWidgets(
    'exposes an empty Column as an insertion target at manual 75 percent',
    (tester) async {
      final json = _modelJsonForView();
      final root = json['root']! as Map<String, Object?>;
      final column = _findNode(root, 'flutter.widgets.Column');
      final columnId = column['id']! as String;
      final children =
          (column['slots']! as Map<String, Object?>)['children']!
              as Map<String, Object?>;
      children['children'] = <Object?>[];
      ((root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>)['child'] =
          column;
      final model = CanvasModel.decode(
        Uint8List.fromList(utf8.encode(jsonEncode(json))),
      );
      final presentation = CanvasViewportPresentation.fit(
        model,
      ).copyWith(mode: 'manual', zoomMicros: 750000);
      final metrics = <CanvasViewportMetrics>[];
      CanvasDropResolver? resolver;

      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: SizedBox(
              width: 500,
              height: 500,
              child: CanvasDocumentView(
                model: model,
                selectedWidgetId: columnId,
                onSelected: (_) {},
                viewportPresentation: presentation,
                onViewportMetricsChanged: metrics.add,
                onDropResolverChanged: (value) => resolver = value,
              ),
            ),
          ),
        ),
      );
      await tester.pump();

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final columnRect = tester.getRect(
        find.byKey(ValueKey('canvas-widget-$columnId')),
      );
      final selectionRect = tester.getRect(
        find.byKey(ValueKey('canvas-selection-outline-$columnId')),
      );
      expect(
        selectionRect,
        columnRect,
        reason: 'the selection affordance must not change real widget layout',
      );
      expect(surface.contains(columnRect.center), isTrue);
      final point = columnRect.center;
      final target = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );

      expect(metrics.last.effectiveScaleMicros, 750000);
      expect(target, isNotNull);
      expect(target!.parentWidgetId, columnId);
      expect(target.slotName, 'children');
      expect(target.insertionIndex, 0);
      expect(target.zone?.isEmpty, isFalse);
    },
  );

  testWidgets(
    'resolves Scaffold body and gives its compact empty FAB zone precedence',
    (tester) async {
      CanvasDropResolver? resolver;

      Future<void> pump(Map<String, Object?> json) async {
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: CanvasModel.decode(
                Uint8List.fromList(utf8.encode(jsonEncode(json))),
              ),
              selectedWidgetId: null,
              onSelected: (_) {},
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        );
        await tester.pump();
      }

      CanvasDropTarget? resolveAt(Rect surface, Offset point) => resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );

      final bothEmpty = _modelJsonForView();
      final emptyRoot = bothEmpty['root']! as Map<String, Object?>;
      final rootId = emptyRoot['id']! as String;
      final emptySlots = emptyRoot['slots']! as Map<String, Object?>;
      (emptySlots['body']! as Map<String, Object?>)['child'] = null;
      emptySlots['floatingActionButton'] = <String, Object?>{
        'kind': 'single',
        'child': null,
      };
      await pump(bothEmpty);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final rootRect = tester.getRect(
        find.byKey(ValueKey('canvas-widget-$rootId')),
      );
      final bodyTarget = resolveAt(surface, rootRect.center);
      expect(bodyTarget, isNotNull);
      expect(bodyTarget!.parentWidgetId, rootId);
      expect(bodyTarget.slotName, 'body');
      expect(bodyTarget.insertionIndex, 0);

      final fabTarget = resolveAt(
        surface,
        rootRect.bottomRight - const Offset(1, 1),
      );
      expect(fabTarget, isNotNull);
      expect(fabTarget!.parentWidgetId, rootId);
      expect(fabTarget.slotName, 'floatingActionButton');
      expect(fabTarget.insertionIndex, 0);
      expect(
        fabTarget.zone!.rightMicros - fabTarget.zone!.leftMicros,
        lessThan(bodyTarget.zone!.rightMicros - bodyTarget.zone!.leftMicros),
        reason: 'the FAB target must stay concise and win the body overlap',
      );

      final occupiedBody = _modelJsonForView();
      final occupiedBodyRoot = occupiedBody['root']! as Map<String, Object?>;
      final occupiedBodySlots =
          occupiedBodyRoot['slots']! as Map<String, Object?>;
      final bodyText = _findNode(occupiedBodyRoot, 'flutter.widgets.Text');
      (occupiedBodySlots['body']! as Map<String, Object?>)['child'] = bodyText;
      occupiedBodySlots['floatingActionButton'] = <String, Object?>{
        'kind': 'single',
        'child': null,
      };
      await pump(occupiedBody);
      final occupiedBodySurface = tester.getRect(
        find.byType(CanvasDocumentView),
      );
      final occupiedBodyRootRect = tester.getRect(
        find.byKey(ValueKey('canvas-widget-${occupiedBodyRoot['id']}')),
      );
      expect(
        resolveAt(
          occupiedBodySurface,
          occupiedBodyRootRect.bottomRight - const Offset(1, 1),
        )?.slotName,
        'floatingActionButton',
        reason: 'an occupied body must not hide an empty FAB target',
      );

      final occupiedFab = _modelJsonForView();
      final occupiedFabRoot = occupiedFab['root']! as Map<String, Object?>;
      final occupiedFabSlots =
          occupiedFabRoot['slots']! as Map<String, Object?>;
      final fabText = _findNode(occupiedFabRoot, 'flutter.widgets.Text');
      (occupiedFabSlots['body']! as Map<String, Object?>)['child'] = null;
      occupiedFabSlots['floatingActionButton'] = <String, Object?>{
        'kind': 'single',
        'child': fabText,
      };
      await pump(occupiedFab);
      expect(
        resolver!(500000, 500000)?.slotName,
        'body',
        reason: 'an occupied FAB must leave the empty body target available',
      );
    },
  );

  testWidgets('resolves only empty Padding and Center child slots', (
    tester,
  ) async {
    CanvasDropResolver? resolver;

    Future<void> pump(Map<String, Object?> json) async {
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();
    }

    for (final type in const [
      'flutter.widgets.Padding',
      'flutter.widgets.Center',
    ]) {
      final empty = _modelJsonForView();
      final root = empty['root']! as Map<String, Object?>;
      final container = _findNode(root, type);
      final containerId = container['id']! as String;
      ((container['slots']! as Map<String, Object?>)['child']!
              as Map<String, Object?>)['child'] =
          null;
      ((root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>)['child'] =
          container;
      await pump(empty);

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final rect = tester.getRect(
        find.byKey(ValueKey('canvas-widget-$containerId')),
      );
      final point = rect.center;
      final target = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
      );
      expect(target?.parentWidgetId, containerId, reason: type);
      expect(target?.slotName, 'child', reason: type);
      expect(target?.insertionIndex, 0, reason: type);

      final occupied = _modelJsonForView();
      final occupiedRoot = occupied['root']! as Map<String, Object?>;
      final occupiedContainer = _findNode(occupiedRoot, type);
      ((occupiedRoot['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>)['child'] =
          occupiedContainer;
      await pump(occupied);
      final occupiedSurface = tester.getRect(find.byType(CanvasDocumentView));
      final occupiedRect = tester.getRect(
        find.byKey(ValueKey('canvas-widget-$containerId')),
      );
      final occupiedPoint = occupiedRect.center;
      expect(
        resolver!(
          ((occupiedPoint.dx - occupiedSurface.left) /
                  occupiedSurface.width *
                  1000000)
              .round(),
          ((occupiedPoint.dy - occupiedSurface.top) /
                  occupiedSurface.height *
                  1000000)
              .round(),
        ),
        isNull,
        reason: '$type.child is occupied',
      );
    }
  });

  testWidgets('resolves only an empty SizedBox child slot', (tester) async {
    const sizedBoxId = '38f49912-8e51-4e62-bd4c-2517ecad4962';
    CanvasDropResolver? resolver;

    Future<void> pump({required bool occupied}) async {
      final json = _modelJsonForView();
      final root = json['root']! as Map<String, Object?>;
      final body =
          (root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>;
      final child = occupied ? _findNode(root, 'flutter.widgets.Text') : null;
      body['child'] = <String, Object?>{
        'id': sizedBoxId,
        'type': 'flutter.widgets.SizedBox',
        'properties': <String, Object?>{
          'width': {'kind': 'double', 'value': 120.0},
          'height': {'kind': 'double', 'value': 80.0},
        },
        'slots': <String, Object?>{
          'child': <String, Object?>{'kind': 'single', 'child': child},
        },
      };
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: CanvasModel.decode(
              Uint8List.fromList(utf8.encode(jsonEncode(json))),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
          ),
        ),
      );
      await tester.pump();
    }

    await pump(occupied: false);
    final surface = tester.getRect(find.byType(CanvasDocumentView));
    final rect = tester.getRect(
      find.byKey(const ValueKey('canvas-widget-$sizedBoxId')),
    );
    final point = rect.center;
    final target = resolver!(
      ((point.dx - surface.left) / surface.width * 1000000).round(),
      ((point.dy - surface.top) / surface.height * 1000000).round(),
    );
    expect(target?.parentWidgetId, sizedBoxId);
    expect(target?.slotName, 'child');
    expect(target?.insertionIndex, 0);

    await pump(occupied: true);
    final occupiedSurface = tester.getRect(find.byType(CanvasDocumentView));
    final occupiedRect = tester.getRect(
      find.byKey(const ValueKey('canvas-widget-$sizedBoxId')),
    );
    final occupiedPoint = occupiedRect.center;
    expect(
      resolver!(
        ((occupiedPoint.dx - occupiedSurface.left) /
                occupiedSurface.width *
                1000000)
            .round(),
        ((occupiedPoint.dy - occupiedSurface.top) /
                occupiedSurface.height *
                1000000)
            .round(),
      ),
      isNull,
    );
  });

  testWidgets(
    'resolves exact existing-widget placement and paints a thin amber marker',
    (tester) async {
      CanvasMovePreviewResolver? moveResolver;
      final model = CanvasModel.decode(fixture.modelBytesForViewTest());
      const sourceId = '5ab6c203-3d32-489c-9d7a-7c14f29637cb';
      const rowId = '1035b7df-df9b-442b-9af2-72b4c90f1462';

      Future<void> pump({CanvasDropTarget? target}) async {
        await tester.pumpWidget(
          MaterialApp(
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
              dropHoverTarget: target,
              dropIndicatorKind: CanvasDropIndicatorKind.widgetMove,
              onMovePreviewResolverChanged: (value) => moveResolver = value,
            ),
          ),
        );
        await tester.pump();
      }

      await pump();
      final target = moveResolver!(sourceId, rowId, 'children', 1);
      expect(target, isNotNull);
      expect(target!.parentWidgetId, rowId);
      expect(target.slotName, 'children');
      expect(target.insertionIndex, 1);
      expect(target.zone, isNotNull);
      expect(target.zone!.isEmpty, isFalse);
      expect(
        target.zone!.rightMicros - target.zone!.leftMicros,
        lessThan(100000),
        reason: 'a list move uses a concise insertion marker, not full fill',
      );

      await pump(target: target);
      final marker = tester.widget<DecoratedBox>(
        find.byKey(const ValueKey('canvas-widget-move-preview-zone')),
      );
      final decoration = marker.decoration as BoxDecoration;
      expect(decoration.color, const Color(0x24D29A17));
      expect(decoration.border!.top.color, const Color(0xffc58b08));
      expect(
        tester
            .widgetList<Semantics>(
              find.ancestor(
                of: find.byKey(
                  const ValueKey('canvas-widget-move-preview-zone'),
                ),
                matching: find.byType(Semantics),
              ),
            )
            .any(
              (widget) =>
                  widget.properties.label ==
                  'Flutter widget move target for children',
            ),
        isTrue,
      );
      expect(
        find.byKey(const ValueKey('canvas-widget-insert-drop-zone')),
        findsNothing,
        reason: 'move preview must not be confused with palette insertion',
      );

      await tester.pumpWidget(const SizedBox.shrink());
      expect(
        moveResolver,
        isNull,
        reason: 'disposing the view clears geometry',
      );
    },
  );

  testWidgets('renders every Icon argument through the real Flutter widget', (
    tester,
  ) async {
    final model = CanvasModel.decode(fixture.iconModelBytesForViewTest());
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final iconFinder = find.descendant(
      of: find.byKey(
        const ValueKey('canvas-widget-${fixture.iconWidgetIdForViewTest}'),
      ),
      matching: find.byType(Icon),
    );
    final icon = tester.widget<Icon>(iconFinder);
    final iconData = icon.icon!;
    expect(iconData.codePoint, 0xe5fc);
    expect(iconData.fontFamily, 'MaterialIcons');
    expect(iconData.fontPackage, isNull);
    expect(iconData.matchTextDirection, isTrue);
    expect(iconData.fontFamilyFallback, isNull);
    expect(icon.size, 32);
    expect(icon.fill, 0.75);
    expect(icon.weight, 600);
    expect(icon.grade, -25);
    expect(icon.opticalSize, 24);
    final context = tester.element(iconFinder);
    expect(icon.color, Theme.of(context).colorScheme.primary);
    expect(icon.shadows, hasLength(1));
    expect(icon.shadows!.single.color, Theme.of(context).colorScheme.shadow);
    expect(icon.shadows!.single.offset, const Offset(-1, 2));
    expect(icon.shadows!.single.blurRadius, 3);
    expect(icon.semanticLabel, 'Reviewed icon');
    expect(icon.textDirection, TextDirection.rtl);
    expect(icon.applyTextScaling, isFalse);
    expect(icon.blendMode, BlendMode.multiply);
    expect(icon.fontWeight, FontWeight.w700);
    expect(
      find.byKey(
        const ValueKey(
          'canvas-widget-outline-${fixture.iconWidgetIdForViewTest}',
        ),
      ),
      findsOneWidget,
    );
  });

  testWidgets(
    'preserves IconTheme inheritance, null IconData and explicit empty shadows',
    (tester) async {
      const inherited = IconThemeData(
        size: 41,
        color: Color(0xff123456),
        fill: 0.3,
        weight: 525,
        grade: 12,
        opticalSize: 18,
        shadows: [
          Shadow(color: Color(0xff654321), offset: Offset(1, 2), blurRadius: 3),
        ],
      );
      Future<void> pump(Map<String, Object?> properties) async {
        final model = CanvasModel.decode(
          fixture.iconModelBytesForViewTest(properties: properties),
        );
        await tester.pumpWidget(
          MaterialApp(
            theme: ThemeData(iconTheme: inherited),
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          ),
        );
      }

      Finder iconFinder() => find.descendant(
        of: find.byKey(
          const ValueKey('canvas-widget-${fixture.iconWidgetIdForViewTest}'),
        ),
        matching: find.byType(Icon),
      );

      await pump({
        'icon': fixture.iconDataValueForViewTest(fontFamily: 'MaterialIcons'),
      });
      final inheritedIcon = tester.widget<Icon>(iconFinder());
      expect(inheritedIcon.icon, isNotNull);
      expect(inheritedIcon.size, isNull);
      expect(inheritedIcon.color, isNull);
      expect(inheritedIcon.fill, isNull);
      expect(inheritedIcon.weight, isNull);
      expect(inheritedIcon.grade, isNull);
      expect(inheritedIcon.opticalSize, isNull);
      expect(inheritedIcon.shadows, isNull);
      expect(tester.getSize(iconFinder()), const Size.square(41));
      final richText = tester.widget<RichText>(
        find.descendant(of: iconFinder(), matching: find.byType(RichText)),
      );
      final inheritedStyle = (richText.text as TextSpan).style!;
      expect(inheritedStyle.color, inherited.color);
      expect(inheritedStyle.fontSize, 41);
      expect(
        {
          for (final variation in inheritedStyle.fontVariations!)
            variation.axis: variation.value,
        },
        const {'FILL': 0.3, 'wght': 525.0, 'GRAD': 12.0, 'opsz': 18.0},
      );
      expect(inheritedStyle.shadows, inherited.shadows);

      await pump({
        'icon': fixture.iconDataValueForViewTest(fontFamily: 'MaterialIcons'),
        'shadows': {'kind': 'shadowList', 'items': <Object?>[]},
      });
      final emptyShadowStyle =
          (tester
                      .widget<RichText>(
                        find.descendant(
                          of: iconFinder(),
                          matching: find.byType(RichText),
                        ),
                      )
                      .text
                  as TextSpan)
              .style!;
      expect(emptyShadowStyle.shadows, isEmpty);

      await pump({
        'icon': fixture.iconDataValueForViewTest(
          codePoint: null,
          fontFamily: null,
        ),
      });
      final icon = tester.widget<Icon>(iconFinder());
      expect(icon.icon, isNull);
      expect(icon.size, isNull);
      expect(icon.color, isNull);
      expect(icon.fill, isNull);
      expect(icon.weight, isNull);
      expect(icon.grade, isNull);
      expect(icon.opticalSize, isNull);
      expect(icon.shadows, isNull);
      expect(tester.getSize(iconFinder()), const Size.square(41));
      expect(
        find.descendant(of: iconFinder(), matching: find.byType(RichText)),
        findsNothing,
      );
    },
  );

  testWidgets(
    'keeps zero-size and clear Icons selectable with overlapping targets',
    (tester) async {
      const firstId = fixture.iconWidgetIdForViewTest;
      const secondId = 'a77ba7a9-61e8-438c-908f-aa495c49e7d7';
      final model = CanvasModel.decode(
        Uint8List.fromList(
          utf8.encode(
            jsonEncode(_modelWithZeroIconSiblings([firstId, secondId])),
          ),
        ),
      );
      String? selected;
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: selected,
          onSelected: (value) => selected = value,
        ),
      );
      await tester.pump();
      await tester.pump();

      expect(
        tester.getSize(find.byKey(const ValueKey('canvas-widget-$firstId'))),
        Size.zero,
      );
      final group = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-group-$firstId'),
      );
      expect(group, findsOneWidget);
      expect(tester.getSize(group), const Size.square(36));
      await tester.tap(group);
      expect(selected, firstId);
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: selected,
          onSelected: (value) => selected = value,
        ),
      );
      await tester.pump();
      await tester.pump();
      await tester.tap(
        find.byKey(
          const ValueKey('canvas-zero-size-widget-target-group-$firstId'),
        ),
      );
      expect(selected, secondId);

      final clearModel = CanvasModel.decode(
        fixture.iconModelBytesForViewTest(
          properties: {
            'icon': fixture.iconDataValueForViewTest(
              fontFamily: 'MaterialIcons',
            ),
            'size': {'kind': 'double', 'value': 24.0},
            'blendMode': {
              'kind': 'enum',
              'type': 'BlendMode',
              'value': 'clear',
            },
          },
        ),
      );
      selected = null;
      await tester.pumpWidget(
        CanvasModelApp(
          model: clearModel,
          selectedWidgetId: null,
          onSelected: (value) => selected = value,
        ),
      );
      final clearTarget = find.byKey(
        const ValueKey('canvas-widget-${fixture.iconWidgetIdForViewTest}'),
      );
      expect(tester.getSize(clearTarget), const Size.square(24));
      expect(tester.widget<Icon>(find.byType(Icon)).blendMode, BlendMode.clear);
      await tester.tap(clearTarget);
      expect(selected, fixture.iconWidgetIdForViewTest);
    },
  );
}

Map<String, Object?> _modelJsonForView() =>
    jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
        as Map<String, Object?>;

Map<String, Object?> _modelWithCenteredSizedBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '38f49912-8e51-4e62-bd4c-2517ecad4962',
          'type': 'flutter.widgets.SizedBox',
          'properties': properties,
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredAspectRatio({
  required double aspectRatio,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '4a07880d-54a7-44c6-97e4-a88df4e44e67',
          'type': 'flutter.widgets.AspectRatio',
          'properties': <String, Object?>{
            'aspectRatio': {'kind': 'double', 'value': aspectRatio},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredBaseline({
  required double baseline,
  required String baselineType,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '0197b4c0-11f0-45b0-9c63-4caa0981d386',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '0197b4c0-11f0-45b1-bfe8-cc84ef5a6fae',
          'type': 'flutter.widgets.Baseline',
          'properties': <String, Object?>{
            'baseline': {'kind': 'double', 'value': baseline},
            'baselineType': {
              'kind': 'enum',
              'type': 'TextBaseline',
              'value': baselineType,
            },
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredIntrinsicHeight({
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '8d1a8689-6d4d-4c64-9d20-fdca66975e86',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '15aa2055-201d-4f3e-bd50-24cc60fa50c0',
          'type': 'flutter.widgets.IntrinsicHeight',
          'properties': <String, Object?>{},
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredIntrinsicWidth({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': 'd3788591-8a73-4a82-b16c-e0b75a47ea22',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': 'bbce8cc2-8ff1-4337-83e2-46f70a579075',
          'type': 'flutter.widgets.IntrinsicWidth',
          'properties': properties,
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredOffstage({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '3041fd16-502f-42aa-8073-b30fb54014dc',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': 'aa0bc346-b863-471f-bf3a-bcaa9bbf5090',
          'type': 'flutter.widgets.Offstage',
          'properties': properties,
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredRotatedBox({
  required int quarterTurns,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '55a1a386-b45f-4a5d-aa5d-e124e9d53851',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '81cdfd65-c958-40cf-ab25-3494d1a9e1fc',
          'type': 'flutter.widgets.RotatedBox',
          'properties': <String, Object?>{
            'quarterTurns': {'kind': 'integer', 'value': quarterTurns},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredSizedOverflowBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '55a1a386-b45f-4a5d-aa5d-e124e9d53851',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': 'd01c13f5-c20d-48bf-a360-5da7b6b36c67',
          'type': 'flutter.widgets.SizedOverflowBox',
          'properties': properties,
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredTransform({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  Size? tightSize,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  Map<String, Object?> centeredChild = <String, Object?>{
    'id': '6408cfe9-e227-43de-916c-bb9d66224de9',
    'type': 'flutter.widgets.Transform',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  if (tightSize case final size?) {
    centeredChild = <String, Object?>{
      'id': '0eefaa90-d45f-4a2c-b87f-ab46971d2d44',
      'type': 'flutter.widgets.SizedBox',
      'properties': <String, Object?>{
        'width': {'kind': 'double', 'value': size.width},
        'height': {'kind': 'double', 'value': size.height},
      },
      'slots': <String, Object?>{
        'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
      },
    };
  }
  body['child'] = <String, Object?>{
    'id': 'c17d0f6c-6dbe-4af5-bdd5-1468b690a0d2',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredAlign({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': 'b51246d4-4e44-4d7c-91bc-a0892df4a341',
          'type': 'flutter.widgets.Align',
          'properties': properties,
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredFractionallySizedBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final fractionallySizedBox = <String, Object?>{
    'id': '403df8b2-b244-4e7c-9ff1-29ba070206b6',
    'type': 'flutter.widgets.FractionallySizedBox',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': 'd4401632-faf9-4765-9165-61de3e851ea6',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 200},
            'height': {'kind': 'integer', 'value': 100},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{
              'kind': 'single',
              'child': fractionallySizedBox,
            },
          },
        }
      : fractionallySizedBox;
  body['child'] = <String, Object?>{
    'id': '5e5dadcf-d272-45d9-aec8-295ac4afc7f0',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredConstrainedBox({
  required Map<String, Object?> constraints,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final constrainedBox = <String, Object?>{
    'id': '93d89766-af04-4fee-af57-a56c4ed5e37c',
    'type': 'flutter.widgets.ConstrainedBox',
    'properties': <String, Object?>{'constraints': constraints},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': '8fb51a53-f998-420d-bf96-f5cd3eaf66c6',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 200},
            'height': {'kind': 'integer', 'value': 100},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{
              'kind': 'single',
              'child': <String, Object?>{
                'id': 'a17a1cf7-a873-44d8-96bd-84b4e8089fa6',
                'type': 'flutter.widgets.Center',
                'properties': <String, Object?>{},
                'slots': <String, Object?>{
                  'child': <String, Object?>{
                    'kind': 'single',
                    'child': constrainedBox,
                  },
                },
              },
            },
          },
        }
      : constrainedBox;
  body['child'] = <String, Object?>{
    'id': 'faef84b3-15d3-4a5c-9fc3-f3a833bb1e59',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _viewBoxConstraints(
  Object? minWidth,
  Object? maxWidth,
  Object? minHeight,
  Object? maxHeight,
) => {
  'kind': 'boxConstraints',
  'minWidth': minWidth,
  'maxWidth': maxWidth,
  'minHeight': minHeight,
  'maxHeight': maxHeight,
};

Map<String, Object?> _modelWithCenteredUnconstrainedBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final unconstrainedBox = <String, Object?>{
    'id': '67247867-79f8-470f-b109-59971aa7392c',
    'type': 'flutter.widgets.UnconstrainedBox',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': '0b6f7f3b-d2dc-49ce-bf61-1834859cc478',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 200},
            'height': {'kind': 'integer', 'value': 100},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{
              'kind': 'single',
              'child': unconstrainedBox,
            },
          },
        }
      : unconstrainedBox;
  body['child'] = <String, Object?>{
    'id': 'c7dd4e0d-a326-4bf9-84a3-f95a759bd1d8',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredLimitedBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  required String incoming,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final limitedBox = <String, Object?>{
    'id': '57b8ce90-edde-4988-a4b7-bbf2eec66922',
    'type': 'flutter.widgets.LimitedBox',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final positionedChild = switch (incoming) {
    'bounded' => limitedBox,
    'widthUnbounded' => <String, Object?>{
      'id': '3d96e466-b5f7-4bb9-b12c-4d78b3ae6a94',
      'type': 'flutter.widgets.UnconstrainedBox',
      'properties': <String, Object?>{
        'constrainedAxis': <String, Object?>{
          'kind': 'enum',
          'type': 'Axis',
          'value': 'vertical',
        },
      },
      'slots': <String, Object?>{
        'child': <String, Object?>{'kind': 'single', 'child': limitedBox},
      },
    },
    'heightUnbounded' => <String, Object?>{
      'id': 'e20426cb-1ab5-4a15-817d-5cd5e582f8a1',
      'type': 'flutter.widgets.UnconstrainedBox',
      'properties': <String, Object?>{
        'constrainedAxis': <String, Object?>{
          'kind': 'enum',
          'type': 'Axis',
          'value': 'horizontal',
        },
      },
      'slots': <String, Object?>{
        'child': <String, Object?>{'kind': 'single', 'child': limitedBox},
      },
    },
    'bothUnbounded' => <String, Object?>{
      'id': 'b263333d-4c52-425f-a844-793339ccdc73',
      'type': 'flutter.widgets.UnconstrainedBox',
      'properties': <String, Object?>{},
      'slots': <String, Object?>{
        'child': <String, Object?>{'kind': 'single', 'child': limitedBox},
      },
    },
    _ => throw ArgumentError.value(incoming, 'incoming'),
  };
  final boundedFrame = <String, Object?>{
    'id': '2c0e55bf-3199-4895-9145-0b487099f3df',
    'type': 'flutter.widgets.SizedBox',
    'properties': <String, Object?>{
      'width': {'kind': 'integer', 'value': 200},
      'height': {'kind': 'integer', 'value': 100},
    },
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': positionedChild},
    },
  };
  body['child'] = <String, Object?>{
    'id': '66a0a157-152c-4bd5-8455-514a87aa4154',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': boundedFrame},
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredOverflowBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final overflowBox = <String, Object?>{
    'id': '7a59d693-7fd5-4c80-a4e2-3d0f908a41f4',
    'type': 'flutter.widgets.OverflowBox',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': '04476d22-29cf-4b9e-aeb3-ee36d0275d08',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 200},
            'height': {'kind': 'integer', 'value': 100},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': overflowBox},
          },
        }
      : overflowBox;
  body['child'] = <String, Object?>{
    'id': '47924710-4a25-4cf0-9495-287483e941d6',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredFittedBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final fittedBox = <String, Object?>{
    'id': 'b587a092-9a65-420a-9d1c-e127cc752f8d',
    'type': 'flutter.widgets.FittedBox',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': 'e5950c77-cf55-4fa5-8595-f62c9fb67662',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 200},
            'height': {'kind': 'integer', 'value': 100},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': fittedBox},
          },
        }
      : fittedBox;
  body['child'] = <String, Object?>{
    'id': 'c01d308e-653d-467f-a113-e77ab22c1775',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredClipOval({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final clipOval = <String, Object?>{
    'id': '91684e03-f63a-4cde-b899-7a50c888f9e7',
    'type': 'flutter.widgets.ClipOval',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': '61fc12b1-ad5c-43c9-af3f-a073f827ebc0',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 80},
            'height': {'kind': 'integer', 'value': 60},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': clipOval},
          },
        }
      : clipOval;
  body['child'] = <String, Object?>{
    'id': 'ecb36777-0ce6-499e-bc30-0c83c10e6198',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredClipRRect({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool bounded = true,
  String textDirection = 'ltr',
  bool explicitDirectionality = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final clipRRect = <String, Object?>{
    'id': '5582922d-9044-4e78-bba8-bb740884a49d',
    'type': 'flutter.widgets.ClipRRect',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final boundedChild = bounded
      ? <String, Object?>{
          'id': '2feef8b0-c5e7-494a-95e7-4b91448484bb',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 80},
            'height': {'kind': 'integer', 'value': 60},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': clipRRect},
          },
        }
      : clipRRect;
  final directionalChild = <String, Object?>{
    'id': '967fd334-2e92-47fc-a1cd-2019ff0f9284',
    'type': 'flutter.widgets.Directionality',
    'properties': <String, Object?>{
      'textDirection': {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': textDirection,
      },
    },
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': boundedChild},
    },
  };
  body['child'] = <String, Object?>{
    'id': '8083347c-b215-4d21-a00e-f17baf26c0e7',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': explicitDirectionality ? directionalChild : boundedChild,
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredClipRect({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool bounded = true,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final clipRect = <String, Object?>{
    'id': '4fcae2d8-2c48-4c9d-b3f0-62bf0d89ef67',
    'type': 'flutter.widgets.ClipRect',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final centeredChild = bounded
      ? <String, Object?>{
          'id': 'a64742a8-e154-4e64-933b-f48bc0a5bc43',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 80},
            'height': {'kind': 'integer', 'value': 60},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': clipRect},
          },
        }
      : clipRect;
  body['child'] = <String, Object?>{
    'id': 'aa46ecaa-f4b7-467e-bbdd-0d91259c4ab5',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': centeredChild},
    },
  };
  return model;
}

Map<String, Object?> _modelWithConstrainedStack({
  required Map<String, Object?> properties,
  required List<Map<String, Object?>> children,
  bool unboundedMainAxis = false,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final stack = <String, Object?>{
    'id': '5a809127-5a50-4dbf-b5fd-464619344ac4',
    'type': 'flutter.widgets.Stack',
    'properties': properties,
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  final constrained = unboundedMainAxis
      ? <String, Object?>{
          'id': 'c2f2877d-3fab-4b71-a232-b4bf44dcd7c0',
          'type': 'flutter.widgets.Row',
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[stack],
            },
          },
        }
      : <String, Object?>{
          'id': 'd376e25c-2809-4d1a-bc48-d54fca11a123',
          'type': 'flutter.widgets.Container',
          'properties': <String, Object?>{
            'constraints': <String, Object?>{
              'kind': 'boxConstraints',
              'minWidth': 100,
              'maxWidth': 200,
              'minHeight': 50,
              'maxHeight': 100,
            },
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': stack},
          },
        };
  body['child'] = <String, Object?>{
    'id': '3ca862de-2561-4f9e-af0f-dfa53e7f4f28',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': constrained},
    },
  };
  return model;
}

Map<String, Object?> _modelWithConstrainedIndexedStack({
  required Map<String, Object?> properties,
  required List<Map<String, Object?>> children,
  bool unboundedMainAxis = false,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final indexedStack = <String, Object?>{
    'id': '910fd547-b4aa-4da2-8bd1-bde08cae3944',
    'type': 'flutter.widgets.IndexedStack',
    'properties': properties,
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  final constrained = unboundedMainAxis
      ? <String, Object?>{
          'id': '95152f05-efab-45e2-a5a0-1a60ef154c84',
          'type': 'flutter.widgets.Row',
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[indexedStack],
            },
          },
        }
      : <String, Object?>{
          'id': '8d2f2dda-13ae-43ae-b972-40931d45d44e',
          'type': 'flutter.widgets.Container',
          'properties': <String, Object?>{
            'constraints': <String, Object?>{
              'kind': 'boxConstraints',
              'minWidth': 100,
              'maxWidth': 200,
              'minHeight': 50,
              'maxHeight': 100,
            },
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': indexedStack},
          },
        };
  body['child'] = <String, Object?>{
    'id': '2fe9c951-a742-4fd9-919d-4ecba721f73f',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': constrained},
    },
  };
  return model;
}

Map<String, Object?> _modelWithConstrainedWrap({
  required Map<String, Object?> properties,
  required List<Map<String, Object?>> children,
  bool unboundedMainAxis = false,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final wrap = <String, Object?>{
    'id': '0cdde885-b68c-4b21-bd47-246721c4e8b2',
    'type': 'flutter.widgets.Wrap',
    'properties': properties,
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  final constrained = unboundedMainAxis
      ? <String, Object?>{
          'id': 'fc7d6ad3-bafe-43f8-9da9-d1742116bc89',
          'type': 'flutter.widgets.Row',
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[wrap],
            },
          },
        }
      : <String, Object?>{
          'id': '5b9856b4-1a5d-44d8-94a7-2106df659fba',
          'type': 'flutter.widgets.Container',
          'properties': <String, Object?>{
            'constraints': <String, Object?>{
              'kind': 'boxConstraints',
              'minWidth': 0,
              'maxWidth': 100,
              'minHeight': 0,
              'maxHeight': 100,
            },
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': wrap},
          },
        };
  body['child'] = <String, Object?>{
    'id': 'a00ef225-fe97-46a1-bc4d-2298da4348dc',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': constrained},
    },
  };
  return model;
}

Map<String, Object?> _viewExpandedNode(
  String id, {
  int? flex,
  required Map<String, Object?> child,
}) => <String, Object?>{
  'id': id,
  'type': 'flutter.widgets.Expanded',
  'properties': <String, Object?>{
    if (flex != null) 'flex': {'kind': 'integer', 'value': flex},
  },
  'slots': <String, Object?>{
    'child': <String, Object?>{'kind': 'single', 'child': child},
  },
};

Map<String, Object?> _viewFlexibleNode(
  String id, {
  int? flex,
  String? fit,
  required Map<String, Object?> child,
}) => <String, Object?>{
  'id': id,
  'type': 'flutter.widgets.Flexible',
  'properties': <String, Object?>{
    if (flex != null) 'flex': {'kind': 'integer', 'value': flex},
    if (fit != null) 'fit': {'kind': 'enum', 'type': 'FlexFit', 'value': fit},
  },
  'slots': <String, Object?>{
    'child': <String, Object?>{'kind': 'single', 'child': child},
  },
};

Map<String, Object?> _viewSpacerNode(String id, {int? flex}) =>
    <String, Object?>{
      'id': id,
      'type': 'flutter.widgets.Spacer',
      'properties': <String, Object?>{
        if (flex != null) 'flex': {'kind': 'integer', 'value': flex},
      },
      'slots': <String, Object?>{},
    };

Map<String, Object?> _modelWithFixedFlex({
  required String parentType,
  String parentId = 'be4b446d-12b9-42c3-a427-03fb2fd472bd',
  Map<String, Object?> parentProperties = const {},
  required List<Map<String, Object?>> children,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final flex = <String, Object?>{
    'id': parentId,
    'type': parentType,
    'properties': parentProperties,
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  body['child'] = <String, Object?>{
    'id': '384718a1-ddea-40a0-82ca-d7cb4ebaf865',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '63657b7c-9c7b-4e7c-a200-c4ae1d37326a',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 300},
            'height': {'kind': 'integer', 'value': 120},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': flex},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithListBody({
  required Map<String, Object?> properties,
  required List<Map<String, Object?>> children,
  String? unboundedParentType,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final listBody = <String, Object?>{
    'id': 'd79aaf47-73ef-43ee-943b-c2d44b92391d',
    'type': 'flutter.widgets.ListBody',
    'properties': properties,
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  final content = unboundedParentType == null
      ? listBody
      : <String, Object?>{
          'id': 'c971dca8-1238-45e6-913f-65579d142c5e',
          'type': unboundedParentType,
          'properties': <String, Object?>{},
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[listBody],
            },
          },
        };
  body['child'] = <String, Object?>{
    'id': '31684f33-2e10-4fc6-9658-77cf0fe7eb55',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '3d80adae-3c10-4a2c-9b76-46179ea53b13',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 300},
            'height': {'kind': 'integer', 'value': 160},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': content},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithOverflowBar({
  required num width,
  required Map<String, Object?> properties,
  required List<Map<String, Object?>> children,
  String? parentType,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final overflowBar = <String, Object?>{
    'id': '765940f9-e63f-46d8-8837-07c52904c9b9',
    'type': 'flutter.widgets.OverflowBar',
    'properties': properties,
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  final content = parentType == null
      ? <String, Object?>{
          'id': '3d80adae-3c10-4a2c-9b76-46179ea53b13',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {
              'kind': width is int ? 'integer' : 'double',
              'value': width,
            },
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': overflowBar},
          },
        }
      : <String, Object?>{
          'id': 'c971dca8-1238-45e6-913f-65579d142c5e',
          'type': parentType,
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[overflowBar],
            },
          },
        };
  body['child'] = <String, Object?>{
    'id': '31684f33-2e10-4fc6-9658-77cf0fe7eb55',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '384718a1-ddea-40a0-82ca-d7cb4ebaf865',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 300},
            'height': {'kind': 'integer', 'value': 160},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': content},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithListView({
  required Map<String, Object?> properties,
  required List<Map<String, Object?>> children,
  String? unboundedParentType,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final listView = <String, Object?>{
    'id': '4febd2a9-b2ef-4f4d-8d67-2df913b46da2',
    'type': 'flutter.widgets.ListView',
    'properties': properties,
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  final child = unboundedParentType == null
      ? <String, Object?>{
          'id': '3d80adae-3c10-4a2c-9b76-46179ea53b13',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 300},
            'height': {'kind': 'integer', 'value': 160},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': listView},
          },
        }
      : <String, Object?>{
          'id': 'f0f34b58-a9d5-46da-8fde-2daf20400796',
          'type': unboundedParentType,
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[listView],
            },
          },
        };
  body['child'] = <String, Object?>{
    'id': '31684f33-2e10-4fc6-9658-77cf0fe7eb55',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  return model;
}

Map<String, Object?> _modelWithGridView({
  required Map<String, Object?> properties,
  required List<Map<String, Object?>> children,
  String? unboundedParentType,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final gridView = <String, Object?>{
    'id': '7c5646ab-89dc-45b0-b147-f46604fc411f',
    'type': 'flutter.widgets.GridView',
    'properties': <String, Object?>{
      'crossAxisCount': {'kind': 'integer', 'value': 2},
      ...properties,
    },
    'slots': <String, Object?>{
      'children': <String, Object?>{'kind': 'list', 'children': children},
    },
  };
  final child = unboundedParentType == null
      ? <String, Object?>{
          'id': 'f929fdb0-0143-4984-88ae-5d456ff6dbd1',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 300},
            'height': {'kind': 'integer', 'value': 160},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': gridView},
          },
        }
      : <String, Object?>{
          'id': '6befa00a-3715-498c-a818-e78c4ff39936',
          'type': unboundedParentType,
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[gridView],
            },
          },
        };
  body['child'] = <String, Object?>{
    'id': '466ce4d2-1179-4e5b-b4d9-1bc09898a75e',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  return model;
}

Map<String, Object?> _modelWithSingleChildScrollView({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  bool unbounded = false,
  Map<String, Object?>? sibling,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final scrollView = <String, Object?>{
    'id': 'cd068a37-bb45-49a3-a622-6ba944878d58',
    'type': 'flutter.widgets.SingleChildScrollView',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final content = unbounded
      ? <String, Object?>{
          'id': 'ecf85138-268e-4a7c-9bc9-b4dc9b1f9e9a',
          'type': 'flutter.widgets.Column',
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[scrollView, ?sibling],
            },
          },
        }
      : <String, Object?>{
          'id': '2f519172-2bf0-4632-856a-e235156fac75',
          'type': 'flutter.widgets.SizedBox',
          'properties': <String, Object?>{
            'width': {'kind': 'integer', 'value': 300},
            'height': {'kind': 'integer', 'value': 160},
          },
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': scrollView},
          },
        };
  body['child'] = <String, Object?>{
    'id': 'a948cfaf-a248-43f1-be76-dd3d4e351756',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': content},
    },
  };
  return model;
}

Map<String, Object?> _viewSizedBoxNode(
  String id, {
  required num width,
  required num height,
}) => <String, Object?>{
  'id': id,
  'type': 'flutter.widgets.SizedBox',
  'properties': <String, Object?>{
    'width': {'kind': width is int ? 'integer' : 'double', 'value': width},
    'height': {'kind': height is int ? 'integer' : 'double', 'value': height},
  },
  'slots': <String, Object?>{
    'child': <String, Object?>{'kind': 'single', 'child': null},
  },
};

Map<String, Object?> _viewTextNode(String id, String data) => <String, Object?>{
  'id': id,
  'type': 'flutter.widgets.Text',
  'properties': <String, Object?>{
    'data': {'kind': 'string', 'value': data},
  },
  'slots': <String, Object?>{},
};

Map<String, Object?> _modelWithCenteredOpacity({
  required double opacity,
  required bool? alwaysIncludeSemantics,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final properties = <String, Object?>{
    'opacity': {'kind': 'double', 'value': opacity},
  };
  if (alwaysIncludeSemantics != null) {
    properties['alwaysIncludeSemantics'] = {
      'kind': 'boolean',
      'value': alwaysIncludeSemantics,
    };
  }
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '47f754c8-9fcb-480c-9578-87cda83bd4d5',
          'type': 'flutter.widgets.Opacity',
          'properties': properties,
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithColoredBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  Map<String, Object?>? sibling,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final coloredBox = <String, Object?>{
    'id': 'ec949ebe-9c66-48b7-8901-c691d7356e07',
    'type': 'flutter.widgets.ColoredBox',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final content = sibling == null
      ? coloredBox
      : <String, Object?>{
          'id': 'ad48ab88-48b9-42fe-ad33-b2324bf78b8a',
          'type': 'flutter.widgets.Column',
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[coloredBox, sibling],
            },
          },
        };
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': content},
    },
  };
  return model;
}

Map<String, Object?> _modelWithPlaceholder({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
  Map<String, Object?>? sibling,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final placeholder = <String, Object?>{
    'id': 'b4d88c9a-9eca-4a53-92a6-71f1631ef235',
    'type': 'flutter.widgets.Placeholder',
    'properties': properties,
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final content = sibling == null
      ? placeholder
      : <String, Object?>{
          'id': 'b4372420-215c-44f2-9360-d71493949483',
          'type': 'flutter.widgets.Column',
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[placeholder, sibling],
            },
          },
        };
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': content},
    },
  };
  return model;
}

Map<String, Object?> _modelWithSafeArea({
  required Map<String, Object?> properties,
  required Map<String, Object?> child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': 'df5babd2-16cf-44c4-b497-24375532ec68',
          'type': canvasSafeAreaWidgetType,
          'properties': properties,
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithDirectionality({
  required String direction,
  required Map<String, Object?> child,
  Map<String, Object?>? sibling,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final directionality = <String, Object?>{
    'id': '1dd83790-acde-4aa4-8d58-4ab79f08042d',
    'type': canvasDirectionalityWidgetType,
    'properties': <String, Object?>{
      'textDirection': {
        'kind': 'enum',
        'type': 'TextDirection',
        'value': direction,
      },
    },
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': child},
    },
  };
  final content = sibling == null
      ? directionality
      : <String, Object?>{
          'id': '3651af07-e8b8-4426-95b2-7476239f5ae4',
          'type': 'flutter.widgets.Column',
          'properties': <String, Object?>{
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
          },
          'slots': <String, Object?>{
            'children': <String, Object?>{
              'kind': 'list',
              'children': <Map<String, Object?>>[directionality, sibling],
            },
          },
        };
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': content},
    },
  };
  return model;
}

Map<String, Object?> _modelWithDecoratedBox({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '607a1c0f-5fd3-438c-ae36-0a054ce05949',
          'type': 'flutter.widgets.DecoratedBox',
          'properties': properties,
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithExcludeSemantics({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': '2a082f33-7251-4a63-9f41-845aa0aa7a80',
          'type': 'flutter.widgets.ExcludeSemantics',
          'properties': properties,
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredContainer({
  required Map<String, Object?> properties,
  required Map<String, Object?>? child,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': 'd9e278fa-32f8-4ef7-a92f-4aef0867435c',
          'type': 'flutter.widgets.Container',
          'properties': properties,
          'slots': <String, Object?>{
            'child': <String, Object?>{'kind': 'single', 'child': child},
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredImage({
  required String id,
  required Map<String, Object?> properties,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': id,
          'type': 'flutter.widgets.Image',
          'properties': properties,
          'slots': <String, Object?>{},
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithCenteredTextField({
  required String id,
  required Map<String, Object?> properties,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Center',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'child': <String, Object?>{
        'kind': 'single',
        'child': <String, Object?>{
          'id': id,
          'type': 'flutter.material.TextField',
          'properties': properties,
          'slots': <String, Object?>{},
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithTextFieldInFlex({
  required String parentType,
  required Map<String, Object?> properties,
}) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': 'c197acdc-e87f-4b42-888c-907554c30262',
    'type': parentType,
    'properties': <String, Object?>{
      'mainAxisSize': {'kind': 'enum', 'type': 'MainAxisSize', 'value': 'min'},
    },
    'slots': <String, Object?>{
      'children': <String, Object?>{
        'kind': 'list',
        'children': <Object?>[
          <String, Object?>{
            'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
            'type': 'flutter.widgets.Align',
            'properties': <String, Object?>{},
            'slots': <String, Object?>{
              'child': <String, Object?>{
                'kind': 'single',
                'child': <String, Object?>{
                  'id': '73d9ec43-3d37-4304-8998-71fe51804284',
                  'type': 'flutter.material.TextField',
                  'properties': properties,
                  'slots': <String, Object?>{},
                },
              },
            },
          },
        ],
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithEmptyContainerInRow() {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Column',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'children': <String, Object?>{
        'kind': 'list',
        'children': <Object?>[
          <String, Object?>{
            'id': '38f49912-8e51-4e62-bd4c-2517ecad4962',
            'type': 'flutter.widgets.Row',
            'properties': <String, Object?>{},
            'slots': <String, Object?>{
              'children': <String, Object?>{
                'kind': 'list',
                'children': <Object?>[
                  <String, Object?>{
                    'id': 'd9e278fa-32f8-4ef7-a92f-4aef0867435c',
                    'type': 'flutter.widgets.Container',
                    'properties': <String, Object?>{},
                    'slots': <String, Object?>{
                      'child': <String, Object?>{
                        'kind': 'single',
                        'child': null,
                      },
                    },
                  },
                ],
              },
            },
          },
        ],
      },
    },
  };
  return model;
}

Map<String, Object?> _viewAlignment({
  String basis = 'physical',
  num horizontal = 0,
  num vertical = 0,
}) => {
  'kind': 'alignmentGeometry',
  'basis': basis,
  'horizontal': horizontal,
  'vertical': vertical,
};

Map<String, Object?> _viewNestedAlignment({
  String basis = 'physical',
  num horizontal = 0,
  num vertical = 0,
}) => {'basis': basis, 'horizontal': horizontal, 'vertical': vertical};

Map<String, Object?> _viewLiteralColor(String argb) => {
  'kind': 'literal',
  'argb': argb,
};

Map<String, Object?> _viewThemeColor(String token) => {
  'kind': 'theme',
  'token': token,
};

Map<String, Object?> _viewBorderSide() => {
  'color': _viewThemeColor('material.colorScheme.outline'),
  'width': 2,
  'style': 'solid',
  'strokeAlign': -1,
};

Map<String, Object?> _viewPhysicalBorder() {
  final side = _viewBorderSide();
  return {
    'kind': 'physical',
    'top': Map<String, Object?>.from(side),
    'right': Map<String, Object?>.from(side),
    'bottom': Map<String, Object?>.from(side),
    'left': Map<String, Object?>.from(side),
  };
}

Map<String, Object?> _viewDirectionalBorder() {
  final side = _viewBorderSide();
  return {
    'kind': 'directional',
    'top': Map<String, Object?>.from(side),
    'start': Map<String, Object?>.from(side),
    'end': Map<String, Object?>.from(side),
    'bottom': Map<String, Object?>.from(side),
  };
}

Map<String, Object?> _viewRadius(num x, num y) => {'x': x, 'y': y};

Map<String, Object?> _viewDirectionalRadius() => {
  'kind': 'directional',
  'topStart': _viewRadius(8, 4),
  'topEnd': _viewRadius(10, 5),
  'bottomEnd': _viewRadius(12, 6),
  'bottomStart': _viewRadius(14, 7),
};

Map<String, Object?> _viewPhysicalRadius() => {
  'kind': 'physical',
  'topLeft': _viewRadius(2, 3),
  'topRight': _viewRadius(4, 5),
  'bottomRight': _viewRadius(6, 7),
  'bottomLeft': _viewRadius(8, 9),
};

Map<String, Object?> _viewBoxShadow() => {
  'id': '0980edcf-8ef3-46c1-bafb-16cf30939cb2',
  'color': _viewThemeColor('material.colorScheme.shadow'),
  'offsetX': 2,
  'offsetY': 3,
  'blurRadius': 4,
  'spreadRadius': -1,
  'blurStyle': 'outer',
};

List<Map<String, Object?>> _viewGradientStops() => [
  {
    'id': '9c979578-cfe9-4232-8768-901a9fc6c3b2',
    'color': _viewLiteralColor('0xFF102030'),
    'stop': 0,
  },
  {
    'id': '417b70c2-566d-40f9-a7fb-9cd18dca7f3d',
    'color': _viewThemeColor('material.colorScheme.primary'),
    'stop': 1,
  },
];

Map<String, Object?> _viewLinearGradient() => {
  'kind': 'linear',
  'begin': _viewNestedAlignment(horizontal: -1, vertical: -1),
  'end': _viewNestedAlignment(basis: 'directional', horizontal: 1, vertical: 1),
  'stops': _viewGradientStops(),
  'tileMode': 'mirror',
  'rotationRadians': 0.25,
};

Map<String, Object?> _viewRadialGradient() => {
  'kind': 'radial',
  'center': _viewNestedAlignment(horizontal: 0.1, vertical: -0.2),
  'radius': 0.75,
  'focal': _viewNestedAlignment(
    basis: 'directional',
    horizontal: 0.4,
    vertical: 0.3,
  ),
  'focalRadius': 0.15,
  'stops': _viewGradientStops(),
  'tileMode': 'decal',
  'rotationRadians': null,
};

Map<String, Object?> _viewSweepGradient() => {
  'kind': 'sweep',
  'center': _viewNestedAlignment(
    basis: 'directional',
    horizontal: -0.2,
    vertical: 0.4,
  ),
  'startAngle': 0.25,
  'endAngle': 5.75,
  'stops': _viewGradientStops(),
  'tileMode': 'repeated',
  'rotationRadians': -0.3,
};

Map<String, Object?> _viewImageProvider({
  String kind = 'asset',
  String assetName = 'assets/images/panel.png',
  String? packageName,
  num? exactScale,
  Map<String, Object?>? resize,
  required Map<String, Object?> resolution,
}) => {
  'kind': kind,
  'assetName': assetName,
  'packageName': packageName,
  'exactScale': exactScale,
  'resize': resize,
  'resolution': resolution,
};

Map<String, Object?> _viewDirectImageProvider({
  required Map<String, Object?> provider,
}) => {'kind': 'imageProvider', 'value': provider};

Map<String, Object?> _viewDecorationImage({
  required Map<String, Object?> image,
  bool onError = false,
  Map<String, Object?>? colorFilter,
  String? fit,
  Map<String, Object?>? alignment,
  Map<String, Object?>? centerSlice,
  String repeat = 'noRepeat',
  bool matchTextDirection = false,
  num scale = 1,
  num opacity = 1,
  String filterQuality = 'medium',
  bool invertColors = false,
  bool isAntiAlias = false,
}) => {
  'image': image,
  'onError': onError,
  'colorFilter': colorFilter,
  'fit': fit,
  'alignment': alignment ?? _viewNestedAlignment(),
  'centerSlice': centerSlice,
  'repeat': repeat,
  'matchTextDirection': matchTextDirection,
  'scale': scale,
  'opacity': opacity,
  'filterQuality': filterQuality,
  'invertColors': invertColors,
  'isAntiAlias': isAntiAlias,
};

Map<String, Object?> _viewBoxDecoration({
  Map<String, Object?>? color,
  Map<String, Object?>? image,
  Map<String, Object?>? border,
  Map<String, Object?>? borderRadius,
  List<Map<String, Object?>> boxShadow = const [],
  Map<String, Object?>? gradient,
  String? backgroundBlendMode,
  String shape = 'rectangle',
}) => {
  'kind': 'boxDecoration',
  'color': color,
  'image': image,
  'border': border,
  'borderRadius': borderRadius,
  'boxShadow': boxShadow,
  'gradient': gradient,
  'backgroundBlendMode': backgroundBlendMode,
  'shape': shape,
};

Map<String, Object?> _modelWithEmptySizedBoxSiblings(List<String> widgetIds) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Column',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'children': <String, Object?>{
        'kind': 'list',
        'children': <Object?>[
          for (final id in widgetIds)
            <String, Object?>{
              'id': id,
              'type': 'flutter.widgets.SizedBox',
              'properties': <String, Object?>{},
              'slots': <String, Object?>{
                'child': <String, Object?>{'kind': 'single', 'child': null},
              },
            },
        ],
      },
    },
  };
  return model;
}

Map<String, Object?> _modelWithOverlappingCustomClippers() {
  final model = _modelWithEmptySizedBoxSiblings(const [
    '38f49912-8e51-4e62-bd4c-2517ecad4962',
    '4fcae2d8-2c48-4c9d-b3f0-62bf0d89ef67',
    '5582922d-9044-4e78-bba8-bb740884a49d',
  ]);
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  final column = body['child']! as Map<String, Object?>;
  final children =
      ((column['slots']! as Map<String, Object?>)['children']!
              as Map<String, Object?>)['children']!
          as List<Object?>;
  children[1] = <String, Object?>{
    'id': '4fcae2d8-2c48-4c9d-b3f0-62bf0d89ef67',
    'type': 'flutter.widgets.ClipRect',
    'properties': <String, Object?>{
      'clipper': <String, Object?>{'kind': 'dartObjectReferencePresence'},
    },
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': null},
    },
  };
  children[2] = <String, Object?>{
    'id': '5582922d-9044-4e78-bba8-bb740884a49d',
    'type': 'flutter.widgets.ClipRRect',
    'properties': <String, Object?>{
      'clipper': <String, Object?>{'kind': 'dartObjectReferencePresence'},
    },
    'slots': <String, Object?>{
      'child': <String, Object?>{'kind': 'single', 'child': null},
    },
  };
  return model;
}

Map<String, Object?> _modelWithZeroIconSiblings(List<String> widgetIds) {
  final model = _modelJsonForView();
  final root = model['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  body['child'] = <String, Object?>{
    'id': '79f0f14a-b985-4b7f-a10f-dbe50e13fe66',
    'type': 'flutter.widgets.Column',
    'properties': <String, Object?>{},
    'slots': <String, Object?>{
      'children': <String, Object?>{
        'kind': 'list',
        'children': <Object?>[
          for (final id in widgetIds)
            <String, Object?>{
              'id': id,
              'type': 'flutter.widgets.Icon',
              'properties': <String, Object?>{
                'icon': fixture.iconDataValueForViewTest(
                  fontFamily: 'MaterialIcons',
                ),
                'size': <String, Object?>{'kind': 'double', 'value': 0.0},
              },
              'slots': <String, Object?>{},
            },
        ],
      },
    },
  };
  return model;
}

Map<String, Object?> _nodeProperties(Map<String, Object?> model, String type) =>
    _findNode(model['root']! as Map<String, Object?>, type)['properties']!
        as Map<String, Object?>;

Map<String, Object?> _findNode(Map<String, Object?> node, String type) {
  if (node['type'] == type) {
    return node;
  }
  final slots = node['slots']! as Map<String, Object?>;
  for (final rawSlot in slots.values) {
    final slot = rawSlot! as Map<String, Object?>;
    final child = slot['child'];
    if (child is Map<String, Object?>) {
      final match = _findNodeOrNull(child, type);
      if (match != null) {
        return match;
      }
    }
    final children = slot['children'];
    if (children is List<Object?>) {
      for (final child in children.cast<Map<String, Object?>>()) {
        final match = _findNodeOrNull(child, type);
        if (match != null) {
          return match;
        }
      }
    }
  }
  throw StateError('Fixture does not contain $type.');
}

Map<String, Object?>? _findNodeOrNull(Map<String, Object?> node, String type) {
  try {
    return _findNode(node, type);
  } on StateError {
    return null;
  }
}

Future<void> _doubleTap(WidgetTester tester, Finder finder) async {
  await tester.tap(finder);
  await tester.pump(kDoubleTapMinTime);
  await tester.tap(finder);
  await tester.pump(kDoubleTapTimeout);
}

CustomPainter _foregroundPainter(WidgetTester tester, String customPaintKey) =>
    tester
        .widget<CustomPaint>(find.byKey(ValueKey(customPaintKey)))
        .foregroundPainter!;

TestRecordingCanvas _recordPainter(CustomPainter painter, Size size) {
  final canvas = TestRecordingCanvas();
  painter.paint(canvas, size);
  return canvas;
}

int _drawRectCount(TestRecordingCanvas canvas) => canvas.invocations
    .where((recorded) => recorded.invocation.memberName == #drawRect)
    .length;

typedef _RecordedLine = ({Offset start, Offset end, Paint paint});

List<_RecordedLine> _drawLines(TestRecordingCanvas canvas) => [
  for (final recorded in canvas.invocations)
    if (recorded.invocation.memberName == #drawLine)
      (
        start: recorded.invocation.positionalArguments[0] as Offset,
        end: recorded.invocation.positionalArguments[1] as Offset,
        paint: recorded.invocation.positionalArguments[2] as Paint,
      ),
];

List<_RecordedLine> _horizontalLinesAt(
  List<_RecordedLine> lines, {
  required double y,
}) => [
  for (final line in lines)
    if ((line.start.dy - y).abs() < 0.000001 &&
        (line.end.dy - y).abs() < 0.000001 &&
        line.end.dx > line.start.dx)
      line,
];

extension on _RecordedLine {
  double get length => (end - start).distance;
}

List<(Offset, Offset)> _lineEndpoints(List<_RecordedLine> lines) => [
  for (final line in lines) (line.start, line.end),
];

List<(Offset, Offset)> _expectedPaddingGuideEndpoints({
  double left = 10,
  double right = 30,
}) {
  final innerLeft = left;
  final innerRight = 100 - right;
  final guideX = (innerLeft + innerRight) / 2;
  const innerTop = 20.0;
  const innerBottom = 80.0;
  const guideY = (innerTop + innerBottom) / 2;
  return [
    (Offset.zero.translate(0, guideY), Offset(innerLeft, guideY)),
    (Offset(innerRight, guideY), const Offset(100, guideY)),
    (Offset(guideX, 0), Offset(guideX, innerTop)),
    (Offset(guideX, innerBottom), Offset(guideX, 120)),
  ];
}
