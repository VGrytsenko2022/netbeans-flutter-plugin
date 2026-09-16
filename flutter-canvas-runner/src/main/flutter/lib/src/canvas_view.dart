import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;
import 'dart:ui'
    as ui
    show BoxHeightStyle, BoxWidthStyle, SemanticsRole, CheckedState, ImageFilter;

import 'package:flutter/gestures.dart';
import 'package:flutter/foundation.dart' show precisionErrorTolerance, ValueListenable;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart'
    show
        ImageFilterConfig,
        OverflowBoxFit,
        ScrollCacheExtent,
        RenderProxyBox,
        RenderTable,
        RenderTransform,
        RenderPositionedBox,
        RenderSliver,
        RenderSliverConstrainedCrossAxis,
        RenderSliverOffstage,
        RenderViewportBase,
        applyGrowthDirectionToAxisDirection,
        RenderFlex,
        RenderConstrainedBox,
        RenderFractionallySizedOverflowBox,
        FlexParentData,
        BoxParentData,
        BoxHitTestResult,
        RenderObjectVisitor,
        RenderObjectWithLayoutCallbackMixin;
import 'package:flutter/scheduler.dart';
import 'package:flutter/semantics.dart';
import 'package:flutter/services.dart';

import 'canvas_drop.dart';
import 'canvas_model.dart';
import 'canvas_table_width.dart';
import 'canvas_runtime.dart';

bool _ignoreDeleteSelected() => false;

// Flutter 3.44.8 IconButton exposes no public defaultStyleOf. Project only
// the compound fallback fields used by our partial-field style composer.
// The real SDK IconButton remains responsible for all colors, overlays,
// selection, variant paint, animation and interactions. No private elements
// or State instances are inspected or manufactured here.
ButtonStyle _iconButtonCompoundDefaults(BuildContext context, String variant) =>
    ButtonStyle(
      minimumSize: const WidgetStatePropertyAll(Size(40, 40)),
      maximumSize: const WidgetStatePropertyAll(Size.infinite),
      shape: const WidgetStatePropertyAll(StadiumBorder()),
      visualDensity: VisualDensity.standard,
      side: variant == 'outlined'
          ? WidgetStateProperty.resolveWith((states) {
              if (states.contains(WidgetState.selected)) return null;
              final colors = Theme.of(context).colorScheme;
              return BorderSide(
                color: states.contains(WidgetState.disabled)
                    ? colors.onSurface.withAlpha((255 * .12).round())
                    : colors.outline,
              );
            })
          : null,
    );

ButtonStyle _iconButtonThemeStyle(BuildContext context) {
  final iconTheme = IconTheme.of(context);
  final defaultColor = identical(
    iconTheme.color,
    Theme.brightnessOf(context) == Brightness.light
        ? kDefaultIconDarkColor
        : kDefaultIconLightColor,
  );
  final icons = IconButton.styleFrom(
    foregroundColor: defaultColor ? null : iconTheme.color,
    iconSize: iconTheme.size == const IconThemeData.fallback().size
        ? null
        : iconTheme.size,
  );
  return IconButtonTheme.of(context).style?.merge(icons) ?? icons;
}

const _textButtonStateLayers = <WidgetState, String>{
  WidgetState.disabled: 'styleDisabled',
  WidgetState.error: 'styleError',
  WidgetState.dragged: 'styleDragged',
  WidgetState.pressed: 'stylePressed',
  WidgetState.selected: 'styleSelected',
  WidgetState.scrolledUnder: 'styleScrolledUnder',
  WidgetState.hovered: 'styleHovered',
  WidgetState.focused: 'styleFocused',
};

const _checkboxStateLayers = <WidgetState, String>{
  WidgetState.disabled: 'Disabled',
  WidgetState.error: 'Error',
  WidgetState.dragged: 'Dragged',
  WidgetState.pressed: 'Pressed',
  WidgetState.selected: 'Selected',
  WidgetState.scrolledUnder: 'ScrolledUnder',
  WidgetState.hovered: 'Hovered',
  WidgetState.focused: 'Focused',
};

double? _sliderNumber(CanvasNode node, String name) =>
    switch (node.properties[name]?.value) {
      CanvasEnumValue(value: 'infinity') => double.infinity,
      CanvasEnumValue(value: 'negativeInfinity') => double.negativeInfinity,
      num value => value.toDouble(),
      _ => null,
    };

double? _listTileNumber(CanvasNode node, String name) =>
    switch (node.properties[name]?.value) {
      CanvasEnumValue(value: 'nan') => double.nan,
      _ => _sliderNumber(node, name),
    };

bool _listTileHasCallback(CanvasNode node, String name) =>
    node.properties[name]?.value == 'noop' ||
    node.properties[name]?.kind == 'dartObjectReferencePresence';

String? _listTileStaticMessage(CanvasNode node, BuildContext? context) {
  final refs = node.properties.entries
      .where((entry) => entry.value.kind == 'dartObjectReferencePresence')
      .map((entry) => entry.key)
      .toList();
  final messages = <String>[
    if (refs.isNotEmpty)
      'ListTile ${node.id} preview limitation for ${refs.join(', ')}: isolated Canvas never executes project Dart. '
          'Callback presence is retained with benign no-ops, including disabled semantic-button presence; '
          'unknown appearance, density, padding and cursor use the actual SDK theme/default as an explicit approximation. '
          'Focus and widget states remain SDK-owned locally, not the referenced project objects.',
    if (_cardShapePreviewUnavailableMessage(node, widgetName: 'ListTile')
        case final String shape)
      shape,
  ];
  if (context != null) {
    Widget? intermediate;
    var material = false;
    context.visitAncestorElements((ancestor) {
      final widget = ancestor.widget;
      if (widget is Material) {
        material = true;
        return false;
      }
      final color = switch (widget) {
        ColoredBox(:final color) => color,
        DecoratedBox(decoration: BoxDecoration(:final color)) => color,
        DecoratedBox(decoration: ShapeDecoration(:final color)) => color,
        _ => null,
      };
      if (intermediate == null && color != null && color.a > 0) {
        intermediate = widget;
      }
      return true;
    });
    if (!material) {
      messages.add(
        'Render ListTile ${node.id}: Material ancestor is unavailable, so ListTile and its child previews cannot be mounted in this context. '
        'No synthetic Material is inserted; child State retention applies only to geometry/slot guards under an existing Material.',
      );
    } else if (intermediate != null) {
      messages.add(
        'ListTile ${node.id}: ink/tile-background preview may be hidden by the intermediate ${intermediate.runtimeType}. '
        'The actual SDK paints on the existing Material ancestor; Canvas does not insert a different ink surface. '
        'The SDK may also report its nonfatal background warning through the existing diagnostic pipeline.',
      );
    }
  }
  if (messages.isEmpty) return null;
  return '${messages.join(' ')} Stored properties and generated Dart are unchanged.';
}

String? _switchListTileStaticMessage(CanvasNode node, BuildContext? context) {
  final inherited = _listTileStaticMessage(node, context)
      ?.replaceAll('ListTile', 'SwitchListTile')
      .replaceAll(
        'including disabled semantic-button presence',
        'without changing the controlled stored value',
      );
  final radius = _listTileNumber(node, 'splashRadius');
  final messages = [
    ?inherited,
    if (radius != null && !radius.isFinite)
      'SwitchListTile ${node.id} splashRadius preview limitation: nonfinite reaction geometry cannot be painted safely. '
          'Only the internal switch reaction is withheld using an explicit radius 0 approximation; '
          'SDK tile geometry, controls and stored properties are unchanged.',
  ];
  return messages.isEmpty ? null : messages.join(' ');
}

String? _checkboxListTileStaticMessage(CanvasNode node, BuildContext? context) {
  final apple = _checkboxUsesCupertino(node, context);
  final refs = node.properties.entries
      .where(
        (entry) =>
            entry.value.kind == 'dartObjectReferencePresence' &&
            !(entry.key == 'onChanged' &&
                node.properties['enabled']?.value == false) &&
            !(apple && {'fillColor', 'overlayColor'}.contains(entry.key)),
      )
      .map((entry) => entry.key)
      .toList();
  final tileShape = _cardShapePreviewUnavailableMessage(
    node,
    widgetName: 'CheckboxListTile',
  );
  final checkboxShape = _cardShapePreviewUnavailableMessage(
    node,
    widgetName: 'CheckboxListTile',
    prefix: 'checkboxShape',
    expectedType: 'OutlinedBorder',
  );
  final direction = _checkboxShapeDirectionMessage(
    node,
    context,
    family: 'checkboxShape',
    owner: 'CheckboxListTile.checkboxShape',
  );
  final messages = <String>[
    if (refs.isNotEmpty)
      'CheckboxListTile ${node.id} preview limitation for ${refs.join(', ')}: isolated Canvas never executes project Dart. '
          'Callback presence is retained with benign no-ops, including disabled onChanged metadata; '
          'unknown appearance, density, padding and cursor use the actual SDK theme/default as an explicit approximation. '
          'Focus and widget states remain SDK-owned locally, not the referenced project objects.',
    if (tileShape case final String shape) shape,
    if (checkboxShape case final String shape) shape,
    if (direction case final String shape) shape,
  ];
  if (context != null) {
    Widget? intermediate;
    var material = false;
    context.visitAncestorElements((ancestor) {
      final widget = ancestor.widget;
      if (widget is Material) {
        material = true;
        return false;
      }
      final color = switch (widget) {
        ColoredBox(:final color) => color,
        DecoratedBox(decoration: BoxDecoration(:final color)) => color,
        DecoratedBox(decoration: ShapeDecoration(:final color)) => color,
        _ => null,
      };
      if (intermediate == null && color != null && color.a > 0) {
        intermediate = widget;
      }
      return true;
    });
    if (!material) {
      messages.add(
        'Render CheckboxListTile ${node.id}: Material ancestor is unavailable, so CheckboxListTile and its child previews cannot be mounted in this context. '
        'No synthetic Material is inserted; child State retention applies only to geometry/slot guards under an existing Material.',
      );
    } else if (intermediate != null) {
      messages.add(
        'CheckboxListTile ${node.id}: ink/tile-background preview may be hidden by the intermediate ${intermediate.runtimeType}. '
        'The actual SDK paints on the existing Material ancestor; Canvas does not insert a different ink surface.',
      );
    }
  }
  if (messages.isEmpty) return null;
  return '${messages.join(' ')} Stored properties and generated Dart are unchanged.';
}

String? _radioTypeKey(CanvasNode node) {
  final type = node.properties['valueType']?.value;
  return type is String
      ? '$type${node.properties['nullableValueType']?.value == true ? '?' : ''}'
      : null;
}

Object? _radioIdentityValue(CanvasNode node, String name) {
  final value = switch (node.properties[name]?.value) {
    CanvasEnumValue(value: 'infinity') => double.infinity,
    CanvasEnumValue(value: 'negativeInfinity') => double.negativeInfinity,
    CanvasEnumValue(value: 'nan') => double.nan,
    final value => value,
  };
  return node.properties['valueType']?.value == 'double' && value is num
      ? value.toDouble()
      : value;
}

_CanvasRadioGroupScope? _canvasRadioScope(
  CanvasNode node,
  BuildContext context,
) {
  final type = _radioTypeKey(node);
  var scope = context
      .dependOnInheritedWidgetOfExactType<_CanvasRadioGroupScope>();
  while (scope != null) {
    if (scope.typeKey == null || scope.typeKey == type) return scope;
    scope = scope.parent;
  }
  return null;
}

bool _radioHasInheritedRegistry(CanvasNode node, BuildContext context) {
  final scope = _canvasRadioScope(node, context);
  if (scope != null) return scope.registry != null;
  final nullable = node.properties['nullableValueType']?.value == true;
  return switch (node.properties['valueType']?.value) {
    'String' =>
      nullable
          ? RadioGroup.maybeOf<String?>(context) != null
          : RadioGroup.maybeOf<String>(context) != null,
    'int' =>
      nullable
          ? RadioGroup.maybeOf<int?>(context) != null
          : RadioGroup.maybeOf<int>(context) != null,
    'double' =>
      nullable
          ? RadioGroup.maybeOf<double?>(context) != null
          : RadioGroup.maybeOf<double>(context) != null,
    'num' =>
      nullable
          ? RadioGroup.maybeOf<num?>(context) != null
          : RadioGroup.maybeOf<num>(context) != null,
    'bool' =>
      nullable
          ? RadioGroup.maybeOf<bool?>(context) != null
          : RadioGroup.maybeOf<bool>(context) != null,
    'Object' =>
      nullable
          ? RadioGroup.maybeOf<Object?>(context) != null
          : RadioGroup.maybeOf<Object>(context) != null,
    _ => false,
  };
}

Object? _radioInheritedSelection(CanvasNode node, BuildContext context) {
  final scoped = _canvasRadioScope(node, context)?.registry;
  if (scoped is _RadioGroupForwardingRegistry) return scoped.groupValue;
  Object? value<T>() => RadioGroup.maybeOf<T>(context)?.groupValue;
  return switch (_radioTypeKey(node)) {
    'String' => value<String>(),
    'String?' => value<String?>(),
    'int' => value<int>(),
    'int?' => value<int?>(),
    'double' => value<double>(),
    'double?' => value<double?>(),
    'num' => value<num>(),
    'num?' => value<num?>(),
    'bool' => value<bool>(),
    'bool?' => value<bool?>(),
    'Object' => value<Object>(),
    'Object?' => value<Object?>(),
    _ => null,
  };
}

String? _radioListTileUnavailableMessage(
  CanvasNode node,
  BuildContext? context,
) {
  final barrier = context == null ? null : _canvasRadioScope(node, context);
  final unresolved = ['valueType', 'value', 'groupValue']
      .where(
        (name) =>
            node.properties[name]?.kind == 'dartObjectReferencePresence' &&
            !(name == 'groupValue' &&
                context != null &&
                _radioInheritedSelection(node, context) != null),
      )
      .toList();
  final missingActivation =
      context != null &&
      node.properties['enabled']?.value == true &&
      !_radioHasCallback(node) &&
      !_radioHasInheritedRegistry(node, context);
  if (barrier?.unavailableReason == null &&
      unresolved.isEmpty &&
      !missingActivation) {
    return null;
  }
  return 'Render RadioListTile ${node.id}: radio selection and navigation preview unavailable. '
      '${barrier?.unavailableReason ?? ''} '
      '${unresolved.isNotEmpty ? 'Unknown ${unresolved.join(', ')} cannot be evaluated; a null modern group value can fall back to the legacy groupValue. ' : ''}'
      '${missingActivation ? 'Enabled true has no callback or matching typed group and the SDK asserts in this context. ' : ''}'
      'Guarded slot children remain in an explicitly noninteractive tile layout; no radio, selection or callback is invented. Stored properties and generated Dart are unchanged.';
}

String? _radioListTileStaticMessage(CanvasNode node, BuildContext? context) {
  final messages = <String>[
    ?_radioListTileUnavailableMessage(node, context),
    ?_listTileStaticMessage(node, context)
        ?.replaceAll('ListTile', 'RadioListTile')
        .replaceAll(
          'including disabled semantic-button presence',
          'without changing controlled group/value',
        ),
    if (['radioScaleFactor', 'splashRadius'].any((name) {
      final value = _listTileNumber(node, name);
      return value != null && !value.isFinite;
    }))
      'RadioListTile ${node.id} nonfinite scale/reaction geometry cannot be painted safely. '
          'Only that control scale/reaction uses an explicit zero approximation; tile children, source and stored values remain unchanged.',
  ];
  return messages.isEmpty ? null : messages.join(' ');
}

bool _radioHasCallback(CanvasNode node) =>
    node.properties['onChanged']?.value == 'noop' ||
    node.properties['onChanged']?.kind == 'dartObjectReferencePresence';

String? _radioUnavailableMessage(CanvasNode node, BuildContext? context) {
  final barrier = context == null ? null : _canvasRadioScope(node, context);
  if (barrier?.unavailableReason != null) {
    return 'Render Radio ${node.id}: group selection preview unavailable. '
        '${barrier!.unavailableReason} The radio is not allowed to join an outer group or invent legacy selection.';
  }
  final unresolved = ['valueType', 'value', 'groupValue', 'groupRegistry']
      .where(
        (name) =>
            node.properties[name]?.kind == 'dartObjectReferencePresence' &&
            !(name == 'groupValue' &&
                context != null &&
                _radioHasInheritedRegistry(node, context)),
      )
      .toList();
  if (unresolved.isNotEmpty) {
    return 'Render Radio ${node.id}: preview unavailable for ${unresolved.join(', ')}. '
        'Isolated Canvas cannot execute project types, values, equality or group registries and does not invent a selected state. '
        'Stored values and generated Dart are unchanged.';
  }
  if (context != null &&
      node.properties['enabled']?.value == true &&
      !_radioHasCallback(node) &&
      !_radioHasInheritedRegistry(node, context)) {
    return 'Render Radio ${node.id}: preview unavailable for enabled. '
        'Flutter 3.44.8 asserts when enabled is true without onChanged or a matching typed RadioGroup/registry in this preview context. '
        'Stored values and generated Dart are unchanged.';
  }
  return null;
}

String? _radioReferenceMessage(CanvasNode node, BuildContext? context) {
  final apple = _checkboxUsesCupertino(node, context);
  final refs = node.properties.entries
      .where((entry) {
        if (entry.value.kind != 'dartObjectReferencePresence') return false;
        if (entry.key == 'groupValue' &&
            context != null &&
            _radioHasInheritedRegistry(node, context)) {
          return false;
        }
        if (entry.key == 'onChanged' &&
            (node.properties['enabled']?.value == false ||
                (context != null &&
                    _radioHasInheritedRegistry(node, context)))) {
          return false;
        }
        if (apple &&
            {
              'fillColor',
              'hoverColor',
              'overlayColor',
              'splashRadius',
              'materialTapTargetSize',
              'visualDensity',
              'backgroundColor',
              'side',
              'innerRadius',
            }.contains(entry.key)) {
          return false;
        }
        return true;
      })
      .map((entry) => entry.key)
      .toList();
  if (refs.isEmpty) return null;
  return 'Radio ${node.id} preview limitation for ${refs.join(', ')}: isolated Canvas never executes project or dependency Dart. '
      '${refs.contains('onChanged') ? 'The project callback is replaced by a benign controlled callback; stored group/value are not changed. ' : ''}'
      '${refs.contains('focusNode') ? 'The SDK uses its isolated local focus ownership, not the project FocusNode. ' : ''}'
      '${refs.any((name) => name != 'onChanged' && name != 'focusNode') ? 'Unknown appearance/cursor/density uses the actual SDK theme/default as an explicit preview approximation. ' : ''}'
      'Stored values and generated Dart are unchanged.';
}

String? _rangeSliderGeometryMessage(
  CanvasNode node,
  BuildContext? context, [
  BoxConstraints? constraints,
]) {
  final min = _sliderNumber(node, 'min') ?? 0;
  final max = _sliderNumber(node, 'max') ?? 1;
  double normalize(String name) =>
      max > min ? (_sliderNumber(node, name)! - min) / (max - min) : 0;
  String? reason;
  if (!normalize('valuesStart').isFinite || !normalize('valuesEnd').isFinite) {
    reason =
        'valuesStart/valuesEnd/min/max: Flutter 3.44.8 range normalization produces NaN or infinity';
  } else if (node.properties['enabled']?.value != false &&
      max > min &&
      (!min.isFinite || !max.isFinite)) {
    reason =
        'min/max: the SDK inverse range interpolation cannot safely convert gesture values between nonfinite endpoints';
  }
  if (reason == null && context != null) {
    final theme = SliderTheme.of(context);
    final localPadding = node.properties['padding']?.value;
    final (horizontal, vertical) = switch (localPadding) {
      CanvasEdgeInsets() => (
        localPadding.left + localPadding.right,
        localPadding.top + localPadding.bottom,
      ),
      CanvasEdgeInsetsDirectional() => (
        localPadding.start + localPadding.end,
        localPadding.top + localPadding.bottom,
      ),
      _ => (theme.padding?.horizontal ?? 0.0, theme.padding?.vertical ?? 0.0),
    };
    if (constraints != null &&
        ((horizontal.isInfinite && !constraints.hasBoundedWidth) ||
            (vertical.isInfinite && !constraints.hasBoundedHeight))) {
      reason =
          'padding: the resolved padding sum overflows on an unbounded parent axis';
    }
    final divisions = node.properties['divisions']?.value as int?;
    if (reason == null &&
        divisions != null &&
        divisions > 10000 &&
        theme.rangeTrackShape != null &&
        !{
          RectangularRangeSliderTrackShape,
          RoundedRectRangeSliderTrackShape,
          GappedRangeSliderTrackShape,
        }.contains(theme.rangeTrackShape.runtimeType)) {
      reason =
          'divisions/SliderTheme.rangeTrackShape: an unreviewed track geometry cannot guarantee SDK tick density suppression within the isolated Canvas 10,000-division paint budget';
    }
    if (reason == null && divisions != null && divisions > 10000) {
      final year2023 =
          (node.properties['year2023']?.value as bool?) ??
          // The pinned SDK still resolves this constructor/theme flag.
          // ignore: deprecated_member_use
          theme.year2023 ??
          true;
      final modern = Theme.of(context).useMaterial3 && !year2023;
      final resolved = theme.copyWith(
        trackHeight: theme.trackHeight ?? (modern ? 16 : 4),
      );
      final ticks =
          theme.rangeTickMarkShape ??
          (modern
              ? const RoundRangeSliderTickMarkShape(tickMarkRadius: 2)
              : const RoundRangeSliderTickMarkShape());
      final interactive =
          node.properties['enabled']?.value != false && max > min;
      final tickWidth = ticks
          .getPreferredSize(isEnabled: interactive, sliderTheme: resolved)
          .width;
      final thumb =
          theme.rangeThumbShape ??
          (modern
              ? const HandleRangeSliderThumbShape()
              : const RoundRangeSliderThumbShape());
      final overlay = theme.overlayShape ?? const RoundSliderOverlayShape();
      final largestPart = [
        tickWidth,
        for (final enabled in {false, interactive}) ...[
          thumb.getPreferredSize(enabled, true).width,
          overlay.getPreferredSize(enabled, true).width,
        ],
      ].reduce(math.max);
      // BaseRangeSliderTrackShape may swap endpoints for a narrow parent.
      // Keep both its part extent and intrinsic width in this conservative
      // bound; never enter an unbounded SDK tick painting loop to measure it.
      final renderWidth = constraints?.hasBoundedWidth == true
          ? constraints!.maxWidth
          : 144 + largestPart;
      if (math.max(renderWidth, largestPart) / divisions >= 3 * tickWidth) {
        reason =
            'divisions/SliderTheme.rangeTickMarkShape: the resolved tick width and parent geometry cannot guarantee SDK density suppression within the isolated Canvas 10,000-division paint budget';
      }
    }
  }
  return reason == null
      ? null
      : 'Render RangeSlider ${node.id}: preview unavailable. $reason. Stored values and generated Dart are unchanged.';
}

String? _rangeSliderReferenceMessage(CanvasNode node) {
  final refs = node.properties.entries
      .where(
        (entry) =>
            entry.value.kind == 'dartObjectReferencePresence' &&
            !(entry.key == 'onChanged' &&
                node.properties['enabled']?.value == false),
      )
      .map((entry) => entry.key)
      .toSet();
  if (refs.isEmpty) return null;
  return 'RangeSlider ${node.id} preview limitation: isolated Canvas never executes project or dependency Dart. '
      '${refs.any((name) => {'onChanged', 'onChangeStart', 'onChangeEnd'}.contains(name)) ? 'Project value callbacks are not invoked; benign local callbacks retain controlled SDK interaction without changing stored values. ' : ''}'
      '${refs.contains('labels') ? 'Project RangeLabels are unavailable; omitted value-indicator labels are an explicit preview approximation. ' : ''}'
      '${refs.contains('overlayColor') ? 'Project overlay appearance is unavailable; the actual SDK direct color/theme/default overlay is an explicit preview approximation. ' : ''}'
      '${refs.any((name) => name.startsWith('mouseCursor')) ? 'Project cursors are unavailable; the SDK theme/default cursor is an explicit preview approximation. ' : ''}'
      '${refs.contains('semanticFormatterCallback') ? 'Project semantics formatter is not invoked; the SDK default percentages are an explicit preview approximation. ' : ''}'
      'Stored values and generated Dart are unchanged.';
}

String? _sliderGeometryMessage(
  CanvasNode node,
  BuildContext? context, [
  BoxConstraints? constraints,
]) {
  final apple = _checkboxUsesCupertino(node, context);
  final min = _sliderNumber(node, 'min') ?? 0;
  final max = _sliderNumber(node, 'max') ?? 1;
  final value = _sliderNumber(node, 'value')!;
  final secondary = _sliderNumber(node, 'secondaryTrackValue');
  double normalize(double value) => max > min ? (value - min) / (max - min) : 0;
  String? reason;
  if (apple && min == max) {
    reason =
        'min/max: Flutter 3.44.8 CupertinoSlider divides by the zero range and produces NaN, even for a disabled equal range';
  } else if (!normalize(value).isFinite) {
    reason =
        'value/min/max: Flutter 3.44.8 range normalization produces NaN or infinity';
  } else if (!apple && secondary != null && !normalize(secondary).isFinite) {
    reason =
        'secondaryTrackValue: Flutter 3.44.8 range normalization produces NaN or infinity';
  } else if (node.properties['enabled']?.value != false &&
      max > min &&
      (apple ? (!min.isFinite || !max.isFinite) : !(max - min).isFinite)) {
    reason =
        'min/max: the SDK inverse range conversion produces nonfinite gesture values or fails interpolation during interaction';
  } else if (apple && constraints != null && !constraints.hasBoundedWidth) {
    reason =
        'parent width: Slider.adaptive uses an infinitely wide CupertinoSlider and requires a bounded width';
  }
  if (reason == null && !apple && context != null) {
    final theme = SliderTheme.of(context);
    final localPadding = node.properties['padding']?.value;
    final (horizontal, vertical) = switch (localPadding) {
      CanvasEdgeInsets() => (
        localPadding.left + localPadding.right,
        localPadding.top + localPadding.bottom,
      ),
      CanvasEdgeInsetsDirectional() => (
        localPadding.start + localPadding.end,
        localPadding.top + localPadding.bottom,
      ),
      _ => (theme.padding?.horizontal ?? 0.0, theme.padding?.vertical ?? 0.0),
    };
    if (constraints != null &&
        ((horizontal.isInfinite && !constraints.hasBoundedWidth) ||
            (vertical.isInfinite && !constraints.hasBoundedHeight))) {
      reason =
          'padding: the resolved padding sum overflows on an unbounded parent axis';
    }
    final divisions = node.properties['divisions']?.value as int?;
    if (reason == null && divisions != null && divisions > 10000) {
      final year2023 =
          (node.properties['year2023']?.value as bool?) ??
          // The SDK still resolves this constructor/theme flag in 3.44.8.
          // ignore: deprecated_member_use
          theme.year2023 ??
          true;
      final resolved = theme.copyWith(
        trackHeight:
            theme.trackHeight ??
            (Theme.of(context).useMaterial3 && !year2023 ? 16 : 4),
      );
      final ticks =
          theme.tickMarkShape ??
          (Theme.of(context).useMaterial3 && !year2023
              ? const RoundSliderTickMarkShape(tickMarkRadius: 2)
              : const RoundSliderTickMarkShape());
      final interactive =
          node.properties['enabled']?.value != false && max > min;
      final tickWidth = ticks
          .getPreferredSize(isEnabled: interactive, sliderTheme: resolved)
          .width;
      final thumb =
          theme.thumbShape ??
          (Theme.of(context).useMaterial3 && !year2023
              ? const HandleThumbShape()
              : const RoundSliderThumbShape());
      final overlay = theme.overlayShape ?? const RoundSliderOverlayShape();
      final largestPart = [
        tickWidth,
        for (final enabled in {false, interactive}) ...[
          thumb.getPreferredSize(enabled, true).width,
          overlay.getPreferredSize(enabled, true).width,
        ],
      ].reduce(math.max);
      // SDK intrinsic width is 144 plus its widest part. Base track shapes
      // swap their endpoints below that width, so retain the part width in
      // this conservative upper bound, including zero-width parents. This
      // mirrors the SDK density predicate without ever entering its loop.
      final renderWidth = constraints?.hasBoundedWidth == true
          ? constraints!.maxWidth
          : 144 + largestPart;
      final trackWidthUpperBound = math.max(renderWidth, largestPart);
      if (trackWidthUpperBound / divisions >= 3.0 * tickWidth) {
        reason =
            'divisions/SliderTheme.tickMarkShape: the resolved tick width and parent geometry cannot guarantee SDK density suppression within the isolated Canvas 10,000-division paint budget';
      }
    }
  }
  return reason == null
      ? null
      : 'Render Slider ${node.id}: preview unavailable. $reason. Stored values and generated Dart are unchanged.';
}

String? _sliderReferenceMessage(CanvasNode node, BuildContext? context) {
  final apple = _checkboxUsesCupertino(node, context);
  final refs = node.properties.entries
      .where(
        (entry) =>
            entry.value.kind == 'dartObjectReferencePresence' &&
            !(entry.key == 'onChanged' &&
                node.properties['enabled']?.value == false) &&
            !(apple &&
                {
                  'overlayColor',
                  'mouseCursor',
                  'focusNode',
                  'semanticFormatterCallback',
                }.contains(entry.key)),
      )
      .map((entry) => entry.key)
      .toSet();
  if (refs.isEmpty) return null;
  return 'Slider ${node.id} preview limitation: isolated Canvas never executes project or dependency Dart. '
      '${refs.any((name) => {'onChanged', 'onChangeStart', 'onChangeEnd'}.contains(name)) ? 'Project value callbacks are not invoked; benign local callbacks retain controlled SDK interaction without changing stored value. ' : ''}'
      '${refs.contains('overlayColor') ? 'Project overlay appearance is unavailable; the actual SDK uses an explicitly approximate direct color/theme/default overlay. ' : ''}'
      '${refs.contains('semanticFormatterCallback') ? 'Project semantics formatter is not invoked; the SDK default percentage is an explicit preview approximation. ' : ''}'
      '${refs.contains('focusNode') ? 'Project focus ownership is unavailable; a persistent isolated FocusNode is used. ' : ''}'
      '${refs.contains('mouseCursor') ? 'Project cursor is unavailable; the SDK theme/default cursor is used. ' : ''}'
      'Stored values and generated Dart are unchanged.';
}

class _SliderPreview extends StatefulWidget {
  const _SliderPreview({
    required this.builder,
    required this.cupertino,
    required this.message,
  });
  final Widget Function(FocusNode) builder;
  final bool cupertino;
  final String message;
  @override
  State<_SliderPreview> createState() => _SliderPreviewState();
}

class _SliderPreviewState extends State<_SliderPreview> {
  final _focus = FocusNode(debugLabel: 'Isolated Canvas Slider');
  bool _resetConfiguration = false;
  @override
  void didUpdateWidget(_SliderPreview oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.cupertino != widget.cupertino) _resetConfiguration = true;
  }

  @override
  void dispose() {
    _focus.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => _TextButtonPreview(
    message: [
      widget.message,
      if (_resetConfiguration)
        'Slider preview lifecycle guard: Flutter 3.44.8 retains a stale Material thumb position after Cupertino value edits. '
            'Canvas recreates only the SDK animation shell on an effective Material/Cupertino boundary, retaining isolated focus ownership, selection and stored values. '
            'Cupertino continues to ignore focus as specified by the SDK.',
    ].where((value) => value.isNotEmpty).join(' '),
    child: widget.builder(_focus),
  );
}

class _SwitchPreview extends StatefulWidget {
  const _SwitchPreview({
    required this.cupertino,
    required this.message,
    required this.builder,
    this.widgetName = 'Switch',
  });
  final String widgetName;
  final bool cupertino;
  final String message;
  final Widget Function(FocusNode) builder;
  @override
  State<_SwitchPreview> createState() => _SwitchPreviewState();
}

class _SwitchPreviewState extends State<_SwitchPreview> {
  final _focus = FocusNode(debugLabel: 'Isolated Canvas Switch');
  bool _resetConfiguration = false;
  @override
  void didUpdateWidget(_SwitchPreview oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.cupertino != widget.cupertino) _resetConfiguration = true;
  }

  @override
  void dispose() {
    _focus.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => _TextButtonPreview(
    message: [
      widget.message,
      if (_resetConfiguration)
        '${widget.widgetName} preview lifecycle guard: Flutter 3.44.8 retains its Cupertino painter flag after a platform-style change. '
            'Canvas recreates only the SDK animation shell on that configuration boundary, retaining focus, selection and stored values.',
    ].where((message) => message.isNotEmpty).join(' '),
    child: widget.builder(_focus),
  );
}

SwitchThemeData _switchEffectiveTheme(CanvasNode node, BuildContext context) {
  final theme = Theme.of(context);
  final component = SwitchTheme.of(context);
  if (node.properties['variant']?.value != 'adaptive') return component;
  final adaptation = theme.getAdaptation<SwitchThemeData>();
  return adaptation != null
      ? adaptation.adapt(theme, component)
      : _checkboxUsesCupertino(node, context)
      ? const SwitchThemeData()
      : component;
}

double? _switchLocalWidth(CanvasNode node, Set<WidgetState> states) {
  for (final entry in {
    ..._checkboxStateLayers,
    WidgetState.any: 'Default',
  }.entries) {
    final value = node.properties['trackOutlineWidth${entry.value}'];
    if (value != null &&
        (entry.key == WidgetState.any || states.contains(entry.key))) {
      return value.kind == 'enum'
          ? double.infinity
          : (value.value as num?)?.toDouble();
    }
  }
  return null;
}

bool _switchWidthPairUnsafe(
  CanvasNode node,
  BuildContext context,
  Set<WidgetState> states,
) {
  final theme = _switchEffectiveTheme(node, context);
  double? resolve(Set<WidgetState> states) =>
      _switchLocalWidth(node, states) ??
      theme.trackOutlineWidth?.resolve(states) ??
      (Theme.of(context).useMaterial3 && !_checkboxUsesCupertino(node, context)
          ? 2.0
          : null);
  final active = resolve({...states, WidgetState.selected});
  final inactive = resolve({...states}..remove(WidgetState.selected));
  // ui.lerpDouble permits equal infinities, but rejects unequal nonfinite
  // endpoints even at a settled zero/one animation position.
  return active != inactive &&
      ((active?.isInfinite ?? false) || (inactive?.isInfinite ?? false));
}

String? _switchWidthMessage(CanvasNode node, BuildContext? context) {
  if (context == null) return null;
  const transient = [
    WidgetState.focused,
    WidgetState.hovered,
    WidgetState.pressed,
  ];
  for (var mask = 0; mask < 8; mask++) {
    final states = <WidgetState>{
      if (node.properties['enabled']?.value == false) WidgetState.disabled,
      for (var i = 0; i < 3; i++)
        if (mask & (1 << i) != 0) transient[i],
    };
    if (_switchWidthPairUnsafe(node, context, states)) {
      return 'Render Switch ${node.id}: trackOutlineWidth preview limitation: '
          'Flutter 3.44.8 cannot interpolate unequal finite/infinite active and inactive outline widths. '
          'Only affected state pairs use the explicit SDK painter-default width 2 approximation. ';
    }
  }
  return null;
}

String? _switchPaddingMessage(
  CanvasNode node,
  BuildContext context,
  BoxConstraints constraints,
) {
  final local = node.properties['padding']?.value;
  final theme = _switchEffectiveTheme(node, context).padding;
  final (horizontal, vertical) = switch (local) {
    CanvasEdgeInsets() => (local.left + local.right, local.top + local.bottom),
    CanvasEdgeInsetsDirectional() => (
      local.start + local.end,
      local.top + local.bottom,
    ),
    _ => (theme?.horizontal ?? 0.0, theme?.vertical ?? 0.0),
  };
  final axes = [
    if (horizontal.isInfinite && !constraints.hasBoundedWidth) 'width',
    if (vertical.isInfinite && !constraints.hasBoundedHeight) 'height',
  ];
  return axes.isEmpty
      ? null
      : 'Render Switch ${node.id}: padding preview unavailable because '
            'the resolved padding sum overflows to infinity on an unbounded ${axes.join('/')} axis. '
            'Flutter cannot lay out this switch in its current parent. Stored values and generated Dart are unchanged.';
}

String? _switchPreviewMessage(CanvasNode node, [BuildContext? context]) {
  final refs = node.properties.entries
      .where(
        (entry) =>
            entry.value.kind == 'dartObjectReferencePresence' &&
            !(entry.key == 'onChanged' &&
                node.properties['enabled']?.value == false),
      )
      .map((entry) => entry.key)
      .toSet();
  final width = _switchWidthMessage(node, context);
  if (refs.isEmpty && width == null) return null;
  return 'Switch ${node.id} preview limitation: isolated Canvas never executes project or dependency Dart. '
      '${refs.intersection({'thumbColor', 'trackColor', 'trackOutlineColor', 'trackOutlineWidth', 'overlayColor', 'thumbIcon'}).isNotEmpty ? 'Configured project state appearance is unavailable; the real SDK switch uses an explicitly approximate theme/default preview. ' : ''}'
      '${refs.contains('onChanged') ? 'Project onChanged is not invoked; a benign local callback preserves enabled behavior without changing the controlled stored value. ' : ''}'
      '${refs.contains('onFocusChange') ? 'Project onFocusChange is not invoked; local SDK focus behavior is retained. ' : ''}'
      '${refs.any((name) => name.endsWith('ThumbImageError')) ? 'Project image error callbacks are not invoked; Canvas reports resource errors locally. ' : ''}'
      '${refs.contains('focusNode') ? 'Project focus ownership is unavailable; isolated SDK focus state is used. ' : ''}'
      '${refs.contains('mouseCursor') ? 'Project cursor is unavailable; the SDK default cursor is used. ' : ''}'
      '${width ?? ''}'
      'Stored values and generated Dart are unchanged.';
}

bool _checkboxUsesCupertino(CanvasNode node, BuildContext? context) =>
    node.properties['variant']?.value == 'adaptive' &&
    context != null &&
    {
      TargetPlatform.iOS,
      TargetPlatform.macOS,
    }.contains(Theme.of(context).platform);

String? _checkboxPreviewMessage(CanvasNode node, BuildContext? context) {
  final apple = _checkboxUsesCupertino(node, context);
  final refs = [
    for (final entry in node.properties.entries)
      if (entry.value.kind == 'dartObjectReferencePresence' &&
          !(entry.key == 'onChanged' &&
              node.properties['enabled']?.value == false) &&
          !(apple && {'fillColor', 'overlayColor'}.contains(entry.key)))
        entry.key,
  ];
  final shape = _cardShapePreviewUnavailableMessage(
    node,
    widgetName: 'Checkbox',
  )?.replaceAll('ShapeBorder', 'OutlinedBorder');
  final direction = _checkboxShapeDirectionMessage(node, context);
  if (refs.isEmpty && shape == null && direction == null) return null;
  return 'Checkbox ${node.id} preview limitation: isolated Canvas never executes project or dependency Dart. '
      '${refs.any((name) => {'fillColor', 'overlayColor', 'side', 'shape'}.contains(name)) || shape != null ? 'Configured project appearance is unavailable; the real SDK checkbox uses an explicitly approximate theme/default preview. ' : ''}'
      '${refs.contains('onChanged') ? 'Project onChanged is not invoked; a benign local callback preserves enabled behavior without changing the controlled stored value. ' : ''}'
      '${refs.contains('focusNode') ? 'Project focus ownership is unavailable; isolated SDK focus state is used. ' : ''}'
      '${refs.contains('mouseCursor') ? 'Project cursor is unavailable; the SDK default cursor is used. ' : ''}'
      '${shape ?? ''}${direction ?? ''} Stored values and generated Dart are unchanged.';
}

String? _checkboxShapeDirectionMessage(
  CanvasNode node,
  BuildContext? context, {
  String family = 'shape',
  String owner = 'Checkbox.shape',
}) {
  String? unsafe;
  final localKind = node.properties['${family}Kind']?.value;
  final availableLocal =
      localKind != null &&
      _cardShapePreviewUnavailableMessage(
            node,
            widgetName: owner,
            prefix: family,
            expectedType: 'OutlinedBorder',
          ) ==
          null;
  if (availableLocal) {
    if (localKind == 'linear') unsafe = 'LinearBorder';
    if (node.properties['${family}Radius']?.value
        is CanvasDirectionalBorderRadiusValue) {
      unsafe = '$localKind with directional corner radii';
    }
  } else if (context != null && !_checkboxUsesCupertino(node, context)) {
    final shape = CheckboxTheme.of(context).shape;
    final radius = switch (shape) {
      RoundedRectangleBorder() => shape.borderRadius,
      BeveledRectangleBorder() => shape.borderRadius,
      ContinuousRectangleBorder() => shape.borderRadius,
      RoundedSuperellipseBorder() => shape.borderRadius,
      _ => null,
    };
    if (shape is LinearBorder || (radius != null && radius is! BorderRadius)) {
      unsafe = 'CheckboxTheme.${shape.runtimeType} with directional geometry';
    }
  }
  return unsafe == null
      ? null
      : '$owner preview limitation: $unsafe requires TextDirection, '
            'but the Flutter 3.44.8 Material and Cupertino checkbox painters do not pass it. '
            'The real checkbox uses an explicit SDK default-shape approximation; the configured shape is not rendered. ';
}

final Object _defaultFabHeroTag = const FloatingActionButton(
  onPressed: null,
).heroTag!;

Object? _fabHeroTag(CanvasNode node) {
  final value = node.properties['heroTag'];
  if (value == null) return _defaultFabHeroTag;
  return value.kind == 'dartObjectReferencePresence' || value.kind == 'null'
      ? null
      : value.value;
}

List<double> _fabElevations(CanvasNode node, BuildContext? context) {
  final theme = context == null ? null : FloatingActionButtonTheme.of(context);
  double? local(String name) {
    final value = node.properties[name];
    return value?.kind == 'enum'
        ? double.infinity
        : (value?.value as num?)?.toDouble();
  }

  final base = local('elevation') ?? theme?.elevation ?? 6;
  return [
    base,
    local('focusElevation') ?? theme?.focusElevation ?? 6,
    local('hoverElevation') ?? theme?.hoverElevation ?? 8,
    local('highlightElevation') ??
        theme?.highlightElevation ??
        (context == null || Theme.of(context).useMaterial3 ? 6 : 12),
    local('disabledElevation') ?? theme?.disabledElevation ?? base,
  ];
}

String? _fabLayoutMessage(CanvasNode node, BuildContext? context) {
  final elevations = _fabElevations(node, context);
  if (elevations.any((value) => value.isInfinite) &&
      !elevations.every((value) => value.isInfinite)) {
    return 'FloatingActionButton.elevation/focusElevation/hoverElevation/highlightElevation/disabledElevation preview unavailable: resolved interaction states mix finite and infinite elevations. Flutter Material interpolates infinity back to finite as NaN and fails its physical-shape assertion. Stored properties and generated Dart remain exact; use finite elevations for interactive preview.';
  }
  if (node.properties['variant']?.value != 'extended') return null;
  final theme = context == null ? null : FloatingActionButtonTheme.of(context);
  final active =
      node.properties['isExtended']?.value != false &&
      node.slot('icon')?.child != null;
  final prop = node.properties['extendedIconLabelSpacing'];
  final spacing = prop?.kind == 'enum'
      ? double.infinity
      : (prop?.value as num?)?.toDouble() ??
            theme?.extendedIconLabelSpacing ??
            8;
  if (active && (spacing < 0 || !spacing.isFinite)) {
    return 'FloatingActionButton.extendedIconLabelSpacing preview unavailable: resolved spacing $spacing produces invalid SDK SizedBox constraints while both icon and label are visible. Stored values and generated Dart are unchanged.';
  }
  final value = node.properties['extendedPadding']?.value;
  final padding = switch (value) {
    CanvasEdgeInsets p => EdgeInsets.fromLTRB(p.left, p.top, p.right, p.bottom),
    CanvasEdgeInsetsDirectional p => EdgeInsets.fromLTRB(
      p.start,
      p.top,
      p.end,
      p.bottom,
    ),
    _ => theme?.extendedPadding?.resolve(TextDirection.ltr),
  };
  if (padding != null &&
      (!padding.isNonNegative ||
          !padding.vertical.isFinite ||
          !(padding.horizontal + (active ? spacing : 0)).isFinite)) {
    return 'FloatingActionButton.extendedPadding preview unavailable: resolved padding and mounted spacing produce invalid or overflowing SDK layout dimensions. Stored values and generated Dart are unchanged.';
  }
  return null;
}

String? _fabPreviewMessage(CanvasNode node, BuildContext? context) {
  final refs = [
    for (final e in node.properties.entries)
      if (e.value.kind == 'dartObjectReferencePresence') e.key,
  ];
  final parts = <String>[?_fabLayoutMessage(node, context)];
  if (refs.isNotEmpty) {
    parts.add(
      'FloatingActionButton.${refs.join('/')} preview limitation: isolated Canvas never executes project Dart. '
      '${refs.contains('heroTag') ? 'Only the unresolved project Hero is disabled; its Object equality and flights are unavailable. ' : ''}'
      '${refs.contains('shape') ? 'Project shape appearance is unavailable; this is an explicit SDK default/theme approximation. ' : ''}'
      '${refs.contains('onPressed') ? 'Project callback is not invoked; local press uses a benign no-op. ' : ''}'
      '${refs.contains('focusNode') || refs.contains('mouseCursor') ? 'Project focus/cursor is unavailable; SDK isolated local state/default cursor is used. ' : ''}'
      'The real button and stored values are preserved.',
    );
  }
  final shape = _cardShapePreviewUnavailableMessage(
    node,
    widgetName: 'FloatingActionButton',
  );
  if (shape != null && !refs.contains('shape')) {
    parts.add('$shape SDK default/theme shape approximation is shown.');
  }
  if (context != null) {
    final duplicates = context
        .findAncestorStateOfType<_CanvasDocumentViewState>()
        ?._fabDuplicateHeroMessage(node);
    if (duplicates != null) parts.add(duplicates);
  }
  return parts.isEmpty ? null : parts.join(' ');
}

String? _textButtonReferenceMessage(CanvasNode node) {
  final refs = [
    for (final entry in node.properties.entries)
      if (entry.value.kind == 'dartObjectReferencePresence') entry.key,
  ];
  if (refs.isEmpty) return null;
  return '${node.type.split('.').last}.${refs.join('/')} preview limitation: isolated Canvas does not execute project or dependency Dart. '
      '${refs.contains('style') ? 'The configured ButtonStyle appearance is unavailable; this is an explicitly labeled SDK default/theme preview. ' : ''}'
      '${refs.contains('shortcut') ? 'The project shortcut and its hint are unavailable; preview supplies no shortcut and never registers global shortcuts. ' : ''}'
      '${refs.any((name) => name.endsWith('Builder'))
          ? node.type == 'flutter.material.MenuItemButton'
                ? 'Project layer content is unavailable; identity layers preserve the real child. MenuItemButton retains native Clip.none unless explicitly overridden. '
                : 'Project layer content is unavailable; identity layers preserve the real child and SDK builder-dependent clipping. '
          : ''}'
      '${refs.contains('focusNode') || refs.contains('statesController') ? 'Project focus/controller state is unavailable; the SDK button uses isolated local state. ' : ''}'
      '${refs.any((name) => name.startsWith('on')) ? 'Project callbacks are not invoked; local button interactions use benign no-ops. ' : ''}'
      'The real SDK button, child, stored values and generated Dart are preserved.';
}

String? _iconButtonPreviewMessage(CanvasNode node, BuildContext? context) {
  final material3 = context == null || Theme.of(context).useMaterial3;
  final refs = [
    for (final entry in node.properties.entries)
      if (entry.value.kind == 'dartObjectReferencePresence' &&
          (material3 ||
              (!entry.key.startsWith('style') &&
                  entry.key != 'statesController')))
        entry.key,
  ];
  if (refs.isEmpty) return null;
  return 'IconButton.${refs.join('/')} preview limitation: isolated Canvas never executes project or dependency Dart. '
      '${refs.contains('style') ? 'Configured ButtonStyle appearance is unavailable; this is an explicit SDK default/theme preview. ' : ''}'
      '${refs.any((name) => name.endsWith('Builder')) ? 'Project layer content is unavailable; identity layers preserve the SDK child. ' : ''}'
      '${refs.contains('focusNode') || refs.contains('statesController') ? 'Project focus/controller state is unavailable; isolated SDK local state is used. ' : ''}'
      '${refs.contains('mouseCursor') ? 'Project cursor is unavailable; the SDK default cursor is used. ' : ''}'
      '${refs.any((name) => name.startsWith('on')) ? 'Project callbacks are not invoked; local interactions use benign no-ops and preserve SDK enabled/long-press rules. ' : ''}'
      'The real SDK button, stored values and generated Dart are preserved.';
}

String? _iconButtonM2GeometryMessage(
  CanvasNode node,
  BuildContext context, [
  BoxConstraints? constraints,
]) {
  if (Theme.of(context).useMaterial3) return null;
  final value = node.properties['iconSize']?.value;
  final size = value is CanvasEnumValue
      ? double.infinity
      : value is num
      ? value.toDouble()
      : IconTheme.of(context).size ?? 24;
  if (size < 0) {
    return 'Render IconButton ${node.id}: iconSize preview unavailable because the resolved Material 2 SizedBox dimension is $size; it must be nonnegative. Stored values and generated Dart are unchanged.';
  }
  final box = node.properties['constraints']?.value;
  final maxWidth = box is CanvasBoxConstraintsValue
      ? box.maxWidth ?? double.infinity
      : double.infinity;
  final maxHeight = box is CanvasBoxConstraintsValue
      ? box.maxHeight ?? double.infinity
      : double.infinity;
  if (constraints != null &&
      size.isInfinite &&
      ((!constraints.hasBoundedWidth && maxWidth.isInfinite) ||
          (!constraints.hasBoundedHeight && maxHeight.isInfinite))) {
    return 'Render IconButton ${node.id}: iconSize preview unavailable because infinity has an unbounded Material 2 layout axis. Stored values and generated Dart are unchanged.';
  }
  final radius = node.properties['splashRadius']?.value;
  final infiniteInk =
      radius is CanvasEnumValue || (radius == null && size.isInfinite);
  if (node.properties['enabled']?.value != false && infiniteInk) {
    return 'Render IconButton ${node.id}: ${radius == null ? 'iconSize-derived splashRadius' : 'splashRadius'} preview unavailable because enabled Material 2 ink animation converts an infinite radius to an integer. Stored values and generated Dart are unchanged.';
  }
  return null;
}

String? _iconButtonMountedIconMessage(CanvasNode node, BuildContext context) {
  if (context.findAncestorWidgetOfExactType<IconButton>() == null) return null;
  final size =
      (node.properties['size']?.value as num?)?.toDouble() ??
      IconTheme.of(context).size ??
      24;
  if (size.isFinite && size >= 0) return null;
  return 'Render Icon ${node.id} inside IconButton: iconSize preview unavailable because the actual mounted IconTheme resolves size $size; an Icon requires a finite nonnegative dimension. The SDK button remains active, and stored values and generated Dart are unchanged.';
}

const _expansionCurves = <String, Curve>{
  'linear': Curves.linear,
  'decelerate': Curves.decelerate,
  'fastLinearToSlowEaseIn': Curves.fastLinearToSlowEaseIn,
  'fastEaseInToSlowEaseOut': Curves.fastEaseInToSlowEaseOut,
  'ease': Curves.ease,
  'easeIn': Curves.easeIn,
  'easeInToLinear': Curves.easeInToLinear,
  'easeInSine': Curves.easeInSine,
  'easeInQuad': Curves.easeInQuad,
  'easeInCubic': Curves.easeInCubic,
  'easeInQuart': Curves.easeInQuart,
  'easeInQuint': Curves.easeInQuint,
  'easeInExpo': Curves.easeInExpo,
  'easeInCirc': Curves.easeInCirc,
  'easeInBack': Curves.easeInBack,
  'easeOut': Curves.easeOut,
  'linearToEaseOut': Curves.linearToEaseOut,
  'easeOutSine': Curves.easeOutSine,
  'easeOutQuad': Curves.easeOutQuad,
  'easeOutCubic': Curves.easeOutCubic,
  'easeOutQuart': Curves.easeOutQuart,
  'easeOutQuint': Curves.easeOutQuint,
  'easeOutExpo': Curves.easeOutExpo,
  'easeOutCirc': Curves.easeOutCirc,
  'easeOutBack': Curves.easeOutBack,
  'easeInOut': Curves.easeInOut,
  'easeInOutSine': Curves.easeInOutSine,
  'easeInOutQuad': Curves.easeInOutQuad,
  'easeInOutCubic': Curves.easeInOutCubic,
  'easeInOutCubicEmphasized': Curves.easeInOutCubicEmphasized,
  'easeInOutQuart': Curves.easeInOutQuart,
  'easeInOutQuint': Curves.easeInOutQuint,
  'easeInOutExpo': Curves.easeInOutExpo,
  'easeInOutCirc': Curves.easeInOutCirc,
  'easeInOutBack': Curves.easeInOutBack,
  'fastOutSlowIn': Curves.fastOutSlowIn,
  'slowMiddle': Curves.slowMiddle,
  'bounceIn': Curves.bounceIn,
  'bounceOut': Curves.bounceOut,
  'bounceInOut': Curves.bounceInOut,
  'elasticIn': Curves.elasticIn,
  'elasticOut': Curves.elasticOut,
  'elasticInOut': Curves.elasticInOut,
};

// Only source-reviewed SDK/preview branch discriminants belong here. Values
// which merely update an existing RenderObject must not reset descendant
// Tooltip State. Resolved preview keys are appended at the document boundary;
// context-dependent diagnostic wrappers stay stable in Tooltip-containing trees.
bool _ownsNativeTooltip(CanvasNode node) =>
    node.type == 'flutter.material.Tooltip' ||
    ((node.type == 'flutter.material.IconButton' ||
            node.type == 'flutter.material.FloatingActionButton') &&
        node.properties['tooltip']?.value is String &&
        (node.properties['tooltip']!.value as String).isNotEmpty);

bool _containsNativeTooltip(CanvasNode node) =>
    _ownsNativeTooltip(node) ||
    node.slots.values.any((slot) => slot.children.any(_containsNativeTooltip));
bool _menuAnchorFollowerAvailable(CanvasNode node) =>
    node.properties['layerLink']?.kind == 'dartObjectReferencePresence' &&
    !(node.slot('menuChildren')?.children.any(_containsNativeTooltip) ?? false);
bool _ownsNativeMenu(CanvasNode node) =>
    node.type == 'flutter.material.MenuAnchor' ||
    node.type == 'flutter.material.MenuBar' ||
    node.type == 'flutter.material.SubmenuButton';

String _tooltipAncestorSdkTopology(CanvasNode node) {
  bool present(String name) =>
      node.properties[name] != null && node.properties[name]!.kind != 'null';
  bool reference(String name) =>
      node.properties[name]?.kind == 'dartObjectReferencePresence';
  bool flag(String name, [bool fallback = false]) =>
      node.properties[name]?.value is bool
      ? node.properties[name]!.value as bool
      : fallback;
  Object? scalar(String name) => switch (node.properties[name]?.value) {
    final CanvasEnumValue value => value.value,
    final String value => value,
    final bool value => value,
    final num value => value.toString(),
    _ => null,
  };
  bool slot(String name) => node.slot(name)?.children.isNotEmpty ?? false;
  String text(String name, String fallback) =>
      scalar(name) is String ? scalar(name)! as String : fallback;
  List<Object?> scrollBranches({required bool sliver}) => [
    // Omitted primary/padding/keyboard policy can inherit context. Retaining
    // that distinct state is necessary; resolving it belongs to the preview.
    scalar('primary'),
    text('scrollDirection', 'vertical'),
    scalar('keyboardDismissBehavior'),
    present('padding'),
    if (sliver) ...[
      flag('shrinkWrap'),
      flag('addAutomaticKeepAlives', true),
      flag('addRepaintBoundaries', true),
      flag('addSemanticIndexes', true),
    ],
  ];
  final List<Object?> branches = switch (node.type) {
    'flutter.material.MenuAnchor' => [
      flag('animated'),
      _menuAnchorFollowerAvailable(node),
      flag('crossAxisUnconstrained', true),
    ],
    'flutter.material.MenuBar' => [slot('children')],
    'flutter.material.SubmenuButton' => [
      flag('animated'),
      slot('menuChildren'),
      slot('leadingIcon'),
      slot('trailingIcon'),
    ],
    // Focus.withExternalFocusNode has a different SDK widget runtime type.
    'flutter.widgets.Focus' => [
      text('variant', 'standard'),
      flag('includeSemantics', true),
    ],
    'flutter.widgets.GestureDetector' => [flag('excludeFromSemantics')],
    'flutter.widgets.NotificationListener' => [
      text('notificationType', 'Notification'),
    ],
    'flutter.widgets.DefaultSelectionStyle' ||
    'flutter.widgets.IconTheme' => [flag('merge')],
    'flutter.widgets.Visibility' => [
      flag('maintainSize')
          ? 'size'
          : flag('maintainState')
          ? (flag('maintainAnimation') ? 'offstage' : 'offstage+ticker')
          : (flag('visible', true) ? 'child' : 'replacement'),
    ],
    'flutter.widgets.SingleChildScrollView' => scrollBranches(sliver: false),
    'flutter.widgets.ListView' => [
      ...scrollBranches(sliver: true),
      present('itemExtent'),
    ],
    'flutter.widgets.GridView' ||
    'flutter.widgets.GridView.extent' => scrollBranches(sliver: true),
    'flutter.widgets.CustomScrollView' => scrollBranches(sliver: true),
    'flutter.widgets.PageView' => scrollBranches(sliver: true),
    'flutter.widgets.ListWheelScrollView' => [
      reference('controller'),
      reference('scrollBehavior'),
      present('itemExtent'),
      flag('renderChildrenOutsideViewport'),
      text('changeReportingBehavior', 'onScrollUpdate'),
    ],
    'flutter.widgets.ClipRect' ||
    'flutter.widgets.ClipOval' ||
    'flutter.widgets.ClipRRect' ||
    'flutter.widgets.ClipRSuperellipse' ||
    'flutter.widgets.PhysicalShape' => [reference('clipper')],
    'flutter.widgets.ClipPath' => [
      reference('shape')
          ? 'shape-unavailable'
          : reference('clipper')
          ? 'clipper-unavailable'
          : 'native',
    ],
    'flutter.material.Card' || canvasSnackBarType || canvasBottomSheetType || canvasDialogType || canvasAlertDialogType || canvasAdaptiveAlertDialogType || canvasSimpleDialogType => [
      _cardShapePreviewUnavailableMessage(node) != null,
    ],
    'flutter.widgets.RadioGroup' => [
      _radioTypeKey(node),
      reference('groupValue'),
    ],
    'flutter.material.RadioListTile' => [
      _radioTypeKey(node),
      text('controlAffinity', 'platform'),
      reference('value'),
      reference('groupValue'),
    ],
    'flutter.material.CheckboxListTile' ||
    'flutter.material.SwitchListTile' => [text('controlAffinity', 'platform')],
    'flutter.material.Scaffold' => [
      flag('extendBody') || flag('extendBodyBehindAppBar'),
    ],
    'flutter.material.AppBar' => [
      flag('primary', true),
      flag('excludeHeaderSemantics'),
      flag('forceMaterialTransparency'),
      present('shapeKind'),
      slot('bottom'),
      slot('flexibleSpace'),
      slot('bottom') &&
          scalar('bottomOpacity') != null &&
          scalar('bottomOpacity') != '1.0' &&
          scalar('bottomOpacity') != '1',
    ],
    'flutter.material.Badge' => [
      flag('isLabelVisible', true),
      present('count') || slot('label'),
      slot('child'),
      // Canvas deliberately recreates the SDK stadium for its changed minimum.
      // Theme-derived minimum changes need the context-dependent companion.
      if (flag('isLabelVisible', true) && (present('count') || slot('label')))
        scalar('largeSize'),
    ],
    'flutter.material.FloatingActionButton' => [
      text('variant', 'standard') == 'extended',
      flag('isExtended', text('variant', 'standard') == 'extended'),
      present('tooltip'),
      present('tooltip') && text('tooltip', '').isNotEmpty,
      // Omission selects the default non-null Hero tag; explicit null removes it.
      node.properties['heroTag']?.kind != 'null',
    ],
    'flutter.material.TextButton' ||
    'flutter.material.OutlinedButton' ||
    'flutter.material.FilledButton' => [
      {'icon', 'tonalIcon'}.contains(text('variant', 'standard')),
      slot('icon'),
      text('iconAlignment', 'start'),
    ],
    'flutter.material.IconButton' => [
      present('tooltip'),
      present('tooltip') && text('tooltip', '').isNotEmpty,
    ],
    'flutter.material.MenuItemButton' => [
      // Native MenuItemButton conditionally inserts MouseRegion and the
      // platform accelerator binding. Its label also has distinct semantic
      // and horizontal/vertical ancestry. Ordinary styles/shortcut labels do
      // not change these paths and therefore retain descendant preview state.
      reference('onHover') || flag('requestFocusOnHover', true),
      flag('enabled', true),
      present('semanticsLabel'),
      text('overflowAxis', 'horizontal'),
    ],
    _ => const [],
  };
  return branches.isEmpty ? '' : '@sdk:${jsonEncode(branches)}';
}

// Preserve the native AlignTransition and child state if finite coordinates
// overflow the resulting paint offset. Do not clamp stored alignment values.
class _AlignTransitionPreview extends StatefulWidget {
  const _AlignTransitionPreview({required this.node, required this.message, required this.child});
  final CanvasNode node;
  final String message;
  final Widget child;
  @override State<_AlignTransitionPreview> createState() => _AlignTransitionPreviewState();
}
class _AlignTransitionPreviewState extends State<_AlignTransitionPreview> {
  bool _blocked = false, _pending = false, _scheduled = false;
  void _changed(bool blocked) {
    _pending = blocked;
    if (_scheduled || _blocked == blocked) return;
    _scheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scheduled = false;
      if (!mounted || _blocked == _pending) return;
      setState(() => _blocked = _pending);
    });
  }
  @override Widget build(BuildContext context) => _TextButtonPreview(
    stableDiagnostic: true,
    message: [
      if (widget.message.isNotEmpty) widget.message,
      if (_blocked) 'AlignTransition ${widget.node.id} geometry preview unavailable: native alignment offset is non-finite. '
        'Unsafe paint, semantics and pointer input are withheld. Reduce alignment coordinates. '
        'Child state, stored values and generated Dart are unchanged.',
    ].join('\n'),
    child: _AlignTransitionGuard(changed: _changed, child: widget.child),
  );
}
class _AlignTransitionGuard extends SingleChildRenderObjectWidget {
  const _AlignTransitionGuard({required this.changed, required super.child});
  final ValueChanged<bool> changed;
  @override _AlignTransitionRenderBox createRenderObject(BuildContext context) => _AlignTransitionRenderBox(changed);
  @override void updateRenderObject(BuildContext context, _AlignTransitionRenderBox render) {
    render.changed = changed;
    render.markNeedsPaint();
    render.markNeedsSemanticsUpdate();
  }
}
class _AlignTransitionRenderBox extends RenderProxyBox {
  _AlignTransitionRenderBox(this.changed);
  ValueChanged<bool> changed;
  bool _blocked = false;
  bool get _finite {
    final aligned = child;
    if (aligned is! RenderPositionedBox || aligned.child == null) return true;
    final matrix = Matrix4.identity();
    aligned.applyPaintTransform(aligned.child!, matrix);
    return matrix.storage.every((value) => value.isFinite);
  }
  @override void paint(PaintingContext context, Offset offset) {
    final blocked = !_finite;
    if (_blocked != blocked) markNeedsSemanticsUpdate();
    _blocked = blocked;
    changed(blocked);
    if (!blocked) super.paint(context, offset);
  }
  @override bool hitTest(BoxHitTestResult result, {required Offset position}) =>
    _finite && super.hitTest(result, position: position);
  @override void visitChildrenForSemantics(RenderObjectVisitor visitor) {
    if (_finite) super.visitChildrenForSemantics(visitor);
  }
}

class _AnimatedMatrixPreview extends StatefulWidget {
  const _AnimatedMatrixPreview({required this.node, required this.message, required this.child});
  final CanvasNode node;
  final String message;
  final Widget child;
  @override State<_AnimatedMatrixPreview> createState() => _AnimatedMatrixPreviewState();
}
class _AnimatedMatrixPreviewState extends State<_AnimatedMatrixPreview> {
  bool _blocked = false, _pending = false, _scheduled = false;
  void _changed(bool blocked) {
    _pending = blocked;
    if (_scheduled || _blocked == blocked) return;
    _scheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scheduled = false;
      if (!mounted || _blocked == _pending) return;
      setState(() => _blocked = _pending);
    });
  }
  @override Widget build(BuildContext context) => _TextButtonPreview(
    stableDiagnostic: true,
    message: [
      if (widget.message.isNotEmpty) widget.message,
      if (_blocked) '${widget.node.type.split('.').last} ${widget.node.id} geometry preview unavailable: the native ${const {'flutter.widgets.AnimatedRotation', 'flutter.widgets.RotationTransition'}.contains(widget.node.type) ? 'rotation' : 'transform'} matrix is non-finite. '
        'Unsafe paint, semantics and pointer input are withheld for this frame. '
        '${const {'flutter.widgets.AnimatedRotation', 'flutter.widgets.RotationTransition'}.contains(widget.node.type) ? 'Reduce Turns or alignment coordinates.' : 'Reduce transform/pivot coordinates or use a nonsingular Matrix4.'} '
        'The SDK tween, Child state, stored values and generated Dart are unchanged.',
    ].join('\n'),
    child: _AnimatedMatrixGuard(changed: _changed, child: widget.child),
  );
}
// A positioned-only native Stack has zero intrinsic extents. The diagnostic
// footprint must not replace them with 48 when queried by IntrinsicWidth/Height.
class _PositionedStackFallback extends LeafRenderObjectWidget {
  const _PositionedStackFallback();
  @override
  RenderObject createRenderObject(BuildContext context) => _PositionedStackFallbackRenderBox();
}

class _PositionedStackFallbackRenderBox extends RenderBox {
  @override
  Size computeDryLayout(BoxConstraints constraints) => constraints.constrain(const Size(48, 48));
  @override
  void performLayout() { size = computeDryLayout(constraints); }
}

// Position can animate without relaying out an unchanged zero-size child.
// Paint notifications also cover those offset-only frames; the surface refresh
// coalesces requests and only rebuilds when the measured target actually moved.
class _PositionedGeometryObserver extends SingleChildRenderObjectWidget {
  const _PositionedGeometryObserver({required super.child});
  VoidCallback _refresh(BuildContext context) => () =>
      context.findAncestorStateOfType<_CanvasDocumentViewState>()
          ?._refreshZeroSizedWidgetTargetsAfterFrame();
  @override
  RenderObject createRenderObject(BuildContext context) =>
      _PositionedGeometryRenderBox(_refresh(context));
  @override
  void updateRenderObject(BuildContext context, covariant _PositionedGeometryRenderBox renderObject) {
    renderObject.refresh = _refresh(context);
  }
}

class _PositionedGeometryRenderBox extends RenderProxyBox {
  _PositionedGeometryRenderBox(this.refresh);
  VoidCallback refresh;
  @override
  void performLayout() {
    super.performLayout();
    refresh();
  }
  @override
  void paint(PaintingContext context, Offset offset) {
    super.paint(context, offset);
    refresh();
  }
}

// Keep child identity, but reset only the native animation shell when Flutter
// cannot interpolate two otherwise valid TextStyles (inherit/paint mismatch).
// Uses the real SDK animator. Preview-only guards do not rewrite the model.
class _AnimatedFractionalPreview extends StatefulWidget {
  const _AnimatedFractionalPreview({
    required this.target,
    required this.message,
  });
  final AnimatedFractionallySizedBox target;
  final String message;
  @override
  State<_AnimatedFractionalPreview> createState() =>
      _AnimatedFractionalPreviewState();
}

class _AnimatedFractionalPreviewState
    extends State<_AnimatedFractionalPreview> {
  GlobalKey _nativeKey = GlobalKey();
  final GlobalKey _childKey = GlobalKey();
  AnimatedFractionallySizedBox? _last;
  double? _beginWidth, _endWidth, _beginHeight, _endHeight;
  bool _blockedWidth = false, _blockedHeight = false;
  String? _transitionMessage;

  RenderFractionallySizedOverflowBox? _displayed() {
    final renderObject = _nativeKey.currentContext?.findRenderObject();
    return renderObject is RenderFractionallySizedOverflowBox
        ? renderObject
        : null;
  }

  bool _safeFactor(double? begin, double? end, double extent, Curve curve) {
    if (end == null) return true;
    for (var i = 0; i <= 100; i++) {
      final t = curve.transform(i / 100);
      final factor = (begin ?? end) * (1 - t) + end * t;
      if (!factor.isFinite || factor < 0 || !(factor * extent).isFinite) {
        return false;
      }
    }
    return true;
  }

  @override
  Widget build(BuildContext context) => _RefreshLayoutObserver(
    builder: (context, constraints) {
      final input = widget.target;
      // Eager first build preserves the SDK's intrinsic measurements. Bounds are
      // not real until the observer's first layout callback.
      final eager = _last == null;
      final blockWidth =
          !eager &&
          (!constraints.hasBoundedWidth ||
              (input.widthFactor != null &&
                  !(input.widthFactor! * constraints.maxWidth).isFinite));
      final blockHeight =
          !eager &&
          (!constraints.hasBoundedHeight ||
              (input.heightFactor != null &&
                  !(input.heightFactor! * constraints.maxHeight).isFinite));
      final width = blockWidth ? null : input.widthFactor;
      final height = blockHeight ? null : input.heightFactor;
      final messages = <String>[if (widget.message.isNotEmpty) widget.message];
      if (blockWidth && (input.widthFactor != null || _endWidth != null) ||
          blockHeight && (input.heightFactor != null || _endHeight != null)) {
        messages.add(
          'AnimatedFractionallySizedBox preview limitation: a factor needs a finite available axis and finite resulting extent. Using null on the unavailable axis; stored values and generated Dart are unchanged.',
        );
      }
      final last = _last;
      final constraintsChanged =
          blockWidth != _blockedWidth || blockHeight != _blockedHeight;
      final changed =
          last == null ||
          last.alignment != input.alignment ||
          last.widthFactor != width ||
          last.heightFactor != height ||
          last.curve != input.curve ||
          last.duration != input.duration;
      if (last == null || constraintsChanged) {
        if (last != null) _nativeKey = GlobalKey();
        _beginWidth = _endWidth = width;
        _beginHeight = _endHeight = height;
        _transitionMessage = null;
      } else if (changed) {
        final displayed = _displayed();
        final restarts =
            last.alignment != input.alignment ||
            width != null && _endWidth != null && width != _endWidth ||
            height != null && _endHeight != null && height != _endHeight;
        // The pinned SDK skips null factor visitors: their existing tween is
        // retained, and can replay if another target restarts the controller.
        if (width != null) {
          if (_endWidth == null) {
            _beginWidth = _endWidth = width;
          } else if (restarts) {
            _beginWidth = displayed?.widthFactor ?? _endWidth;
            _endWidth = width;
          }
        }
        if (height != null) {
          if (_endHeight == null) {
            _beginHeight = _endHeight = height;
          } else if (restarts) {
            _beginHeight = displayed?.heightFactor ?? _endHeight;
            _endHeight = height;
          }
        }
        _transitionMessage = null;
      }
      // Recheck after resize as well: an unchanged factor may overflow new bounds.
      if (!eager &&
          input.duration != Duration.zero &&
          (!_safeFactor(
                _beginWidth,
                _endWidth,
                constraints.maxWidth,
                input.curve,
              ) ||
              !_safeFactor(
                _beginHeight,
                _endHeight,
                constraints.maxHeight,
                input.curve,
              ))) {
        _nativeKey = GlobalKey();
        _beginWidth = _endWidth = width;
        _beginHeight = _endHeight = height;
        _transitionMessage =
            'AnimatedFractionallySizedBox preview limitation: this curve produces unsafe factor interpolation. Showing the target without this transition; stored values and generated Dart are unchanged.';
      }
      if (_transitionMessage != null) messages.add(_transitionMessage!);
      _blockedWidth = blockWidth;
      _blockedHeight = blockHeight;
      _last = AnimatedFractionallySizedBox(
        alignment: input.alignment,
        widthFactor: width,
        heightFactor: height,
        curve: input.curve,
        duration: input.duration,
      );
      return _TextButtonPreview(
        message: messages.join(' '),
        child: AnimatedFractionallySizedBox(
          key: _nativeKey,
          alignment: input.alignment,
          widthFactor: width,
          heightFactor: height,
          curve: input.curve,
          duration: input.duration,
          onEnd: null,
          child: input.child == null
              ? null
              : KeyedSubtree(key: _childKey, child: input.child!),
        ),
      );
    },
  );
}

class _AnimatedPhysicalPreview extends StatefulWidget {
  const _AnimatedPhysicalPreview({required this.target,required this.message});
  final AnimatedPhysicalModel target;
  final String message;
  @override State<_AnimatedPhysicalPreview> createState()=>_AnimatedPhysicalPreviewState();
}
class _AnimatedPhysicalPreviewState extends State<_AnimatedPhysicalPreview> {
  GlobalKey _nativeKey=GlobalKey();
  final GlobalKey _childKey=GlobalKey();
  late double _beginElevation;
  late BorderRadius _beginRadius;
  bool _unsafe=false;
  @override void initState(){
    super.initState();_beginElevation=widget.target.elevation;_beginRadius=widget.target.borderRadius??BorderRadius.zero;
  }
  @override void didUpdateWidget(covariant _AnimatedPhysicalPreview oldWidget){
    super.didUpdateWidget(oldWidget);
    final old=oldWidget.target,next=widget.target;
    final displayed=_childKey.currentContext?.findAncestorWidgetOfExactType<PhysicalModel>();
    final radius=next.borderRadius??BorderRadius.zero;
    // A change to any of the four targets restarts all SDK tweens, even when
    // animateColor/animateShadowColor are false. Curve-only edits retain begin.
    if(old.elevation!=next.elevation||(old.borderRadius??BorderRadius.zero)!=radius||
       old.color!=next.color||old.shadowColor!=next.shadowColor){
      _beginElevation=displayed?.elevation??old.elevation;
      _beginRadius=displayed?.borderRadius??old.borderRadius??BorderRadius.zero;
    }
    _unsafe=false;
    if(next.duration!=Duration.zero){
      for(var i=0;i<=100;i++){
        final t=next.curve.transform(i/100);
        final elevation=_beginElevation+(next.elevation-_beginElevation)*t;
        final corners=BorderRadius.lerp(_beginRadius,radius,t)!;
        if(!elevation.isFinite||elevation<0||
           [corners.topLeft,corners.topRight,corners.bottomLeft,corners.bottomRight]
             .any((r)=>!r.x.isFinite||!r.y.isFinite)){
          _unsafe=true;break;
        }
      }
    }
    if(_unsafe){_nativeKey=GlobalKey();_beginElevation=next.elevation;_beginRadius=radius;}
  }
  @override Widget build(BuildContext context){
    final v=widget.target;
    return _TextButtonPreview(
      message:[widget.message,if(_unsafe)'AnimatedPhysicalModel preview limitation: this curve produces unsafe elevation/radius interpolation. Showing the target without this transition; stored values and generated Dart are unchanged.'].where((s)=>s.isNotEmpty).join(' '),
      child:AnimatedPhysicalModel(key:_nativeKey,shape:v.shape,clipBehavior:v.clipBehavior,
        borderRadius:v.borderRadius,elevation:v.elevation,color:v.color,shadowColor:v.shadowColor,
        animateColor:v.animateColor,animateShadowColor:v.animateShadowColor,curve:v.curve,duration:v.duration,
        onEnd:null,child:KeyedSubtree(key:_childKey,child:v.child)));
  }
}

class _AnimatedTextStylePreview extends StatefulWidget {
  const _AnimatedTextStylePreview({required this.target, required this.message});
  final AnimatedDefaultTextStyle target;
  final String message;
  @override
  State<_AnimatedTextStylePreview> createState() => _AnimatedTextStylePreviewState();
}

class _AnimatedTextStylePreviewState extends State<_AnimatedTextStylePreview> {
  GlobalKey _nativeKey = GlobalKey();
  final GlobalKey _childKey = GlobalKey();
  String? _transitionMessage;

  TextStyle? _displayedStyle() => _childKey.currentContext
      ?.getInheritedWidgetOfExactType<DefaultTextStyle>()?.style;

  @override
  void didUpdateWidget(covariant _AnimatedTextStylePreview oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.target.style == widget.target.style) return;
    _transitionMessage = null;
    final current = _displayedStyle() ?? oldWidget.target.style;
    try {
      // Preflight the reviewed curve, including overshoot. Incompatible inherit
      // and missing Paint/color peers fail before SDK build can poison Canvas.
      for (var step = 0; step <= 100; step++) {
        final style = TextStyle.lerp(current, widget.target.style, widget.target.curve.transform(step / 100))!;
        if ([style.fontSize, style.letterSpacing, style.wordSpacing, style.height, style.decorationThickness]
            .any((v) => v != null && !v.isFinite) || (style.fontSize != null && style.fontSize! < 0)) {
          throw ArgumentError('Unsafe interpolated text metrics');
        }
      }
    } on FlutterError {
      _transitionMessage = 'Flutter TextStyle.lerp cannot interpolate this inherit/paint transition.';
    } on TypeError {
      _transitionMessage = 'Flutter TextStyle.lerp requires a matching color when interpolating a Paint.';
    } on ArgumentError {
      _transitionMessage = 'Flutter TextStyle.lerp produced unsafe text metrics.';
    }
    if (_transitionMessage != null) _nativeKey = GlobalKey();
  }

  @override
  Widget build(BuildContext context) {
    final value = widget.target;
    return _TextButtonPreview(
      message: [widget.message, if (_transitionMessage != null)
          'AnimatedDefaultTextStyle preview limitation: $_transitionMessage Showing the target without this transition; stored values and generated Dart are unchanged.']
          .where((m) => m.isNotEmpty).join(' '),
      child: NotificationListener<SizeChangedLayoutNotification>(
        onNotification: (_) {
          context.findAncestorStateOfType<_CanvasDocumentViewState>()?._refreshZeroSizedWidgetTargetsAfterFrame();
          return false;
        },
        child: SizeChangedLayoutNotifier(
          child: AnimatedDefaultTextStyle(
            key: _nativeKey, style: value.style, textAlign: value.textAlign,
            softWrap: value.softWrap, overflow: value.overflow, maxLines: value.maxLines,
            textWidthBasis: value.textWidthBasis, textHeightBehavior: value.textHeightBehavior,
            duration: value.duration, curve: value.curve, onEnd: null,
            child: KeyedSubtree(key: _childKey, child: value.child),
          ),
        ),
      ),
    );
  }
}

class _AnimatedMatrixGuard extends SingleChildRenderObjectWidget {
  const _AnimatedMatrixGuard({required this.changed, required super.child});
  final ValueChanged<bool> changed;
  @override _AnimatedMatrixRenderBox createRenderObject(BuildContext context) => _AnimatedMatrixRenderBox(changed);
  @override void updateRenderObject(BuildContext context, _AnimatedMatrixRenderBox render) {
    render.changed = changed;
    render.markNeedsPaint();
    render.markNeedsSemanticsUpdate();
  }
}
class _AnimatedMatrixRenderBox extends RenderProxyBox {
  _AnimatedMatrixRenderBox(this.changed);
  ValueChanged<bool> changed;
  bool _blocked = false;
  bool get _finite {
    final rotation = child;
    if (rotation is! RenderTransform || rotation.child == null) return true;
    final matrix = Matrix4.identity();
    rotation.applyPaintTransform(rotation.child!, matrix);
    return matrix.storage.every((value) => value.isFinite);
  }
  @override void paint(PaintingContext context, Offset offset) {
    final blocked = !_finite;
    if (_blocked != blocked) markNeedsSemanticsUpdate();
    _blocked = blocked;
    changed(blocked);
    if (!blocked) super.paint(context, offset);
  }
  @override bool hitTest(BoxHitTestResult result, {required Offset position}) =>
    _finite && super.hitTest(result, position: position);
  @override void visitChildrenForSemantics(RenderObjectVisitor visitor) {
    if (_finite) super.visitChildrenForSemantics(visitor);
  }
}

class _MenuItemPreview extends StatefulWidget {
  const _MenuItemPreview({
    required this.node,
    required this.message,
    required this.child,
  });
  final CanvasNode node;
  final String message;
  final Widget child;
  @override
  State<_MenuItemPreview> createState() => _MenuItemPreviewState();
}

class _MenuItemPreviewState extends State<_MenuItemPreview> {
  String? _geometryMessage;
  String? _pendingMessage;
  bool _scheduled = false;
  String get message => [
    if (widget.message.isNotEmpty) widget.message,
    if (_geometryMessage != null)
      '${widget.node.type.split('.').last} ${widget.node.id} geometry preview unavailable: $_geometryMessage. The real SDK subtree and model children are retained, but unsafe paint, semantics and pointer input are withheld. Stored values and generated Dart are unchanged.',
  ].join('\n');
  void _changed(String? value) {
    _pendingMessage = value;
    if (_scheduled || _geometryMessage == value) return;
    _scheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scheduled = false;
      if (!mounted || _geometryMessage == _pendingMessage) return;
      setState(() => _geometryMessage = _pendingMessage);
      context
          .findAncestorStateOfType<_CanvasDocumentViewState>()
          ?._refreshZeroSizedWidgetTargetsAfterFrame();
    });
  }

  @override
  Widget build(BuildContext context) => _TextButtonPreview(
    message: message,
    stableDiagnostic: true,
    child: _MenuItemGeometryGuard(
      fallbackWidth: MediaQuery.sizeOf(context).width,
      changed: _changed,
      child: widget.child,
    ),
  );
}

class _MenuItemGeometryGuard extends SingleChildRenderObjectWidget {
  const _MenuItemGeometryGuard({
    required this.fallbackWidth,
    required this.changed,
    required super.child,
  });
  final double fallbackWidth;
  final ValueChanged<String?> changed;
  @override
  _MenuItemGeometryRenderBox createRenderObject(BuildContext context) =>
      _MenuItemGeometryRenderBox(fallbackWidth, changed);
  @override
  void updateRenderObject(
    BuildContext context,
    _MenuItemGeometryRenderBox render,
  ) {
    render.fallbackWidth = fallbackWidth;
    render.changed = changed;
    render.markNeedsLayout();
  }
}

class _MenuItemGeometryRenderBox extends RenderProxyBox {
  _MenuItemGeometryRenderBox(this.fallbackWidth, this.changed);
  double fallbackWidth;
  ValueChanged<String?> changed;
  bool blocked = false;
  bool _unbounded = false;
  bool get _labelNeedsBoundedWidth {
    // The pinned native menu label is the first Flex below TextButton. Read
    // only public topology, including SDK parent-menu axis overrides. A finite
    // native style constraint already bounds the label and needs no quarantine.
    bool? visit(RenderObject render) {
      if (render is RenderConstrainedBox &&
          render.additionalConstraints.hasBoundedWidth) {
        return false;
      }
      if (render is RenderFlex) {
        if (render.direction != Axis.horizontal) return false;
        var needs = false;
        render.visitChildren((child) {
          final data = child.parentData;
          if (data is FlexParentData &&
              data.flex != null &&
              data.flex! > 0 &&
              (render.mainAxisSize == MainAxisSize.max ||
                  data.fit == FlexFit.tight)) {
            needs = true;
          }
        });
        return needs;
      }
      bool? result;
      render.visitChildren((child) {
        result ??= visit(child);
      });
      return result;
    }

    return visit(child!) ?? false;
  }

  bool _requiresQuarantine(BoxConstraints value) =>
      !value.hasBoundedWidth && _labelNeedsBoundedWidth;
  BoxConstraints _safe(BoxConstraints value) => !_requiresQuarantine(value)
      ? value
      : value.copyWith(maxWidth: math.max(value.minWidth, fallbackWidth));
  @override
  Size computeDryLayout(BoxConstraints constraints) =>
      !_requiresQuarantine(constraints)
      ? super.computeDryLayout(constraints)
      : constraints.constrain(Size.zero);
  @override
  double? computeDryBaseline(
    BoxConstraints constraints,
    TextBaseline baseline,
  ) => !_requiresQuarantine(constraints)
      ? super.computeDryBaseline(constraints, baseline)
      : null;
  @override
  double? computeDistanceToActualBaseline(TextBaseline baseline) =>
      blocked ? null : super.computeDistanceToActualBaseline(baseline);
  bool _overflows(RenderObject render) {
    if (render is RenderFlex && render.hasSize) {
      var overflow = false;
      render.visitChildren((child) {
        if (child is! RenderBox ||
            !child.hasSize ||
            child.parentData is! BoxParentData) {
          return;
        }
        final offset = (child.parentData! as BoxParentData).offset;
        final start = render.direction == Axis.horizontal
            ? offset.dx
            : offset.dy;
        final end = render.direction == Axis.horizontal
            ? offset.dx + child.size.width
            : offset.dy + child.size.height;
        final limit = render.direction == Axis.horizontal
            ? render.size.width
            : render.size.height;
        overflow |=
            !start.isFinite ||
            !end.isFinite ||
            start < -precisionErrorTolerance ||
            end > limit + precisionErrorTolerance;
      });
      if (overflow) return true;
    }
    var overflow = false;
    render.visitChildren((child) {
      if (!overflow) overflow = _overflows(child);
    });
    return overflow;
  }

  @override
  void performLayout() {
    child!.layout(_safe(constraints), parentUsesSize: true);
    _unbounded = _requiresQuarantine(constraints);
    if (_unbounded) {
      if (!blocked) markNeedsSemanticsUpdate();
      blocked = true;
      changed('the native menu label requires a bounded horizontal layout');
    }
    size = _unbounded ? constraints.constrain(Size.zero) : child!.size;
  }

  @override
  void paint(PaintingContext context, Offset offset) {
    if (_unbounded) return;
    // Inspect public descendant geometry only after layout, at the legal
    // painting boundary, before an overflowing native Flex paints diagnostics.
    final overflow = _overflows(child!);
    if (blocked != overflow) markNeedsSemanticsUpdate();
    blocked = overflow;
    changed(
      overflow
          ? 'the native menu label or one of its real slots exceeds the available layout extent'
          : null,
    );
    if (!blocked) super.paint(context, offset);
  }

  @override
  bool hitTest(BoxHitTestResult result, {required Offset position}) =>
      !blocked && super.hitTest(result, position: position);
  @override
  void visitChildrenForSemantics(RenderObjectVisitor visitor) {
    if (!blocked) super.visitChildrenForSemantics(visitor);
  }
}

// The preview is a logical application surface. Keep native menus/tooltips in
// its own overlay coordinate space for zoom and clipping. MenuAnchor root-overlay
// requests are explicitly approximated by this local overlay; LookupBoundary
// cannot be used here because it hides View.of from real EditableText children.
// This content entry is never recreated on edits.
class _MenuAnchorPreview extends StatefulWidget {
  const _MenuAnchorPreview({
    super.key,
    required this.owner,
    this.menuBar = false,
  });
  final _CanvasNodeView owner;
  final bool menuBar;
  @override
  State<_MenuAnchorPreview> createState() => _MenuAnchorPreviewState();
}

class _MenuPanelPreviewScope extends InheritedWidget {
  const _MenuPanelPreviewScope({required this.linked, required super.child});
  final bool linked;
  static bool active(BuildContext context) =>
      context.getInheritedWidgetOfExactType<_MenuPanelPreviewScope>() != null;
  static bool linkedOf(BuildContext context) =>
      context
          .dependOnInheritedWidgetOfExactType<_MenuPanelPreviewScope>()
          ?.linked ??
      false;
  @override
  bool updateShouldNotify(_MenuPanelPreviewScope oldWidget) =>
      linked != oldWidget.linked;
}

class _MenuAnchorPreviewState extends State<_MenuAnchorPreview> {
  final MenuController controller = MenuController();
  final LayerLink _layerLink = LayerLink();
  final GlobalKey _menuProbeKey = GlobalKey(debugLabel: 'menu-overlay-probe');
  AnimationStatus _status = AnimationStatus.dismissed;
  bool get interactive =>
      controller.isOpen && _status != AnimationStatus.reverse;
  bool _notifyScheduled = false;
  void _changed() {
    if (_notifyScheduled) return;
    _notifyScheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _notifyScheduled = false;
      if (mounted) {
        context
            .findAncestorStateOfType<_CanvasDocumentViewState>()
            ?._menuChanged();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final owner = widget.owner;
    final documentState = context
        .findAncestorStateOfType<_CanvasDocumentViewState>();
    final menuBar =
        widget.menuBar || owner.node.type == 'flutter.material.MenuBar';
    final submenu = owner.node.type == 'flutter.material.SubmenuButton';
    var style = submenu
        ? owner._submenuMenuStyle(context)
        : owner._menuAnchorStyle(context);
    final themeStyle = MenuTheme.of(context).style;
    var unsafeResolution = false;
    T? resolve<T>(WidgetStateProperty<T?>? Function(MenuStyle style) field) =>
        (() {
          try {
            return (style == null ? null : field(style)?.resolve({})) ??
                (themeStyle == null ? null : field(themeStyle)?.resolve({}));
          } catch (_) {
            // ThemeData.lerp can expose a finite/non-finite state property
            // while the host theme is animating. Do not resolve it again in
            // this isolated preview; quarantine the complete menu style.
            unsafeResolution = true;
            return null;
          }
        })();
    final minimum = resolve<Size>((value) => value.minimumSize) ?? Size.zero;
    final maximum =
        resolve<Size>((value) => value.maximumSize) ??
        const Size(double.infinity, double.infinity);
    final fixed = resolve<Size>((value) => value.fixedSize);
    final elevation = resolve<double>((value) => value.elevation) ?? 3;
    final menuPadding =
        (resolve<EdgeInsetsGeometry>((value) => value.padding) ??
                const EdgeInsets.symmetric(vertical: 8))
            .resolve(Directionality.of(context));
    final invalidGeometry =
        unsafeResolution ||
        !menuPadding.isNonNegative ||
        !minimum.isFinite ||
        minimum.width < 0 ||
        minimum.height < 0 ||
        maximum.width.isNaN ||
        maximum.height.isNaN ||
        maximum.width < minimum.width ||
        maximum.height < minimum.height ||
        (fixed != null &&
            (fixed.width.isNaN ||
                fixed.height.isNaN ||
                fixed.width < 0 ||
                fixed.height < 0)) ||
        !elevation.isFinite ||
        elevation < 0 ||
        [
          menuPadding.left,
          menuPadding.top,
          menuPadding.right,
          menuPadding.bottom,
        ].any((value) => !value.isFinite);
    if (invalidGeometry) {
      // Do not let a finite->infinite/NaN transition reach MenuStyle.lerp:
      // Flutter asserts in dart:ui/lerp.dart before the popup can be painted.
      // Use a complete finite quarantine style for the isolated preview. The
      // model and generated source still retain every original value, while
      // the native children remain mounted and selectable.
      style = const MenuStyle(
        minimumSize: WidgetStatePropertyAll(Size.zero),
        maximumSize: WidgetStatePropertyAll(Size(100000, 100000)),
        fixedSize: WidgetStatePropertyAll(Size(100000, 100000)),
        padding: WidgetStatePropertyAll(EdgeInsets.symmetric(vertical: 8)),
        elevation: WidgetStatePropertyAll(3.0),
      );
    }
    Widget safeMenuTheme(Widget child) => invalidGeometry
        ? MenuTheme(
            data: MenuThemeData(style: style),
            child: child,
          )
        : child;
    final padding = owner._buttonReferencePresent('reservedPadding')
        ? null
        : owner._edgeInsetsGeometry('reservedPadding');
    final resolvedPadding = padding?.resolve(Directionality.of(context));
    final unavailablePadding =
        resolvedPadding != null &&
        [
          resolvedPadding.left,
          resolvedPadding.top,
          resolvedPadding.right,
          resolvedPadding.bottom,
        ].any((value) => !value.isFinite);
    final message = <String>[
      if (menuBar) ...[
        if (owner._buttonReferencePresent('controller'))
          'MenuBar controller project reference is not executed in isolated Canvas. A stable preview-owned controller is used.',
        if (owner._buttonReferencePresent('style'))
          'MenuBar style project reference is not executed in isolated Canvas. Native defaults are used; unknown project content and behavior are unavailable.',
      ] else if (submenu) ...[
        ?_textButtonReferenceMessage(owner.node),
        for (final name in [
          'controller',
          'focusNode',
          'menuStyle',
          'alignmentOffset',
          'hoverOpenDelayUs',
          'submenuIcon',
          'submenuIconDefault',
          'submenuIconDisabled',
          'submenuIconHovered',
          'submenuIconFocused',
        ])
          if (owner._buttonReferencePresent(name))
            'SubmenuButton $name project reference is not executed in isolated Canvas. ${name == 'controller' ? 'A stable preview-owned controller is used.' : 'Native defaults are used; unknown project content and behavior are unavailable.'}',
        if (owner._buttonReferencePresent('statesController'))
          'SubmenuButton statesController project reference is not executed. The pinned SDK uses it only for the menu-padding prepass, not the internal TextButton states. Preview padding uses the native empty state set.',
      ],
      if (!submenu && !menuBar)
        for (final name in [
          'controller',
          'childFocusNode',
          'style',
          'alignmentOffset',
          'reservedPadding',
          'layerLink',
          'builder',
        ])
          if (owner._buttonReferencePresent(name))
            'MenuAnchor $name project reference is not executed in isolated Canvas. ${name == 'builder'
                ? 'The actual Child (or native empty fallback) is shown; Preview menu is a Designer action, not a generated opener.'
                : name == 'controller'
                ? 'A stable preview-owned controller is used.'
                : name == 'layerLink'
                ? 'An isolated preview-owned link is used; project connectivity is unavailable.'
                : 'Native defaults are used for this reference.'}',
      if (owner._boolean('useRootOverlay') == true)
        '${submenu ? 'SubmenuButton' : 'MenuAnchor'} root-overlay preview is isolated to the logical Canvas viewport: root and nearest overlay are intentionally the same here. The source useRootOverlay flag is unchanged.',
      if (owner._buttonReferencePresent('layerLink') &&
          !_menuAnchorFollowerAvailable(owner.node))
        'MenuAnchor LayerLink follower preview is unavailable with descendant Tooltip overlays: the pinned SDK cannot compute their overlay transform through a follower. The isolated link is omitted; the project reference and generated Dart are unchanged.',
      if (unavailablePadding)
        'MenuAnchor reservedPadding preview unavailable: nonfinite reserved padding cannot safely bound this isolated menu. Native default padding is used; stored values and generated Dart are unchanged.',
      if (invalidGeometry)
        '${menuBar
            ? 'MenuBar'
            : submenu
            ? 'SubmenuButton'
            : 'MenuAnchor'} inherited menu geometry preview unavailable: nonfinite, negative or inconsistent resolved constraints, elevation or padding. Native default geometry is substituted without changing model children, stored values or generated Dart.',
    ].join('\n');
    _changed();
    final menuSlot = menuBar ? 'children' : 'menuChildren';
    final children = [
      for (final (index, child) in owner._children(menuSlot).indexed)
        _MenuPanelPreviewScope(
          linked: _menuAnchorFollowerAvailable(owner.node),
          child: Focus(
            // Menu overlays own their primary focus. This passive ancestor
            // lets Designer shortcuts (notably F2) bubble out of a native
            // menu without requesting focus or changing menu state.
            canRequestFocus: false,
            skipTraversal: true,
            onKeyEvent: documentState?._onKeyEvent,
            child: index == 0
                ? KeyedSubtree(key: _menuProbeKey, child: child)
                : child,
          ),
        ),
    ];
    void opened() {
      _status = AnimationStatus.forward;
      _changed();
    }

    void closed() {
      _status = AnimationStatus.dismissed;
      _changed();
    }

    void statusChanged(AnimationStatus status) {
      _status = status;
      _changed();
    }

    if (menuBar) {
      return _TextButtonPreview(
        message: message,
        stableDiagnostic: true,
        child: safeMenuTheme(
          MenuBar(
            controller: controller,
            style: style,
            clipBehavior: owner._clipBehavior() ?? Clip.none,
            children: [for (final child in owner._children('children')) child],
          ),
        ),
      );
    }
    if (submenu) {
      return _MenuItemPreview(
        node: owner.node,
        message: message,
        child: _MenuItemAnchorScope(
          child: safeMenuTheme(
            SubmenuButton(
              controller: controller,
              onHover: owner._buttonReferencePresent('onHover') ? (_) {} : null,
              onFocusChange: owner._buttonReferencePresent('onFocusChange')
                  ? (_) {}
                  : null,
              onOpen: opened,
              onClose: closed,
              style: owner._elevatedButtonStyle(context),
              menuStyle: style,
              alignmentOffset: owner._offset('alignmentOffset'),
              clipBehavior: owner._clipBehavior() ?? Clip.hardEdge,
              submenuIcon: owner._submenuIcon(),
              useRootOverlay: false,
              hoverOpenDelay: Duration(
                microseconds: owner._integer('hoverOpenDelayUs') ?? 0,
              ),
              animated: owner._boolean('animated') ?? false,
              onAnimationStatusChanged: statusChanged,
              leadingIcon: owner._single('leadingIcon'),
              trailingIcon: owner._single('trailingIcon'),
              menuChildren: children,
              child: owner._single('child'),
            ),
          ),
        ),
      );
    }
    return _TextButtonPreview(
      message: message,
      stableDiagnostic: true,
      child: safeMenuTheme(
        MenuAnchor(
          controller: controller,
          style: style,
          alignmentOffset: owner._offset('alignmentOffset') ?? Offset.zero,
          reservedPadding: unavailablePadding ? null : padding,
          layerLink: _menuAnchorFollowerAvailable(owner.node)
              ? _layerLink
              : null,
          clipBehavior: owner._clipBehavior() ?? Clip.hardEdge,
          // This deprecated pinned argument is stored but intentionally inert.
          // ignore: deprecated_member_use
          anchorTapClosesMenu: owner._boolean('anchorTapClosesMenu') ?? false,
          consumeOutsideTap: owner._boolean('consumeOutsideTap') ?? false,
          onOpen: opened,
          onClose: closed,
          crossAxisUnconstrained:
              owner._boolean('crossAxisUnconstrained') ?? true,
          // A LookupBoundary would hide View.of from EditableText. Keep the
          // supported local Overlay and label root-placement isolation above.
          useRootOverlay: false,
          animated: owner._boolean('animated') ?? false,
          onAnimationStatusChanged: statusChanged,
          menuChildren: children,
          child: owner._single('child'),
        ),
      ),
    );
  }
}

class _CanvasViewportOverlay extends StatefulWidget {
  const _CanvasViewportOverlay({required this.child});
  final Widget child;
  @override
  State<_CanvasViewportOverlay> createState() => _CanvasViewportOverlayState();
}

class _CanvasViewportOverlayState extends State<_CanvasViewportOverlay> {
  late final OverlayEntry _content = OverlayEntry(
    opaque: true,
    maintainState: true,
    builder: (_) => widget.child,
  );
  @override
  void didUpdateWidget(_CanvasViewportOverlay oldWidget) {
    super.didUpdateWidget(oldWidget);
    _content.markNeedsBuild();
  }

  @override
  void dispose() {
    _content.remove();
    _content.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Overlay(
    key: const ValueKey('canvas-logical-viewport-overlay'),
    initialEntries: [_content],
  );
}

class _MenuItemAnchorScope extends InheritedWidget {
  const _MenuItemAnchorScope({required super.child});
  static bool active(BuildContext context) =>
      context.getInheritedWidgetOfExactType<_MenuItemAnchorScope>() != null;
  @override
  bool updateShouldNotify(_MenuItemAnchorScope oldWidget) => false;
}

class _TooltipAnchorScope extends InheritedWidget {
  const _TooltipAnchorScope({required super.child});
  static bool active(BuildContext context) =>
      context.dependOnInheritedWidgetOfExactType<_TooltipAnchorScope>() != null;
  @override
  bool updateShouldNotify(_TooltipAnchorScope oldWidget) => false;
}

class _TooltipPreview extends StatefulWidget {
  const _TooltipPreview({super.key, required this.node, required this.builder});
  final CanvasNode node;
  final Widget Function(GlobalKey, GlobalKey) builder;
  @override
  State<_TooltipPreview> createState() => _TooltipPreviewState();
}

class _TooltipPreviewState extends State<_TooltipPreview> {
  final sdkKey = GlobalKey<TooltipState>();
  var anchorKey = GlobalKey();
  String? message;
  @override
  Widget build(BuildContext context) => widget.builder(sdkKey, anchorKey);
}

class _ExpansionTilePreview extends StatefulWidget {
  const _ExpansionTilePreview({
    super.key,
    required this.node,
    required this.builder,
  });
  final CanvasNode node;
  final Widget Function(ExpansibleController, GlobalKey) builder;
  @override
  State<_ExpansionTilePreview> createState() => _ExpansionTilePreviewState();
}

class _ExpansionTilePreviewState extends State<_ExpansionTilePreview> {
  final controller = ExpansibleController();
  final sdkKey = GlobalKey();
  int _seedRevision = 0;
  bool get _seed => widget.node.properties['initiallyExpanded']?.value == true;
  @override
  void initState() {
    super.initState();
    if (_seed) controller.expand();
    controller.addListener(_changed);
  }

  void _changed() {
    if (!mounted) return;
    setState(() {});
    context
        .findAncestorStateOfType<_CanvasDocumentViewState>()
        ?._refreshZeroSizedWidgetTargetsAfterFrame();
  }

  @override
  void didUpdateWidget(covariant _ExpansionTilePreview oldWidget) {
    super.didUpdateWidget(oldWidget);
    if ((oldWidget.node.properties['initiallyExpanded']?.value == true) !=
        _seed) {
      final revision = ++_seedRevision;
      // SDK initiallyExpanded is only an initialization seed. A Designer edit
      // explicitly previews the new seed without remounting the real SDK State.
      // Controller notifications cannot run during this ancestor's build.
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (!mounted || revision != _seedRevision) return;
        if (_seed) {
          controller.expand();
        } else {
          controller.collapse();
        }
      });
    }
  }

  RenderBox? get headerBox {
    RenderBox? result;
    void visit(Element element) {
      if (result != null) return;
      if (element.widget is ListTile) {
        final render = element.findRenderObject();
        if (render is RenderBox && render.attached) result = render;
        return;
      }
      element.visitChildren(visit);
    }

    final element = sdkKey.currentContext;
    if (element is Element) visit(element);
    return result;
  }

  @override
  void dispose() {
    controller.removeListener(_changed);
    controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.builder(controller, sdkKey);
}

class _ListTilePreview extends StatefulWidget {
  const _ListTilePreview({
    required this.node,
    required this.message,
    required this.slots,
    required this.builder,
    required this.riskyUnboundedHeight,
    required this.hasMaterial,
    this.widgetName = 'ListTile',
  });
  final CanvasNode node;
  final String? message;
  final Map<String, Widget> slots;
  final Widget Function(Map<String, Widget>) builder;
  final bool riskyUnboundedHeight;
  final bool hasMaterial;
  final String widgetName;
  @override
  State<_ListTilePreview> createState() => _ListTilePreviewState();
}

class _ListTilePreviewState extends State<_ListTilePreview> {
  final _keys = <String, GlobalKey>{};
  final _slotRenderers = <String, _ListTileSlotRenderBox>{};
  final _slotMessages = <String, String>{};
  String? _geometryMessage;
  String _message = '';
  bool _scheduled = false;
  void _scheduleMessage() {
    if (_scheduled) return;
    _scheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scheduled = false;
      if (!mounted) return;
      final messages = <String>[
        if (widget.message case final String message) message,
        if (_geometryMessage case final String message)
          'Render ${widget.widgetName} ${widget.node.id}: geometry preview unavailable: $message. The SDK subtree is retained but unsafe paint/semantics and pointer input are withheld.',
        ..._slotMessages.values,
      ];
      final message = messages.join(' ');
      if (_message != message) {
        setState(() => _message = message);
        context
            .findAncestorStateOfType<_CanvasDocumentViewState>()
            ?._refreshZeroSizedWidgetTargetsAfterFrame();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final names = widget.slots.keys.toSet();
    _slotMessages.removeWhere((name, _) => !names.contains(name));
    _scheduleMessage();
    if (!widget.hasMaterial) {
      return _TextButtonPreview(
        message: widget.message ?? '',
        child: const SizedBox.shrink(),
      );
    }
    final slots = <String, Widget>{
      for (final entry in widget.slots.entries)
        entry.key: _ListTileSlotGuard(
          key: ValueKey(entry.key),
          sideSlot:
              entry.key == 'leading' ||
              entry.key == 'trailing' ||
              entry.key == 'secondary',
          onRenderer: (renderer) {
            if (renderer == null) {
              _slotRenderers.remove(entry.key);
            } else {
              _slotRenderers[entry.key] = renderer;
            }
          },
          onBlocked: (blocked) {
            if (blocked) {
              _slotMessages[entry.key] =
                  '${widget.widgetName} ${widget.node.id}.${entry.key} preview unavailable: '
                  'the mounted slot consumes the entire nonzero tile content width, which Flutter 3.44.8 rejects. '
                  'Only this slot paint/semantics is withheld and it reports zero width; child State, model and generated Dart are retained.';
            } else {
              _slotMessages.remove(entry.key);
            }
            _scheduleMessage();
          },
          child: KeyedSubtree(
            key: _keys.putIfAbsent(entry.key, GlobalKey.new),
            child: entry.value,
          ),
        ),
    };
    return _TextButtonPreview(
      message: _message.isEmpty ? widget.message ?? '' : _message,
      child: _ListTileLayoutGuard(
        riskyUnboundedHeight: widget.riskyUnboundedHeight,
        slotRenderers: _slotRenderers,
        widgetName: widget.widgetName,
        onMessage: (message) {
          _geometryMessage = message;
          _scheduleMessage();
        },
        child: widget.builder(slots),
      ),
    );
  }
}

class _ListTileSlotGuard extends SingleChildRenderObjectWidget {
  const _ListTileSlotGuard({
    super.key,
    required this.sideSlot,
    required this.onRenderer,
    required this.onBlocked,
    required super.child,
  });
  final bool sideSlot;
  final ValueChanged<_ListTileSlotRenderBox?> onRenderer;
  final ValueChanged<bool> onBlocked;
  @override
  _ListTileSlotRenderBox createRenderObject(BuildContext context) {
    final render = _ListTileSlotRenderBox(sideSlot, onBlocked, onRenderer);
    onRenderer(render);
    return render;
  }

  @override
  void updateRenderObject(BuildContext context, _ListTileSlotRenderBox render) {
    render.sideSlot = sideSlot;
    render.onBlocked = onBlocked;
    render.onRenderer = onRenderer;
    onRenderer(render);
    render.markNeedsLayout();
  }
}

class _ListTileSlotRenderBox extends RenderProxyBox {
  _ListTileSlotRenderBox(this.sideSlot, this.onBlocked, this.onRenderer);
  bool sideSlot;
  ValueChanged<bool> onBlocked;
  ValueChanged<_ListTileSlotRenderBox?> onRenderer;
  bool blocked = false;
  bool _fullWidth(Size size, BoxConstraints constraints) =>
      sideSlot &&
      constraints.maxWidth > 0 &&
      size.width == constraints.maxWidth;
  bool get hasInvalidOffset {
    final data = parentData;
    return data is BoxParentData &&
        (!data.offset.dx.isFinite || !data.offset.dy.isFinite);
  }

  @override
  Size computeDryLayout(BoxConstraints constraints) {
    final measured = child?.getDryLayout(constraints) ?? Size.zero;
    return _fullWidth(measured, constraints)
        ? Size(0, measured.height)
        : measured;
  }

  @override
  void performLayout() {
    super.performLayout();
    final previous = blocked;
    blocked = _fullWidth(size, constraints);
    if (blocked) size = Size(0, size.height);
    if (previous != blocked) markNeedsSemanticsUpdate();
    onBlocked(blocked);
  }

  @override
  void paint(PaintingContext context, Offset offset) {
    if (!blocked) super.paint(context, offset);
  }

  @override
  bool hitTest(BoxHitTestResult result, {required Offset position}) =>
      !blocked && super.hitTest(result, position: position);
  @override
  void visitChildrenForSemantics(RenderObjectVisitor visitor) {
    if (!blocked) super.visitChildrenForSemantics(visitor);
  }

  @override
  void dispose() {
    onRenderer(null);
    super.dispose();
  }
}

class _ListTileLayoutGuard extends SingleChildRenderObjectWidget {
  const _ListTileLayoutGuard({
    required this.riskyUnboundedHeight,
    required this.slotRenderers,
    required this.onMessage,
    this.widgetName = 'ListTile',
    required super.child,
  });
  final bool riskyUnboundedHeight;
  final Map<String, _ListTileSlotRenderBox> slotRenderers;
  final ValueChanged<String?> onMessage;
  final String widgetName;
  @override
  _ListTileLayoutRenderBox createRenderObject(BuildContext context) =>
      _ListTileLayoutRenderBox(
        riskyUnboundedHeight,
        slotRenderers,
        onMessage,
        widgetName,
      );
  @override
  void updateRenderObject(
    BuildContext context,
    _ListTileLayoutRenderBox render,
  ) {
    render.riskyUnboundedHeight = riskyUnboundedHeight;
    render.onMessage = onMessage;
    render.widgetName = widgetName;
    render.clearIntrinsicDiagnostic();
    render.markNeedsLayout();
  }
}

// The public SDK ListTile still owns layout. Only a genuinely unavailable host
// layout is parked at zero constraints; no dimensions are inserted into source.
// Actual post-layout slot offsets decide the finite-paint guard.
class _ListTileLayoutRenderBox extends RenderProxyBox {
  _ListTileLayoutRenderBox(
    this.riskyUnboundedHeight,
    this.slots,
    this.onMessage,
    this.widgetName,
  );
  bool riskyUnboundedHeight;
  final Map<String, _ListTileSlotRenderBox> slots;
  ValueChanged<String?> onMessage;
  String widgetName;
  bool _blocked = false;
  String? _intrinsicMessage;
  void clearIntrinsicDiagnostic() => _intrinsicMessage = null;
  double _finiteIntrinsic(double value) {
    if (value.isFinite) return value;
    // No build, callback, child mutation or layout runs during an intrinsic
    // query. Cache only the reason for the next normal layout's diagnostic.
    _intrinsicMessage =
        'the SDK returned a nonfinite intrinsic dimension; its unavailable extent is projected to zero for this parent';
    return 0;
  }

  @override
  double computeMinIntrinsicWidth(double height) =>
      _finiteIntrinsic(super.computeMinIntrinsicWidth(height));
  @override
  double computeMaxIntrinsicWidth(double height) =>
      _finiteIntrinsic(super.computeMaxIntrinsicWidth(height));
  @override
  double computeMinIntrinsicHeight(double width) =>
      _finiteIntrinsic(super.computeMinIntrinsicHeight(width));
  @override
  double computeMaxIntrinsicHeight(double width) =>
      _finiteIntrinsic(super.computeMaxIntrinsicHeight(width));
  @override
  Size computeDryLayout(BoxConstraints constraints) {
    if (!constraints.hasBoundedWidth) {
      return constraints.constrain(Size.zero);
    }
    final measured = super.computeDryLayout(constraints);
    return measured.isFinite ? measured : constraints.constrain(Size.zero);
  }

  @override
  double? computeDistanceToActualBaseline(TextBaseline baseline) {
    if (_blocked) return null;
    final value = super.computeDistanceToActualBaseline(baseline);
    return value == null || value.isFinite ? value : null;
  }

  @override
  double? computeDryBaseline(
    BoxConstraints constraints,
    TextBaseline baseline,
  ) {
    if (!constraints.hasBoundedWidth) return null;
    final value = super.computeDryBaseline(constraints, baseline);
    return value == null || value.isFinite ? value : null;
  }

  @override
  void performLayout() {
    String? message = _intrinsicMessage;
    if (!constraints.hasBoundedWidth) {
      message =
          'the parent supplies unbounded width; $widgetName requires a bounded horizontal layout';
    } else if (!constraints.hasBoundedHeight && riskyUnboundedHeight) {
      try {
        final measured = child!.getDryLayout(constraints);
        if (!measured.isFinite) {
          message =
              'resolved minTileHeight/minVerticalPadding produce a nonfinite height in this unbounded parent';
        }
      } on FlutterError {
        message =
            'nonfinite height settings cannot be safely preflighted for these mounted children in an unbounded parent';
      }
    }
    final quarantine = message != null;
    child!.layout(
      quarantine
          ? widgetName == 'ExpansionTile'
                // Its SDK Column must still lay out the real header/body at
                // natural height; tight zero height would cause Flex overflow.
                ? const BoxConstraints(maxWidth: 0)
                : BoxConstraints.tight(Size.zero)
          : constraints,
      parentUsesSize: true,
    );
    size = quarantine ? constraints.constrain(Size.zero) : child!.size;
    if (!quarantine) {
      final invalid = slots.entries
          .where((entry) => entry.value.hasInvalidOffset)
          .map((entry) => entry.key)
          .toList();
      if (invalid.isNotEmpty) {
        message =
            'the SDK resolved nonfinite positions for ${invalid.join(', ')} from horizontalTitleGap/minLeadingWidth/minVerticalPadding/minTileHeight';
      }
    }
    final blocked = message != null;
    if (_blocked != blocked) markNeedsSemanticsUpdate();
    _blocked = blocked;
    onMessage(message);
  }

  @override
  void paint(PaintingContext context, Offset offset) {
    if (!_blocked) super.paint(context, offset);
  }

  @override
  bool hitTest(BoxHitTestResult result, {required Offset position}) =>
      !_blocked && super.hitTest(result, position: position);
  @override
  void visitChildrenForSemantics(RenderObjectVisitor visitor) {
    if (!_blocked) super.visitChildrenForSemantics(visitor);
  }
}

class _CanvasRadioGroupScope extends InheritedWidget {
  const _CanvasRadioGroupScope({
    required this.typeKey,
    required this.registry,
    required this.unavailableReason,
    required this.parent,
    required super.child,
  });
  final String? typeKey;
  final Object? registry;
  final String? unavailableReason;
  final _CanvasRadioGroupScope? parent;

  @override
  bool updateShouldNotify(_CanvasRadioGroupScope oldWidget) => true;
}

String? _radioGroupStaticMessage(CanvasNode node) {
  final unknown = ['valueType', 'groupValue']
      .where(
        (name) => node.properties[name]?.kind == 'dartObjectReferencePresence',
      )
      .toList();
  if (unknown.isNotEmpty) {
    return 'Render RadioGroup ${node.id}: typed selection, navigation and group semantics preview unavailable for ${unknown.join(', ')}. '
        'Isolated Canvas cannot execute project types, values or equality. The editable child is retained; stored values and generated Dart are unchanged.';
  }
  if (node.properties['onChanged']?.kind == 'dartObjectReferencePresence') {
    return 'RadioGroup ${node.id} preview limitation for onChanged: isolated Canvas never executes the project callback. '
        'The actual SDK group uses a benign controlled callback; groupValue and generated Dart are unchanged.';
  }
  return null;
}

String _bottomSheetPreviewMessage(CanvasNode node) {
  final sources = node.properties.entries.where((e) => e.value.kind == 'dartObjectReferencePresence').map((e) => e.key).toList();
  return 'BottomSheet ${node.id}: caller owns routes, dismissal and controller disposal. '
      'Canvas uses its own preview controller, never executes project builders/events, and keeps the design visible. '
      '${sources.isEmpty ? '' : 'Project sources unavailable: ${sources.join(', ')}; native defaults are used for source-owned appearance.'}';
}

String _snackBarPreviewMessage(CanvasNode node) {
  final sources = node.properties.entries.where((e)=>e.value.kind=='dartObjectReferencePresence').map((e)=>e.key).toList();
  for (final action in node.slot('action')?.children ?? <CanvasNode>[]) {
    sources.addAll(action.properties.entries.where((e)=>e.value.kind=='dartObjectReferencePresence').map((e)=>'Action.${e.key}'));
  }
  return '${node.type.split('.').last} ${node.id}: caller owns ScaffoldMessenger presentation, timeouts and closed results. '
      'Canvas never executes project sources; activation/dismissal are blocked and the animation stays completed. '
      '${sources.isEmpty ? '' : 'Project preview unavailable for: ${sources.join(', ')}; appearance uses native defaults and source labels use a placeholder.'}';
}

String _simpleDialogPreviewMessage(CanvasNode node) {
  final sources = node.properties.entries.where((e) => e.value.kind == 'dartObjectReferencePresence').map((e) => e.key).toList();
  return '${node.type == canvasSimpleDialogType ? 'SimpleDialog' : 'SimpleDialogOption'} ${node.id}: '
      'the caller owns showDialog and Navigator.pop results. Canvas never executes event handlers or project sources.'
      '${sources.isEmpty ? '' : ' Native preview defaults replace project sources for: ${sources.join(', ')}.'}';
}

String? _radioGroupPreviewMessage(CanvasNode node, BuildContext? context) {
  _RadioGroupPreviewState? state;
  void visit(Element element) {
    if (element is StatefulElement &&
        element.state is _RadioGroupPreviewState &&
        (element.state as _RadioGroupPreviewState).widget.node.id == node.id) {
      state = element.state as _RadioGroupPreviewState;
      return;
    }
    if (state == null) element.visitChildElements(visit);
  }

  context?.visitChildElements(visit);
  return state?.message ?? _radioGroupStaticMessage(node);
}

// Every registered SDK client is observed, including disabled/Offstage/retained
// Visibility clients. No stored-tree visibility guess or private SDK State is used.
class _RadioGroupForwardingRegistry<T> {
  _RadioGroupForwardingRegistry(this.changed);
  final VoidCallback changed;
  RadioGroupRegistry<T>? _delegate;
  Object? get groupValue => _delegate?.groupValue;
  final _clients = <RadioClient<T>, String>{};
  final _forwarded = <RadioClient<T>>{};
  final _members = <String, _RadioGroupMemberRegistry<T>>{};
  Set<String> conflicts = {};
  bool semanticsBlocked = false;
  _RenderRadioGroupSemanticsGate? gate;
  bool _disposed = false;

  RadioGroupRegistry<T> forRadio(String id) =>
      _members.putIfAbsent(id, () => _RadioGroupMemberRegistry(this, id));

  void attach(RadioGroupRegistry<T> delegate) {
    if (!identical(_delegate, delegate)) {
      for (final client in _forwarded) {
        _delegate?.unregisterClient(client);
      }
      _forwarded.clear();
      _delegate = delegate;
    }
    _reconcile();
  }

  void register(RadioClient<T> client, String id) {
    if (_disposed) return;
    _clients[client] = id;
    _reconcile();
  }

  void unregister(RadioClient<T> client) {
    final id = _clients.remove(client);
    if (_forwarded.remove(client)) _delegate?.unregisterClient(client);
    if (id != null && !_clients.containsValue(id)) _members.remove(id);
    if (!_disposed) _reconcile();
  }

  void _reconcile() {
    final delegate = _delegate;
    if (delegate == null || _disposed) return;
    final selected = _clients.keys
        .where((client) => client.radioValue == delegate.groupValue)
        .toSet();
    final blocked = selected.length > 1 ? selected : <RadioClient<T>>{};
    conflicts = blocked.map((client) => _clients[client]!).toSet();
    for (final client in _clients.keys) {
      if (blocked.contains(client)) {
        if (_forwarded.remove(client)) delegate.unregisterClient(client);
      } else if (_forwarded.add(client)) {
        delegate.registerClient(client);
      }
    }
    gate?.markNeedsSemanticsUpdate();
    changed();
  }

  void dispose() {
    _disposed = true;
    for (final client in _forwarded) {
      _delegate?.unregisterClient(client);
    }
    _forwarded.clear();
    _clients.clear();
    _members.clear();
    _delegate = null;
    gate = null;
  }
}

// RadioListTile registers itself and hides its inner Radio. Observe only that
// public RadioClient, synchronously after descendant mount/rebuild, before the
// SDK's queued post-frame duplicate-selection assertion. No proxy client or
// additional RadioGroup is created and no private State fields are inspected.
class _RadioListTileRegistration<T> extends StatelessWidget {
  const _RadioListTileRegistration({
    required this.registry,
    required this.child,
  });
  final RadioGroupRegistry<T> registry;
  final Widget child;
  @override
  Widget build(BuildContext context) {
    // Mirror every direct inherited dependency of the pinned SDK tile. Its
    // didChangeDependencies resets the public registry even for a theme-only
    // update with a cached child widget, without a Canvas model replacement.
    RadioGroup.maybeOf<T>(context);
    ListTileTheme.of(context);
    Theme.of(context);
    RadioTheme.of(context);
    return child;
  }

  @override
  StatelessElement createElement() =>
      _RadioListTileRegistrationElement<T>(this);
}

class _RadioListTileRegistrationElement<T> extends StatelessElement {
  _RadioListTileRegistrationElement(_RadioListTileRegistration<T> super.widget);
  @override
  void performRebuild() {
    super.performRebuild();
    final owner = widget as _RadioListTileRegistration<T>;
    visitChildren((child) {
      if (child is StatefulElement &&
          child.widget is RadioListTile<T> &&
          child.state is RadioClient<T>) {
        // updateChild can skip an identical cached widget while this SDK
        // element is independently dirty from the same inherited update.
        // Flush that queued didChangeDependencies before restoring membership.
        child.rebuild();
        (child.state as RadioClient<T>).registry = owner.registry;
      } else {
        throw StateError(
          'RadioListTile registry observer requires its direct SDK RadioClient child.',
        );
      }
    });
  }
}

class _RadioGroupMemberRegistry<T> extends RadioGroupRegistry<T> {
  _RadioGroupMemberRegistry(this.owner, this.id);
  final _RadioGroupForwardingRegistry<T> owner;
  final String id;
  @override
  T? get groupValue => owner._delegate?.groupValue;
  @override
  ValueChanged<T?> get onChanged => owner._delegate?.onChanged ?? (_) {};
  @override
  void registerClient(RadioClient<T> client) => owner.register(client, id);
  @override
  void unregisterClient(RadioClient<T> client) => owner.unregister(client);
}

class _RadioGroupSemanticsGate<T> extends SingleChildRenderObjectWidget {
  const _RadioGroupSemanticsGate({
    required this.registry,
    required super.child,
  });
  final _RadioGroupForwardingRegistry<T> registry;
  @override
  _RenderRadioGroupSemanticsGate createRenderObject(BuildContext context) {
    final render = _RenderRadioGroupSemanticsGate((blocked) {
      registry.semanticsBlocked = blocked;
      registry.changed();
    }, () => registry.gate = null);
    registry.gate = render;
    return render;
  }

  @override
  void updateRenderObject(
    BuildContext context,
    _RenderRadioGroupSemanticsGate renderObject,
  ) {
    registry.gate = renderObject;
    renderObject.markNeedsSemanticsUpdate();
  }
}

// This boundary checks the assembled public semantics tree, independently from
// typed registry membership. The SDK role validator is type-blind and ignores
// subtrees rooted at another radioGroup role. Mirror that exact rule, not values.
class _RenderRadioGroupSemanticsGate extends RenderProxyBox {
  _RenderRadioGroupSemanticsGate(this.changed, this.disposed);
  final ValueChanged<bool> changed;
  final VoidCallback disposed;
  @override
  void describeSemanticsConfiguration(SemanticsConfiguration config) {
    super.describeSemanticsConfiguration(config);
    config.isSemanticBoundary = true;
  }

  bool _invalid(SemanticsNode node) {
    if (node.getSemanticsData().role == ui.SemanticsRole.radioGroup) {
      var checked = 0;
      bool visit(SemanticsNode child) {
        final data = child.getSemanticsData();
        if (data.role == ui.SemanticsRole.radioGroup) return true;
        if (data.flagsCollection.isInMutuallyExclusiveGroup) {
          if (data.flagsCollection.isChecked == ui.CheckedState.isTrue) {
            checked++;
          }
        } else {
          child.visitChildren(visit);
        }
        return true;
      }

      node.visitChildren(visit);
      return checked > 1;
    }
    var invalid = false;
    node.visitChildren((child) {
      invalid |= _invalid(child);
      return true;
    });
    return invalid;
  }

  @override
  void assembleSemanticsNode(
    SemanticsNode node,
    SemanticsConfiguration config,
    Iterable<SemanticsNode> children,
  ) {
    final blocked = children.any(_invalid);
    changed(blocked);
    node.updateWith(
      config: config,
      childrenInInversePaintOrder: blocked ? const [] : children.toList(),
    );
  }

  @override
  void dispose() {
    disposed();
    super.dispose();
  }
}

class _RadioGroupPreview extends StatefulWidget {
  const _RadioGroupPreview({required this.node, required this.child});
  final CanvasNode node;
  final Widget child;
  @override
  State<_RadioGroupPreview> createState() => _RadioGroupPreviewState();
}

class _RadioGroupPreviewState extends State<_RadioGroupPreview> {
  final _childKey = GlobalKey();
  String? _dynamicMessage;
  String? get message =>
      _dynamicMessage ?? _radioGroupStaticMessage(widget.node);

  @override
  void didUpdateWidget(_RadioGroupPreview oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (_radioTypeKey(oldWidget.node) != _radioTypeKey(widget.node)) {
      _dynamicMessage = null;
    }
  }

  void _messageChanged(String? value) {
    if (!mounted || _dynamicMessage == value) return;
    setState(() => _dynamicMessage = value);
    context
        .findAncestorStateOfType<_CanvasDocumentViewState>()
        ?._refreshZeroSizedWidgetTargetsAfterFrame();
  }

  @override
  Widget build(BuildContext context) {
    final node = widget.node;
    final child = KeyedSubtree(key: _childKey, child: widget.child);
    final type = _radioTypeKey(node);
    final unknown =
        type == null ||
        node.properties['groupValue']?.kind == 'dartObjectReferencePresence';
    if (unknown) {
      _dynamicMessage = null;
      return _TextButtonPreview(
        message: _radioGroupStaticMessage(node)!,
        child: _CanvasRadioGroupScope(
          typeKey: type,
          registry: null,
          unavailableReason: _radioGroupStaticMessage(node),
          parent: context
              .dependOnInheritedWidgetOfExactType<_CanvasRadioGroupScope>(),
          child: ExcludeSemantics(child: child),
        ),
      );
    }
    Widget host<T>() => _RadioGroupHost<T>(
      node: node,
      messageChanged: _messageChanged,
      child: child,
    );
    return switch (type) {
      'String' => host<String>(),
      'String?' => host<String?>(),
      'int' => host<int>(),
      'int?' => host<int?>(),
      'double' => host<double>(),
      'double?' => host<double?>(),
      'num' => host<num>(),
      'num?' => host<num?>(),
      'bool' => host<bool>(),
      'bool?' => host<bool?>(),
      'Object?' => host<Object?>(),
      _ => host<Object>(),
    };
  }
}

class _RadioGroupHost<T> extends StatefulWidget {
  const _RadioGroupHost({
    required this.node,
    required this.child,
    required this.messageChanged,
  });
  final CanvasNode node;
  final Widget child;
  final ValueChanged<String?> messageChanged;
  @override
  State<_RadioGroupHost<T>> createState() => _RadioGroupHostState<T>();
}

class _RadioGroupHostState<T> extends State<_RadioGroupHost<T>> {
  late final _registry = _RadioGroupForwardingRegistry<T>(_scheduleMessage);
  bool _scheduled = false;
  String? _message;
  void _scheduleMessage() {
    if (_scheduled) return;
    _scheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _scheduled = false;
      if (!mounted) return;
      final ids = _registry.conflicts.toList()..sort();
      final parts = <String>[
        if (ids.isNotEmpty)
          'RadioGroup ${widget.node.id}: group navigation preview unavailable because mounted Radio clients ${ids.join(', ')} equal groupValue. Disabled and offstage clients count in the SDK registry. Conflicting clients are withheld only from group navigation; their controlled appearance is retained.',
        if (_registry.semanticsBlocked)
          'RadioGroup ${widget.node.id}: group semantics preview unavailable because the actual semantics subtree contains multiple checked mutually-exclusive controls. The invalid group semantics subtree is withheld; visual children and their state remain.',
        if (ids.isNotEmpty || _registry.semanticsBlocked)
          'Stored values and generated Dart are unchanged. Normal SDK navigation/semantics recover automatically when the conflict is resolved.',
        if (_radioGroupStaticMessage(widget.node) case final String message)
          message,
      ];
      final message = parts.isEmpty ? null : parts.join(' ');
      if (_message != message) {
        setState(() => _message = message);
        widget.messageChanged(message);
      }
    });
  }

  @override
  void dispose() {
    _registry.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final parent = context
        .dependOnInheritedWidgetOfExactType<_CanvasRadioGroupScope>();
    return _TextButtonPreview(
      message: _message ?? _radioGroupStaticMessage(widget.node) ?? '',
      child: _RadioGroupSemanticsGate<T>(
        registry: _registry,
        child: RadioGroup<T>(
          groupValue: _radioIdentityValue(widget.node, 'groupValue') as T?,
          onChanged: (_) {},
          child: Builder(
            builder: (context) {
              _registry.attach(RadioGroup.maybeOf<T>(context)!);
              return _CanvasRadioGroupScope(
                typeKey: _radioTypeKey(widget.node),
                registry: _registry,
                unavailableReason: null,
                parent: parent,
                child: widget.child,
              );
            },
          ),
        ),
      ),
    );
  }
}

/// Removes Designer-only cursor/opaque annotations inside an application
/// MouseRegion, so the real SDK mouse tracking and cursor ancestry stay exact.
class _CanvasMouseRegionCursorScope extends InheritedWidget {
  const _CanvasMouseRegionCursorScope({required super.child});
  static bool active(BuildContext context) =>
      context
          .dependOnInheritedWidgetOfExactType<
            _CanvasMouseRegionCursorScope
          >() !=
      null;
  @override
  bool updateShouldNotify(_CanvasMouseRegionCursorScope oldWidget) => false;
}

String? _mouseRegionPreviewMessage(CanvasNode node) =>
    node.type == 'flutter.widgets.MouseRegion' &&
        node.properties['cursor']?.kind == 'dartObjectReferencePresence'
    ? 'MouseRegion.cursor preview unavailable: the project MouseCursor reference '
          'is not executed in Canvas. MouseCursor.defer is used as an explicit '
          'preview approximation; saved properties and generated Dart stay exact.'
    : null;

String? _focusPreviewMessage(CanvasNode node) {
  final references = ['focusNode', 'parentNode']
      .where(
        (name) => node.properties[name]?.kind == 'dartObjectReferencePresence',
      )
      .toList();
  if (references.isEmpty) return null;
  final external = node.properties['variant']?.value == 'withExternalFocusNode';
  return 'Focus ${node.id} preview limitation for ${references.join(', ')}: '
      'isolated Canvas never executes project FocusNode references or factories. '
      '${references.contains('focusNode') ? 'A Canvas-owned FocusNode with SDK defaults approximates the unknown project node. ' : ''}'
      '${references.contains('parentNode') ? 'The nearest preview focus ancestor is used instead of the unknown project parentNode. ' : ''}'
      '${external ? 'The external constructor ignores stored widget-owned key callbacks, focus flags and debugLabel, exactly as generated Dart does. ' : ''}'
      'Application callbacks are not executed; local key callbacks return KeyEventResult.ignored. '
      'Saved properties and generated Dart stay exact.';
}

/// Project-owned FocusNode instances cannot cross the isolated Canvas boundary.
/// This owner supplies a stable, locally disposed approximation only when a
/// reference is present. Otherwise the real Focus widget owns its SDK node.
class _CanvasFocusPreview extends StatefulWidget {
  const _CanvasFocusPreview({required this.node, required this.child});
  final CanvasNode node;
  final Widget child;
  @override
  State<_CanvasFocusPreview> createState() => _CanvasFocusPreviewState();
}

class _CanvasFocusPreviewState extends State<_CanvasFocusPreview> {
  final _localNode = FocusNode(
    debugLabel: 'Isolated Canvas FocusNode approximation',
  );
  final _childKey = GlobalKey();

  @override
  void didUpdateWidget(_CanvasFocusPreview oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.node.properties['variant']?.value !=
            'withExternalFocusNode' &&
        widget.node.properties['variant']?.value == 'withExternalFocusNode') {
      // A prior standard Focus may have written its attributes to this local
      // node. The new external branch cannot inherit those inactive fields.
      _localNode
        ..canRequestFocus = true
        ..skipTraversal = false
        ..descendantsAreFocusable = true
        ..descendantsAreTraversable = true
        ..debugLabel = 'Isolated Canvas FocusNode approximation'
        ..onKeyEvent = null;
      // ignore: deprecated_member_use
      _localNode.onKey = null;
    }
  }

  @override
  void dispose() {
    _localNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final node = widget.node;
    bool? boolean(String name) => node.properties[name]?.value is bool
        ? node.properties[name]!.value as bool
        : null;
    bool callback(String name) =>
        node.properties[name]?.kind == 'callbackPresence' ||
        node.properties[name]?.kind == 'dartObjectReferencePresence';
    final child = KeyedSubtree(key: _childKey, child: widget.child);
    final autofocus = boolean('autofocus') ?? false;
    final includeSemantics = boolean('includeSemantics') ?? true;
    final ValueChanged<bool>? onFocusChange = callback('onFocusChange')
        ? (_) {}
        : null;
    final external =
        node.properties['variant']?.value == 'withExternalFocusNode';
    final Widget focus;
    if (external) {
      focus = Focus.withExternalFocusNode(
        key: ValueKey('canvas-focus-${node.id}'),
        focusNode: _localNode,
        autofocus: autofocus,
        onFocusChange: onFocusChange,
        includeSemantics: includeSemantics,
        child: child,
      );
    } else {
      focus = Focus(
        key: ValueKey('canvas-focus-${node.id}'),
        focusNode:
            node.properties['focusNode']?.kind == 'dartObjectReferencePresence'
            ? _localNode
            : null,
        autofocus: autofocus,
        onFocusChange: onFocusChange,
        onKeyEvent: callback('onKeyEvent')
            ? (_, _) => KeyEventResult.ignored
            : null,
        // ignore: deprecated_member_use
        onKey: callback('onKey') ? (_, _) => KeyEventResult.ignored : null,
        canRequestFocus: boolean('canRequestFocus'),
        skipTraversal: boolean('skipTraversal'),
        descendantsAreFocusable: boolean('descendantsAreFocusable'),
        descendantsAreTraversable: boolean('descendantsAreTraversable'),
        includeSemantics: includeSemantics,
        debugLabel: node.properties['debugLabel']?.value as String?,
        child: child,
      );
    }
    return _TextButtonPreview(
      message: _focusPreviewMessage(node) ?? '',
      child: focus,
    );
  }
}

String? _sliverPaddingPreviewMessage(CanvasNode node) =>
    node.type == 'flutter.widgets.SliverPadding' &&
        node.properties['padding']?.kind == 'dartObjectReferencePresence'
    ? 'SliverPadding preview limitation: project padding is not executed. Zero padding is an explicit approximation; the nested sliver is retained. Generated Dart uses the exact project geometry. Run the app to verify insets.'
    : null;

String? _viewportSliverPreviewMessage(CanvasNode node) {
  if (node.type != 'flutter.widgets.SliverFillViewport' && node.type != 'flutter.widgets.SliverFillViewport.delegate') return null;
  if (node.properties['delegate']?.kind == 'dartObjectReferencePresence') {
    return 'SliverFillViewport preview limitation: project delegate is not executed; its children are not previewed. Generated Dart retains the exact list, builder or custom delegate. Run the app to verify content.';
  }
  if (node.properties['semanticIndexCallback']?.kind == 'dartObjectReferencePresence') {
    return 'SliverFillViewport preview limitation: project semantic index callback is not executed. Native local indexes are a preview approximation; all visual children and the generated project reference are retained.';
  }
  return null;
}

String? _dynamicSliverPreviewMessage(CanvasNode node) {
  if (!isCanvasDynamicSliverType(node.type)) return null;
  final refs = node.properties.entries.where((e) => e.value.kind == 'dartObjectReferencePresence').map((e) => e.key).toList();
  if (refs.isEmpty) return null;
  if (isCanvasPrototypeSliverType(node.type)) {
    return 'SliverPrototypeExtentList preview limitation: ${refs.join(', ')} project code is not executed. '
        'Project builder/delegate items are not previewed; the prototype is measured but not painted. '
        'Generated Dart retains the exact references. Run the app to verify dynamic content and key-index mapping.';
  }
  if (node.type.startsWith('flutter.widgets.SliverVariedExtentList')) {
    return 'SliverVariedExtentList preview limitation: ${refs.join(', ')} project code is not executed. '
        'Project item builders/delegates are not previewed. For a project extent callback, natural child sizing is an explicit approximation; no callback result is fabricated. '
        'Generated Dart retains the exact typed references. Run the app to verify per-index sizes and key-index mapping.';
  }
  if (node.type.startsWith('flutter.widgets.SliverFixedExtentList')) {
    return 'SliverFixedExtentList preview limitation: ${refs.join(', ')} project code is not executed. '
        'Project builder/delegate items are not previewed; item extent is preserved. '
        'Generated Dart retains the exact references. Run the app to verify dynamic content and key-index mapping.';
  }
  return 'Sliver preview limitation: ${refs.join(', ')} project code is not executed. '
      'Builder/delegate items are not previewed; custom grid geometry uses an explicit 2-column approximation. '
      'Generated Dart keeps the exact project references. Run the application to verify dynamic content.';
}

String? _listViewExtentPreviewMessage(CanvasNode node) =>
    node.properties['itemExtentBuilder']?.kind == 'dartObjectReferencePresence'
    ? 'ListView item-extent preview limitation: isolated Canvas does not execute project or dependency Dart. '
          'The configured callback may be null; its per-index sizes and null out-of-range result are unknown. '
          'Natural child sizing with every configured child retained is an explicit approximation, not the project result. '
          'No constant extent callback is fabricated; stored values and generated Dart are unchanged.'
    : null;

String? _listWheelPreviewMessage(CanvasNode node) {
  final refs = <String>[
    if (node.properties['controller']?.kind == 'dartObjectReferencePresence')
      'controller',
    if (node.properties['scrollBehavior']?.kind ==
        'dartObjectReferencePresence')
      'scrollBehavior',
    if (node.properties['onSelectedItemChanged']?.kind ==
        'dartObjectReferencePresence')
      'onSelectedItemChanged',
  ];
  if (refs.isEmpty) return null;
  return 'ListWheelScrollView project ${refs.join(', ')} reference${refs.length == 1 ? '' : 's'} '
      'are not executed in isolated Canvas; native preview defaults are used. '
      'Stored values and generated Dart are preserved.';
}

String? _textFieldBuilderPreviewMessage(CanvasNode node) {
  final counter =
      node.properties['buildCounter']?.kind == 'dartObjectReferencePresence';
  final menu =
      node.properties['contextMenuBuilder']?.kind ==
      'dartObjectReferencePresence';
  if (!counter && !menu) return null;
  return 'TextField builder preview limitation: isolated Canvas does not execute project or dependency Dart. '
      '${counter ? 'The configured counter callback may be null or return null or different content; its result is unknown. The actual SDK default counter is an explicit approximation, not the project result. ' : ''}'
      '${menu ? 'The configured context-menu callback may be null or return different content; its result is unknown. The actual SDK platform menu is an explicit approximation, not the project result. ' : ''}'
      'The existing noninteractive Designer focus/input guard, stored values and generated Dart are preserved.';
}

String? _appBarPredicatePreviewMessage(CanvasNode node) =>
    node.properties['notificationPredicate']?.kind ==
        'dartObjectReferencePresence'
    ? 'Custom AppBar scroll predicate preview unavailable. Generated Dart uses '
          'the configured ScrollNotificationPredicate; isolated Canvas does '
          'not execute project or dependency Dart. The actual SDK depth-zero '
          'predicate is retained as an explicit approximation for scrolled-under '
          'elevation. This predicate does not consume or stop notifications.'
    : null;

String? _navigationBarPreviewMessage(CanvasNode node) {
  final count = node.slot('destinations')?.children.length ?? 0;
  final selected = node.properties['selectedIndex']?.value;
  final invalidIndex = selected is int && (selected < 0 || selected >= count);
  final unavailable = <String>[
    if (node.properties['indicatorShape']?.kind ==
        'dartObjectReferencePresence')
      'indicatorShape',
    if (node.properties['overlayColor']?.kind == 'dartObjectReferencePresence')
      'overlayColor',
    if (node.properties['labelTextStyle']?.kind ==
        'dartObjectReferencePresence')
      'labelTextStyle',
    if (node.properties['labelPadding']?.kind == 'dartObjectReferencePresence')
      'labelPadding',
  ];
  final messages = <String>[
    if (count < 2)
      'NavigationBar requires at least two destination widgets; add them in the Slots tab before the generated app is run.',
    if (invalidIndex)
      'NavigationBar selectedIndex is outside the current destination list; Canvas previews destination 0 without changing the stored value.',
    if (unavailable.isNotEmpty)
      'Canvas does not execute project references for ${unavailable.join(', ')}; the SDK/theme fallback is shown while the exact source values are retained.',
  ];
  return messages.isEmpty
      ? null
      : '${messages.join(' ')} Stored values, child identities and generated Dart remain unchanged.';
}

String? _navigationRailPreviewMessage(CanvasNode node) {
  final destinations = node.slot('destinations')?.children.length ?? 0;
  final selected = node.properties['selectedIndex']?.value;
  final invalidIndex =
      selected is int && (selected < 0 || selected >= destinations);
  final extended = node.properties['extended']?.value == true;
  final labelType = node.properties['labelType']?.value;
  final labelConflict = extended && labelType is String && labelType != 'none';
  final minWidth = node.properties['minWidth']?.value;
  final minExtendedWidth = node.properties['minExtendedWidth']?.value;
  final widthConflict =
      minWidth is num && minExtendedWidth is num && minExtendedWidth < minWidth;
  final unavailable = <String>[
    if (node.properties['unselectedLabelTextStyle']?.kind ==
        'dartObjectReferencePresence')
      'unselectedLabelTextStyle',
    if (node.properties['selectedLabelTextStyle']?.kind ==
        'dartObjectReferencePresence')
      'selectedLabelTextStyle',
    if (node.properties['unselectedIconTheme']?.kind ==
        'dartObjectReferencePresence')
      'unselectedIconTheme',
    if (node.properties['selectedIconTheme']?.kind ==
        'dartObjectReferencePresence')
      'selectedIconTheme',
    if (node.properties['indicatorShape']?.kind ==
        'dartObjectReferencePresence')
      'indicatorShape',
  ];
  final messages = <String>[
    if (destinations > 0)
      'Canvas adapts destination slot widgets as NavigationRailDestination icons with synthetic labels; application-owned NavigationRailDestination fields remain in generated Dart.',
    if (invalidIndex)
      'NavigationRail selectedIndex is outside the current destination list; Canvas previews no selection without changing the stored value.',
    if (labelConflict)
      'NavigationRail extended mode requires labelType null or none; Canvas previews the rail collapsed without changing either stored value.',
    if (widthConflict)
      'NavigationRail minExtendedWidth must be at least minWidth; Canvas omits the invalid extended width without changing stored values.',
    if (unavailable.isNotEmpty)
      'Canvas does not execute project references for ${unavailable.join(', ')}; the SDK/theme fallback is shown while the exact source values are retained.',
  ];
  return messages.isEmpty
      ? null
      : '${messages.join(' ')} Stored values, child identities and generated Dart remain unchanged.';
}

String? _navigationDrawerPreviewMessage(CanvasNode node) {
  final children = node.slot('children')?.children.length ?? 0;
  final selected = node.properties['selectedIndex']?.value;
  final invalidIndex =
      selected is int && (selected < 0 || selected >= children);
  final unavailable = <String>[
    if (node.properties['indicatorShape']?.kind ==
        'dartObjectReferencePresence')
      'indicatorShape',
  ];
  final messages = <String>[
    if (children > 0)
      'Canvas adapts destination slot widgets as NavigationDrawerDestination entries with synthetic labels; application-owned destination fields remain in generated Dart.',
    if (invalidIndex)
      'NavigationDrawer selectedIndex is outside the current destination list; Canvas previews no selection without changing the stored value.',
    if (unavailable.isNotEmpty)
      'Canvas does not execute project references for ${unavailable.join(', ')}; the SDK/theme fallback is shown while the exact source values are retained.',
  ];
  return messages.isEmpty
      ? null
      : '${messages.join(' ')} Stored values, child identities and generated Dart remain unchanged.';
}

String? _drawerPreviewMessage(CanvasNode node) {
  final unavailable = <String>[
    if (node.properties['shape']?.kind == 'dartObjectReferencePresence')
      'shape',
  ];
  return unavailable.isEmpty
      ? null
      : 'Canvas does not execute project references for ${unavailable.join(', ')}; '
            'the Drawer theme/SDK fallback is shown while the exact source values '
            'are retained. Stored values, child identity and generated Dart remain unchanged.';
}

String? _bottomAppBarPreviewMessage(CanvasNode node) {
  final unavailable = <String>[
    if (node.properties['shape']?.kind == 'dartObjectReferencePresence')
      'shape',
  ];
  return unavailable.isEmpty
      ? null
      : 'Canvas does not execute project references for ${unavailable.join(', ')}; '
            'the BottomAppBar SDK/theme rectangular fallback is shown while '
            'the exact source values are retained. Stored values, child identity '
            'and generated Dart remain unchanged.';
}

String? _bottomNavigationBarPreviewMessage(CanvasNode node) {
  final items = node.slot('items')?.children.length ?? 0;
  final selected = node.properties['currentIndex']?.value;
  final invalidIndex = selected is int && (selected < 0 || selected >= items);
  final unavailable = <String>[
    if (node.properties['selectedIconTheme']?.kind ==
        'dartObjectReferencePresence')
      'selectedIconTheme',
    if (node.properties['unselectedIconTheme']?.kind ==
        'dartObjectReferencePresence')
      'unselectedIconTheme',
    if (node.properties['selectedLabelStyle']?.kind ==
        'dartObjectReferencePresence')
      'selectedLabelStyle',
    if (node.properties['unselectedLabelStyle']?.kind ==
        'dartObjectReferencePresence')
      'unselectedLabelStyle',
    if (node.properties['mouseCursor']?.kind == 'dartObjectReferencePresence')
      'mouseCursor',
  ];
  final messages = <String>[
    if (items < 2)
      'BottomNavigationBar requires at least two item widgets; add them in the Slots tab before the generated app is run.',
    if (invalidIndex)
      'BottomNavigationBar currentIndex is outside the current item list; Canvas previews item 0 without changing the stored value.',
    if (unavailable.isNotEmpty)
      'Canvas does not execute project references for ${unavailable.join(', ')}; the SDK/theme fallback is shown while the exact source values are retained.',
  ];
  return messages.isEmpty
      ? null
      : '${messages.join(' ')} Stored values, child identities and generated Dart remain unchanged.';
}

String? _materialPreviewMessage(CanvasNode node) {
  final unavailable = <String>[
    if (node.properties['textStyle']?.kind == 'dartObjectReferencePresence')
      'textStyle',
    if (node.properties['shape']?.kind == 'dartObjectReferencePresence')
      'shape',
    if (node.properties['borderRadius']?.kind == 'dartObjectReferencePresence')
      'borderRadius',
  ];
  final typeValue = node.properties['materialType']?.value;
  final type = typeValue is CanvasEnumValue ? typeValue.value : typeValue;
  final circle = type == 'circle';
  final shapePresent =
      node.properties['shape']?.kind != null &&
      node.properties['shape']?.kind != 'null';
  final radiusPresent =
      node.properties['borderRadius']?.kind != null &&
      node.properties['borderRadius']?.kind != 'null';
  final conflicts = <String>[
    if (shapePresent && radiusPresent)
      'shape and borderRadius are mutually exclusive',
    if (circle && (shapePresent || radiusPresent))
      'circle Material cannot use shape or borderRadius',
  ];
  final messages = <String>[
    if (unavailable.isNotEmpty)
      'Canvas does not execute project references for ${unavailable.join(', ')}; the SDK/theme fallback is shown while the exact source values are retained.',
    if (conflicts.isNotEmpty)
      'Material has an SDK constructor conflict: ${conflicts.join('; ')}. Canvas omits the conflicting shape value without changing the stored model.',
  ];
  return messages.isEmpty
      ? null
      : '${messages.join(' ')} Stored values, child identity and generated Dart remain unchanged.';
}

String? _scrollbarPreviewMessage(CanvasNode node) {
  final unavailable = <String>[
    if (node.properties['controller']?.kind == 'dartObjectReferencePresence')
      'controller',
    if (node.properties['radius']?.kind == 'dartObjectReferencePresence')
      'radius',
    if (node.properties['notificationPredicate']?.kind ==
        'dartObjectReferencePresence')
      'notificationPredicate',
  ];
  if (unavailable.isEmpty) return null;
  return 'Canvas does not execute project references for ${unavailable.join(', ')}; '
      'the Scrollbar SDK/theme fallback is shown while the exact source values are retained. '
      'Stored child identity and generated Dart remain unchanged.';
}

String? _scaffoldScrimPreviewMessage(CanvasNode node) =>
    node.properties['bottomSheetScrimBuilder']?.kind ==
        'dartObjectReferencePresence'
    ? 'Scaffold ${node.id} bottom-sheet scrim preview unavailable: Canvas never '
          'executes the project bottomSheetScrimBuilder. The actual SDK default '
          'scrim is retained as an explicit preview approximation. A project '
          'builder may return a different widget or null; generated Dart retains '
          'that exact reference and behavior.'
    : null;

String? _notificationListenerPreviewMessage(CanvasNode node) =>
    node.properties['notificationType']?.kind == 'dartObjectReferencePresence'
    ? 'NotificationListener ${node.id} type preview unavailable: Canvas cannot '
          'load or execute the project Notification subtype. Notification is '
          'used as an explicit preview approximation; exact subtype filtering '
          'and application callbacks require the generated application. '
          'Local callbacks return false and never stop notification bubbling. '
          'Saved properties and generated Dart retain the exact project type.'
    : null;

/// A type change replaces the SDK element but preserves the keyed model child.
class _CanvasNotificationListenerPreview extends StatefulWidget {
  const _CanvasNotificationListenerPreview({
    required this.node,
    required this.child,
  });
  final CanvasNode node;
  final Widget child;
  @override
  State<_CanvasNotificationListenerPreview> createState() =>
      _CanvasNotificationListenerPreviewState();
}

class _CanvasNotificationListenerPreviewState
    extends State<_CanvasNotificationListenerPreview> {
  final _childKey = GlobalKey();

  @override
  Widget build(BuildContext context) {
    final node = widget.node;
    final callbackKind = node.properties['onNotification']?.kind;
    final present =
        callbackKind == 'callbackPresence' ||
        callbackKind == 'dartObjectReferencePresence';
    final child = KeyedSubtree(key: _childKey, child: widget.child);
    Widget listener<T extends Notification>() => NotificationListener<T>(
      key: ValueKey('canvas-notification-listener-${node.id}'),
      onNotification: present ? (_) => false : null,
      child: child,
    );
    final listenerWidget = switch (node.properties['notificationType']?.value) {
      'LayoutChangedNotification' => listener<LayoutChangedNotification>(),
      'ScrollNotification' => listener<ScrollNotification>(),
      'ScrollStartNotification' => listener<ScrollStartNotification>(),
      'ScrollUpdateNotification' => listener<ScrollUpdateNotification>(),
      'OverscrollNotification' => listener<OverscrollNotification>(),
      'ScrollEndNotification' => listener<ScrollEndNotification>(),
      'UserScrollNotification' => listener<UserScrollNotification>(),
      'SizeChangedLayoutNotification' =>
        listener<SizeChangedLayoutNotification>(),
      'ScrollMetricsNotification' => listener<ScrollMetricsNotification>(),
      'OverscrollIndicatorNotification' =>
        listener<OverscrollIndicatorNotification>(),
      'DraggableScrollableNotification' =>
        listener<DraggableScrollableNotification>(),
      'KeepAliveNotification' => listener<KeepAliveNotification>(),
      'NavigationNotification' => listener<NavigationNotification>(),
      _ => listener<Notification>(),
    };
    return _TextButtonPreview(
      message: _notificationListenerPreviewMessage(node) ?? '',
      child: listenerWidget,
    );
  }
}

class _TextButtonPreview extends StatefulWidget {
  const _TextButtonPreview({
    required this.message,
    required this.child,
    this.stableDiagnostic = false,
  });
  final String message;
  final Widget child;
  final bool stableDiagnostic;

  @override
  State<_TextButtonPreview> createState() => _TextButtonPreviewState();
}

class _TextButtonPreviewState extends State<_TextButtonPreview> {
  // Tooltip adds/removes internal render wrappers when its message changes
  // between empty and nonempty. Retain the actual SDK button across that move.
  final _contentKey = GlobalKey();

  @override
  Widget build(BuildContext context) {
    final owner = context.findAncestorWidgetOfExactType<_CanvasNodeView>();
    final document = context
        .findAncestorStateOfType<_CanvasDocumentViewState>();
    final linkedMenuDiagnostic = _MenuPanelPreviewScope.linkedOf(context);
    if (widget.stableDiagnostic ||
        linkedMenuDiagnostic ||
        (owner != null &&
            (document?._tooltipAncestorIds.contains(owner.node.id) ?? false))) {
      // This ancestor may acquire a diagnostic from theme/layout as well as
      // model edits. Keep an open descendant OverlayPortal in the same branch.
      // Outside Tooltip-containing model subtrees the existing preview is exact.
      return Stack(
        fit: StackFit.passthrough,
        clipBehavior: Clip.none,
        children: [
          Semantics(
            tooltip: widget.message.isEmpty ? null : widget.message,
            child: KeyedSubtree(key: _contentKey, child: widget.child),
          ),
          if (widget.message.isNotEmpty)
            Positioned(
              top: 0,
              right: 0,
              child: IgnoreBaseline(
                child: TooltipVisibility(
                  visible: !linkedMenuDiagnostic,
                  child: Tooltip(
                    message: widget.message,
                    child: const ColoredBox(
                      color: Color(0xfffef3c7),
                      child: Icon(
                        Icons.info_outline,
                        size: 12,
                        color: Color(0xff92400e),
                      ),
                    ),
                  ),
                ),
              ),
            ),
        ],
      );
    }
    return Tooltip(
      message: widget.message,
      child: Semantics(
        tooltip: widget.message.isEmpty ? null : widget.message,
        child: KeyedSubtree(key: _contentKey, child: widget.child),
      ),
    );
  }
}

bool _ignoreInlineTextCommit(
  String widgetId,
  String text,
  bool compositionObserved,
) => false;

String _customClipperPreviewUnavailableMessage({
  required String widgetName,
  required String expectedType,
}) =>
    'Custom $widgetName preview unavailable. Generated Dart uses the '
    'configured $expectedType; isolated Canvas does not execute project '
    'or dependency Dart.';

// Preview owns no mutable notifier and never reads an application ValueListenable.
ValueListenable<Object?> _valueListenablePreviewSource(CanvasNode node) {
  if (node.properties['nullableValueType']?.value == true ||
      node.properties['valueListenable']?.kind == 'dartObjectReferencePresence') {
    return const AlwaysStoppedAnimation<Object?>(null);
  }
  return switch (node.properties['valueType']?.value) {
    'String' => const AlwaysStoppedAnimation<Object?>(''),
    'bool' => const AlwaysStoppedAnimation<Object?>(false),
    'double' => const AlwaysStoppedAnimation<Object?>(0.0),
    'int' || 'num' || 'Object' => const AlwaysStoppedAnimation<Object?>(0),
    _ => const AlwaysStoppedAnimation<Object?>(null),
  };
}

String? _customClipperPreviewUnavailableMessageForNode(
  CanvasNode node, {
  BuildContext? context,
  BoxConstraints? constraints,
}) {
  if (node.type == 'flutter.material.MenuItemButton' ||
      node.type == 'flutter.material.SubmenuButton') {
    _MenuItemPreviewState? state;
    void visit(Element element) {
      if (element is StatefulElement &&
          element.state is _MenuItemPreviewState &&
          (element.state as _MenuItemPreviewState).widget.node.id == node.id) {
        state = element.state as _MenuItemPreviewState;
        return;
      }
      if (state == null) element.visitChildElements(visit);
    }

    context?.visitChildElements(visit);
    return state?.message;
  }
  if (node.type == 'flutter.material.Tooltip') {
    final state = context
        ?.findAncestorStateOfType<_CanvasDocumentViewState>()
        ?._tooltipPreviewKeyFor(node.id)
        ?.currentState;
    return state is _TooltipPreviewState ? state.message : null;
  }
  if (isCanvasStackPositionedWidgetType(node.type) && node.type != 'flutter.widgets.PositionedTransition'
      && node.type != 'flutter.widgets.RelativePositionedTransition') {
    final refs = node.properties.entries.where((entry) => entry.value.kind == 'dartObjectReferencePresence').map((entry) => entry.key).toList();
    return refs.isEmpty ? null : '${node.type.split('flutter.widgets.').last} ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
        'Custom edges/sizes preview as null, rectangle as (0,0,48,48), duration as 300 ms, curve as linear, onEnd is not called.';
  }
  if (node.type == 'flutter.widgets.AnimatedPhysicalModel') {
    final refs=node.properties.entries.where((p)=>p.value.kind=='dartObjectReferencePresence').map((p)=>p.key).toList();
    return refs.isEmpty?null:'AnimatedPhysicalModel ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
      'Color previews blue, shadow color black, elevation zero, border radius null/zero, duration 300 ms, curve linear, and On end absent. Source preserves the exact typed values.';
  }
  if (node.type == 'flutter.widgets.DefaultTextStyle' || node.type == 'flutter.widgets.DefaultTextStyle.merge') {
    final refs = ['style', 'textHeightBehavior', 'maxLines']
        .where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence').toList();
    return refs.isEmpty ? null : '${node.type} ${node.id}: project-owned ${refs.join(', ')} are not executed. '
        'Custom style previews as empty TextStyle (direct) or inherited (merge); unknown height behavior/maxLines previews as null. '
        'In merge mode null inherits the parent value. Generated Dart and stored values retain the references.';
  }
  if (node.type == 'flutter.widgets.RotationTransition') {
    final refs = ['turns', 'alignment'].where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return refs.isEmpty ? null : 'RotationTransition ${node.id}: project-owned ${refs.join(', ')} are not executed. '
        'Canvas previews a stopped rotation of 0 turns for animation references and center for Alignment references; generated Dart retains live values.';
  }
  if (node.type == 'flutter.widgets.RelativePositionedTransition') {
    final messages = <String>[];
    if (node.properties['rect']?.kind == 'dartObjectReferencePresence') {
      messages.add('project-owned Animation<Rect?> is not executed; preview uses Rect.fromLTWH(0,0,48,48)');
    }
    if (node.properties['size']?.kind == 'dartObjectReferencePresence') {
      messages.add('project-owned Size is not executed; preview uses Size(48,48)');
    }
    return messages.isEmpty ? null : 'RelativePositionedTransition ${node.id}: ${messages.join('; ')}. Reference Size is not the actual Stack size or a scale.';
  }
  if (node.type == 'flutter.widgets.PositionedTransition') {
    return node.properties['rect']?.kind != 'dartObjectReferencePresence' ? null
        : 'PositionedTransition ${node.id}: project-owned Animation<RelativeRect> is not executed. '
          'Canvas previews stopped zero physical insets (fills the Stack); generated Dart retains the live animation.';
  }
  if (node.type == 'flutter.widgets.SizeTransition') {
    final refs = ['sizeFactor', 'axisAlignment', 'alignment', 'fixedCrossAxisSizeFactor']
        .where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return refs.isEmpty ? null : 'SizeTransition ${node.id}: project-owned ${refs.join(', ')} are not executed. '
        'Canvas previews sizeFactor at 1 and nullable alignment/cross-axis values at null, using native axis/RTL defaults; generated Dart retains live values.';
  }
  if (node.type == 'flutter.widgets.ScaleTransition') {
    final refs = ['scale', 'alignment'].where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return refs.isEmpty ? null : 'ScaleTransition ${node.id}: project-owned ${refs.join(', ')} are not executed. '
        'Canvas previews a stopped scale of 1 for animation references and center for Alignment references; generated Dart retains live values.';
  }
  if (node.type == 'flutter.widgets.SlideTransition') {
    return node.properties['position']?.kind != 'dartObjectReferencePresence' ? null
        : 'SlideTransition ${node.id}: project-owned position animation is not executed. '
          'Canvas previews a stopped zero offset; generated Dart uses the typed animation and its live updates.';
  }
  if (node.type == 'flutter.widgets.FadeTransition' || node.type == 'flutter.widgets.SliverFadeTransition') {
    return node.properties['opacity']?.kind != 'dartObjectReferencePresence' ? null
        : '${node.type.split('.').last} ${node.id}: project-owned opacity animation is not executed. '
          'Canvas previews a stopped opacity of 1; generated Dart uses the typed animation and its live updates.';
  }
  if(node.type==canvasDatePickerDialogType)return _datePickerPreviewMessage(node);
  if(node.type==canvasCalendarDatePickerType)return _calendarDatePickerPreviewMessage(node);
  if(node.type==canvasInputDatePickerFormFieldType)return _inputDatePickerFormFieldPreviewMessage(node);
  if (node.type == canvasBottomSheetType) return _bottomSheetPreviewMessage(node);
  if (node.type == canvasSnackBarType || node.type == canvasSnackBarActionType) return _snackBarPreviewMessage(node);
  if (node.type == canvasSimpleDialogType || node.type == canvasSimpleDialogOptionType) return _simpleDialogPreviewMessage(node);
  if(isCanvasAlertDialogType(node.type)){return _alertDialogPreviewMessage(node);}
  if(node.type==canvasDialogType || node.type==canvasFullscreenDialogType){return _dialogPreviewMessage(node);}
  if(node.type==canvasTimePickerDialogType){return _timePickerDialogPreviewMessage(node);}
  if(node.type==canvasDateRangePickerDialogType)return _dateRangePickerPreviewMessage(node);
  if(isCanvasDataTable(node.type)||isCanvasDataDescriptor(node.type)) {
    final message=_dataTablePreviewMessage(node);return message.isEmpty?null:message;
  }
  if (const {'flutter.widgets.Table','flutter.widgets.TableRow','flutter.widgets.TableCell'}.contains(node.type)) {
    final refs=node.properties.entries.where((e)=>e.value.kind=='dartObjectReferencePresence').map((e)=>e.key).toList();
    return refs.isEmpty?null:'${node.type} ${node.id}: project-owned ${refs.join(', ')} are not executed. '
        'Canvas uses default flex widths, no source border/decoration, and private preview keys. Generated Dart retains typed sources.';
  }
  if (node.type == 'flutter.widgets.Flow' || node.type == 'flutter.widgets.Flow.unwrapped') {
    return '${node.type} ${node.id}: project FlowDelegate code is not executed. '
        'Canvas previews a constrained 256 by 192 area, children at most 48 by 48, '
        'left-to-right rows with 8 pixel gaps. Custom transforms, opacity and repaint '
        'notifications run only in the generated application.';
  }
  if (node.type == 'flutter.widgets.CustomMultiChildLayout' || node.type == 'flutter.widgets.LayoutId') {
    return '${node.type} ${node.id}: project delegates and IDs are not executed. '
        'Canvas uses private per-node IDs and a constrained 256 by 192 vertical-cell layout. '
        'Custom layout, relayout and source ID equality are tested in the generated application.';
  }
  if (node.type == 'flutter.widgets.CustomSingleChildLayout') {
    return 'CustomSingleChildLayout ${node.id}: project delegate is not executed. '
        'Canvas uses a centered, constrained 128 by 96 logical-pixel preview. '
        'Source owns parent size, child constraints, position and relayout notifications.';
  }
  if (node.type == 'flutter.widgets.CustomPaint') {
    final refs = ['painter', 'foregroundPainter', 'size'].where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence').toList();
    return refs.isEmpty ? null : 'CustomPaint ${node.id}: project-owned ${refs.join(', ')} are not executed. '
        'Canvas substitutes inert painters and Size.zero; source artwork, repaint, custom hit testing and semantics are not previewed.';
  }
  if (node.type == 'flutter.widgets.ShaderMask') {
    return node.properties['shaderCallback']?.kind == 'dartObjectReferencePresence'
        ? 'ShaderMask ${node.id}: project-owned ShaderCallback is not executed. Canvas substitutes an opaque-white shader and keeps Blend mode; this is neutral with the default modulate, not every blend mode.' : null;
  }
  if (node.type == 'flutter.widgets.BackdropGroup') {
    return node.properties['backdropKey']?.kind == 'dartObjectReferencePresence'
        ? 'BackdropGroup ${node.id}: project-owned BackdropKey is not executed. Canvas creates a local group key.' : null;
  }
  if (node.type == 'flutter.widgets.BackdropFilter' || node.type == 'flutter.widgets.BackdropFilter.grouped') {
    final fields = <String>['backdropGroupKey'];
    final config = node.properties['filterConfig'];
    if (config?.kind == 'dartObjectReferencePresence') {
      fields.add('filterConfig');
    } else if (config?.value == 'compose') {
      fields.addAll(['configInner', 'configOuter']);
    } else if (config == null || config.kind == 'null' || config.value == 'wrap') {
      final filter = node.properties['filter'];
      if (filter?.kind == 'dartObjectReferencePresence') {
        fields.add('filter');
      } else {
        fields.addAll(switch (filter?.value) {
          'blur' => ['bounds'], 'matrix' => ['matrix4'],
          'compose' => ['inner','outer'], 'shader' => ['shader'], _ => <String>[],
        });
      }
    }
    final refs = fields.where((field) => node.properties[field]?.kind == 'dartObjectReferencePresence').toList();
    return refs.isEmpty ? null : '${node.type.split('.').skip(2).join('.')} ${node.id}: project-owned ${refs.join(', ')} are not executed. Canvas uses neutral filters/matrices, null bounds and no explicit backdrop key.';
  }
  if (node.type == 'flutter.widgets.ImageFiltered') {
    if (node.properties['imageFilter']?.kind == 'dartObjectReferencePresence') {
      return 'ImageFiltered ${node.id}: project-owned ImageFilter is not executed. Canvas uses zero-sigma blur.';
    }
    final mode = node.properties['imageFilter']?.value;
    final fields = switch (mode) {
      'blur' => ['bounds'],
      'matrix' => ['matrix4'],
      'compose' => ['inner', 'outer'],
      'shader' => ['shader'],
      _ => <String>[],
    };
    final refs = fields.where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence').toList();
    return refs.isEmpty ? null : 'ImageFiltered ${node.id}: project-owned ${refs.join(', ')} are not executed. '
        'Canvas uses null bounds or an identity filter/matrix. Shader filters require Impeller in the running app.';
  }
  if (node.type == 'flutter.widgets.ColorFiltered') {
    if(node.properties['colorFilter']?.kind == 'dartObjectReferencePresence') {
      return 'ColorFiltered ${node.id}: project-owned ColorFilter is not executed; preview uses an identity matrix. Inactive local drafts are preserved.';
    }
    if(node.properties['colorFilter']?.value == 'mode' && node.properties['color']?.kind == 'dartObjectReferencePresence') {
      return 'ColorFiltered ${node.id}: project-owned Color is not executed; mode preview uses transparent source color and the selected BlendMode.';
    }
  }
  if (node.type == 'flutter.widgets.RawImage') {
    final refs = node.properties.entries.where((entry) => entry.value.kind == 'dartObjectReferencePresence').map((entry) => entry.key).toList();
    return refs.isEmpty ? null : 'RawImage ${node.id}: project-owned ${refs.join(', ')} are not executed. '
        'Decoded image, opacity animation and center slice preview as null; source dimensions/color use null, scale 1 and alignment center. '
        'Image decoding and disposal remain in project Dart code.';
  }
  if (node.type == 'flutter.widgets.FadeInImage') {
    final refs = node.properties.entries.where((p)=>p.value.kind == 'dartObjectReferencePresence').map((p)=>p.key).toList();
    return refs.isEmpty ? null : 'FadeInImage ${node.id}: project-owned ${refs.join(', ')} are not executed. '
      'Canvas substitutes ${refs.map((p)=>switch(p){
'placeholder' || 'image'=>'$p = built-in image','fadeOutDurationUs'=>'$p = 300000','fadeInDurationUs'=>'$p = 700000',
'fadeOutCurve'=>'$p = easeOut','fadeInCurve'=>'$p = easeIn','alignment'=>'$p = center',
'placeholderErrorBuilder' || 'imageErrorBuilder'=>'$p = safe error placeholder',_=>'$p = native default'}).join(', ')}. '
      'No project factory, callback or arbitrary network request is executed.';
  }
  if (node.type == 'flutter.material.AnimatedIcon') {
    final refs=['icon','progress','color','size'].where((p)=>node.properties[p]?.kind=='dartObjectReferencePresence').toList();
    return refs.isEmpty ? null : 'AnimatedIcon ${node.id}: project-owned ${refs.join(', ')} are not executed. '
      'Canvas substitutes ${refs.map((p)=>switch(p){'icon'=>'icon = AnimatedIcons.menu_close','progress'=>'progress = 0','color'=>'color = IconTheme',_=>'size = IconTheme'}).join(', ')}. '
      'Stored values and generated Dart are unchanged.';
  }
  if (node.type == 'flutter.widgets.AnimatedModalBarrier') {
    final refs = ['color', 'onDismiss', 'clipDetailsNotifier'].where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return 'AnimatedModalBarrier ${node.id}: Canvas suppresses dismissal callbacks, route changes and alert sounds. '
      '${refs.isEmpty ? '' : 'Project-owned ${refs.join(', ')} are not executed; Canvas substitutes ${refs.map((name) => switch (name) { 'color' => 'color = AlwaysStoppedAnimation<Color?>(null)', 'onDismiss' => 'onDismiss = no-op', _ => 'clipDetailsNotifier = null' }).join(', ')}. '}'
      'Native application behavior and stored values are unchanged. Bounded width and height are required.';
  }
  if (node.type == 'flutter.widgets.ModalBarrier') {
    final refs = ['color', 'onDismiss', 'clipDetailsNotifier'].where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return 'ModalBarrier ${node.id}: Canvas suppresses dismissal callbacks, route changes and alert sounds. '
      '${refs.isEmpty ? '' : 'Project-owned ${refs.join(', ')} are not executed; Canvas substitutes ${refs.map((name) => switch (name) { 'color' => 'color = transparent', 'onDismiss' => 'onDismiss = no-op', _ => 'clipDetailsNotifier = null' }).join(', ')}. '}'
      'Native application behavior and stored values are unchanged. Bounded width and height are required.';
  }
  if (node.type == 'flutter.widgets.MatrixTransition') {
    final refs = ['animation', 'onTransform', 'alignment'].where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return refs.isEmpty ? null : 'MatrixTransition ${node.id}: project-owned ${refs.join(', ')} are not executed. '
      'Canvas substitutes ${refs.map((name) => switch(name) { 'animation' => 'animation = 0', 'onTransform' => 'onTransform = identity', _ => 'alignment = center' }).join(', ')}. '
      'Other local values and generated Dart are unchanged.';
  }
  if (node.type == 'flutter.widgets.AlignTransition') {
    final refs=['alignment','widthFactor','heightFactor'].where((name)=>node.properties[name]?.kind=='dartObjectReferencePresence');
    return refs.isEmpty ? null : 'AlignTransition ${node.id}: project-owned ${refs.join(', ')} are not executed. '
      'Canvas substitutes ${refs.map((name) => name == 'alignment' ? 'alignment = center' : '$name = null').join(', ')}. '
      'Other local values are unchanged; generated Dart retains live source values.';
  }
  if (node.type == 'flutter.widgets.DecoratedBoxTransition') {
    return node.properties['decoration']?.kind == 'dartObjectReferencePresence'
        ? 'DecoratedBoxTransition ${node.id}: project-owned Animation<Decoration> is not executed. '
          'Canvas previews a stopped empty BoxDecoration; generated Dart retains custom and ShapeDecoration animations.'
        : null;
  }
  if (node.type == 'flutter.widgets.DefaultTextStyleTransition') {
    final refs = ['style', 'maxLines']
        .where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence').toList();
    return refs.isEmpty ? null : 'DefaultTextStyleTransition ${node.id}: project-owned ${refs.join(', ')} are not executed. '
        'Style previews as AlwaysStoppedAnimation<TextStyle> with empty TextStyle; unknown Max lines previews as null. '
        'The source owns animation timing and lifecycle. Generated Dart and stored values retain the exact references.';
  }
  if (node.type == 'flutter.widgets.AnimatedDefaultTextStyle') {
    final refs = ['style', 'textHeightBehavior', 'maxLines', 'curve', 'durationUs', 'onEnd']
        .where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence').toList();
    return refs.isEmpty ? null : 'AnimatedDefaultTextStyle ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
        'Custom style previews as empty TextStyle, height behavior/maxLines as null, curve as linear, duration as 300 ms and callback as absent. Generated Dart retains the references.';
  }
  if (node.type == 'flutter.material.Theme' &&
      node.properties['data']?.kind == 'dartObjectReferencePresence') {
    return 'Theme ${node.id}: project-owned ThemeData is not executed. Canvas uses ThemeData.fallback() (Material 3 light), not the parent theme. Stored values and generated Dart are unchanged.';
  }
  if (node.type == 'flutter.material.AnimatedTheme') {
    final refs=node.properties.entries.where((p)=>p.value.kind=='dartObjectReferencePresence').map((p)=>p.key).toList();
    return refs.isEmpty?null:'AnimatedTheme ${node.id}: project-owned ${refs.join(', ')} are not executed. Unknown ThemeData uses ThemeData.fallback() (Material 3 light), not the parent theme; unknown curve uses linear, duration 200 ms, and On end is inert. Stored values and generated Dart are unchanged.';
  }
  if (node.type == 'flutter.widgets.AnimatedSwitcher') {
    final refs = node.properties.entries.where((p) => p.value.kind == 'dartObjectReferencePresence').map((p) => p.key).toList();
    return 'AnimatedSwitcher ${node.id}: Canvas isolates transition keys for repeated child types. '
      'Flutter 3.44.8 defaultTransitionBuilder can omit outgoing children when transition keys repeat; generated Dart retains SDK behavior. '
      '${refs.isEmpty ? '' : 'Project-owned ${refs.join(', ')} are not executed: unresolved duration uses 300 ms, reverseDuration null, curves linear, and builders the default fade/centered Stack. '}'
      'Same-type unkeyed children update without a transition. Stored values are unchanged.';
  }
  if (node.type == 'flutter.widgets.AnimatedCrossFade') {
    final refs = node.properties.entries.where((p) => p.value.kind == 'dartObjectReferencePresence').map((p) => p.key).toList();
    return refs.isEmpty ? null : 'AnimatedCrossFade ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
      'Their previews use linear curves, topCenter alignment, 300 ms duration, null reverseDuration, native defaultLayoutBuilder and no onEnd handler. Stored values and generated Dart are unchanged.';
  }
  if (node.type == 'flutter.widgets.AnimatedSize') {
    final refs = ['alignment', 'curve', 'durationUs', 'reverseDurationUs', 'onEnd']
        .where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence').toList();
    return refs.isEmpty ? null : 'AnimatedSize ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
        'Preview uses center alignment, linear curve, 300 ms duration, null reverseDuration and no onEnd handler for those references.';
  }
  if (node.type == 'flutter.widgets.AnimatedContainer') {
    final refs = node.properties.entries.where((entry) => entry.value.kind == 'dartObjectReferencePresence').map((entry) => entry.key).toList();
    return refs.isEmpty ? null : 'AnimatedContainer ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
      'Custom geometry, color and decorations preview as null; custom duration uses 300 ms, curve uses linear, onEnd is not called. '
      'Clipping previews as none when its project-owned background cannot be rendered.';
  }
  if (node.type == 'flutter.widgets.AnimatedRotation') {
    final refs = ['turns', 'alignment', 'curve', 'durationUs', 'onEnd'].where(
      (name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return refs.isEmpty ? null : 'AnimatedRotation ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
        'Canvas uses zero for custom Turns, center for a custom Alignment, linear for a custom Curve, 300 ms for a custom Duration, and no completion callback. Generated Dart retains exact references.';
  }
  if (node.type == 'flutter.widgets.AnimatedScale') {
    final refs = ['scale', 'alignment', 'curve', 'durationUs', 'onEnd'].where(
      (name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return refs.isEmpty ? null : 'AnimatedScale ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
        'Canvas uses 1 for a custom scale, center for a custom Alignment, linear for a custom Curve, 300 ms for a custom Duration, and no completion callback. Generated Dart retains exact references.';
  }
  if (node.type == 'flutter.widgets.AnimatedSlide') {
    final refs = ['offset', 'curve', 'durationUs', 'onEnd'].where(
      (name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return refs.isEmpty ? null : 'AnimatedSlide ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
        'Canvas uses zero for a custom Offset, linear for a custom Curve, 300 ms for a custom Duration, and no completion callback. Generated Dart retains exact references.';
  }
  if (node.type == 'flutter.widgets.AnimatedPadding') {
    final refs = ['padding', 'curve', 'durationUs', 'onEnd'].where(
      (name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return refs.isEmpty ? null : 'AnimatedPadding ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
        'Canvas uses 16 pixels per side for custom EdgeInsetsGeometry, linear for a custom Curve, 300 ms for a custom Duration, and no completion callback. Generated Dart retains exact references.';
  }
  if (node.type == 'flutter.widgets.AnimatedFractionallySizedBox') {
    final refs = ['alignment','widthFactor','heightFactor','curve','durationUs','onEnd']
        .where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence').toList();
    return refs.isEmpty ? null : 'AnimatedFractionallySizedBox ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
        'Using center alignment, null factors, linear curve, 300 ms duration and no callback where required; generated Dart keeps the references.';
  }
  if (node.type == 'flutter.widgets.AnimatedAlign') {
    final refs = ['alignment', 'curve', 'durationUs', 'onEnd'].where(
      (name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return refs.isEmpty ? null : 'AnimatedAlign ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
        'Canvas uses center for a custom AlignmentGeometry, linear for a custom Curve, 300 ms for a custom Duration, and no completion callback. Generated Dart retains exact references.';
  }
  if (node.type == 'flutter.widgets.AnimatedOpacity') {
    final refs = ['curve', 'durationUs', 'onEnd'].where(
      (name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return refs.isEmpty ? null : 'AnimatedOpacity ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. '
        'Canvas uses linear for a custom Curve, 300 ms for a custom Duration, and no completion callback. Generated Dart retains exact references.';
  }
  if (node.type == 'flutter.widgets.SliverPersistentHeader') {
    return 'SliverPersistentHeader preview limitation: project delegate is not executed. '
        'Canvas shows a 56–112 logical-pixel header with the exact pinned/floating flags. '
        'Custom content, extents, rebuild, vsync, snap, stretch and show-on-screen behavior are unavailable here. '
        'Generated Dart retains the delegate; run the app to verify its behavior.';
  }
  if (node.type == 'flutter.material.FlexibleSpaceBar') {
    final refs = ['titlePadding', 'stretchModes'].where((name) => node.properties[name]?.kind == 'dartObjectReferencePresence');
    return refs.isEmpty ? null : 'FlexibleSpaceBar ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed. Native default padding/effects are shown; stored values and generated Dart retain exact references.';
  }
  if (isCanvasSliverAppBarType(node.type)) {
    final refs = node.properties.entries.where((entry) => entry.value.kind == 'dartObjectReferencePresence').map((entry) => entry.key).toList();
    return refs.isEmpty ? null : 'SliverAppBar ${node.id} preview limitation: project-owned ${refs.join(', ')} are not executed in Canvas. Native/theme fallback is displayed; generated Dart keeps the exact references.';
  }
  if (node.type == 'flutter.widgets.SliverFloatingHeader') {
    final messages = <String>[];
    final refs = node.properties.entries.where((e) => e.value.kind == 'dartObjectReferencePresence').map((e) => e.key).toList();
    if (refs.isNotEmpty) messages.add('Project-owned ${refs.join(', ')} are not executed; Canvas uses the SDK defaults for those values.');
    for (final name in ['animationStyleDurationUs', 'animationStyleReverseDurationUs']) {
      final value = node.properties[name];
      if (value?.kind == 'integer' && value!.value is int && (value.value as int) < 0) {
        messages.add('Negative $name is unsafe when native animation starts; only its preview duration uses zero.');
      }
    }
    return messages.isEmpty ? null : 'SliverFloatingHeader ${node.id} preview limitation: ${messages.join(' ')} Stored values and generated Dart are unchanged.';
  }
  if (node.type == 'flutter.widgets.LayoutBuilder' &&
      node.properties['builder']?.kind == 'dartObjectReferencePresence') {
    return 'LayoutBuilder preview limitation: project builder is not executed. '
        'Its constraint-dependent subtree is unavailable in isolated Canvas; an empty box is shown. '
        'Generated Dart retains the exact builder. Run the app to verify responsive layout. '
        'Flutter LayoutBuilder does not support intrinsic/dry layout.';
  }
  if ((node.type == 'flutter.widgets.TweenAnimationBuilder' ||
      node.type == 'flutter.widgets.TweenAnimationBuilder.sliver') &&
      ['tween', 'builder', 'curve', 'durationUs', 'onEnd'].any(
        (name) => node.properties[name]?.kind == 'dartObjectReferencePresence')) {
    return 'TweenAnimationBuilder preview limitation: project tweens, builders, durations, curves and onEnd are not executed. '
        'Canvas displays Child with an isolated 0-to-1 tween; run the application to test value-dependent output, target changes and completion.';
  }
  if ((node.type == 'flutter.widgets.ValueListenableBuilder' ||
      node.type == 'flutter.widgets.ValueListenableBuilder.sliver') &&
      (node.properties['valueListenable']?.kind == 'dartObjectReferencePresence' ||
       node.properties['builder']?.kind == 'dartObjectReferencePresence')) {
    return 'ValueListenableBuilder preview limitation: project sources/getters/factories and builders are not executed. '
        'No value changes are simulated. Child is shown unchanged as a design-time fallback, not custom builder output. '
        'Generated Dart retains selected T, source and builder; run the app to verify values and listener ownership.';
  }
  if ((node.type == 'flutter.widgets.AnimatedBuilder' ||
      node.type == 'flutter.widgets.AnimatedBuilder.sliver') &&
      (node.properties['animation']?.kind == 'dartObjectReferencePresence' ||
       node.properties['builder']?.kind == 'dartObjectReferencePresence')) {
    return 'AnimatedBuilder preview limitation: project Listenable objects/getters/factories and builders are not executed. '
        'No notifications are simulated. Child is shown unchanged as a design-time fallback, not custom builder output. '
        'Generated Dart retains both exact bindings; run the app to verify behavior and subscription ownership.';
  }
  if ((node.type == 'flutter.widgets.ListenableBuilder' ||
      node.type == 'flutter.widgets.ListenableBuilder.sliver') &&
      (node.properties['listenable']?.kind == 'dartObjectReferencePresence' ||
       node.properties['builder']?.kind == 'dartObjectReferencePresence')) {
    return 'ListenableBuilder preview limitation: project Listenable objects/getters/factories and builders are not executed. '
        'No notifications are simulated. Child is shown unchanged as a design-time fallback, not custom builder output. '
        'Generated Dart retains both exact bindings; run the app to verify behavior and subscription ownership.';
  }
  if ((node.type == 'flutter.widgets.DeviceOrientationBuilder' ||
      node.type == 'flutter.widgets.DeviceOrientationBuilder.sliver') &&
      node.properties['builder']?.kind == 'dartObjectReferencePresence') {
    return 'DeviceOrientationBuilder preview limitation: project builder is not executed. '
        'An empty result matching the box/sliver placement is shown; run the app to see custom content. '
        'Orientation comes from MediaQuery, not parent layout constraints.';
  }
  if (node.type == 'flutter.widgets.OrientationBuilder' &&
      node.properties['builder']?.kind == 'dartObjectReferencePresence') {
    return 'OrientationBuilder preview limitation: project builder is not executed. '
        'Its constraint-dependent subtree is unavailable in isolated Canvas; an empty box is shown. '
        'Generated Dart retains the exact builder. Run the app to verify responsive layout. '
        'Flutter OrientationBuilder does not support intrinsic/dry layout.';
  }
  if (node.type == 'flutter.widgets.SliverLayoutBuilder' &&
      node.properties['builder']?.kind == 'dartObjectReferencePresence') {
    return 'SliverLayoutBuilder preview limitation: project builder is not executed. '
        'Its constraint-dependent subtree is unavailable in isolated Canvas; a zero-extent sliver is shown. '
        'Generated Dart retains the exact builder. Run the app to verify its sliver result and responsive layout.';
  }
  if (node.type == 'flutter.widgets.SliverPadding') return _sliverPaddingPreviewMessage(node);
  if (node.type.startsWith('flutter.widgets.SliverFillViewport')) return _viewportSliverPreviewMessage(node);
  if (isCanvasDynamicSliverType(node.type)) return _dynamicSliverPreviewMessage(node);
  if (node.type == 'flutter.widgets.ListView') {
    return _listViewExtentPreviewMessage(node);
  }
  if (node.type == 'flutter.material.TextField') {
    return _textFieldBuilderPreviewMessage(node);
  }
  if (node.type == 'flutter.material.AppBar') {
    return _appBarPredicatePreviewMessage(node);
  }
  if (node.type == 'flutter.material.NavigationBar') {
    return _navigationBarPreviewMessage(node);
  }
  if (node.type == 'flutter.material.NavigationRail') {
    return _navigationRailPreviewMessage(node);
  }
  if (node.type == 'flutter.material.NavigationDrawer') {
    return _navigationDrawerPreviewMessage(node);
  }
  if (node.type == 'flutter.material.Drawer') {
    return _drawerPreviewMessage(node);
  }
  if (node.type == 'flutter.material.BottomAppBar') {
    return _bottomAppBarPreviewMessage(node);
  }
  if (node.type == 'flutter.material.BottomNavigationBar') {
    return _bottomNavigationBarPreviewMessage(node);
  }
  if (node.type == 'flutter.material.Scaffold') {
    return _scaffoldScrimPreviewMessage(node);
  }
  if (node.type == 'flutter.widgets.MouseRegion') {
    return _mouseRegionPreviewMessage(node);
  }
  if (node.type == 'flutter.widgets.Focus') {
    return _focusPreviewMessage(node);
  }
  if (node.type == 'flutter.widgets.NotificationListener') {
    return _notificationListenerPreviewMessage(node);
  }
  if (node.type == 'flutter.material.FloatingActionButton') {
    return _fabPreviewMessage(node, context);
  }
  if (node.type == 'flutter.material.IconButton') {
    return context == null
        ? _iconButtonPreviewMessage(node, context)
        : _iconButtonM2GeometryMessage(node, context) ??
              _iconButtonPreviewMessage(node, context);
  }
  if (node.type == 'flutter.material.Checkbox') {
    return _checkboxPreviewMessage(node, context);
  }
  if (node.type == 'flutter.material.Switch') {
    return context != null && constraints != null
        ? _switchPaddingMessage(node, context, constraints) ??
              _switchPreviewMessage(node, context)
        : _switchPreviewMessage(node, context);
  }
  if (node.type == 'flutter.material.Radio') {
    return _radioUnavailableMessage(node, context) ??
        _radioReferenceMessage(node, context);
  }
  if (node.type == 'flutter.widgets.RadioGroup') {
    return _radioGroupPreviewMessage(node, context);
  }
  if (node.type == 'flutter.material.ListTile') {
    _ListTilePreviewState? state;
    void visit(Element element) {
      if (element is StatefulElement &&
          element.state is _ListTilePreviewState &&
          (element.state as _ListTilePreviewState).widget.node.id == node.id) {
        state = element.state as _ListTilePreviewState;
        return;
      }
      if (state == null) element.visitChildElements(visit);
    }

    context?.visitChildElements(visit);
    return state?._message.isNotEmpty == true
        ? state!._message
        : _listTileStaticMessage(node, context);
  }
  if (node.type == 'flutter.material.CheckboxListTile' ||
      node.type == 'flutter.material.SwitchListTile' ||
      node.type == 'flutter.material.RadioListTile' ||
      node.type == 'flutter.material.ExpansionTile') {
    _ListTilePreviewState? state;
    void visit(Element element) {
      if (element is StatefulElement &&
          element.state is _ListTilePreviewState &&
          (element.state as _ListTilePreviewState).widget.node.id == node.id) {
        state = element.state as _ListTilePreviewState;
        return;
      }
      if (state == null) element.visitChildElements(visit);
    }

    context?.visitChildElements(visit);
    return state?._message.isNotEmpty == true
        ? state!._message
        : node.type == 'flutter.material.SwitchListTile'
        ? _switchListTileStaticMessage(node, context)
        : node.type == 'flutter.material.RadioListTile'
        ? _radioListTileStaticMessage(node, context)
        : node.type == 'flutter.material.ExpansionTile'
        ? null
        : _checkboxListTileStaticMessage(node, context);
  }
  if (node.type == 'flutter.material.RangeSlider') {
    return _rangeSliderGeometryMessage(node, context, constraints) ??
        _rangeSliderReferenceMessage(node);
  }
  if (node.type == 'flutter.material.Slider') {
    return _sliderGeometryMessage(node, context, constraints) ??
        _sliderReferenceMessage(node, context);
  }
  if (node.type == 'flutter.widgets.Icon' && context != null) {
    return _iconButtonMountedIconMessage(node, context);
  }
  if (node.type == 'flutter.material.ElevatedButton' ||
      node.type == 'flutter.material.TextButton' ||
      node.type == 'flutter.material.OutlinedButton' ||
      node.type == 'flutter.material.FilledButton') {
    return _textButtonReferenceMessage(node);
  }
  if (node.type == 'flutter.material.RefreshIndicator') {
    return _refreshIndicatorReferenceMessage(node);
  }
  if (node.type == 'flutter.material.RefreshProgressIndicator') {
    return context == null
        ? null
        : _refreshProgressUnavailableMessage(
            node,
            context,
            constraints: constraints,
          );
  }
  if (node.type == 'flutter.material.CircularProgressIndicator') {
    return context == null
        ? null
        : _circularProgressUnavailableMessage(
            node,
            context,
            constraints: constraints,
          );
  }
  if (node.type == 'flutter.material.LinearProgressIndicator') {
    return _linearProgressUnavailableMessage(
      node,
      context: context,
      constraints: constraints,
    );
  }
  if (node.type == 'flutter.material.Card') {
    return _cardShapePreviewUnavailableMessage(node);
  }
  if (node.type == 'flutter.widgets.ClipPath' &&
      node.properties['shape']?.kind == 'dartObjectReferencePresence') {
    return _customClipperPreviewUnavailableMessage(
      widgetName: 'ClipPath.shape',
      expectedType: 'ShapeBorder',
    );
  }
  if (node.properties['clipper']?.kind != 'dartObjectReferencePresence') {
    return null;
  }
  final expectedType = switch (node.type) {
    'flutter.widgets.ClipRect' ||
    'flutter.widgets.ClipOval' => 'CustomClipper<Rect>',
    'flutter.widgets.ClipRRect' => 'CustomClipper<RRect>',
    'flutter.widgets.ClipRSuperellipse' => 'CustomClipper<RSuperellipse>',
    'flutter.widgets.ClipPath' ||
    'flutter.widgets.PhysicalShape' => 'CustomClipper<Path>',
    _ => null,
  };
  return expectedType == null
      ? null
      : _customClipperPreviewUnavailableMessage(
          widgetName: _displayType(node.type),
          expectedType: expectedType,
        );
}

// This is an isolated-preview complexity budget, not an SDK/source domain limit.
const _maximumCanvasCardShapePoints = 4096;

String? _progressSemanticsUnavailableMessage(
  CanvasNode node,
  String widgetName,
) {
  final semanticValue = node.properties['semanticsValue']?.value as String?;
  // Exact Flutter 3.44.8 progressBar validator: fixed 0/100 range, including
  // its parsed NaN acceptance. LoadingSpinner has no corresponding check.
  if (node.properties.containsKey('value') && semanticValue != null) {
    final numeric = double.tryParse(semanticValue);
    final percent = semanticValue.endsWith('%')
        ? double.tryParse(semanticValue.substring(0, semanticValue.length - 1))
        : null;
    final parsed = numeric ?? percent;
    if (semanticValue.isEmpty || parsed == null || parsed < 0 || parsed > 100) {
      return '$widgetName.semanticsValue preview unavailable: the pinned SDK progressBar '
          'role requires a numeric value from 0 to 100, or a percentage from 0% to 100%. '
          'This explicit string fails SDK semantics validation. Indeterminate loadingSpinner accepts free text. '
          'The stored properties and generated Dart remain unchanged.';
    }
  }
  return null;
}

String? _refreshProgressUnavailableMessage(
  CanvasNode node,
  BuildContext context, {
  BoxConstraints? constraints,
}) {
  if (node.properties['valueColor']?.kind == 'dartObjectReferencePresence') {
    return 'RefreshProgressIndicator.valueColor preview unavailable: generated Dart uses the configured '
        'Animation<Color?> reference; isolated Canvas does not execute project or dependency Dart '
        'or substitute an unrelated animation. Properties remain editable.';
  }
  final semanticFailure = _progressSemanticsUnavailableMessage(
    node,
    'RefreshProgressIndicator',
  );
  if (semanticFailure != null) return semanticFailure;
  if (constraints == null) return null;
  EdgeInsets insets(String name, double fallback) {
    final value = node.properties[name]?.value;
    final geometry = switch (value) {
      CanvasEdgeInsets p => EdgeInsets.fromLTRB(
        p.left,
        p.top,
        p.right,
        p.bottom,
      ),
      CanvasEdgeInsetsDirectional p => EdgeInsetsDirectional.fromSTEB(
        p.start,
        p.top,
        p.end,
        p.bottom,
      ),
      _ => EdgeInsets.all(fallback),
    };
    return geometry.resolve(Directionality.of(context));
  }

  final margin = insets('indicatorMargin', 4);
  final padding = insets('indicatorPadding', 12);
  if (!margin.horizontal.isFinite ||
      !margin.vertical.isFinite ||
      !padding.horizontal.isFinite ||
      !padding.vertical.isFinite) {
    return 'RefreshProgressIndicator.indicatorMargin/indicatorPadding preview unavailable: inset sums '
        'overflow finite SDK layout geometry. The stored properties and generated Dart remain unchanged.';
  }
  final materialSize = const BoxConstraints.tightFor(
    width: 41,
    height: 41,
  ).enforce(constraints.deflate(margin)).constrain(Size.zero);
  final paintSize = BoxConstraints.tight(
    materialSize,
  ).deflate(padding).constrain(Size.zero);
  if (!materialSize.isFinite || !paintSize.isFinite) {
    return 'RefreshProgressIndicator preview unavailable: resolved parent constraints exceed finite '
        'SDK indicator geometry. The stored properties and generated Dart remain unchanged.';
  }
  final material = Theme.of(context);
  final theme = ProgressIndicatorTheme.of(context);
  Color? color(String name) {
    final value = node.properties[name]?.value;
    if (value is int) return Color(value);
    if (value is CanvasThemeToken) {
      return _colorSchemeRole(
        material.colorScheme,
        value.wireId.split('.').last,
      );
    }
    return null;
  }

  // Opacity wraps only the arrow/arc, not Material. With alpha zero the real
  // SDK skips painting: do not diagnose geometry that it never paints.
  final activeColor =
      color('valueColor') ??
      color('color') ??
      theme.color ??
      material.colorScheme.primary;
  // Refresh uses the pinned SDK's byte-quantized Color.opacity getter.
  // ignore: deprecated_member_use
  if (Color.getAlphaFromOpacity(activeColor.opacity) == 0) return null;
  double? number(String name) =>
      (node.properties[name]?.value as num?)?.toDouble();
  final effectiveValue = number('value')?.clamp(0, 1);
  final arrowVisible = effectiveValue != null && effectiveValue > 0.1;
  if (arrowVisible && paintSize.width != paintSize.height) {
    return 'RefreshProgressIndicator.indicatorPadding preview unavailable: a visible SDK arrow requires '
        'a square inner paint area, but the parent constraints and insets produce ${paintSize.width} × ${paintSize.height}. '
        'Adjust the parent size or indicator insets. The stored properties and generated Dart remain unchanged.';
  }
  final width = node.properties.containsKey('strokeWidth')
      ? number('strokeWidth') ?? theme.strokeWidth ?? 4
      : RefreshProgressIndicator.defaultStrokeWidth;
  final align = number('strokeAlign') ?? theme.strokeAlign ?? 0;
  final offset = width / 2 * -align;
  final arcWidth = paintSize.width - offset * 2;
  final arcHeight = paintSize.height - offset * 2;
  final arrowRadius = arrowVisible
      ? width * 2 * ((effectiveValue - 0.1) / (.33 - .1)).clamp(0, 1)
      : 0.0;
  if (!offset.isFinite ||
      !(offset * 2).isFinite ||
      !arcWidth.isFinite ||
      !arcHeight.isFinite ||
      !(offset + arcWidth).isFinite ||
      !(offset + arcHeight).isFinite ||
      !arrowRadius.isFinite ||
      !(paintSize.width / 2 + arrowRadius).isFinite ||
      !(paintSize.width / 2 - arrowRadius).isFinite) {
    return 'RefreshProgressIndicator.strokeWidth/strokeAlign preview unavailable: resolved arc or arrow '
        'coordinates overflow finite SDK geometry. The stored signed values and generated Dart remain unchanged.';
  }
  return null;
}

String? _circularProgressUnavailableMessage(
  CanvasNode node,
  BuildContext context, {
  BoxConstraints? constraints,
}) {
  final material = Theme.of(context);
  final apple =
      material.platform == TargetPlatform.iOS ||
      material.platform == TargetPlatform.macOS;
  // The SDK's Cupertino branch reads ONLY backgroundColor and effective value.
  // In particular it does not use the external controller or color animation.
  if (node.properties['variant']?.value == 'adaptive' && apple) return null;
  final unresolved = [
    for (final name in ['controller', 'valueColor'])
      if (node.properties[name]?.kind == 'dartObjectReferencePresence') name,
  ];
  if (unresolved.isNotEmpty) {
    return 'CircularProgressIndicator preview unavailable for ${unresolved.join(' and ')}. '
        'Generated Dart uses the configured project references; isolated Canvas does not execute project '
        'or dependency Dart or substitute an unrelated animation. Properties remain editable.';
  }
  final semanticFailure = _progressSemanticsUnavailableMessage(
    node,
    'CircularProgressIndicator',
  );
  if (semanticFailure != null) return semanticFailure;
  final theme = ProgressIndicatorTheme.of(context);
  final year2023 =
      node.properties['year2023']?.value as bool? ??
      // ignore: deprecated_member_use
      theme.year2023 ??
      true;
  double? number(String name) {
    final value = node.properties[name]?.value;
    return value is CanvasEnumValue
        ? double.infinity
        : (value as num?)?.toDouble();
  }

  final width = number('strokeWidth') ?? theme.strokeWidth ?? 4;
  final align =
      number('strokeAlign') ??
      theme.strokeAlign ??
      (material.useMaterial3 && !year2023 ? -1 : 0);
  final offset = width / 2 * -align;
  if (!offset.isFinite || !(offset * 2).isFinite) {
    return 'CircularProgressIndicator.strokeWidth/strokeAlign preview unavailable: '
        'the resolved stroke offset overflows finite SDK arc geometry. '
        'The stored signed values and generated Dart remain unchanged.';
  }
  if (constraints == null) return null;
  final geometry = node.properties['padding']?.value;
  final EdgeInsetsGeometry? localPadding = switch (geometry) {
    CanvasEdgeInsets p => EdgeInsets.fromLTRB(p.left, p.top, p.right, p.bottom),
    CanvasEdgeInsetsDirectional p => EdgeInsetsDirectional.fromSTEB(
      p.start,
      p.top,
      p.end,
      p.bottom,
    ),
    _ => null,
  };
  final padding =
      (localPadding ??
              theme.circularTrackPadding ??
              (material.useMaterial3 && !year2023
                  ? const EdgeInsets.all(4)
                  : EdgeInsets.zero))
          .resolve(Directionality.of(context));
  if (!padding.horizontal.isFinite || !padding.vertical.isFinite) {
    return 'CircularProgressIndicator.padding preview unavailable: resolved padding sums '
        'overflow finite layout geometry. The stored properties and generated Dart remain unchanged.';
  }
  final box = node.properties['constraints']?.value;
  final sdkConstraints = box is CanvasBoxConstraintsValue
      ? BoxConstraints(
          minWidth: box.minWidth ?? double.infinity,
          maxWidth: box.maxWidth ?? double.infinity,
          minHeight: box.minHeight ?? double.infinity,
          maxHeight: box.maxHeight ?? double.infinity,
        )
      : theme.constraints ??
            BoxConstraints(
              minWidth: material.useMaterial3 && !year2023 ? 40 : 36,
              minHeight: material.useMaterial3 && !year2023 ? 40 : 36,
            );
  final paintSize = sdkConstraints
      .enforce(constraints.deflate(padding))
      .constrain(Size.zero);
  if (!paintSize.isFinite) {
    return 'CircularProgressIndicator.constraints preview unavailable: infinite minimum size '
        'requires bounded parent constraints. The stored properties and generated Dart remain unchanged.';
  }
  final arcWidth = paintSize.width - offset * 2;
  final arcHeight = paintSize.height - offset * 2;
  if (!arcWidth.isFinite ||
      !arcHeight.isFinite ||
      !(offset + arcWidth).isFinite ||
      !(offset + arcHeight).isFinite) {
    return 'CircularProgressIndicator.constraints/strokeWidth/strokeAlign preview unavailable: '
        'the resolved size and stroke offset overflow finite SDK arc bounds. '
        'The stored properties and generated Dart remain unchanged.';
  }
  return null;
}

String? _linearProgressUnavailableMessage(
  CanvasNode node, {
  BuildContext? context,
  BoxConstraints? constraints,
}) {
  final unresolved = [
    for (final entry in const {
      'controller': 'AnimationController',
      'valueColor': 'Animation<Color?>',
    }.entries)
      if (node.properties[entry.key]?.kind == 'dartObjectReferencePresence')
        '${entry.key} (${entry.value})',
  ];
  if (unresolved.isNotEmpty) {
    return 'LinearProgressIndicator preview unavailable for ${unresolved.join(' and ')}. '
        'Generated Dart uses the configured project references; isolated Canvas does not execute project '
        'or dependency Dart or substitute an unrelated animation. Properties remain editable.';
  }
  final semanticFailure = _progressSemanticsUnavailableMessage(
    node,
    'LinearProgressIndicator',
  );
  if (semanticFailure != null) return semanticFailure;
  if (context == null) return null;
  final theme = ProgressIndicatorTheme.of(context);
  double? number(String name) {
    final value = node.properties[name]?.value;
    return value is CanvasEnumValue
        ? double.infinity
        : (value as num?)?.toDouble();
  }

  final year2023 =
      // ignore: deprecated_member_use
      node.properties['year2023']?.value as bool? ?? theme.year2023 ?? true;
  if (!Theme.of(context).useMaterial3 &&
      !year2023 &&
      node.properties.containsKey('value') &&
      (number('stopIndicatorRadius') ?? theme.stopIndicatorRadius ?? 0) > 0 &&
      !node.properties.containsKey('stopIndicatorColor') &&
      theme.stopIndicatorColor == null) {
    return 'LinearProgressIndicator.stopIndicatorColor preview unavailable: '
        'Material 2 with year2023 false and a positive stopIndicatorRadius has no '
        'local or themed stop color. The pinned SDK requires that color when painting '
        'a determinate stop indicator. Set stopIndicatorColor or its theme value; '
        'the stored properties and generated Dart remain unchanged.';
  }
  if (constraints != null && !constraints.hasBoundedWidth) {
    return 'LinearProgressIndicator preview unavailable: the parent provides '
        'unbounded width, but the SDK progress track requires a bounded width. '
        'Set a width constraint on the parent or place the indicator in a bounded slot. '
        'The stored properties and generated Dart remain unchanged.';
  }
  if (constraints != null &&
      (number('minHeight') ?? theme.linearMinHeight ?? 4).isInfinite &&
      !constraints.hasBoundedHeight) {
    return 'LinearProgressIndicator.minHeight preview unavailable: infinite '
        'minimum height requires a bounded parent height. Set a height constraint or '
        'a finite minHeight. The stored properties and generated Dart remain unchanged.';
  }
  return null;
}

String _alertDialogPreviewMessage(CanvasNode node) {
  final sources=node.properties.entries.where((e)=>e.value.kind=='dartObjectReferencePresence').map((e)=>e.key).toList();
  return 'AlertDialog ${node.id}: actions own Events; the route owns dismissal/result. '
    'Adaptive uses Cupertino on iOS/macOS (ignores Icon and Material styling), Material elsewhere '
    '(ignores adaptive controllers and inset animation). '
    '${sources.isEmpty?'':'Project sources not executed: ${sources.join(', ')}. Native defaults/theme and owned scrolling are used. '}'
    'Lazy content requires bounded intrinsic dimensions; no modal route is created in Canvas.';
}
String? _dialogPreviewMessage(CanvasNode node) {
  final shape=node.type==canvasDialogType?_cardShapePreviewUnavailableMessage(node,widgetName:'Dialog'):null;
  if(shape!=null)return shape;
  final sources=node.properties.entries.where((entry)=>entry.value.kind=='dartObjectReferencePresence').map((entry)=>entry.key).toList();
  final value=node.properties['semanticsRole']?.value;
  final role=value is CanvasEnumValue?value.value:'dialog';
  final roleMessage=!const {'none','dialog','alertDialog'}.contains(role)
    ? 'Dialog ${node.id}: $role semantics may require a supporting parent/child structure. Canvas excludes this semantic subtree; Run/Debug preserves and validates the configured role. ' : '';
  if(sources.isEmpty)return roleMessage.isEmpty?null:roleMessage;
  return '${roleMessage}Dialog ${node.id}: project sources are not executed in Canvas: ${sources.join(', ')}. '
    'The preview uses native theme/default values for these properties (duration: 100ms ordinary / zero fullscreen, curve: decelerate). '
    'Run/Debug uses the saved project sources; no modal route is created by this surface.';
}
String? _cardShapePreviewUnavailableMessage(
  CanvasNode node, {
  String widgetName = 'Card',
  String prefix = 'shape',
  String expectedType = 'ShapeBorder',
}) {
  if (node.properties[prefix]?.kind == 'dartObjectReferencePresence') {
    return _customClipperPreviewUnavailableMessage(
      widgetName: '$widgetName.$prefix',
      expectedType: expectedType,
    );
  }
  final kind = node.properties['${prefix}Kind']?.value;
  final points = node.properties['${prefix}Points']?.value as num? ?? 5;
  if ((kind == 'star' || kind == 'polygon') &&
      points > _maximumCanvasCardShapePoints) {
    return '$widgetName.$prefix $kind preview unavailable: requested $points points exceeds the isolated Canvas budget of $_maximumCanvasCardShapePoints. Generated Dart preserves the configured $expectedType; child and properties remain editable.';
  }
  return null;
}

final Uint8List _unavailableImageBytes = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAA'
  'AARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/'
  'j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=',
);

TextDirection _canvasTextDirection(String locale) {
  final language = locale.split(RegExp('[-_]')).first.toLowerCase();
  return const {
        'ar',
        'arc',
        'ckb',
        'dv',
        'fa',
        'he',
        'ks',
        'ku',
        'nqo',
        'ps',
        'sd',
        'syr',
        'ug',
        'ur',
        'yi',
      }.contains(language)
      ? TextDirection.rtl
      : TextDirection.ltr;
}

class NativeCanvasApp extends StatelessWidget {
  const NativeCanvasApp({required this.runtime, super.key});

  final CanvasRuntimeController runtime;

  @override
  Widget build(BuildContext context) {
    runtime.bindSurfaceView(View.of(context));
    return AnimatedBuilder(
      animation: runtime,
      builder: (context, _) {
        final model = runtime.model;
        if (model == null) {
          return MaterialApp(
            debugShowCheckedModeBanner: false,
            home: _RuntimeStatus(
              message:
                  runtime.errorMessage ??
                  (runtime.closed
                      ? 'Flutter Canvas session ended'
                      : 'Waiting for the Flutter Designer model…'),
              failed: runtime.errorMessage != null,
            ),
          );
        }
        return CanvasModelApp(
          model: model,
          imageResources: runtime.imageResources,
          onImageError: runtime.reportImageRenderError,
          selectedWidgetId: runtime.selectedWidgetId,
          onSelected: runtime.selectFromCanvas,
          onInteraction: runtime.interactFromCanvas,
          interactionInputSynchronized: runtime.interactionInputSynchronized,
          onDeleteSelected: runtime.deleteSelectedFromCanvas,
          inlineTextEditEnabled: runtime.inlineTextEditNegotiated,
          onInlineTextCommit: runtime.commitInlineTextEdit,
          dropHoverTarget: runtime.dropHoverTarget,
          dropIndicatorKind: runtime.hasWidgetMovePreview
              ? CanvasDropIndicatorKind.widgetMove
              : CanvasDropIndicatorKind.paletteInsertion,
          onDropResolverChanged: runtime.setDropResolver,
          onMovePreviewResolverChanged: runtime.setMovePreviewResolver,
          viewportPresentation: runtime.viewportPresentation,
          onViewportPresentationChanged: runtime.updateViewportFromCanvas,
          onViewportMetricsChanged: runtime.reportViewportMetrics,
        );
      },
    );
  }
}

/// One decoded presentation with its exact Flutter adaptive target applied.
class CanvasModelApp extends StatelessWidget {
  const CanvasModelApp({
    required this.model,
    required this.selectedWidgetId,
    required this.onSelected,
    this.imageResources,
    this.onImageError,
    this.onInteraction,
    this.interactionInputSynchronized = true,
    this.onDeleteSelected = _ignoreDeleteSelected,
    this.inlineTextEditEnabled = false,
    this.onInlineTextCommit = _ignoreInlineTextCommit,
    this.dropHoverTarget,
    this.dropIndicatorKind = CanvasDropIndicatorKind.paletteInsertion,
    this.onDropResolverChanged,
    this.onMovePreviewResolverChanged,
    this.viewportPresentation,
    this.onViewportPresentationChanged,
    this.onViewportMetricsChanged,
    super.key,
  });

  final CanvasModel model;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
  final CanvasImageResourceBundle? imageResources;
  final CanvasImageErrorReporter? onImageError;
  final VoidCallback? onInteraction;
  final bool interactionInputSynchronized;
  final bool Function() onDeleteSelected;
  final bool inlineTextEditEnabled;
  final bool Function(String, String, bool) onInlineTextCommit;
  final CanvasDropTarget? dropHoverTarget;
  final CanvasDropIndicatorKind dropIndicatorKind;
  final ValueChanged<CanvasDropResolver?>? onDropResolverChanged;
  final ValueChanged<CanvasMovePreviewResolver?>? onMovePreviewResolverChanged;
  final CanvasViewportPresentation? viewportPresentation;
  final ValueChanged<CanvasViewportPresentation>? onViewportPresentationChanged;
  final ValueChanged<CanvasViewportMetrics>? onViewportMetricsChanged;

  @override
  Widget build(BuildContext context) {
    final dark = model.profile.brightness == 'dark';
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      themeMode: dark ? ThemeMode.dark : ThemeMode.light,
      theme: _theme(model.profile, Brightness.light),
      darkTheme: _theme(model.profile, Brightness.dark),
      home: Directionality(
        textDirection: _canvasTextDirection(model.profile.locale),
        child: CanvasDocumentView(
          model: model,
          imageResources: imageResources,
          onImageError: onImageError,
          selectedWidgetId: selectedWidgetId,
          onSelected: onSelected,
          onInteraction: onInteraction,
          interactionInputSynchronized: interactionInputSynchronized,
          onDeleteSelected: onDeleteSelected,
          inlineTextEditEnabled: inlineTextEditEnabled,
          onInlineTextCommit: onInlineTextCommit,
          dropHoverTarget: dropHoverTarget,
          dropIndicatorKind: dropIndicatorKind,
          onDropResolverChanged: onDropResolverChanged,
          onMovePreviewResolverChanged: onMovePreviewResolverChanged,
          viewportPresentation: viewportPresentation,
          onViewportPresentationChanged: onViewportPresentationChanged,
          onViewportMetricsChanged: onViewportMetricsChanged,
        ),
      ),
    );
  }
}

class CanvasDocumentView extends StatefulWidget {
  const CanvasDocumentView({
    required this.model,
    required this.selectedWidgetId,
    required this.onSelected,
    this.imageResources,
    this.onImageError,
    this.onInteraction,
    this.interactionInputSynchronized = true,
    this.onDeleteSelected = _ignoreDeleteSelected,
    this.inlineTextEditEnabled = false,
    this.onInlineTextCommit = _ignoreInlineTextCommit,
    this.dropHoverTarget,
    this.dropIndicatorKind = CanvasDropIndicatorKind.paletteInsertion,
    this.onDropResolverChanged,
    this.onMovePreviewResolverChanged,
    this.viewportPresentation,
    this.onViewportPresentationChanged,
    this.onViewportMetricsChanged,
    super.key,
  });

  final CanvasModel model;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
  final CanvasImageResourceBundle? imageResources;
  final CanvasImageErrorReporter? onImageError;
  final VoidCallback? onInteraction;
  final bool interactionInputSynchronized;
  final bool Function() onDeleteSelected;
  final bool inlineTextEditEnabled;
  final bool Function(String, String, bool) onInlineTextCommit;
  final CanvasDropTarget? dropHoverTarget;
  final CanvasDropIndicatorKind dropIndicatorKind;
  final ValueChanged<CanvasDropResolver?>? onDropResolverChanged;
  final ValueChanged<CanvasMovePreviewResolver?>? onMovePreviewResolverChanged;
  final CanvasViewportPresentation? viewportPresentation;
  final ValueChanged<CanvasViewportPresentation>? onViewportPresentationChanged;
  final ValueChanged<CanvasViewportMetrics>? onViewportMetricsChanged;

  @override
  State<CanvasDocumentView> createState() => _CanvasDocumentViewState();
}

class _CanvasDocumentViewState extends State<CanvasDocumentView> {
  static const int _microsPerSurface = 1000000;
  static const double _minimumTerminalBand = 36;
  static const double _minimumZeroSizedWidgetTarget = 36;
  static const double _moveInsertionMarkerExtent = 12;
  static const double _scaffoldFabDropExtent = 72;

  final GlobalKey _surfaceKey = GlobalKey();
  final Map<String, GlobalKey> _nodeKeys = <String, GlobalKey>{};
  // Geometry aliases only: never reuse retained switcher GlobalKeys for ordinary rendering.
  final Map<String, GlobalKey> _switcherGeometryKeys = <String, GlobalKey>{};
  final Map<String, GlobalKey<_MenuAnchorPreviewState>> _switcherMenuKeys = {};
  final Map<String, GlobalKey> _switcherTooltipKeys = {};
  GlobalKey? _tooltipPreviewKeyFor(String id) => _switcherTooltipKeys[id] ?? _tooltipPreviewKeys[id];
  GlobalKey? _geometryNodeKey(String id) => _switcherGeometryKeys[id] ?? _nodeKeys[id];
  final Map<String, GlobalKey<_MenuAnchorPreviewState>> _menuAnchorPreviewKeys =
      {};
  Rect? _menuChromeRect;
  String? _menuChromeOwnerId;
  bool _menuChromeOpen = false;
  void _menuChanged() {
    if (mounted) _refreshZeroSizedWidgetTargetsAfterFrame();
  }

  _MenuAnchorPreviewState? _menuState(String id) =>
      (_switcherMenuKeys[id] ?? _menuAnchorPreviewKeys[id])?.currentState;
  RenderBox? _menuPanelBox(String id) {
    final menu = _menuState(id);
    if (menu == null || !menu.interactive) return null;
    RenderBox? box;
    menu._menuProbeKey.currentContext?.visitAncestorElements((element) {
      if (element.widget is SingleChildScrollView) {
        final render = element.findRenderObject();
        if (render is RenderBox && render.attached) box = render;
        return false;
      }
      return true;
    });
    return box;
  }

  final Map<String, GlobalKey> _expansionTilePreviewKeys =
      <String, GlobalKey>{};
  final Map<String, GlobalKey> _tooltipPreviewKeys = <String, GlobalKey>{};
  final Map<String, GlobalKey> _tooltipAnchorPreviewKeys =
      <String, GlobalKey>{};
  final Set<String> _tooltipAncestorIds = {};
  // Structural reparenting while an OverlayPortal is open cannot happen inside
  // the viewport LayoutBuilder's layout callback. Scalar edits and selection
  // retain State; changed Tooltip ancestry gets fresh preview keys so the old
  // overlay is disposed instead of GlobalKey-reparented across layout owners.
  void _invalidateStructurallyMovedTooltipKeys(CanvasNode previous) {
    String sdkTopology(CanvasNode node) {
      if (node.type != 'flutter.widgets.Container') {
        final source = _tooltipAncestorSdkTopology(node);
        if (node.type == 'flutter.material.ExpansionTile') {
          final ownerContext = _geometryNodeKey(node.id)?.currentContext ?? context;
          final owner = ownerContext
              .findAncestorWidgetOfExactType<_CanvasNodeView>();
          final theme = ExpansionTileTheme.of(ownerContext);
          bool localShape(String family) =>
              node.properties['${family}Kind']?.value != null &&
              _cardShapePreviewUnavailableMessage(
                    node,
                    widgetName: 'ExpansionTile',
                    prefix: family,
                  ) ==
                  null;
          final shaped =
              localShape('shape') ||
              localShape('collapsedShape') ||
              theme.shape != null ||
              theme.collapsedShape != null;
          Color? localColor(String name) {
            final property = node.properties[name];
            if (property?.kind == 'color') return Color(property!.value as int);
            final value = property?.value;
            return value is CanvasThemeToken
                ? owner?._themeColor(ownerContext, value)
                : null;
          }

          // SDK chooses Material vs DecoratedBox from shape presence, then
          // inserts transparent Material only for positive animated alpha.
          // Endpoint zero/nonzero changes may cross that branch; ordinary RGB
          // and positive-to-positive alpha updates never change this token.
          final alphaBranches = [
            ((localColor('collapsedBackgroundColor') ??
                            theme.collapsedBackgroundColor)
                        ?.a ??
                    0) >
                0,
            ((localColor('backgroundColor') ?? theme.backgroundColor)?.a ?? 0) >
                0,
            (theme.backgroundColor?.a ?? 0) > 0,
          ];
          return '$source@expansion:${shaped ? 'shape' : jsonEncode(alphaBranches)}';
        }
        return node.type == 'flutter.material.FloatingActionButton'
            ? '$source@fab:${_fabElevations(node, context).every((value) => value.isInfinite)}:${_fabLayoutMessage(node, context) != null}'
            : source;
      }
      bool present(String name) =>
          node.properties[name] != null &&
          node.properties[name]!.kind != 'null';
      final decoration = node.properties['decoration']?.value;
      final decorationPadding =
          decoration is CanvasBoxDecorationValue && decoration.border != null;
      final clip = node.properties['clipBehavior']?.value;
      // The actual pinned Container inserts each of these wrappers only while
      // present. Numeric/color/shape changes within the same topology preserve
      // the Tooltip's State. The last bit is the Designer inset-guide wrapper.
      return '@container:${[present('alignment'), present('padding') || decorationPadding, present('color'), clip is CanvasEnumValue && clip.value != 'none', present('decoration'), present('foregroundDecoration'), present('constraints') || present('width') || present('height'), present('margin'), present('transform'), present('padding') || present('margin')].map((value) => value ? '1' : '0').join()}';
    }

    Map<
      String,
      ({String path, String tooltips, bool contains, bool? rawTooltip})
    >
    placements(CanvasNode root) {
      final result =
          <
            String,
            ({String path, String tooltips, bool contains, bool? rawTooltip})
          >{};
      final containsTooltip = <String, bool>{};
      bool recordTooltipDescendants(CanvasNode node) {
        var contains = _ownsNativeTooltip(node) || _ownsNativeMenu(node);
        for (final slot in node.slots.values) {
          for (final child in slot.children) {
            if (recordTooltipDescendants(child)) contains = true;
          }
        }
        return containsTooltip[node.id] = contains;
      }

      recordTooltipDescendants(root);
      bool visit(CanvasNode node, String path, String tooltips, bool visible) {
        if (node.type == 'flutter.material.TooltipVisibility') {
          visible = node.properties['visible']!.value as bool;
        }
        path = '$path:${node.type}${sdkTopology(node)}';
        final isTooltip = _ownsNativeTooltip(node);
        final isOverlayOwner = isTooltip || _ownsNativeMenu(node);
        final ancestry = isOverlayOwner ? '$tooltips/${node.id}' : tooltips;
        var contains = isOverlayOwner;
        for (final slot in node.slots.entries) {
          for (var i = 0; i < slot.value.children.length; i++) {
            final child = slot.value.children[i];
            // The pinned Tooltip adds/removes its RawTooltip when its nearest
            // visibility scope changes. Only a nested Tooltip branch needs
            // fresh keys: its open Portal cannot be reparented during layout.
            // Ordinary anchors and the outer Tooltip retain their SDK State.
            final rawTooltipBranch = isTooltip && containsTooltip[child.id]!
                ? '@rawTooltip:$visible'
                : '';
            if (visit(
              child,
              '$path$rawTooltipBranch/${slot.key}/$i/${child.id}',
              ancestry,
              visible,
            )) {
              contains = true;
            }
          }
        }
        final nested =
            isTooltip &&
            node.slots.values.any(
              (slot) =>
                  slot.children.any((child) => containsTooltip[child.id]!),
            );
        result[node.id] = (
          path: path,
          tooltips: ancestry,
          contains: contains,
          rawTooltip: nested ? visible : null,
        );
        return contains;
      }

      visit(root, root.id, '', true);
      return result;
    }

    final old = placements(previous);
    final next = placements(widget.model.root);
    for (final entry in next.entries) {
      final before = old[entry.key];
      if (before == null) continue;
      final after = entry.value;
      if (before.rawTooltip != after.rawTooltip) {
        final preview = _tooltipPreviewKeyFor(entry.key)?.currentState;
        if (preview is _TooltipPreviewState) preview.anchorKey = GlobalKey();
      }
      final affected =
          before.tooltips != after.tooltips ||
          before.contains != after.contains ||
          (before.path != after.path &&
              (before.contains ||
                  after.contains ||
                  before.tooltips.isNotEmpty ||
                  after.tooltips.isNotEmpty));
      if (!affected) continue;
      _nodeKeys.remove(entry.key);
      _tooltipPreviewKeys.remove(entry.key);
      _tooltipAnchorPreviewKeys.remove(entry.key);
      _expansionTilePreviewKeys.remove(entry.key);
      _menuAnchorPreviewKeys.remove(entry.key);
    }
  }

  void _refreshTooltipAncestors() {
    _tooltipAncestorIds.clear();
    bool visit(CanvasNode node) {
      var contains = _ownsNativeTooltip(node) || _ownsNativeMenu(node);
      for (final slot in node.slots.values) {
        for (final child in slot.children) {
          if (visit(child)) contains = true;
        }
      }
      if (contains) _tooltipAncestorIds.add(node.id);
      return contains;
    }

    visit(widget.model.root);
  }

  (int, Duration)? _lastTooltipSelectionPointer;
  String? _tooltipSelectedAnchorId;
  void _selectTooltipAnchor(PointerDownEvent event, String id) {
    final pointer = (event.pointer, event.timeStamp);
    if (_lastTooltipSelectionPointer == pointer) return;
    _lastTooltipSelectionPointer = pointer;
    _tooltipSelectedAnchorId = null;
    // A menu item/descendant is already inside Flutter's native focus scope.
    // Selecting it must not move focus to the document node: doing so makes
    // SubmenuButton close its own menu before MenuItemButton receives the tap.
    // Tooltip anchors outside a native menu retain the document focus behavior.
    _selectWidget(
      id,
      requestCanvasFocus: !_pointerInsideInteractiveMenu(event.position),
    );
    _tooltipSelectedAnchorId = id;
  }

  bool _pointerInsideInteractiveMenu(Offset position) {
    for (final entry in {..._menuAnchorPreviewKeys, ..._switcherMenuKeys}.entries) {
      final box = _menuPanelBox(entry.key);
      if (box == null || !box.hasSize || !box.attached) continue;
      final rect = box.localToGlobal(Offset.zero) & box.size;
      if (rect.contains(position)) return true;
    }
    return false;
  }

  void _finishTooltipAnchorPointer(PointerEvent event) {
    final pointer = _lastTooltipSelectionPointer;
    if (pointer?.$1 != event.pointer) return;
    scheduleMicrotask(() {
      if (_lastTooltipSelectionPointer == pointer) {
        _lastTooltipSelectionPointer = null;
        _tooltipSelectedAnchorId = null;
      }
    });
  }

  final FocusNode _focusNode = FocusNode(debugLabel: 'native-canvas');
  final Map<Object, List<String>> _fabHeroOwners = {};
  void _refreshFabHeroOwners() {
    _fabHeroOwners.clear();
    Iterable<CanvasNode> descendants(CanvasNode current) sync* {
      yield current;
      for (final slot in current.slots.values) {
        for (final child in slot.children) {
          yield* descendants(child);
        }
      }
    }

    for (final node in descendants(widget.model.root)) {
      if (node.type == 'flutter.material.FloatingActionButton') {
        final tag = _fabHeroTag(node);
        if (tag != null) {
          _fabHeroOwners.putIfAbsent(tag, () => []).add(node.id);
        }
      }
    }
  }

  String? _fabDuplicateHeroMessage(CanvasNode node) {
    final tag = _fabHeroTag(node);
    final matches = _fabHeroOwners[tag] ?? const <String>[];
    return matches.length < 2
        ? null
        : 'FloatingActionButton.heroTag warning: stored widgets ${matches.join(', ')} share ${identical(tag, _defaultFabHeroTag) ? 'the SDK default Hero tag' : 'an equal literal Hero tag'}. '
              'Mounted Heroes on one route must have unique tags for flights. Canvas preserves the SDK tags and does not run route transitions.';
  }

  _ViewportGeometry? _viewportGeometry;
  CanvasViewportMetrics? _lastReportedViewportMetrics;
  List<_ZeroSizedWidgetTargetGroup> _zeroSizedWidgetTargets = const [];
  bool _zeroSizedWidgetTargetRefreshScheduled = false;
  _InlineTextEditSession? _inlineTextEditSession;

  @override
  void initState() {
    super.initState();
    _refreshFabHeroOwners();
    _refreshTooltipAncestors();
    widget.onDropResolverChanged?.call(_resolveDrop);
    widget.onMovePreviewResolverChanged?.call(_resolveMovePreview);
  }

  @override
  void didUpdateWidget(CanvasDocumentView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.model != widget.model) {
      _switcherGeometryKeys.clear();
      _switcherMenuKeys.clear();
      _switcherTooltipKeys.clear();
      _invalidateStructurallyMovedTooltipKeys(oldWidget.model.root);
      _refreshFabHeroOwners();
      _refreshTooltipAncestors();
    }
    _nodeKeys.removeWhere((id, _) => !widget.model.widgetIds.contains(id));
    _menuAnchorPreviewKeys.removeWhere(
      (id, _) => !widget.model.widgetIds.contains(id),
    );
    _tooltipPreviewKeys.removeWhere(
      (id, _) => !widget.model.widgetIds.contains(id),
    );
    _tooltipAnchorPreviewKeys.removeWhere(
      (id, _) => !widget.model.widgetIds.contains(id),
    );
    _expansionTilePreviewKeys.removeWhere(
      (id, _) => !widget.model.widgetIds.contains(id),
    );
    if (_inlineTextEditSession case final session?
        when !_inlineTextEditStillCurrent(session)) {
      _inlineTextEditSession = null;
      _restoreCanvasFocusAfterFrame();
    }
    if (oldWidget.onDropResolverChanged != widget.onDropResolverChanged) {
      oldWidget.onDropResolverChanged?.call(null);
      widget.onDropResolverChanged?.call(_resolveDrop);
    }
    if (oldWidget.onMovePreviewResolverChanged !=
        widget.onMovePreviewResolverChanged) {
      oldWidget.onMovePreviewResolverChanged?.call(null);
      widget.onMovePreviewResolverChanged?.call(_resolveMovePreview);
    }
  }

  @override
  void dispose() {
    widget.onDropResolverChanged?.call(null);
    widget.onMovePreviewResolverChanged?.call(null);
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final profile = widget.model.profile;
    final viewport = Size(profile.logicalWidth, profile.logicalHeight);
    final dark = profile.brightness == 'dark';
    final presentation =
        widget.viewportPresentation ??
        CanvasViewportPresentation.fit(widget.model);
    return Focus(
      focusNode: _focusNode,
      onKeyEvent: _onKeyEvent,
      child: Scaffold(
        backgroundColor: dark
            ? const Color(0xff202124)
            : const Color(0xffe7e9ed),
        body: SizedBox.expand(
          key: _surfaceKey,
          child: LayoutBuilder(
            builder: (context, constraints) {
              final geometry = _ViewportGeometry.calculate(
                surface: constraints.biggest,
                viewport: viewport,
                presentation: presentation,
              );
              _viewportGeometry = geometry;
              _reportViewportAfterFrame(presentation, geometry);
              _refreshZeroSizedWidgetTargetsAfterFrame();
              return Listener(
                key: const ValueKey('canvas-interaction-surface'),
                behavior: HitTestBehavior.opaque,
                onPointerDown: (event) {
                  if (widget.interactionInputSynchronized) {
                    if (_inlineTextEditSession == null &&
                        !_pointerInsideInteractiveMenu(event.position)) {
                      _focusNode.requestFocus();
                    }
                    widget.onInteraction?.call();
                  }
                },
                onPointerSignal: widget.interactionInputSynchronized
                    ? _onPointerSignal
                    : null,
                child: Stack(
                  clipBehavior: Clip.hardEdge,
                  children: [
                    Positioned(
                      left: geometry.left,
                      top: geometry.top,
                      width: geometry.renderedWidth,
                      height: geometry.renderedHeight,
                      child: FittedBox(
                        fit: BoxFit.fill,
                        child: Container(
                          width: viewport.width,
                          height: viewport.height,
                          clipBehavior: Clip.hardEdge,
                          decoration: BoxDecoration(
                            color: dark
                                ? const Color(0xff121212)
                                : Colors.white,
                            border: Border.all(
                              color: dark
                                  ? const Color(0xff5f6368)
                                  : const Color(0xff9aa0a6),
                            ),
                            boxShadow: const [
                              BoxShadow(
                                color: Color(0x33000000),
                                blurRadius: 12,
                                offset: Offset(0, 4),
                              ),
                            ],
                          ),
                          child: MediaQuery(
                            data: MediaQuery.of(context).copyWith(
                              size: viewport,
                              devicePixelRatio: profile.devicePixelRatio,
                              textScaler: TextScaler.linear(
                                profile.textScaleFactor,
                              ),
                              platformBrightness: dark
                                  ? Brightness.dark
                                  : Brightness.light,
                            ),
                            child: _CanvasViewportOverlay(
                              child: ClipRect(
                                child: _CanvasNodeView(
                                  node: widget.model.root,
                                  imageResources:
                                      widget.imageResources ??
                                      CanvasImageResourceBundle.empty,
                                  onImageError: widget.onImageError,
                                  selectedWidgetId: widget.selectedWidgetId,
                                  onSelected: _selectWidget,
                                  nodeKey: _nodeKey,
                                  designerFocusParent: _focusNode,
                                  overlayScale: geometry.scale,
                                  inlineTextEditEnabled:
                                      widget.inlineTextEditEnabled &&
                                      widget.interactionInputSynchronized,
                                  inlineTextEditingWidgetId:
                                      _inlineTextEditSession?.widgetId,
                                  onBeginInlineTextEdit: _beginInlineTextEdit,
                                  onCommitInlineTextEdit: _commitInlineTextEdit,
                                  onCancelInlineTextEdit: _cancelInlineTextEdit,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                    for (final target in _zeroSizedWidgetTargets)
                      Positioned.fromRect(
                        rect: target.rect,
                        child: _ZeroSizedWidgetTarget(
                          widgetIds: target.widgetIds,
                          widgetTypes: target.widgetTypes,
                          previewUnavailableMessages:
                              target.previewUnavailableMessages,
                          selectedWidgetId: widget.selectedWidgetId,
                          dark: dark,
                          onSelected: _selectWidget,
                        ),
                      ),
                    if (widget.dropHoverTarget case final target?
                        when target.zone != null)
                      _DropZoneOverlay(
                        target: target,
                        constraints: constraints,
                        indicatorKind: widget.dropIndicatorKind,
                      ),
                    if (_menuChromeRect case final rect?
                        when _menuChromeOwnerId != null)
                      Positioned.fromRect(
                        rect: rect,
                        child: Material(
                          elevation: 2,
                          borderRadius: BorderRadius.circular(4),
                          child: TextButton(
                            key: ValueKey(
                              'canvas-menu-preview-$_menuChromeOwnerId',
                            ),
                            onPressed: !widget.interactionInputSynchronized
                                ? null
                                : _menuChromeOpen
                                ? () {
                                    _menuState(
                                      _menuChromeOwnerId!,
                                    )?.controller.close();
                                    _menuChanged();
                                  }
                                : () {
                                    _menuState(
                                      _menuChromeOwnerId!,
                                    )?.controller.open();
                                    _menuChanged();
                                  },
                            child: Text(
                              _menuChromeOpen
                                  ? 'Close preview'
                                  : 'Preview menu',
                            ),
                          ),
                        ),
                      ),
                    if (geometry.horizontalScrollable)
                      Positioned(
                        left: 8,
                        right: geometry.verticalScrollable ? 20 : 8,
                        bottom: 4,
                        height: _CanvasViewportScrollbar.hitThickness,
                        child: _CanvasViewportScrollbar(
                          key: const ValueKey(
                            'canvas-horizontal-viewport-scrollbar',
                          ),
                          axis: Axis.horizontal,
                          valueMicros: presentation.horizontalScrollMicros,
                          viewportFraction: geometry.horizontalViewportFraction,
                          onChanged: (value) => _changeViewport(
                            presentation.copyWith(
                              horizontalScrollMicros: value,
                            ),
                          ),
                        ),
                      ),
                    if (geometry.verticalScrollable)
                      Positioned(
                        top: 8,
                        bottom: geometry.horizontalScrollable ? 20 : 8,
                        right: 4,
                        width: _CanvasViewportScrollbar.hitThickness,
                        child: _CanvasViewportScrollbar(
                          key: const ValueKey(
                            'canvas-vertical-viewport-scrollbar',
                          ),
                          axis: Axis.vertical,
                          valueMicros: presentation.verticalScrollMicros,
                          viewportFraction: geometry.verticalViewportFraction,
                          onChanged: (value) => _changeViewport(
                            presentation.copyWith(verticalScrollMicros: value),
                          ),
                        ),
                      ),
                    if (!widget.interactionInputSynchronized)
                      const Positioned.fill(
                        child: _InteractionBarrierOverlay(),
                      ),
                  ],
                ),
              );
            },
          ),
        ),
      ),
    );
  }

  void _refreshZeroSizedWidgetTargetsAfterFrame() {
    if (_zeroSizedWidgetTargetRefreshScheduled) {
      return;
    }
    _zeroSizedWidgetTargetRefreshScheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _zeroSizedWidgetTargetRefreshScheduled = false;
      if (!mounted) {
        return;
      }
      final surface = _renderBox(_surfaceKey);
      final currentGeometry = _viewportGeometry;
      if (surface == null || currentGeometry == null) {
        return;
      }
      final surfaceRect = _finiteGlobalRect(surface);
      if (surfaceRect == null) {
        return;
      }
      final viewportRect = Rect.fromLTWH(
        currentGeometry.left,
        currentGeometry.top,
        currentGeometry.renderedWidth,
        currentGeometry.renderedHeight,
      );
      final coincidentTargets = <Rect, List<CanvasNode>>{};
      for (final node in _zeroSizedDesignerTargets(widget.model.root)) {
        if (!_isInPaintedSliverVisibilityBranch(widget.model.root, node.id)) continue;
        if(node.type=='flutter.widgets.TableRow'||isCanvasDataDescriptor(node.type)) {
          final rect=isCanvasDataDescriptor(node.type)?_dataDescriptorGlobalRect(node):_tableRowGlobalRect(node);
          if(rect!=null) {
            final rendered=rect.shift(-surfaceRect.topLeft);
            final handle=_ignorePointerHandleRect(rendered,viewportRect);
            coincidentTargets.putIfAbsent(_boundedDesignerHitRect(handle,viewportRect),()=> <CanvasNode>[]).add(node);
          }
          continue;
        }
        if (isCanvasSliverWidgetType(node.type)) {
          final rect = _sliverGlobalRect(node, surfaceRect);
          if (rect != null) {
            // Small external handle avoids stealing taps from ordinary children.
            final render = _geometryNodeKey(node.id)!.currentContext!.findRenderObject() as RenderSliver;
            final direction = applyGrowthDirectionToAxisDirection(
                render.constraints.axisDirection, render.constraints.growthDirection);
            final width = math.min(24.0, rect.width);
            final height = math.min(24.0, rect.height);
            final handle = Rect.fromLTWH(
                direction == AxisDirection.left ? rect.right - width : rect.left,
                direction == AxisDirection.up ? rect.bottom - height : rect.top,
                width, height).shift(-surfaceRect.topLeft);
            coincidentTargets.putIfAbsent(handle, () => <CanvasNode>[]).add(node);
          }
          continue;
        }
        final box = _renderBox(_geometryNodeKey(node.id));
        // Spacer owns no child where instrumentation can safely live. An
        // external target is therefore required even when stretch gives its
        // internal SizedBox a non-zero cross-axis extent.
        final ignoresPointers = _ignoresPointersForNode(node);
        final alwaysUsesSurfaceOverlay =
            node.type == canvasSpacerWidgetType || ignoresPointers;
        if (box == null || (!alwaysUsesSurfaceOverlay && !box.size.isEmpty)) {
          continue;
        }
        final globalRect = _finiteGlobalRect(box);
        if (globalRect == null) {
          continue;
        }
        final rendered = globalRect.shift(-surfaceRect.topLeft);
        // The real IgnorePointer must remain transparent to pointer hits.
        // Put its Designer selection handle outside a nonempty body when
        // space permits; never replace the body with a full-area hit target.
        final handleRect = ignoresPointers && !box.size.isEmpty
            ? _ignorePointerHandleRect(rendered, viewportRect)
            : rendered;
        final target = _boundedDesignerHitRect(
          handleRect,
          viewportRect,
          minimumExtent: _minimumZeroSizedWidgetTarget,
        );
        coincidentTargets.putIfAbsent(target, () => <CanvasNode>[]).add(node);
      }
      final targets = <_ZeroSizedWidgetTargetGroup>[
        for (final target in coincidentTargets.entries)
          _ZeroSizedWidgetTargetGroup(
            rect: target.key,
            widgetIds: List.unmodifiable([
              for (final node in target.value) node.id,
            ]),
            widgetTypes: List.unmodifiable([
              for (final node in target.value) node.type,
            ]),
            previewUnavailableMessages: List.unmodifiable([
              for (final node in target.value)
                _customClipperPreviewUnavailableMessageForNode(
                  node,
                  context: _geometryNodeKey(node.id)?.currentContext,
                  constraints: _renderBox(_geometryNodeKey(node.id))?.constraints,
                ),
            ]),
          ),
      ];
      CanvasNode? owningMenu(CanvasNode node) {
        final selected = widget.selectedWidgetId;
        if (selected == null || _findCanvasNode(node, selected) == null) {
          return null;
        }
        for (final slot in node.slots.values) {
          for (final child in slot.children) {
            final nested = owningMenu(child);
            if (nested != null) return nested;
          }
        }
        return _ownsNativeMenu(node) && _renderBox(_geometryNodeKey(node.id)) != null
            ? node
            : null;
      }

      final menu = owningMenu(widget.model.root);
      final anchor = menu == null ? null : _renderBox(_geometryNodeKey(menu.id));
      final anchorRect = anchor == null
          ? null
          : _finiteGlobalRect(anchor)?.shift(-surfaceRect.topLeft);
      final chrome = anchorRect == null
          ? null
          : Rect.fromLTWH(
              anchorRect.left.clamp(
                0.0,
                math.max(0.0, surface.size.width - 132),
              ),
              (anchorRect.top >= 36
                      ? anchorRect.top - 36
                      : anchorRect.bottom + 4)
                  .clamp(0.0, math.max(0.0, surface.size.height - 32)),
              132,
              32,
            );
      final open =
          menu != null && (_menuState(menu.id)?.controller.isOpen ?? false);
      if (_sameTargets(_zeroSizedWidgetTargets, targets) &&
          _menuChromeRect == chrome &&
          _menuChromeOwnerId == menu?.id &&
          _menuChromeOpen == open) {
        return;
      }
      setState(() {
        _zeroSizedWidgetTargets = List.unmodifiable(targets);
        _menuChromeRect = chrome;
        _menuChromeOwnerId = menu?.id;
        _menuChromeOpen = open;
      });
    });
  }

  Iterable<CanvasNode> _zeroSizedDesignerTargets(CanvasNode node) sync* {
    if (isCanvasSliverWidgetType(node.type) ||
        node.type == 'flutter.widgets.LayoutBuilder' ||
        node.type == 'flutter.widgets.OrientationBuilder' ||
        node.type == 'flutter.widgets.DeviceOrientationBuilder' ||
        node.type == 'flutter.widgets.ListenableBuilder' ||
        node.type == 'flutter.widgets.AnimatedBuilder' ||
        node.type == 'flutter.widgets.TweenAnimationBuilder' ||
        node.type == 'flutter.widgets.ValueListenableBuilder' ||
        (node.type == 'flutter.widgets.SizedBox' &&
            (node.slot('child')?.children.isEmpty ?? true)) ||
        (node.type == 'flutter.widgets.Container' &&
            (node.slot('child')?.children.isEmpty ?? true)) ||
        node.type == 'flutter.widgets.DecoratedBox' ||
        node.type == 'flutter.widgets.DecoratedBoxTransition' ||
        node.type == 'flutter.widgets.AlignTransition' ||
        node.type == 'flutter.widgets.MatrixTransition' ||
        node.type == 'flutter.widgets.ModalBarrier' ||
        node.type == 'flutter.widgets.AnimatedModalBarrier' ||
        node.type == 'flutter.widgets.FadeInImage' ||
        node.type == 'flutter.widgets.RawImage' || node.type == 'flutter.widgets.ColorFiltered' ||
        node.type == 'flutter.widgets.ImageFiltered' || node.type == 'flutter.widgets.BackdropFilter' || node.type == 'flutter.widgets.BackdropFilter.grouped' || node.type == 'flutter.widgets.BackdropGroup' || node.type == 'flutter.widgets.ShaderMask' || node.type == 'flutter.widgets.CustomPaint' || isCanvasDataDescriptor(node.type) || node.type == 'flutter.widgets.TableRow' || node.type == 'flutter.widgets.Table' || node.type == 'flutter.widgets.TableCell' || node.type == 'flutter.widgets.Flow' || node.type == 'flutter.widgets.Flow.unwrapped' || node.type == 'flutter.widgets.CustomSingleChildLayout' || node.type == 'flutter.widgets.CustomMultiChildLayout' || node.type == 'flutter.widgets.LayoutId' ||
        node.type == 'flutter.material.AnimatedIcon' ||
        node.type == 'flutter.widgets.ExcludeSemantics' ||
        node.type == 'flutter.widgets.ExcludeFocus' ||
        node.type == 'flutter.widgets.ExcludeFocusTraversal' ||
        node.type == 'flutter.widgets.Visibility' ||
        node.type == 'flutter.widgets.TickerMode' ||
        node.type == 'flutter.widgets.DefaultTextHeightBehavior' ||
        node.type == 'flutter.widgets.DefaultSelectionStyle' ||
        node.type == 'flutter.widgets.IconTheme' ||
        node.type == 'flutter.widgets.IgnorePointer' ||
        node.type == 'flutter.widgets.GestureDetector' ||
        node.type == 'flutter.widgets.Listener' ||
        node.type == 'flutter.widgets.MouseRegion' ||
        node.type == 'flutter.widgets.Focus' ||
        node.type == 'flutter.widgets.NotificationListener' ||
        node.type == 'flutter.widgets.AbsorbPointer' ||
        node.type == 'flutter.widgets.BlockSemantics' ||
        node.type == 'flutter.widgets.MergeSemantics' ||
        node.type == 'flutter.widgets.IndexedSemantics' ||
        node.type == 'flutter.widgets.RepaintBoundary' ||
        node.type == 'flutter.widgets.AnimatedPadding' ||
        node.type == 'flutter.widgets.AnimatedSlide' ||
        node.type == 'flutter.widgets.AnimatedScale' ||
        node.type == 'flutter.widgets.RotationTransition' ||
        node.type == 'flutter.widgets.SizeTransition' ||
        node.type == 'flutter.widgets.ScaleTransition' ||
        isCanvasStackPositionedWidgetType(node.type) ||
        node.type == 'flutter.widgets.AnimatedPhysicalModel' ||
        node.type == 'flutter.widgets.AnimatedFractionallySizedBox' ||
        node.type == 'flutter.widgets.AnimatedDefaultTextStyle' ||
        node.type == 'flutter.widgets.DefaultTextStyle' ||
        node.type == 'flutter.widgets.DefaultTextStyle.merge' ||
        node.type == 'flutter.widgets.DefaultTextStyleTransition' ||
        node.type == 'flutter.material.AnimatedTheme' ||
        node.type == 'flutter.material.Theme' ||
        node.type == 'flutter.widgets.AnimatedSwitcher' ||
        node.type == 'flutter.widgets.AnimatedCrossFade' ||
        node.type == 'flutter.widgets.AnimatedSize' ||
        node.type == 'flutter.widgets.AnimatedContainer' ||
        node.type == 'flutter.widgets.AnimatedRotation' ||
        node.type == 'flutter.widgets.ColoredBox' ||
        ((node.type == 'flutter.widgets.Opacity' || node.type == 'flutter.widgets.AnimatedOpacity' || node.type == 'flutter.widgets.FadeTransition' || node.type == 'flutter.widgets.SlideTransition') &&
            (node.slot('child')?.children.isEmpty ?? true)) ||
        ((node.type == 'flutter.widgets.Align' || node.type == 'flutter.widgets.AnimatedAlign') &&
            ((node.slot('child')?.children.isEmpty ?? true) ||
                node.properties['widthFactor']?.value == 0 ||
                node.properties['heightFactor']?.value == 0)) ||
        ((node.type == 'flutter.widgets.FractionallySizedBox' || node.type == 'flutter.widgets.AnimatedFractionallySizedBox') &&
            ((node.slot('child')?.children.isEmpty ?? true) ||
                node.properties['widthFactor']?.value == 0 ||
                node.properties['heightFactor']?.value == 0)) ||
        node.type == 'flutter.widgets.Baseline' ||
        node.type == 'flutter.widgets.IntrinsicHeight' ||
        node.type == 'flutter.widgets.IntrinsicWidth' ||
        node.type == 'flutter.widgets.Offstage' ||
        node.type == canvasDirectionalityWidgetType ||
        node.type == 'flutter.widgets.Placeholder' ||
        node.type == 'flutter.widgets.ClipOval' ||
        node.type == 'flutter.widgets.ClipRRect' ||
        node.type == 'flutter.widgets.ClipRSuperellipse' ||
        node.type == 'flutter.widgets.PhysicalModel' ||
        node.type == 'flutter.widgets.PhysicalShape' ||
        node.type == 'flutter.widgets.ClipPath' ||
        node.type == 'flutter.widgets.ClipRect' ||
        node.type == 'flutter.widgets.RotatedBox' ||
        node.type == 'flutter.widgets.PreferredSize' ||
        node.type == 'flutter.widgets.SizedOverflowBox' ||
        node.type == 'flutter.widgets.Transform' ||
        node.type == 'flutter.widgets.ConstrainedBox' ||
        node.type == 'flutter.widgets.UnconstrainedBox' ||
        node.type == 'flutter.widgets.LimitedBox' ||
        node.type == 'flutter.widgets.OverflowBox' ||
        node.type == 'flutter.widgets.FittedBox' ||
        node.type == 'flutter.widgets.Expanded' ||
        node.type == 'flutter.widgets.Flexible' ||
        node.type == canvasSafeAreaWidgetType ||
        node.type == canvasSpacerWidgetType ||
        node.type == 'flutter.widgets.Stack' ||
        node.type == 'flutter.widgets.IndexedStack' ||
        node.type == 'flutter.widgets.Wrap' ||
        node.type == 'flutter.widgets.ListBody' ||
        node.type == 'flutter.widgets.OverflowBar' ||
        node.type == 'flutter.widgets.ListView' ||
        node.type == 'flutter.widgets.GridView' ||
        node.type == 'flutter.widgets.GridView.extent' ||
        node.type == 'flutter.widgets.CustomScrollView' ||
        node.type == 'flutter.widgets.SingleChildScrollView' ||
        node.type == 'flutter.widgets.Image' ||
        node.type == 'flutter.widgets.FadeInImage' ||
        node.type == 'flutter.widgets.RawImage' || node.type == 'flutter.widgets.ColorFiltered' ||
        node.type == 'flutter.widgets.ImageFiltered' || node.type == 'flutter.widgets.BackdropFilter' || node.type == 'flutter.widgets.BackdropFilter.grouped' || node.type == 'flutter.widgets.BackdropGroup' || node.type == 'flutter.widgets.ShaderMask' || node.type == 'flutter.widgets.CustomPaint' || isCanvasDataDescriptor(node.type) || node.type == 'flutter.widgets.TableRow' || node.type == 'flutter.widgets.Table' || node.type == 'flutter.widgets.TableCell' || node.type == 'flutter.widgets.Flow' || node.type == 'flutter.widgets.Flow.unwrapped' || node.type == 'flutter.widgets.CustomSingleChildLayout' || node.type == 'flutter.widgets.CustomMultiChildLayout' || node.type == 'flutter.widgets.LayoutId' ||
        node.type == 'flutter.widgets.ImageIcon' ||
        node.type == 'flutter.material.Divider' ||
        node.type == 'flutter.material.VerticalDivider' ||
        node.type == 'flutter.material.Card' ||
        node.type == canvasBottomSheetType || node.type == canvasSimpleDialogType || node.type == canvasSimpleDialogOptionType || isCanvasAlertDialogType(node.type) || node.type == canvasDialogType || node.type == canvasFullscreenDialogType ||
        node.type == 'flutter.material.Badge' ||
        node.type == 'flutter.material.CircleAvatar' ||
        node.type == 'flutter.material.Switch' ||
        node.type == 'flutter.material.Radio' ||
        node.type == 'flutter.widgets.RadioGroup' ||
        node.type == 'flutter.material.ListTile' ||
        node.type == 'flutter.material.CheckboxListTile' ||
        node.type == 'flutter.material.SwitchListTile' ||
        node.type == 'flutter.material.RadioListTile' ||
        node.type == 'flutter.material.ExpansionTile' ||
        node.type == 'flutter.material.Tooltip' ||
        node.type == 'flutter.material.TooltipVisibility' ||
        node.type == 'flutter.material.TooltipTheme' ||
        node.type == 'flutter.material.MenuItemButton' ||
        _ownsNativeMenu(node) ||
        node.type == 'flutter.material.RangeSlider' ||
        node.type == 'flutter.material.Slider' ||
        node.type == 'flutter.material.LinearProgressIndicator' ||
        node.type == 'flutter.material.CircularProgressIndicator' ||
        node.type == 'flutter.material.RefreshProgressIndicator' ||
        node.type == 'flutter.material.RefreshIndicator' ||
        node.type == 'flutter.material.TextButton' ||
        node.type == 'flutter.material.OutlinedButton' ||
        node.type == 'flutter.material.FilledButton' ||
        node.type == 'flutter.material.FloatingActionButton' ||
        node.type == 'flutter.widgets.Icon') {
      yield node;
    }
    for (final slotEntry in node.slots.entries) {
      for (final index in _interactiveChildIndexes(
        node,
        slotEntry.key,
        slotEntry.value,
      )) {
        final child = slotEntry.value.children[index];
        yield* _zeroSizedDesignerTargets(child);
      }
    }
  }

  static bool _sameTargets(
    List<_ZeroSizedWidgetTargetGroup> left,
    List<_ZeroSizedWidgetTargetGroup> right,
  ) {
    if (left.length != right.length) {
      return false;
    }
    for (var targetIndex = 0; targetIndex < left.length; targetIndex++) {
      final leftTarget = left[targetIndex];
      final rightTarget = right[targetIndex];
      if (leftTarget.rect != rightTarget.rect ||
          leftTarget.widgetIds.length != rightTarget.widgetIds.length ||
          leftTarget.widgetTypes.length != rightTarget.widgetTypes.length ||
          leftTarget.previewUnavailableMessages.length !=
              rightTarget.previewUnavailableMessages.length) {
        return false;
      }
      for (
        var widgetIndex = 0;
        widgetIndex < leftTarget.widgetIds.length;
        widgetIndex++
      ) {
        if (leftTarget.widgetIds[widgetIndex] !=
                rightTarget.widgetIds[widgetIndex] ||
            leftTarget.widgetTypes[widgetIndex] !=
                rightTarget.widgetTypes[widgetIndex] ||
            leftTarget.previewUnavailableMessages[widgetIndex] !=
                rightTarget.previewUnavailableMessages[widgetIndex]) {
          return false;
        }
      }
    }
    return true;
  }

  void _reportViewportAfterFrame(
    CanvasViewportPresentation presentation,
    _ViewportGeometry geometry,
  ) {
    final metrics = CanvasViewportMetrics(
      presentation: presentation,
      effectiveScaleMicros: (geometry.scale * canvasViewportMicros).round(),
      horizontalScrollable: geometry.horizontalScrollable,
      verticalScrollable: geometry.verticalScrollable,
    );
    if (metrics == _lastReportedViewportMetrics) {
      return;
    }
    _lastReportedViewportMetrics = metrics;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted || _lastReportedViewportMetrics != metrics) {
        return;
      }
      widget.onViewportMetricsChanged?.call(metrics);
    });
  }

  void _onPointerSignal(PointerSignalEvent event) {
    if (event is! PointerScrollEvent) {
      return;
    }
    final geometry = _viewportGeometry;
    final presentation =
        widget.viewportPresentation ??
        CanvasViewportPresentation.fit(widget.model);
    if (geometry == null ||
        geometry.presentation != presentation ||
        !presentation.matchesModel(widget.model)) {
      return;
    }
    final keyboard = HardwareKeyboard.instance;
    if (keyboard.isControlPressed) {
      final effectiveZoom = presentation.mode == 'fit'
          ? (geometry.scale * canvasViewportMicros).round()
          : presentation.zoomMicros;
      final direction = event.scrollDelta.dy == 0
          ? event.scrollDelta.dx
          : event.scrollDelta.dy;
      if (direction == 0) {
        return;
      }
      final stepCount = math.max(1, (direction.abs() / 100).round());
      final delta = (direction < 0 ? 100000 : -100000) * stepCount;
      _changeViewport(
        presentation.copyWith(
          mode: 'manual',
          zoomMicros: (effectiveZoom + delta).clamp(
            minimumCanvasZoomMicros,
            maximumCanvasZoomMicros,
          ),
        ),
      );
      return;
    }
    final horizontal = keyboard.isShiftPressed;
    final delta = horizontal
        ? (event.scrollDelta.dy == 0
              ? event.scrollDelta.dx
              : event.scrollDelta.dy)
        : event.scrollDelta.dy;
    final overflow = horizontal
        ? geometry.horizontalOverflow
        : geometry.verticalOverflow;
    if (overflow <= 0 || delta == 0) {
      return;
    }
    final current = horizontal
        ? presentation.horizontalScrollMicros
        : presentation.verticalScrollMicros;
    final next = (current + delta / overflow * canvasViewportMicros)
        .round()
        .clamp(0, canvasViewportMicros);
    _changeViewport(
      horizontal
          ? presentation.copyWith(horizontalScrollMicros: next)
          : presentation.copyWith(verticalScrollMicros: next),
    );
  }

  void _changeViewport(CanvasViewportPresentation presentation) {
    if (_inlineTextEditSession == null) {
      _focusNode.requestFocus();
    }
    widget.onViewportPresentationChanged?.call(presentation);
  }

  void _selectWidget(String widgetId, {bool requestCanvasFocus = true}) {
    if (!_isInPaintedSliverVisibilityBranch(widget.model.root, widgetId)) return;
    final anchorId = _tooltipSelectedAnchorId;
    if (anchorId != null) {
      final candidate = _findCanvasNode(widget.model.root, widgetId);
      if (candidate != null && _findCanvasNode(candidate, anchorId) != null) {
        return;
      }
    }
    if (_inlineTextEditSession != null &&
        SchedulerBinding.instance.schedulerPhase ==
            SchedulerPhase.persistentCallbacks) {
      return;
    }
    if (_inlineTextEditSession?.widgetId != widgetId) {
      _cancelInlineTextEdit();
    }
    if (_inlineTextEditSession != null) {
      return;
    }
    if (requestCanvasFocus && !_widgetInsideInteractiveMenu(widgetId)) {
      _focusNode.requestFocus();
    }
    widget.onSelected(widgetId);
  }

  bool _widgetInsideInteractiveMenu(String widgetId) {
    bool visit(CanvasNode node) {
      if (_ownsNativeMenu(node) &&
          _menuState(node.id)?.interactive == true &&
          _findCanvasNode(node, widgetId) != null) {
        return true;
      }
      for (final slot in node.slots.values) {
        for (final child in slot.children) {
          if (visit(child)) return true;
        }
      }
      return false;
    }

    return visit(widget.model.root);
  }

  KeyEventResult _onKeyEvent(FocusNode node, KeyEvent event) {
    if (!widget.interactionInputSynchronized) {
      return KeyEventResult.handled;
    }
    if (event is! KeyDownEvent) {
      return KeyEventResult.ignored;
    }
    final keyboard = HardwareKeyboard.instance;
    final unmodified =
        !keyboard.isControlPressed &&
        !keyboard.isShiftPressed &&
        !keyboard.isAltPressed &&
        !keyboard.isMetaPressed;
    if (event.logicalKey == LogicalKeyboardKey.f2 && unmodified) {
      if (_inlineTextEditSession != null) {
        return KeyEventResult.handled;
      }
      final selected = widget.selectedWidgetId;
      final began = selected != null && _beginInlineTextEdit(selected);
      return began ? KeyEventResult.handled : KeyEventResult.ignored;
    }
    if (event.logicalKey == LogicalKeyboardKey.delete &&
        _inlineTextEditSession != null) {
      return KeyEventResult.ignored;
    }
    if (event.logicalKey != LogicalKeyboardKey.delete ||
        keyboard.isControlPressed ||
        keyboard.isShiftPressed ||
        keyboard.isAltPressed ||
        keyboard.isMetaPressed) {
      return KeyEventResult.ignored;
    }
    return widget.onDeleteSelected()
        ? KeyEventResult.handled
        : KeyEventResult.ignored;
  }

  bool _beginInlineTextEdit(String widgetId) {
    if (!widget.inlineTextEditEnabled ||
        !widget.interactionInputSynchronized ||
        widget.selectedWidgetId != widgetId) {
      return false;
    }
    final node = _findCanvasNode(widget.model.root, widgetId);
    if (node == null ||
        node.type != 'flutter.widgets.Text' ||
        node.properties['data']?.value is! String ||
        !_isInteractiveDescendant(widget.model.root, widgetId)) {
      return false;
    }
    if (_inlineTextEditSession?.widgetId == widgetId) {
      return true;
    }
    setState(() {
      _inlineTextEditSession = _InlineTextEditSession(
        widgetId: widgetId,
        presentationSequence: widget.model.presentationSequence,
        documentId: widget.model.documentId,
        logicalRevisionId: widget.model.logicalRevisionId,
      );
    });
    return true;
  }

  bool _commitInlineTextEdit(
    String widgetId,
    String text,
    bool compositionObserved,
  ) {
    final session = _inlineTextEditSession;
    if (session == null ||
        session.widgetId != widgetId ||
        !_inlineTextEditStillCurrent(session)) {
      _cancelInlineTextEdit();
      return false;
    }
    final accepted = widget.onInlineTextCommit(
      widgetId,
      text,
      compositionObserved,
    );
    if (accepted) {
      _cancelInlineTextEdit();
    }
    return accepted;
  }

  void _cancelInlineTextEdit() {
    if (_inlineTextEditSession == null) {
      return;
    }
    setState(() => _inlineTextEditSession = null);
    _restoreCanvasFocusAfterFrame();
  }

  bool _inlineTextEditStillCurrent(_InlineTextEditSession session) {
    if (!widget.inlineTextEditEnabled ||
        !widget.interactionInputSynchronized ||
        widget.selectedWidgetId != session.widgetId ||
        widget.model.presentationSequence != session.presentationSequence ||
        widget.model.documentId != session.documentId ||
        widget.model.logicalRevisionId != session.logicalRevisionId) {
      return false;
    }
    return _findCanvasNode(widget.model.root, session.widgetId)?.type ==
            'flutter.widgets.Text' &&
        _isInteractiveDescendant(widget.model.root, session.widgetId);
  }

  void _restoreCanvasFocusAfterFrame() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted &&
          widget.interactionInputSynchronized &&
          _inlineTextEditSession == null) {
        _focusNode.requestFocus();
      }
    });
  }

  Rect? _dataDescriptorGlobalRect(CanvasNode descriptor) {
    CanvasNode? owner;int rowIndex=-1,columnIndex=-1;
    void visit(CanvasNode n) {
      if(isCanvasDataTable(n.type)) {
        final columns=n.slot('columns')!.children,rows=n.slot('rows')?.children??<CanvasNode>[];
        final column=columns.indexWhere((c)=>c.id==descriptor.id);
        if(column>=0){owner=n;rowIndex=0;columnIndex=column;return;}
        for(var r=0;r<rows.length;r++) {
          if(rows[r].id==descriptor.id){owner=n;rowIndex=r+1;return;}
          final cell=rows[r].slot('cells')!.children.indexWhere((c)=>c.id==descriptor.id);
          if(cell>=0){owner=n;rowIndex=r+1;columnIndex=cell;return;}
        }
      }
      for (final slot in n.slots.values) {
        for (final child in slot.children) {
          if (owner == null) visit(child);
        }
      }
    }
    visit(widget.model.root);
    if(owner==null)return null;
    final ownerContext=_geometryNodeKey(owner!.id)?.currentContext;
    RenderObject? render=ownerContext?.findRenderObject();
    if(owner!.type==canvasPaginatedDataTableType) {
      // The custom header/actions may themselves contain tables. Select the native
      // data table by its owned first-column label before looking for RenderTable.
      final firstColumn=owner!.slot('columns')!.children.first;
      Element? native;
      void findDataTable(Element element) {
        final widget=element.widget;
        if(widget is DataTable&&widget.columns.first.label is _CanvasNodeView
            &&(widget.columns.first.label as _CanvasNodeView).node.id==firstColumn.slot('label')!.children.single.id) {
          native=element;return;
        }
        element.visitChildElements((child){if(native==null)findDataTable(child);});
      }
      if(ownerContext is Element)findDataTable(ownerContext);
      render=native?.findRenderObject();
    }
    RenderTable? table;
    void find(RenderObject object) {
      if(object is RenderTable){table=object;return;}
      object.visitChildren((child){if(table==null)find(child);});
    }
    if(render==null||!render.attached)return null;find(render);
    if(table==null||!table!.hasSize||rowIndex<0||rowIndex>=table!.rows)return null;
    if(columnIndex<0)return _finiteTransformedRect(table!.getTransformTo(null),table!.getRowBox(rowIndex));
    final columnCount=owner!.slot('columns')!.children.length;
    final implicitCheckbox=table!.columns-columnCount;
    final boxes=table!.row(rowIndex).toList();
    final nativeColumn=columnIndex+implicitCheckbox;
    if(nativeColumn<0||nativeColumn>=boxes.length)return null;
    final cell=boxes[nativeColumn];
    return _finiteGlobalRect(cell);
  }

  Rect? _tableRowGlobalRect(CanvasNode row) {
    CanvasNode? owner;
    void visit(CanvasNode current) {
      if(current.type=='flutter.widgets.Table'&&(current.slot('children')?.children.any((n)=>n.id==row.id)??false)) owner=current;
      if(owner==null) for(final slot in current.slots.values) { for(final child in slot.children) { visit(child); } }
    }
    visit(widget.model.root);
    if(owner==null) return null;
    final render=_geometryNodeKey(owner!.id)?.currentContext?.findRenderObject();
    RenderTable? table;
    void find(RenderObject current) {
      if(current is RenderTable) { table=current; return; }
      current.visitChildren((child){if(table==null) find(child);});
    }
    if(render==null||!render.attached) return null;
    find(render);
    if(table==null||!table!.hasSize) return null;
    final index=owner!.slot('children')!.children.indexWhere((n)=>n.id==row.id);
    if(index<0||index>=table!.rows) return null;
    return _finiteTransformedRect(table!.getTransformTo(null),table!.getRowBox(index));
  }

  GlobalKey _nodeKey(String id) =>
      _nodeKeys.putIfAbsent(id, () => GlobalKey(debugLabel: 'canvas-$id'));

  CanvasDropTarget? _resolveDrop(
    int xMicros,
    int yMicros, [
    CanvasPaletteDragSource? source,
  ]) {
    if (xMicros < 0 ||
        xMicros > _microsPerSurface ||
        yMicros < 0 ||
        yMicros > _microsPerSurface) {
      return null;
    }
    final surface = _renderBox(_surfaceKey);
    if (surface == null || surface.size.isEmpty) {
      return null;
    }
    final surfaceRect = _finiteGlobalRect(surface);
    if (surfaceRect == null) {
      return null;
    }
    final point = Offset(
      surfaceRect.left + surfaceRect.width * xMicros / _microsPerSurface,
      surfaceRect.top + surfaceRect.height * yMicros / _microsPerSurface,
    );
    final candidates = <_DropCandidate>[];
    final effectiveSource =
        source ??
        CanvasPaletteDragSource(
          token: '',
          widgetType: 'flutter.widgets.Text',
          traits: const {},
        );
    _collectDropCandidates(
      widget.model.root,
      point,
      surfaceRect,
      0,
      candidates,
      effectiveSource,
    );
    if (candidates.isEmpty) {
      return null;
    }
    candidates.sort((left, right) {
      final depth = right.depth.compareTo(left.depth);
      if (depth != 0) {
        return depth;
      }
      final priority = right.slot.overlapPriority.compareTo(
        left.slot.overlapPriority,
      );
      return priority != 0 ? priority : left.area.compareTo(right.area);
    });
    final selected = candidates.first;
    return CanvasDropTarget(
      parentWidgetId: selected.node.id,
      slotName: selected.slot.slotName,
      insertionIndex: selected.insertionIndex,
      zone: _normalizeZone(surfaceRect, selected.zone),
    );
  }

  CanvasDropTarget? _resolveMovePreview(
    String sourceWidgetId,
    String parentWidgetId,
    String slotName,
    int insertionIndex,
  ) {
    final source = _findCanvasNode(widget.model.root, sourceWidgetId);
    final parentNode = _findCanvasNode(widget.model.root, parentWidgetId);
    final surface = _renderBox(_surfaceKey);
    final parentBox =
        parentNode != null &&
            _ownsNativeMenu(parentNode) &&
            slotName == 'menuChildren'
        ? _menuPanelBox(parentWidgetId) ?? _renderBox(_geometryNodeKey(parentWidgetId))
        : _renderBox(_geometryNodeKey(parentWidgetId));
    if (source == null ||
        parentNode == null ||
        surface == null ||
        surface.size.isEmpty ||
        (parentBox == null && !isCanvasSliverWidgetType(parentNode.type) && !isCanvasDataDescriptor(parentNode.type))) {
      return null;
    }
    if(!canvasDataDescriptorPlacement(source.type,parentNode.type,slotName))return null;
    if (source.type == canvasTableRowTrait &&
        !(parentNode.type == canvasTableType && slotName == 'children')) { return null; }
    if (source.type == canvasTableCellType &&
        !(parentNode.type == canvasTableRowTrait && slotName == 'children')) { return null; }
    if (isCanvasStackPositionedWidgetType(source.type) &&
        !(parentNode.type == 'flutter.widgets.Stack' && slotName == 'children')) { return null; }
    if (source.type == canvasSliverCrossAxisExpandedType &&
        !isCanvasSliverCrossAxisExpandedDestination(parentNode.type, slotName)) {
      return null;
    }
    final requiredChildOwner = _requiredChildOwner(
      widget.model.root,
      sourceWidgetId,
    );
    if (requiredChildOwner != null &&
        (requiredChildOwner.id != parentWidgetId ||
            slotName !=
                (const {'flutter.widgets.SliverFloatingHeader', 'flutter.widgets.LayoutId'}.contains(requiredChildOwner.type) ? 'child' : canvasReviewedRequiredWrapperSlot(requiredChildOwner.type)))) {
      // A move cannot expose an invalid empty required slot. Keep same-slot
      // no-op previews; the host still owns final mutation validation.
      return null;
    }
    if (!_isInteractiveDescendant(widget.model.root, parentWidgetId) ||
        !_isEligibleDropSlot(parentNode, slotName)) {
      return null;
    }
    final modelSlot = parentNode.slot(slotName);
    final reviewedSlot = canvasDropSlotForWidgetSlot(parentNode.type, slotName);
    final slotKind = modelSlot?.kind ?? reviewedSlot?.modelSlotKind;
    if (slotKind == null) {
      return null;
    }
    final surfaceRect = _finiteGlobalRect(surface);
    if (isCanvasSliverWidgetType(parentNode.type)) {
      final children = modelSlot?.children.where((child) => child.id != sourceWidgetId).toList() ?? <CanvasNode>[];
      if (surfaceRect == null || reviewedSlot == null ||
          !reviewedSlot.acceptsSource(CanvasPaletteDragSource(token: 'move-preview', widgetType: source.type,
            traits: isCanvasSliverWidgetType(source.type) ? const {canvasSliverWidgetTrait} : const {})) ||
          isCanvasFlexRestrictedWidgetType(source.type) ||
          _findCanvasNode(source, parentWidgetId) != null ||
          insertionIndex < 0 || insertionIndex > children.length ||
          (slotKind == 'single' && (insertionIndex != 0 || children.isNotEmpty))) {
        return null;
      }
      final rect = isCanvasSliverAppBarType(parentNode.type)
          ? _sliverAppBarDropZone(parentNode, slotName, surfaceRect)
          : _sliverGlobalRect(parentNode, surfaceRect);
      if (rect == null) return null;
      return CanvasDropTarget(parentWidgetId: parentWidgetId, slotName: slotName,
          insertionIndex: insertionIndex, zone: _normalizeZone(surfaceRect, rect));
    }
    if (isCanvasDataDescriptor(parentNode.type)) {
      final rect = _dataDescriptorGlobalRect(parentNode);
      final children = modelSlot?.children.where((child) => child.id != sourceWidgetId).toList() ?? <CanvasNode>[];
      if (surfaceRect == null || rect == null || rect.isEmpty ||
          insertionIndex < 0 || insertionIndex > children.length ||
          _findCanvasNode(source, parentWidgetId) != null ||
          !canvasDropTargetAcceptsSource(parentWidgetType: parentNode.type, slotName: slotName,
              currentChildCount: children.length, insertionIndex: insertionIndex,
              source: CanvasPaletteDragSource(token: 'move-preview', widgetType: source.type,
                  traits: canvasDataCellTypes.contains(source.type) ? const {'flutter.material.DataCell'} : const {}))) {
        return null;
      }
      if (slotKind == 'single') {
        if (insertionIndex != 0 || children.isNotEmpty) return null;
        return CanvasDropTarget(parentWidgetId: parentWidgetId, slotName: slotName,
            insertionIndex: 0, zone: _normalizeZone(surfaceRect, rect));
      }
      // Moving one cell between rows would make both rows ragged. Only reorder
      // within the same row; whole-column moves use the owning table.
      if (!(modelSlot?.children.any((child) => child.id == sourceWidgetId) ?? false)) return null;
      if (children.isEmpty) return null;
      final before = insertionIndex < children.length;
      final reference = _dataDescriptorGlobalRect(children[before ? insertionIndex : children.length - 1]);
      if (reference == null) return null;
      final rtl = _resolvedTextDirection(parentNode) == TextDirection.rtl;
      final edge = before ? (rtl ? reference.right : reference.left) : (rtl ? reference.left : reference.right);
      final half = math.min(_moveInsertionMarkerExtent / 2, rect.width / 2);
      final center = edge.clamp(rect.left + half, rect.right - half);
      return CanvasDropTarget(parentWidgetId: parentWidgetId, slotName: slotName,
          insertionIndex: insertionIndex,
          zone: _normalizeZone(surfaceRect, Rect.fromLTRB(center-half, rect.top, center+half, rect.bottom)));
    }
    if (parentBox == null) return null;
    final renderedParentRect = _finiteGlobalRect(parentBox);
    if (surfaceRect == null ||
        renderedParentRect == null ||
        !_hasFiniteGlobalInverse(parentBox)) {
      return null;
    }
    final zeroSizedParent = parentBox.size.isEmpty;
    final parentRect = zeroSizedParent
        ? _boundedDesignerHitRect(renderedParentRect, surfaceRect)
        : Offset.zero & parentBox.size;
    if (parentRect.isEmpty && !zeroSizedParent) {
      return null;
    }

    final Rect zone;
    if (slotKind == 'single') {
      if (insertionIndex != 0) {
        return null;
      }
      final retainedChildren =
          modelSlot?.children
              .where((child) => child.id != sourceWidgetId)
              .length ??
          0;
      if (retainedChildren != 0) {
        return null;
      }
      zone = switch (reviewedSlot?.zonePlacement) {
        CanvasDropZonePlacement.menuItemChild ||
        CanvasDropZonePlacement.listTileLeading ||
        CanvasDropZonePlacement.listTileTitle ||
        CanvasDropZonePlacement.listTileSubtitle ||
        CanvasDropZonePlacement.listTileTrailing => _listTileDropZone(
          _expansionHeaderRect(parentNode, parentBox, parentRect),
          reviewedSlot!.zonePlacement,
          _resolvedTextDirection(parentNode),
        ),
        CanvasDropZonePlacement.badgeLabel => _badgeLabelZone(parentRect),
        CanvasDropZonePlacement.fullNode || null => parentRect,
        CanvasDropZonePlacement.terminalList => _terminalZone(
          parentNode,
          parentBox,
          parentRect,
          slotName,
        ),
        CanvasDropZonePlacement.existingChild => Rect.zero,
        CanvasDropZonePlacement.bottomRightCompact => _bottomRightCompactZone(
          parentRect,
        ),
        CanvasDropZonePlacement.appBarLeading => _appBarLeadingZone(
          parentNode,
          parentBox,
          parentRect,
        ),
        CanvasDropZonePlacement.appBarTitle => _appBarTitleZone(
          parentNode,
          parentBox,
          parentRect,
        ),
        CanvasDropZonePlacement.appBarActions => _terminalZone(
          parentNode,
          parentBox,
          _appBarToolbarZone(parentNode, parentBox, parentRect),
          slotName,
        ),
        CanvasDropZonePlacement.appBarFlexibleSpace => parentRect,
        CanvasDropZonePlacement.flexibleSpaceBarTitle => _flexibleSpaceTitleZone(parentRect),
        CanvasDropZonePlacement.appBarBottom => _appBarBottomZone(
          parentNode,
          parentBox,
          parentRect,
        ),
      };
    } else if (slotKind == 'list') {
      final children = <CanvasNode>[
        for (final child in modelSlot?.children ?? const <CanvasNode>[])
          if (child.id != sourceWidgetId) child,
      ];
      if (insertionIndex < 0 || insertionIndex > children.length) {
        return null;
      }
      if (zeroSizedParent && children.isNotEmpty) {
        return null;
      }
      final listParentRect =
          parentNode.type == 'flutter.material.AppBar' && slotName == 'actions'
          ? _appBarToolbarZone(parentNode, parentBox, parentRect)
          : parentNode.type == 'flutter.material.ExpansionTile' &&
                slotName == 'children'
          ? _expansionBodyRect(parentNode, parentBox, parentRect)
          : parentRect;
      zone =
          parentNode.type == 'flutter.material.AppBar' &&
              slotName == 'actions' &&
              children.isEmpty
          ? _terminalZone(
              parentNode,
              parentBox,
              listParentRect,
              slotName,
              effectiveChildren: children,
            )
          : _listMoveInsertionZone(
              parentNode,
              parentBox,
              listParentRect,
              children,
              insertionIndex,
              slotName,
            );
    } else {
      return null;
    }
    if (zone.isEmpty) {
      return null;
    }
    final globalZone = zeroSizedParent
        ? zone
        : _finiteGlobalRect(parentBox, localRect: zone);
    if (globalZone == null) {
      return null;
    }
    final boundedGlobalZone = _boundedDesignerHitRect(globalZone, surfaceRect);
    if (boundedGlobalZone.isEmpty) {
      return null;
    }
    return CanvasDropTarget(
      parentWidgetId: parentWidgetId,
      slotName: slotName,
      insertionIndex: insertionIndex,
      zone: _normalizeZone(surfaceRect, boundedGlobalZone),
    );
  }

  Rect _listMoveInsertionZone(
    CanvasNode parentNode,
    RenderBox parentBox,
    Rect parentRect,
    List<CanvasNode> children,
    int insertionIndex,
    String slotName,
  ) {
    if (parentNode.type == 'flutter.widgets.CustomScrollView' &&
        slotName == 'slivers') {
      return parentRect;
    }
    if ((parentNode.type == 'flutter.widgets.GridView' ||
            parentNode.type == 'flutter.widgets.GridView.extent') &&
        slotName == 'children') {
      return _gridInsertionZone(
        parentNode,
        parentBox,
        parentRect,
        children,
        insertionIndex,
        markerExtent: _moveInsertionMarkerExtent,
      );
    }
    if ((parentNode.type == 'flutter.widgets.Stack' ||
            parentNode.type == 'flutter.widgets.IndexedStack' ||
            parentNode.type == 'flutter.widgets.Wrap') &&
        slotName == 'children') {
      return parentRect;
    }
    if (children.isEmpty) {
      return parentRect;
    }
    final overflowBarVertical =
        parentNode.type == 'flutter.widgets.OverflowBar' &&
        _isOverflowBarVertical(parentNode, parentBox);
    final horizontal =
        parentNode.type == 'flutter.widgets.Row' ||
        (isCanvasDataTable(parentNode.type) && slotName == 'columns') ||
        parentNode.type == 'flutter.material.AppBar' ||
        _isHorizontalListBody(parentNode) ||
        _isHorizontalListView(parentNode) ||
        _isHorizontalCustomScrollView(parentNode) ||
        _isHorizontalPageView(parentNode) ||
        (parentNode.type == 'flutter.widgets.OverflowBar' &&
            !overflowBarVertical);
    final reverse = switch (parentNode.type) {
      'flutter.widgets.ListBody' => _isVisuallyReversedListBody(parentNode),
      'flutter.widgets.ListView' => _isVisuallyReversedListView(parentNode),
      'flutter.widgets.PageView' =>
        _booleanValue(parentNode, 'reverse') == true,
      'flutter.widgets.OverflowBar' =>
        overflowBarVertical
            ? _enumValue(parentNode, 'overflowDirection') == 'up'
            : _resolvedTextDirection(parentNode) == TextDirection.rtl,
      _ =>
        horizontal
            ? _resolvedTextDirection(parentNode) == TextDirection.rtl
            : _enumValue(parentNode, 'verticalDirection') == 'up',
    };
    final referenceIndex = insertionIndex < children.length
        ? insertionIndex
        : children.length - 1;
    final referenceNode = children[referenceIndex];
    final reference = _renderBox(_geometryNodeKey(referenceNode.id));
    final rowRect = isCanvasDataDescriptor(referenceNode.type)?_dataDescriptorGlobalRect(referenceNode):referenceNode.type == canvasTableRowTrait
        ? _tableRowGlobalRect(referenceNode) : null;
    final renderedReferenceRect = rowRect != null
        ? Rect.fromPoints(parentBox.globalToLocal(rowRect.topLeft), parentBox.globalToLocal(rowRect.bottomRight))
        : reference == null ? null : _finiteRectInAncestor(reference, parentBox);
    if (renderedReferenceRect == null) {
      return Rect.zero;
    }
    final referenceRect = renderedReferenceRect.intersect(parentRect);
    if (referenceRect.isEmpty) {
      return Rect.zero;
    }
    final beforeExisting = insertionIndex < children.length;
    final edge = horizontal
        ? (beforeExisting
              ? (reverse ? referenceRect.right : referenceRect.left)
              : (reverse ? referenceRect.left : referenceRect.right))
        : (beforeExisting
              ? (reverse ? referenceRect.bottom : referenceRect.top)
              : (reverse ? referenceRect.top : referenceRect.bottom));
    if (horizontal) {
      final half = math.min(
        _moveInsertionMarkerExtent / 2,
        parentRect.width / 2,
      );
      final center = edge.clamp(
        parentRect.left + half,
        parentRect.right - half,
      );
      return Rect.fromLTRB(
        center - half,
        parentRect.top,
        center + half,
        parentRect.bottom,
      );
    }
    final half = math.min(
      _moveInsertionMarkerExtent / 2,
      parentRect.height / 2,
    );
    final center = edge.clamp(parentRect.top + half, parentRect.bottom - half);
    return Rect.fromLTRB(
      parentRect.left,
      center - half,
      parentRect.right,
      center + half,
    );
  }

  CanvasNode? _requiredChildOwner(CanvasNode node, String childId) {
    if (const {'flutter.widgets.SliverFloatingHeader', 'flutter.widgets.LayoutId'}.contains(node.type) && node.slot('child')?.child?.id == childId) return node;
    if (isCanvasReviewedRequiredChildWrapperWidgetType(node.type) &&
        node.slot(canvasReviewedRequiredWrapperSlot(node.type)!)?.child?.id ==
            childId) {
      return node;
    }
    for (final slot in node.slots.values) {
      for (final child in slot.children) {
        final owner = _requiredChildOwner(child, childId);
        if (owner != null) return owner;
      }
    }
    return null;
  }

  CanvasNode? _findCanvasNode(CanvasNode node, String id) {
    if (node.id == id) {
      return node;
    }
    for (final slot in node.slots.values) {
      for (final child in slot.children) {
        final found = _findCanvasNode(child, id);
        if (found != null) {
          return found;
        }
      }
    }
    return null;
  }

  CanvasDropZone _normalizeZone(Rect surface, Rect zone) {
    int micros(double value, double origin, double extent) =>
        ((value - origin) / extent * _microsPerSurface).round().clamp(
          0,
          _microsPerSurface,
        );
    return CanvasDropZone(
      leftMicros: micros(zone.left, surface.left, surface.width),
      topMicros: micros(zone.top, surface.top, surface.height),
      rightMicros: micros(zone.right, surface.left, surface.width),
      bottomMicros: micros(zone.bottom, surface.top, surface.height),
    );
  }

  void _collectDropCandidates(
    CanvasNode node,
    Offset point,
    Rect surfaceRect,
    int depth,
    List<_DropCandidate> result,
    CanvasPaletteDragSource source,
  ) {
    if (isCanvasRequiredChildWrapperWidgetType(source.widgetType)) {
      // CanvasDropTarget identifies a parent slot, so only existing non-root
      // children can be represented on the current negotiated wire. Do not
      // invent a root sentinel: the host has no field it could revalidate.
      for (final slotEntry in node.slots.entries) {
        final modelSlot = slotEntry.value;
        final wrapSlot = canvasExistingChildWrapTargetSlot(
          parentWidgetType: node.type,
          slotName: slotEntry.key,
        );
        if ((source.widgetType == canvasTableCellType &&
              !(node.type == canvasTableRowTrait && slotEntry.key == 'children')) ||
            (isCanvasStackPositionedWidgetType(source.widgetType) &&
              !(node.type == 'flutter.widgets.Stack' && slotEntry.key == 'children')) ||
            (source.widgetType == canvasSliverCrossAxisExpandedType &&
              !isCanvasSliverCrossAxisExpandedDestination(node.type, slotEntry.key)) ||
            wrapSlot == null ||
            modelSlot.kind != wrapSlot.modelSlotKind ||
            !wrapSlot.acceptsSource(source)) {
          continue;
        }
        for (final index in _interactiveChildIndexes(
          node,
          slotEntry.key,
          modelSlot,
        )) {
          final child = modelSlot.children[index];
          if (!canvasWrapperAcceptsExistingChild(
            wrapperWidgetType: source.widgetType,
            childWidgetType: child.type,
          )) {
            continue;
          }
          final box = _renderBox(_geometryNodeKey(child.id));
          final sliverZone = isCanvasSliverWidgetType(child.type)
              ? _sliverGlobalRect(child, surfaceRect) : null;
          final zone = sliverZone != null
              ? (sliverZone.contains(point) ? sliverZone : null)
              : box == null ? null : _resolvedGlobalDropZone(
                  box, Offset.zero & box.size, point, surfaceRect);
          if (zone != null) {
            result.add(
              _DropCandidate(
                node,
                depth + 1,
                zone.width * zone.height,
                wrapSlot,
                index,
                zone,
              ),
            );
          }
        }
      }
    } else if(isCanvasFlexParentDataWidgetType(source.widgetType)&&node.type==canvasDataColumnType) {
      final child=node.slot('label')!.children.single;
      if(!isCanvasFlexRestrictedWidgetType(child.type)) {
        final box=_renderBox(_geometryNodeKey(child.id));
        final zone=box==null?null:_resolvedGlobalDropZone(box,Offset.zero&box.size,point,surfaceRect);
        if(zone!=null) {
          result.add(_DropCandidate(node,depth+1,zone.width*zone.height,
          const CanvasDropSlotSemantics.wrapExistingSingle(slotName:'label',overlapPriority:6),0,zone));
        }
      }
    } else if (isCanvasFlexParentDataWidgetType(source.widgetType) &&
        (node.type == 'flutter.widgets.Row' ||
            node.type == 'flutter.widgets.Column')) {
      final modelSlot = node.slot('children');
      if (modelSlot?.kind == 'list') {
        for (var index = 0; index < modelSlot!.children.length; index++) {
          final child = modelSlot.children[index];
          if (isCanvasFlexRestrictedWidgetType(child.type)) {
            continue;
          }
          final box = _renderBox(_geometryNodeKey(child.id));
          if (box == null) {
            continue;
          }
          final zone = _resolvedGlobalDropZone(
            box,
            Offset.zero & box.size,
            point,
            surfaceRect,
          );
          if (zone != null) {
            result.add(
              _DropCandidate(
                node,
                depth + 1,
                zone.width * zone.height,
                canvasFlexWrapDropSlot,
                index,
                zone,
              ),
            );
          }
        }
      }
    } else {
      for (final dropSlot in canvasDropSlotsForWidgetType(node.type)) {
        // Prototype is measurement-only. Its explicit tree/Slots target remains
        // available, but pointer drops on the painted list address real children.
        if (isCanvasPrototypeSliverType(node.type) && dropSlot.slotName == 'prototypeItem') continue;
        if (node.type == 'flutter.widgets.SliverResizingHeader' && dropSlot.slotName != 'child') continue;
        if (!dropSlot.acceptsSource(source) ||
            !_isEligibleDropSlot(node, dropSlot.slotName)) {
          continue;
        }
        final modelSlot = node.slot(dropSlot.slotName);
        if (modelSlot != null && modelSlot.kind != dropSlot.modelSlotKind) {
          continue;
        }
        final currentChildCount = modelSlot?.children.length ?? 0;
        final insertionIndex = dropSlot.insertionIndexFor(currentChildCount);
        // Use the same parent-data restrictions as model validation and the host.
        // In particular, a flex wrapper over a non-Flex parent is not an append.
        if (insertionIndex == null || !canvasDropTargetAcceptsSource(
          parentWidgetType: node.type, slotName: dropSlot.slotName,
          currentChildCount: currentChildCount, insertionIndex: insertionIndex,
          source: source)) { continue; }
        if (isCanvasSliverWidgetType(node.type)) {
          final zone = isCanvasSliverAppBarType(node.type)
              ? _sliverAppBarDropZone(node, dropSlot.slotName, surfaceRect) : _sliverGlobalRect(node, surfaceRect);
          if (zone != null && zone.contains(point)) {
            result.add(_DropCandidate(node, depth, zone.width * zone.height,
                dropSlot, insertionIndex, zone));
          }
          continue;
        }
        if(isCanvasDataDescriptor(node.type)) {
          final rect=_dataDescriptorGlobalRect(node);
          if(rect!=null&&rect.contains(point))result.add(_DropCandidate(node,depth,rect.width*rect.height,dropSlot,insertionIndex,rect));
          continue;
        }
        final box = _ownsNativeMenu(node) && dropSlot.slotName == 'menuChildren'
            ? _menuPanelBox(node.id) ?? _renderBox(_geometryNodeKey(node.id))
            : _renderBox(_geometryNodeKey(node.id));
        if (box != null) {
          if (dropSlot.zonePlacement == CanvasDropZonePlacement.existingChild) {
            continue;
          }
          final localParent = Offset.zero & box.size;
          final localZone = switch (dropSlot.zonePlacement) {
            CanvasDropZonePlacement.menuItemChild ||
            CanvasDropZonePlacement.listTileLeading ||
            CanvasDropZonePlacement.listTileTitle ||
            CanvasDropZonePlacement.listTileSubtitle ||
            CanvasDropZonePlacement.listTileTrailing => _listTileDropZone(
              _expansionHeaderRect(node, box, localParent),
              dropSlot.zonePlacement,
              _resolvedTextDirection(node),
            ),
            CanvasDropZonePlacement.badgeLabel => _badgeLabelZone(localParent),
            CanvasDropZonePlacement.fullNode => localParent,
            CanvasDropZonePlacement.terminalList => _terminalZone(
              node,
              box,
              localParent,
              dropSlot.slotName,
            ),
            CanvasDropZonePlacement.existingChild => Rect.zero,
            CanvasDropZonePlacement.bottomRightCompact =>
              _bottomRightCompactZone(localParent),
            CanvasDropZonePlacement.appBarLeading => _appBarLeadingZone(
              node,
              box,
              localParent,
            ),
            CanvasDropZonePlacement.appBarTitle => _appBarTitleZone(
              node,
              box,
              localParent,
            ),
            CanvasDropZonePlacement.appBarActions => _terminalZone(
              node,
              box,
              _appBarToolbarZone(node, box, localParent),
              dropSlot.slotName,
            ),
            CanvasDropZonePlacement.appBarFlexibleSpace => localParent,
            CanvasDropZonePlacement.flexibleSpaceBarTitle => _flexibleSpaceTitleZone(localParent),
            CanvasDropZonePlacement.appBarBottom => _appBarBottomZone(
              node,
              box,
              localParent,
            ),
          };
          final zone = _resolvedGlobalDropZone(
            box,
            localZone,
            point,
            surfaceRect,
            placement: dropSlot.zonePlacement,
            direction: _resolvedTextDirection(node),
          );
          if (zone != null) {
            result.add(
              _DropCandidate(
                node,
                depth,
                zone.width * zone.height,
                dropSlot,
                insertionIndex,
                zone,
              ),
            );
          }
        }
      }
    }
    for (final slotEntry in node.slots.entries) {
      if (_ownsNativeMenu(node) && slotEntry.key == 'menuChildren') {
        final panel = _menuPanelBox(node.id);
        final panelRect = panel == null ? null : _finiteGlobalRect(panel);
        if (panelRect == null || !panelRect.contains(point)) continue;
      }
      for (final index in _interactiveChildIndexes(
        node,
        slotEntry.key,
        slotEntry.value,
      )) {
        final child = slotEntry.value.children[index];
        _collectDropCandidates(
          child,
          point,
          surfaceRect,
          depth + 1,
          result,
          source,
        );
      }
    }
  }

  Iterable<int> _interactiveChildIndexes(
    CanvasNode node,
    String slotName,
    CanvasSlot slot,
  ) sync* {
    if (!_isInteractiveSlot(node, slotName)) return;
    if (node.type == 'flutter.material.IconButton') {
      final selected =
          Theme.of(context).useMaterial3 &&
          node.properties['isSelected']?.value == true &&
          (node.slot('selectedIcon')?.children.isNotEmpty ?? false);
      if ((slotName == 'selectedIcon') != selected) return;
    }
    if (node.type == 'flutter.widgets.IndexedStack' && slotName == 'children') {
      final indexValue = node.properties['index'];
      final index = indexValue == null
          ? 0
          : indexValue.kind == 'null'
          ? null
          : indexValue.value as int;
      if (index != null && index >= 0 && index < slot.children.length) {
        yield index;
      }
      return;
    }
    for (var index = 0; index < slot.children.length; index++) {
      yield index;
    }
  }

  bool _isInteractiveSlot(CanvasNode node, String slotName) {
    if (node.type == 'flutter.widgets.AnimatedCrossFade') {
      // Only the top child accepts native pointer input. The other branch stays editable in the tree/Slots.
      return slotName == (_enumValue(node, 'crossFadeState') == 'showSecond' ? 'secondChild' : 'firstChild');
    }
    if (_ownsNativeMenu(node) && slotName == 'menuChildren') {
      return _menuState(node.id)?.interactive ?? false;
    }
    if (node.type == 'flutter.material.ExpansionTile') {
      if (slotName == 'children') {
        return _expansionState(node)?.controller.isExpanded ??
            node.properties['initiallyExpanded']?.value == true;
      }
      if (slotName == 'trailing') {
        return node.properties['showTrailingIcon']?.value != false;
      }
    }
    if (node.type == 'flutter.material.FloatingActionButton' &&
        slotName == 'icon') {
      return node.properties['variant']?.value == 'extended';
    }
    if ((node.type == 'flutter.material.TextButton' ||
            node.type == 'flutter.material.OutlinedButton' ||
            node.type == 'flutter.material.FilledButton') &&
        slotName == 'icon') {
      return {'icon', 'tonalIcon'}.contains(node.properties['variant']?.value);
    }
    if (node.type == 'flutter.material.Badge' && slotName == 'label') {
      return !node.properties.containsKey('count') &&
          node.properties['isLabelVisible']?.value != false;
    }
    if (isCanvasSliverVisibilityType(node.type)) {
      final visible = node.properties['visible']?.value != false;
      final maintain = node.type.endsWith('.maintain') || node.properties['maintainState']?.value == true;
      return slotName == 'sliver' ? visible : slotName == 'replacementSliver' && !visible && !maintain;
    }
    if (node.type != 'flutter.widgets.Visibility') return true;
    final visible = node.properties['visible']?.value != false;
    return slotName == 'child'
        ? visible
        : slotName == 'replacement' &&
              !visible &&
              node.properties['maintainState']?.value != true;
  }

  // Model-tree selection remains host-authoritative, but invisible branches
  // must not acquire geometry or open a Designer editor from an F2 request.
  bool _isInteractiveDescendant(CanvasNode node, String widgetId) {
    if (node.id == widgetId) return true;
    for (final entry in node.slots.entries) {
      for (final index in _interactiveChildIndexes(
        node,
        entry.key,
        entry.value,
      )) {
        if (_isInteractiveDescendant(entry.value.children[index], widgetId)) {
          return true;
        }
      }
    }
    return false;
  }

  Rect _ignorePointerHandleRect(Rect body, Rect viewport) {
    const extent = _minimumZeroSizedWidgetTarget;
    Rect? fallback;
    for (final origin in [
      body.topLeft - const Offset(extent, extent),
      body.topLeft - const Offset(extent, 0),
      body.topLeft - const Offset(0, extent),
      body.topRight,
      body.bottomLeft,
      body.topRight - const Offset(0, extent),
      body.bottomRight,
      body.bottomLeft - const Offset(extent, 0),
    ]) {
      final candidate = _boundedDesignerHitRect(
        origin & const Size.square(extent),
        viewport,
        minimumExtent: extent,
      );
      fallback ??= candidate;
      if (!candidate.overlaps(body)) return candidate;
    }
    // A body filling the viewport leaves no outside space. Only this compact
    // explicit Designer handle may overlap it; the remaining body stays inert.
    return fallback!;
  }

  Rect _boundedDesignerHitRect(
    Rect rendered,
    Rect surface, {
    double minimumExtent = _minimumTerminalBand,
  }) {
    if (!rendered.isFinite || !surface.isFinite || surface.isEmpty) {
      return Rect.zero;
    }
    final visible = rendered.intersect(surface);
    final rawCenter = visible.isEmpty ? rendered.center : visible.center;
    final width = math.min(
      surface.width,
      math.max(visible.width, minimumExtent),
    );
    final height = math.min(
      surface.height,
      math.max(visible.height, minimumExtent),
    );
    final centerX = rawCenter.dx.clamp(
      surface.left + width / 2,
      surface.right - width / 2,
    );
    final centerY = rawCenter.dy.clamp(
      surface.top + height / 2,
      surface.bottom - height / 2,
    );
    return Rect.fromCenter(
      center: Offset(centerX, centerY),
      width: width,
      height: height,
    );
  }

  Rect _bottomRightCompactZone(Rect parent) {
    if (parent.width <= 0 || parent.height <= 0) {
      return Rect.zero;
    }
    final width = _scaffoldFabDropExtent.clamp(1.0, parent.width);
    final height = _scaffoldFabDropExtent.clamp(1.0, parent.height);
    return Rect.fromLTRB(
      parent.right - width,
      parent.bottom - height,
      parent.right,
      parent.bottom,
    );
  }

  Rect _badgeLabelZone(Rect parent) => Rect.fromLTRB(
    parent.center.dx,
    parent.top,
    parent.right,
    parent.center.dy,
  );

  // Four disjoint Designer insertion handles, not fabricated SDK slot sizes.
  // Empty slots have no RenderBox; retain logical leading/trailing under RTL.
  Rect _listTileDropZone(
    Rect parent,
    CanvasDropZonePlacement placement,
    TextDirection direction,
  ) {
    final start = parent.left + parent.width * .25;
    final end = parent.right - parent.width * .25;
    return switch (placement) {
      CanvasDropZonePlacement.listTileLeading
          when direction == TextDirection.ltr =>
        Rect.fromLTRB(parent.left, parent.top, start, parent.bottom),
      CanvasDropZonePlacement.listTileTrailing
          when direction == TextDirection.rtl =>
        Rect.fromLTRB(parent.left, parent.top, start, parent.bottom),
      CanvasDropZonePlacement.listTileLeading ||
      CanvasDropZonePlacement.listTileTrailing => Rect.fromLTRB(
        end,
        parent.top,
        parent.right,
        parent.bottom,
      ),
      CanvasDropZonePlacement.menuItemChild => Rect.fromLTRB(
        start,
        parent.top,
        end,
        parent.bottom,
      ),
      CanvasDropZonePlacement.listTileTitle => Rect.fromLTRB(
        start,
        parent.top,
        end,
        parent.center.dy,
      ),
      CanvasDropZonePlacement.listTileSubtitle => Rect.fromLTRB(
        start,
        parent.center.dy,
        end,
        parent.bottom,
      ),
      _ => parent,
    };
  }

  bool _isEligibleDropSlot(CanvasNode node, String slotName) {
    if (node.type == 'flutter.material.Badge' && slotName == 'label') {
      // Visibility affects mounted descendants, not structural editing. Count
      // owns its generated label and must never advertise a label destination.
      return !node.properties.containsKey('count');
    }
    return _isInteractiveSlot(node, slotName);
  }

  _ExpansionTilePreviewState? _expansionState(CanvasNode node) {
    _ExpansionTilePreviewState? result;
    void visit(Element element) {
      if (result != null) return;
      if (element is StatefulElement &&
          element.state is _ExpansionTilePreviewState &&
          (element.state as _ExpansionTilePreviewState).widget.node.id ==
              node.id) {
        result = element.state as _ExpansionTilePreviewState;
        return;
      }
      element.visitChildren(visit);
    }

    final element = _geometryNodeKey(node.id)?.currentContext;
    if (element is Element) visit(element);
    return result;
  }

  Rect _expansionHeaderRect(CanvasNode node, RenderBox box, Rect parent) {
    if (node.type != 'flutter.material.ExpansionTile') return parent;
    final header = _expansionState(node)?.headerBox;
    return header == null
        ? parent
        : (_finiteRectInAncestor(header, box)?.intersect(parent) ?? parent);
  }

  Rect _expansionBodyRect(CanvasNode node, RenderBox box, Rect parent) {
    // Always resolve against the actual tile, including when a move resolver
    // already narrowed its candidate rectangle to this body.
    parent = Offset.zero & box.size;
    final header = _expansionHeaderRect(node, box, parent);
    final top = header.bottom.clamp(parent.top, parent.bottom);
    if ((node.slot('children')?.children.isNotEmpty ?? false) &&
        top < parent.bottom) {
      return Rect.fromLTRB(parent.left, top, parent.right, parent.bottom);
    }
    // An expanded empty body remains zero-sized in the SDK. Its compact append
    // band is Designer-only geometry over the header edge, never fake children.
    return Rect.fromLTRB(
      parent.left,
      math.max(parent.top, top - math.min(16, header.height / 4)),
      parent.right,
      parent.bottom,
    );
  }

  Rect _terminalZone(
    CanvasNode node,
    RenderBox parentBox,
    Rect parent,
    String slotName, {
    List<CanvasNode>? effectiveChildren,
  }) {
    if (node.type == 'flutter.material.ExpansionTile' &&
        slotName == 'children') {
      parent = _expansionBodyRect(node, parentBox, parent);
    }
    if (parent.width <= 0 || parent.height <= 0) {
      return Rect.zero;
    }
    if (node.type == 'flutter.widgets.CustomScrollView' &&
        slotName == 'slivers') {
      return parent;
    }
    final children =
        effectiveChildren ??
        node.slot(slotName)?.children ??
        const <CanvasNode>[];
    if (children.isEmpty) {
      // With no siblings there is only one legal ordering result: index 0.
      // Expose the complete visible container instead of making users find a
      // synthetic terminal edge on an otherwise blank linear container.
      if ((node.type == 'flutter.material.AppBar' || isCanvasSliverAppBarType(node.type)) && slotName == 'actions') {
        final width = math.min(parent.width, math.max(72.0, parent.width / 3));
        return _resolvedTextDirection(node) == TextDirection.rtl
            ? Rect.fromLTRB(
                parent.left,
                parent.top,
                parent.left + width,
                parent.bottom,
              )
            : Rect.fromLTRB(
                parent.right - width,
                parent.top,
                parent.right,
                parent.bottom,
              );
      }
      return parent;
    }
    if ((node.type == 'flutter.widgets.GridView' ||
            node.type == 'flutter.widgets.GridView.extent') &&
        slotName == 'children') {
      return _gridInsertionZone(
        node,
        parentBox,
        parent,
        children,
        children.length,
        markerExtent: _minimumTerminalBand,
      );
    }
    final last = _renderBox(_geometryNodeKey(children.last.id));
    if (last == null) {
      return Rect.zero;
    }
    final lastRect = _finiteRectInAncestor(last, parentBox);
    if (lastRect == null) {
      return Rect.zero;
    }
    final overflowBarVertical =
        node.type == 'flutter.widgets.OverflowBar' &&
        _isOverflowBarVertical(node, parentBox);
    if (node.type == 'flutter.widgets.Column' ||
        _ownsNativeMenu(node) ||
        overflowBarVertical ||
        (node.type == 'flutter.widgets.ListBody' &&
            !_isHorizontalListBody(node)) ||
        (node.type == 'flutter.widgets.ListView' &&
            !_isHorizontalListView(node)) ||
        (node.type == 'flutter.widgets.PageView' &&
            !_isHorizontalPageView(node))) {
      final upward = switch (node.type) {
        'flutter.widgets.ListBody' ||
        'flutter.widgets.ListView' ||
        'flutter.widgets.PageView' => _booleanValue(node, 'reverse') == true,
        'flutter.widgets.OverflowBar' =>
          _enumValue(node, 'overflowDirection') == 'up',
        _ => _enumValue(node, 'verticalDirection') == 'up',
      };
      final band = _minimumTerminalBand.clamp(1.0, parent.height);
      return upward
          ? Rect.fromLTRB(
              parent.left,
              parent.top,
              parent.right,
              (lastRect.top + band).clamp(parent.top, parent.bottom),
            )
          : Rect.fromLTRB(
              parent.left,
              (lastRect.bottom - band).clamp(parent.top, parent.bottom),
              parent.right,
              parent.bottom,
            );
    }
    final rightToLeft = switch (node.type) {
      'flutter.widgets.ListBody' => _isVisuallyReversedListBody(node),
      'flutter.widgets.ListView' => _isVisuallyReversedListView(node),
      'flutter.widgets.PageView' =>
        _isHorizontalPageView(node)
            ? (_resolvedTextDirection(node) == TextDirection.rtl) !=
                  (_booleanValue(node, 'reverse') == true)
            : false,
      _ => _resolvedTextDirection(node) == TextDirection.rtl,
    };
    final band = _minimumTerminalBand.clamp(1.0, parent.width);
    return rightToLeft
        ? Rect.fromLTRB(
            parent.left,
            parent.top,
            (lastRect.left + band).clamp(parent.left, parent.right),
            parent.bottom,
          )
        : Rect.fromLTRB(
            (lastRect.right - band).clamp(parent.left, parent.right),
            parent.top,
            parent.right,
            parent.bottom,
          );
  }

  Rect _gridInsertionZone(
    CanvasNode node,
    RenderBox parentBox,
    Rect parent,
    List<CanvasNode> children,
    int insertionIndex, {
    required double markerExtent,
  }) {
    if (parent.isEmpty || children.isEmpty) {
      return children.isEmpty ? parent : Rect.zero;
    }
    final beforeExisting = insertionIndex < children.length;
    final referenceIndex = beforeExisting
        ? insertionIndex
        : children.length - 1;
    final reference = _renderBox(_geometryNodeKey(children[referenceIndex].id));
    if (reference == null) {
      return Rect.zero;
    }
    final renderedReferenceRect = _finiteRectInAncestor(reference, parentBox);
    if (renderedReferenceRect == null) {
      return Rect.zero;
    }
    final referenceRect = renderedReferenceRect.intersect(parent);
    if (referenceRect.isEmpty) {
      return Rect.zero;
    }

    final crossAxisCount = _integerValue(node, 'crossAxisCount') ?? 2;
    final atGroupBoundary = beforeExisting
        ? insertionIndex % crossAxisCount == 0
        : children.length % crossAxisCount == 0;
    final horizontalMain = _isHorizontalGridView(node);
    final reverse = _booleanValue(node, 'reverse') == true;
    final direction = _resolvedTextDirection(node);
    final mainForwardPositive = horizontalMain
        ? (direction == TextDirection.ltr) != reverse
        : !reverse;
    final crossForwardPositive = horizontalMain
        ? true
        : direction == TextDirection.ltr;
    final useMainAxisEdge = atGroupBoundary;
    final forwardPositive = useMainAxisEdge
        ? mainForwardPositive
        : crossForwardPositive;
    final leading = beforeExisting;
    final useMinimumEdge = leading == forwardPositive;
    final verticalEdge = useMainAxisEdge == horizontalMain;
    final edge = verticalEdge
        ? (useMinimumEdge ? referenceRect.left : referenceRect.right)
        : (useMinimumEdge ? referenceRect.top : referenceRect.bottom);
    final extent = verticalEdge
        ? markerExtent.clamp(1.0, parent.width)
        : markerExtent.clamp(1.0, parent.height);
    final half = extent / 2;

    if (verticalEdge) {
      final center = edge.clamp(parent.left + half, parent.right - half);
      return Rect.fromLTRB(
        center - half,
        referenceRect.top,
        center + half,
        referenceRect.bottom,
      );
    }
    final center = edge.clamp(parent.top + half, parent.bottom - half);
    return Rect.fromLTRB(
      referenceRect.left,
      center - half,
      referenceRect.right,
      center + half,
    );
  }

  TextDirection _resolvedTextDirection(CanvasNode node) {
    final explicit = _enumValue(node, 'textDirection');
    if (explicit == 'rtl') {
      return TextDirection.rtl;
    }
    if (explicit == 'ltr') {
      return TextDirection.ltr;
    }
    final context = _geometryNodeKey(node.id)?.currentContext;
    return context == null
        ? TextDirection.ltr
        : Directionality.maybeOf(context) ?? TextDirection.ltr;
  }

  static bool _isHorizontalListView(CanvasNode node) =>
      node.type == 'flutter.widgets.ListView' &&
      _enumValue(node, 'scrollDirection') == 'horizontal';

  static bool _isHorizontalPageView(CanvasNode node) =>
      node.type == 'flutter.widgets.PageView' &&
      _enumValue(node, 'scrollDirection') == 'horizontal';

  static bool _isHorizontalGridView(CanvasNode node) =>
      (node.type == 'flutter.widgets.GridView' ||
          node.type == 'flutter.widgets.GridView.extent') &&
      _enumValue(node, 'scrollDirection') == 'horizontal';

  static bool _isHorizontalCustomScrollView(CanvasNode node) =>
      node.type == 'flutter.widgets.CustomScrollView' &&
      _enumValue(node, 'scrollDirection') == 'horizontal';

  static bool _isHorizontalListBody(CanvasNode node) =>
      node.type == 'flutter.widgets.ListBody' &&
      _enumValue(node, 'mainAxis') == 'horizontal';

  bool _isOverflowBarVertical(CanvasNode node, RenderBox parentBox) {
    final children = node.slot('children')?.children ?? const <CanvasNode>[];
    if (children.isEmpty || !parentBox.size.width.isFinite) {
      return false;
    }
    var actualWidth = _numberValue(node, 'spacing') ?? 0.0;
    actualWidth *= children.length - 1;
    for (final child in children) {
      final childBox = _renderBox(_geometryNodeKey(child.id));
      if (childBox == null || !childBox.size.width.isFinite) {
        return false;
      }
      actualWidth += childBox.size.width;
    }
    // This is the exact branch used by Flutter's _RenderOverflowBar:
    // children and spacing remain a row at equality, and become a column only
    // when their combined width is strictly greater than the available width.
    return actualWidth > parentBox.size.width;
  }

  bool _isVisuallyReversedListBody(CanvasNode node) {
    final reversed = _booleanValue(node, 'reverse') == true;
    if (!_isHorizontalListBody(node)) {
      return reversed;
    }
    final rightToLeft = _resolvedTextDirection(node) == TextDirection.rtl;
    return rightToLeft != reversed;
  }

  bool _isVisuallyReversedListView(CanvasNode node) {
    final reversed = _booleanValue(node, 'reverse') == true;
    if (!_isHorizontalListView(node)) {
      return reversed;
    }
    final rightToLeft = _resolvedTextDirection(node) == TextDirection.rtl;
    return rightToLeft != reversed;
  }

  Rect _flexibleSpaceTitleZone(Rect parent) => Rect.fromLTRB(
    parent.left, math.max(parent.top, parent.bottom - kToolbarHeight), parent.right, parent.bottom);

  Rect _appBarToolbarZone(CanvasNode node, RenderBox parentBox, Rect parent) {
    final bottom = node.slot('bottom')?.child;
    final bottomBox = bottom == null ? null : _renderBox(_geometryNodeKey(bottom.id));
    final bottomRect = bottomBox == null
        ? null
        : _finiteRectInAncestor(bottomBox, parentBox);
    final renderedBottomHeight = bottomRect?.intersect(parent).height ?? 0.0;
    final fallbackBottom = node.slot('bottom')?.children.isEmpty ?? true
        ? math.min(_minimumTerminalBand, parent.height / 3)
        : 0.0;
    final bottomHeight = math.max(renderedBottomHeight, fallbackBottom);
    if (isCanvasSliverAppBarType(node.type)) {
      final topInset = _booleanValue(node, 'primary') == false ? 0.0
          : MediaQuery.maybeOf(_geometryNodeKey(node.id)!.currentContext!)?.padding.top ?? 0.0;
      final top = math.min(parent.bottom, parent.top + topInset);
      final height = _numberValue(node, 'toolbarHeight') ?? (node.type == 'flutter.material.SliverAppBar' ? 56.0 : 64.0);
      return Rect.fromLTRB(parent.left, top, parent.right, math.max(top, math.min(top + height, parent.bottom - bottomHeight)));
    }
    return Rect.fromLTRB(
      parent.left,
      parent.top,
      parent.right,
      math.max(parent.top, parent.bottom - bottomHeight),
    );
  }

  Rect _appBarBottomZone(CanvasNode node, RenderBox parentBox, Rect parent) {
    if (isCanvasSliverAppBarType(node.type)) {
      final height = math.min(_minimumTerminalBand, parent.height / 3);
      return Rect.fromLTRB(parent.left, parent.bottom - height, parent.right, parent.bottom);
    }
    final toolbar = _appBarToolbarZone(node, parentBox, parent);
    return Rect.fromLTRB(
      parent.left,
      toolbar.bottom,
      parent.right,
      parent.bottom,
    );
  }

  Rect _appBarLeadingZone(CanvasNode node, RenderBox parentBox, Rect parent) {
    final toolbar = _appBarToolbarZone(node, parentBox, parent);
    final width = math.min(
      toolbar.width,
      math.max(
        _minimumTerminalBand,
        _numberValue(node, 'leadingWidth') ?? kToolbarHeight,
      ),
    );
    return _resolvedTextDirection(node) == TextDirection.rtl
        ? Rect.fromLTRB(
            toolbar.right - width,
            toolbar.top,
            toolbar.right,
            toolbar.bottom,
          )
        : Rect.fromLTRB(
            toolbar.left,
            toolbar.top,
            toolbar.left + width,
            toolbar.bottom,
          );
  }

  Rect _appBarTitleZone(CanvasNode node, RenderBox parentBox, Rect parent) {
    final toolbar = _appBarToolbarZone(node, parentBox, parent);
    final leading = _appBarLeadingZone(node, parentBox, parent);
    final trailingWidth = math.min(
      toolbar.width / 3,
      math.max(_minimumTerminalBand, toolbar.width / 4),
    );
    return _resolvedTextDirection(node) == TextDirection.rtl
        ? Rect.fromLTRB(
            toolbar.left + trailingWidth,
            toolbar.top,
            leading.left,
            toolbar.bottom,
          )
        : Rect.fromLTRB(
            leading.right,
            toolbar.top,
            toolbar.right - trailingWidth,
            toolbar.bottom,
          );
  }

  /// Slivers have no RenderBox. Project their paint geometry into the same
  /// surface coordinates used by the host, clipped to the owning viewport.
  // Retained hidden slivers can still have layout and even native hit testing.
  // Designer handles must address only the currently painted branch; the tree
  // remains the explicit editing surface for inactive/hidden branches.
  Object? _sliverVisibilityGeometryModel;
  Set<String> _paintedSliverVisibilityBranchIds = const {};

  bool _isInPaintedSliverVisibilityBranch(CanvasNode root, String id) {
    // Canvas models are immutable. Build once per model rather than walking
    // the entire tree separately for every sliver/box overlay or pointer hit.
    if (!identical(_sliverVisibilityGeometryModel, widget.model)) {
      final ids = <String>{};
      void visit(CanvasNode node) {
        ids.add(node.id);
        for (final entry in node.slots.entries) {
          if ((isCanvasSliverVisibilityType(node.type) || node.type == 'flutter.widgets.AnimatedCrossFade') && !_isInteractiveSlot(node, entry.key)) continue;
          for (final child in entry.value.children) {
            visit(child);
          }
        }
      }
      visit(root);
      _paintedSliverVisibilityBranchIds = ids;
      _sliverVisibilityGeometryModel = widget.model;
    }
    return _paintedSliverVisibilityBranchIds.contains(id);
  }

  Rect? _sliverAppBarDropZone(CanvasNode node, String slot, Rect surfaceRect) {
    final painted = _sliverGlobalRect(node, surfaceRect);
    if (painted == null) return null;
    RenderBox? box;
    void visit(RenderObject render) {
      if (box != null) return;
      if (render is RenderBox && render.hasSize) { box = render; return; }
      render.visitChildren(visit);
    }
    final render = _geometryNodeKey(node.id)?.currentContext?.findRenderObject();
    if (render == null) return null;
    visit(render);
    final parent = box;
    if (parent == null) return null;
    final bounds = Offset.zero & parent.size;
    final local = switch (slot) {
      'leading' => _appBarLeadingZone(node, parent, bounds),
      'title' => _appBarTitleZone(node, parent, bounds),
      'actions' => _terminalZone(node, parent, _appBarToolbarZone(node, parent, bounds), 'actions'),
      'bottom' => _appBarBottomZone(node, parent, bounds),
      'flexibleSpace' => bounds,
      _ => Rect.zero,
    };
    final global = _finiteTransformedRect(parent.getTransformTo(null), local)?.intersect(painted);
    return global == null || global.isEmpty ? null : global;
  }

  Rect? _sliverGlobalRect(CanvasNode node, Rect surfaceRect) {
    if (!_isInPaintedSliverVisibilityBranch(widget.model.root, node.id)) return null;
    final render = _geometryNodeKey(node.id)?.currentContext?.findRenderObject();
    if (render is! RenderSliver || !render.attached || render.geometry == null) {
      return null;
    }
    for (RenderObject? ancestor = render.parent; ancestor != null; ancestor = ancestor.parent) {
      // Offstage descendants are laid out but have no painted Canvas geometry.
      // Keep only the owning SliverOffstage's compact selection/drop handle.
      if (ancestor is _PrototypeMeasurementRenderBox ||
          ancestor is RenderSliverOffstage && ancestor.offstage) {
        return null;
      }
    }
    final horizontal = render.constraints.axis == Axis.horizontal;
    final extent = render.geometry!.paintExtent;
    if (extent == 0 && (render.constraints.scrollOffset > 0 ||
        render.constraints.remainingPaintExtent <= 0)) {
      return null;
    }
    final direction = applyGrowthDirectionToAxisDirection(
        render.constraints.axisDirection, render.constraints.growthDirection);
    final reversed = direction == AxisDirection.up || direction == AxisDirection.left;
    final start = extent == 0 && reversed ? -24.0 : 0.0;
    // Constrained slivers report their actual lane width in geometry, while
    // groups can inherit a child's geometry and still occupy the full constraint.
    final crossExtent = render is RenderSliverConstrainedCrossAxis
        ? render.geometry!.crossAxisExtent! : render.constraints.crossAxisExtent;
    final hitCrossExtent = crossExtent == 0 ? 24.0 : crossExtent;
    final local = horizontal
        ? Rect.fromLTWH(start, 0, math.max(24, extent), hitCrossExtent)
        : Rect.fromLTWH(0, start, hitCrossExtent, math.max(24, extent));
    final global = _finiteTransformedRect(render.getTransformTo(null), local);
    if (global == null) return null;
    var clip = surfaceRect;
    for (RenderObject? ancestor = render.parent; ancestor != null; ancestor = ancestor.parent) {
      if (ancestor is RenderViewportBase) {
        final viewport = _finiteGlobalRect(ancestor);
        if (viewport == null) return null;
        clip = clip.intersect(viewport);
        break;
      }
    }
    final rect = global.intersect(clip);
    return rect.isFinite && !rect.isEmpty ? rect : null;
  }

  static RenderBox? _renderBox(GlobalKey? key) {
    final renderObject = key?.currentContext?.findRenderObject();
    for (
      RenderObject? ancestor = renderObject?.parent;
      ancestor != null;
      ancestor = ancestor.parent
    ) {
      if (ancestor is _PrototypeMeasurementRenderBox ||
          ancestor is RenderSliverOffstage && ancestor.offstage ||
          ancestor is _MenuItemGeometryRenderBox && ancestor.blocked) {
        return null;
      }
    }
    return renderObject is RenderBox && renderObject.attached
        ? renderObject
        : null;
  }

  Rect? _resolvedGlobalDropZone(
    RenderBox box,
    Rect localZone,
    Offset globalPoint,
    Rect surfaceRect, {
    CanvasDropZonePlacement? placement,
    TextDirection direction = TextDirection.ltr,
  }) {
    final renderedBox = _finiteGlobalRect(box);
    if (renderedBox == null || !_hasFiniteGlobalInverse(box)) {
      return null;
    }
    if (box.size.isEmpty) {
      final bounded = _boundedDesignerHitRect(renderedBox, surfaceRect);
      final synthetic = switch (placement) {
        CanvasDropZonePlacement.badgeLabel => _badgeLabelZone(bounded),
        CanvasDropZonePlacement.menuItemChild ||
        CanvasDropZonePlacement.listTileLeading ||
        CanvasDropZonePlacement.listTileTitle ||
        CanvasDropZonePlacement.listTileSubtitle ||
        CanvasDropZonePlacement.listTileTrailing => _listTileDropZone(
          bounded,
          placement!,
          direction,
        ),
        _ => bounded,
      };
      return !synthetic.isEmpty && synthetic.contains(globalPoint)
          ? synthetic
          : null;
    }
    if (!localZone.isFinite || localZone.isEmpty) {
      return null;
    }
    final localPoint = _finiteLocalPoint(box, globalPoint);
    if (localPoint == null || !localZone.contains(localPoint)) {
      return null;
    }
    final renderedZone = _finiteGlobalRect(box, localRect: localZone);
    if (renderedZone == null) {
      return null;
    }
    final bounded = _boundedDesignerHitRect(renderedZone, surfaceRect);
    return bounded.isEmpty ? null : bounded;
  }

  static Rect? _finiteGlobalRect(RenderBox box, {Rect? localRect}) =>
      _finiteTransformedRect(
        box.getTransformTo(null),
        localRect ?? (Offset.zero & box.size),
      );

  static Rect? _finiteRectInAncestor(RenderBox box, RenderBox ancestor) =>
      _finiteTransformedRect(
        box.getTransformTo(ancestor),
        Offset.zero & box.size,
      );

  static Rect? _finiteTransformedRect(Matrix4 transform, Rect localRect) {
    if (!localRect.isFinite) {
      return null;
    }
    final storage = transform.storage;
    double homogeneousWeight(double x, double y) =>
        storage[3] * x + storage[7] * y + storage[15];
    final weights = <double>[
      homogeneousWeight(localRect.left, localRect.top),
      homogeneousWeight(localRect.right, localRect.top),
      homogeneousWeight(localRect.left, localRect.bottom),
      homogeneousWeight(localRect.right, localRect.bottom),
    ];
    final positiveWeight = weights.first > 0;
    if (weights.any(
      (weight) =>
          !weight.isFinite || weight == 0 || (weight > 0) != positiveWeight,
    )) {
      return null;
    }
    final rect = MatrixUtils.transformRect(transform, localRect);
    // RenderTransform permits singular and projective matrices whose painted
    // bounds contain NaN or infinity, or cross a projective horizon between
    // finite corners. Such geometry has no truthful finite IDE hit region, so
    // omit its synthetic target/drop zone instead of inventing one that could
    // steal interaction from a visible ancestor.
    return rect.isFinite ? rect : null;
  }

  static bool _hasFiniteGlobalInverse(RenderBox box) {
    final inverse = Matrix4.tryInvert(box.getTransformTo(null));
    return inverse != null && !inverse.storage.any((value) => !value.isFinite);
  }

  static Offset? _finiteLocalPoint(RenderBox box, Offset point) {
    final local = box.globalToLocal(point);
    return local.dx.isFinite && local.dy.isFinite ? local : null;
  }

  static String? _enumValue(CanvasNode node, String propertyName) {
    final value = node.properties[propertyName]?.value;
    return value is CanvasEnumValue ? value.value : null;
  }

  static bool? _booleanValue(CanvasNode node, String propertyName) {
    final property = node.properties[propertyName];
    return property?.kind == 'boolean' ? property!.value as bool : null;
  }

  static int? _integerValue(CanvasNode node, String propertyName) {
    final property = node.properties[propertyName];
    return property?.kind == 'integer' ? property!.value as int : null;
  }

  static double? _numberValue(CanvasNode node, String propertyName) {
    final value = node.properties[propertyName]?.value;
    return value is num ? value.toDouble() : null;
  }
}

class _InteractionBarrierOverlay extends StatelessWidget {
  const _InteractionBarrierOverlay();

  @override
  Widget build(BuildContext context) {
    final dark = Theme.of(context).brightness == Brightness.dark;
    return AbsorbPointer(
      key: const ValueKey('canvas-interaction-fence-overlay'),
      absorbing: true,
      child: ColoredBox(
        color: dark ? const Color(0x26000000) : const Color(0x14000000),
        child: Center(
          child: Semantics(
            liveRegion: true,
            label: 'Synchronizing Canvas input',
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: dark ? const Color(0xff303134) : const Color(0xfff8f9fa),
                border: Border.all(
                  color: dark
                      ? const Color(0xff5f6368)
                      : const Color(0xffbdc1c6),
                ),
                borderRadius: BorderRadius.circular(3),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x26000000),
                    blurRadius: 4,
                    offset: Offset(0, 1),
                  ),
                ],
              ),
              child: const Padding(
                padding: EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                child: Text(
                  'Synchronizing input…',
                  style: TextStyle(fontSize: 12),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _DropCandidate {
  const _DropCandidate(
    this.node,
    this.depth,
    this.area,
    this.slot,
    this.insertionIndex,
    this.zone,
  );

  final CanvasNode node;
  final int depth;
  final double area;
  final CanvasDropSlotSemantics slot;
  final int insertionIndex;
  final Rect zone;
}

class _ViewportGeometry {
  const _ViewportGeometry({
    required this.presentation,
    required this.scale,
    required this.left,
    required this.top,
    required this.renderedWidth,
    required this.renderedHeight,
    required this.availableWidth,
    required this.availableHeight,
    required this.horizontalOverflow,
    required this.verticalOverflow,
  });

  static const double _inset = 16;

  final CanvasViewportPresentation presentation;
  final double scale;
  final double left;
  final double top;
  final double renderedWidth;
  final double renderedHeight;
  final double availableWidth;
  final double availableHeight;
  final double horizontalOverflow;
  final double verticalOverflow;

  bool get horizontalScrollable => horizontalOverflow > 0.5;
  bool get verticalScrollable => verticalOverflow > 0.5;
  double get horizontalViewportFraction =>
      (availableWidth / renderedWidth).clamp(0.0, 1.0);
  double get verticalViewportFraction =>
      (availableHeight / renderedHeight).clamp(0.0, 1.0);

  static _ViewportGeometry calculate({
    required Size surface,
    required Size viewport,
    required CanvasViewportPresentation presentation,
  }) {
    final availableWidth = math.max(1.0, surface.width - _inset * 2);
    final availableHeight = math.max(1.0, surface.height - _inset * 2);
    final scale = presentation.mode == 'fit'
        ? math.min(
            1.0,
            math.min(
              availableWidth / viewport.width,
              availableHeight / viewport.height,
            ),
          )
        : presentation.zoomMicros / canvasViewportMicros;
    final renderedWidth = viewport.width * scale;
    final renderedHeight = viewport.height * scale;
    final horizontalOverflow = math.max(0.0, renderedWidth - availableWidth);
    final verticalOverflow = math.max(0.0, renderedHeight - availableHeight);
    final horizontalProgress =
        presentation.horizontalScrollMicros / canvasViewportMicros;
    final verticalProgress =
        presentation.verticalScrollMicros / canvasViewportMicros;
    return _ViewportGeometry(
      presentation: presentation,
      scale: scale,
      left: horizontalOverflow > 0
          ? _inset - horizontalOverflow * horizontalProgress
          : (surface.width - renderedWidth) / 2,
      top: verticalOverflow > 0
          ? _inset - verticalOverflow * verticalProgress
          : (surface.height - renderedHeight) / 2,
      renderedWidth: renderedWidth,
      renderedHeight: renderedHeight,
      availableWidth: availableWidth,
      availableHeight: availableHeight,
      horizontalOverflow: horizontalOverflow,
      verticalOverflow: verticalOverflow,
    );
  }
}

class _CanvasViewportScrollbar extends StatefulWidget {
  const _CanvasViewportScrollbar({
    required this.axis,
    required this.valueMicros,
    required this.viewportFraction,
    required this.onChanged,
    super.key,
  });

  final Axis axis;
  final int valueMicros;
  final double viewportFraction;
  final ValueChanged<int> onChanged;

  // Keep the pointer target comfortably usable while rendering a deliberately
  // restrained six-pixel enterprise scrollbar.
  static const double hitThickness = 12;
  static const double visualThickness = 6;

  @override
  State<_CanvasViewportScrollbar> createState() =>
      _CanvasViewportScrollbarState();
}

class _CanvasViewportScrollbarState extends State<_CanvasViewportScrollbar> {
  static const Duration _feedbackDuration = Duration(milliseconds: 90);

  bool _hovered = false;
  bool _dragging = false;

  void _setHovered(bool value) {
    if (_hovered != value) {
      setState(() => _hovered = value);
    }
  }

  void _setDragging(bool value) {
    if (_dragging != value) {
      setState(() => _dragging = value);
    }
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final axis = widget.axis;
        final extent = axis == Axis.horizontal
            ? constraints.maxWidth
            : constraints.maxHeight;
        final thumbExtent = math.min(
          extent,
          math.max(24.0, extent * widget.viewportFraction),
        );
        final travel = math.max(0.0, extent - thumbExtent);
        final thumbOffset = travel * widget.valueMicros / canvasViewportMicros;
        int valueAt(Offset position) {
          if (travel == 0) {
            return 0;
          }
          final coordinate = axis == Axis.horizontal
              ? position.dx
              : position.dy;
          return ((coordinate - thumbExtent / 2) /
                  travel *
                  canvasViewportMicros)
              .round()
              .clamp(0, canvasViewportMicros);
        }

        void changeBy(int delta) {
          widget.onChanged(
            (widget.valueMicros + delta).clamp(0, canvasViewportMicros),
          );
        }

        final increasedValue = (widget.valueMicros + 100000).clamp(
          0,
          canvasViewportMicros,
        );
        final decreasedValue = (widget.valueMicros - 100000).clamp(
          0,
          canvasViewportMicros,
        );

        final dark = Theme.of(context).brightness == Brightness.dark;
        final emphasized = _hovered || _dragging;
        final trackColor = dark
            ? Color(emphasized ? 0x3dffffff : 0x24ffffff)
            : Color(emphasized ? 0x335f666d : 0x1f5f666d);
        final thumbColor = _dragging
            ? (dark ? const Color(0xff6ca5dc) : const Color(0xff3f78b5))
            : _hovered
            ? (dark ? const Color(0xbfe4e6e8) : const Color(0xbf50575e))
            : (dark ? const Color(0x99d0d3d6) : const Color(0x995f666d));
        final axisName = axis == Axis.horizontal ? 'horizontal' : 'vertical';

        return Semantics(
          label: axis == Axis.horizontal
              ? 'Canvas horizontal scroll'
              : 'Canvas vertical scroll',
          value: '${(widget.valueMicros / 10000).round()}%',
          slider: true,
          increasedValue: increasedValue == widget.valueMicros
              ? null
              : '${(increasedValue / 10000).round()}%',
          decreasedValue: decreasedValue == widget.valueMicros
              ? null
              : '${(decreasedValue / 10000).round()}%',
          onIncrease: increasedValue == widget.valueMicros
              ? null
              : () => changeBy(100000),
          onDecrease: decreasedValue == widget.valueMicros
              ? null
              : () => changeBy(-100000),
          child: MouseRegion(
            onEnter: (_) => _setHovered(true),
            onExit: (_) => _setHovered(false),
            child: GestureDetector(
              behavior: HitTestBehavior.opaque,
              onPanDown: (details) {
                _setDragging(true);
                widget.onChanged(valueAt(details.localPosition));
              },
              onPanUpdate: (details) =>
                  widget.onChanged(valueAt(details.localPosition)),
              onPanEnd: (_) => _setDragging(false),
              onPanCancel: () => _setDragging(false),
              child: Stack(
                children: [
                  Positioned(
                    left: axis == Axis.horizontal ? 0 : null,
                    right: 0,
                    top: axis == Axis.vertical ? 0 : null,
                    bottom: 0,
                    width: axis == Axis.vertical
                        ? _CanvasViewportScrollbar.visualThickness
                        : null,
                    height: axis == Axis.horizontal
                        ? _CanvasViewportScrollbar.visualThickness
                        : null,
                    child: AnimatedContainer(
                      key: ValueKey(
                        'canvas-$axisName-viewport-scrollbar-track',
                      ),
                      duration: _feedbackDuration,
                      curve: Curves.easeOut,
                      decoration: BoxDecoration(
                        color: trackColor,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  Positioned(
                    left: axis == Axis.horizontal ? thumbOffset : null,
                    right: axis == Axis.vertical ? 0 : null,
                    top: axis == Axis.vertical ? thumbOffset : null,
                    bottom: axis == Axis.horizontal ? 0 : null,
                    width: axis == Axis.horizontal
                        ? thumbExtent
                        : _CanvasViewportScrollbar.visualThickness,
                    height: axis == Axis.vertical
                        ? thumbExtent
                        : _CanvasViewportScrollbar.visualThickness,
                    child: AnimatedContainer(
                      key: ValueKey(
                        'canvas-$axisName-viewport-scrollbar-thumb',
                      ),
                      duration: _feedbackDuration,
                      curve: Curves.easeOut,
                      decoration: BoxDecoration(
                        color: thumbColor,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

class _DropZoneOverlay extends StatelessWidget {
  const _DropZoneOverlay({
    required this.target,
    required this.constraints,
    required this.indicatorKind,
  });

  final CanvasDropTarget target;
  final BoxConstraints constraints;
  final CanvasDropIndicatorKind indicatorKind;

  @override
  Widget build(BuildContext context) {
    const denominator = 1000000.0;
    final zone = target.zone!;
    final left = constraints.maxWidth * zone.leftMicros / denominator;
    final top = constraints.maxHeight * zone.topMicros / denominator;
    final width =
        constraints.maxWidth *
        (zone.rightMicros - zone.leftMicros) /
        denominator;
    final height =
        constraints.maxHeight *
        (zone.bottomMicros - zone.topMicros) /
        denominator;
    final widgetMove = indicatorKind == CanvasDropIndicatorKind.widgetMove;
    return Positioned(
      left: left,
      top: top,
      width: width,
      height: height,
      child: IgnorePointer(
        child: Semantics(
          label: widgetMove
              ? 'Flutter widget move target for ${target.slotName}'
              : 'Flutter widget insertion target for ${target.slotName}',
          child: DecoratedBox(
            key: ValueKey(
              widgetMove
                  ? 'canvas-widget-move-preview-zone'
                  : 'canvas-widget-insert-drop-zone',
            ),
            decoration: BoxDecoration(
              color: widgetMove
                  ? const Color(0x24D29A17)
                  : const Color(0x261A73E8),
              border: Border.all(
                color: widgetMove
                    ? const Color(0xffc58b08)
                    : const Color(0xff1a73e8),
                width: widgetMove ? 1.5 : 2,
              ),
              borderRadius: BorderRadius.circular(widgetMove ? 2 : 4),
            ),
          ),
        ),
      ),
    );
  }
}

@immutable
class _InlineTextEditSession {
  const _InlineTextEditSession({
    required this.widgetId,
    required this.presentationSequence,
    required this.documentId,
    required this.logicalRevisionId,
  });

  final String widgetId;
  final int presentationSequence;
  final String documentId;
  final int logicalRevisionId;
}

/// Flutter 3.44.8's maintained-size Visibility marks only paint dirty when
/// visible changes. If IgnorePointer does not also change, semantics can stay
/// stale (or assert on the next child update). Invalidate the actual SDK render
/// object; do not replace its layout, paint, hit, focus or semantics behavior.
class _CanvasVisibility extends StatefulWidget {
  const _CanvasVisibility({required this.visibility});

  final Visibility visibility;

  @override
  State<_CanvasVisibility> createState() => _CanvasVisibilityState();
}

class _CanvasVisibilityState extends State<_CanvasVisibility> {
  final _sdkKey = GlobalKey();

  @override
  void didUpdateWidget(_CanvasVisibility oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.visibility.visible != widget.visibility.visible &&
        oldWidget.visibility.maintainSize) {
      _sdkKey.currentContext?.findRenderObject()?.markNeedsSemanticsUpdate();
    }
  }

  @override
  Widget build(BuildContext context) =>
      KeyedSubtree(key: _sdkKey, child: widget.visibility);
}

/// The pinned SDK's maintained-size sliver visibility also dirties only paint
/// on visible changes. Refresh native semantics without replacing SDK behavior.
class _CanvasSliverVisibility extends StatefulWidget {
  const _CanvasSliverVisibility({required this.visibility});
  final SliverVisibility visibility;
  @override
  State<_CanvasSliverVisibility> createState() => _CanvasSliverVisibilityState();
}
class _CanvasSliverVisibilityState extends State<_CanvasSliverVisibility> {
  final _sdkKey = GlobalKey();
  @override
  void didUpdateWidget(_CanvasSliverVisibility oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.visibility.visible != widget.visibility.visible && oldWidget.visibility.maintainSize) {
      _sdkKey.currentContext?.findRenderObject()?.markNeedsSemanticsUpdate();
    }
  }
  @override
  Widget build(BuildContext context) => KeyedSubtree(key: _sdkKey, child: widget.visibility);
}

/// Native medium/large mount title twice. Canonical model keys follow the
/// active presentation; mirror keys are stable and private to that mount.
class _CanvasSliverAppBarTitle extends StatefulWidget {
  const _CanvasSliverAppBarTitle({required this.builder});
  final Widget Function(GlobalKey Function(String)? keys) builder;
  @override
  State<_CanvasSliverAppBarTitle> createState() => _CanvasSliverAppBarTitleState();
}
class _CanvasSliverAppBarTitleState extends State<_CanvasSliverAppBarTitle> {
  final _mirrorKeys = <String, GlobalKey>{};
  @override
  Widget build(BuildContext context) {
    final settings = context.dependOnInheritedWidgetOfExactType<FlexibleSpaceBarSettings>();
    final inToolbar = context.findAncestorWidgetOfExactType<NavigationToolbar>() != null;
    final active = inToolbar == (settings?.isScrolledUnder ?? false);
    return widget.builder(active ? null : (id) => _mirrorKeys.putIfAbsent(id, GlobalKey.new));
  }
}


// Runtime type, not Designer StableId, controls unkeyed Flutter replacement.
String _switcherRuntimeType(CanvasNode node) => node.type.split('.').take(3).join('.');

class _SwitcherBranch extends InheritedWidget {
  const _SwitcherBranch({required this.active, required super.child});
  final bool active;
  @override
  bool updateShouldNotify(_SwitcherBranch oldWidget) => active != oldWidget.active;
}

// Flutter 3.44.8 wraps transition.key rather than the entry sequence number.
// Keep an unkeyed outer wrapper so repeated child types retain independent
// preview episodes. No project builder is executed. Source keeps SDK defaults.
Widget _switcherPreviewTransition(Widget child, Animation<double> animation) =>
    KeyedSubtree(child: AnimatedSwitcher.defaultTransitionBuilder(child, animation));

Widget _switcherPreviewLayout(Widget? current, List<Widget> previous) => Stack(
  alignment: Alignment.center,
  children: [
    for (final child in [...previous, ?current])
      KeyedSubtree(
        key: child.key,
        child: _SwitcherBranch(
          active: identical(child, current),
          child: IgnorePointer(
            ignoring: !identical(child, current),
            child: ExcludeFocus(
              excluding: !identical(child, current),
              child: ExcludeSemantics(excluding: !identical(child, current), child: child),
            ),
          ),
        ),
      ),
  ],
);

/// Each retained SDK entry owns separate instrumentation keys. A -> B -> A and
/// moving descendants out of a still-fading entry must never duplicate GlobalKeys.
class _SwitcherEntryView extends StatefulWidget {
  const _SwitcherEntryView({super.key, required this.node, required this.builder});
  final CanvasNode node;
  final Widget Function(GlobalKey Function(String)) builder;
  @override
  State<_SwitcherEntryView> createState() => _SwitcherEntryViewState();
}

class _SwitcherEntryViewState extends State<_SwitcherEntryView> {
  final Map<String, GlobalKey> _keys = {};
  final Map<String, GlobalKey<_MenuAnchorPreviewState>> _menus = {};
  final Map<String, GlobalKey> _tooltips = {};
  _CanvasDocumentViewState? _owner;
  bool _scheduled = false;
  GlobalKey _key(String id) => _keys.putIfAbsent(id, () => GlobalKey(debugLabel: 'switcher-$id'));
  bool get _active {
    var active = true;
    context.visitAncestorElements((element) {
      if (element.widget case _SwitcherBranch(active: false)) { active = false; return false; }
      return true;
    });
    return active;
  }
  void _release() {
    final registry = _owner?._switcherGeometryKeys;
    registry?.removeWhere((id, key) => identical(_keys[id], key));
    _owner?._switcherMenuKeys.removeWhere((id,key)=>identical(_menus[id],key));
    _owner?._switcherTooltipKeys.removeWhere((id,key)=>identical(_tooltips[id],key));
  }
  @override
  void didUpdateWidget(_SwitcherEntryView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!identical(oldWidget.node, widget.node)) {
      // Unkeyed same-type replacements update existing native State, even when
      // the editor allocates new IDs. Reassociate structurally matching keys.
      final retained = <String, GlobalKey>{};
      final menus = <String, GlobalKey<_MenuAnchorPreviewState>>{};
      final tooltips = <String, GlobalKey>{};
      void match(CanvasNode old, CanvasNode next) {
        if (_switcherRuntimeType(old) != _switcherRuntimeType(next)) return;
        if (_keys[old.id] case final key?) retained[next.id] = key;
        if (_menus[old.id] case final key?) menus[next.id] = key;
        if (_tooltips[old.id] case final key?) tooltips[next.id] = key;
        for (final slot in next.slots.entries) {
          final prior = old.slot(slot.key)?.children ?? const <CanvasNode>[];
          for (var i = 0; i < math.min(prior.length, slot.value.children.length); i++) {
            match(prior[i], slot.value.children[i]);
          }
        }
      }
      match(oldWidget.node, widget.node);
      _release(); _keys..clear()..addAll(retained);
      _menus..clear()..addAll(menus);
      _tooltips..clear()..addAll(tooltips);
    }
  }
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    context.dependOnInheritedWidgetOfExactType<_SwitcherBranch>();
    _owner = context.findAncestorStateOfType<_CanvasDocumentViewState>();
  }
  @override
  Widget build(BuildContext context) {
    if (!_scheduled) {
      _scheduled = true;
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _scheduled = false;
        if (!mounted || _owner?.mounted != true) return;
        if (_active) {
          for (final entry in _keys.entries) {
            if (_owner!.widget.model.widgetIds.contains(entry.key)) {
              _owner!._switcherGeometryKeys[entry.key] = entry.value;
            }
          }
          for(final entry in _menus.entries) {
            if(_owner!.widget.model.widgetIds.contains(entry.key)) _owner!._switcherMenuKeys[entry.key]=entry.value;
          }
          for(final entry in _tooltips.entries) {
            if(_owner!.widget.model.widgetIds.contains(entry.key)) _owner!._switcherTooltipKeys[entry.key]=entry.value;
          }
        } else { _release(); }
        _owner!._refreshZeroSizedWidgetTargetsAfterFrame();
      });
    }
    return widget.builder(_key);
  }
  @override
  void dispose() { _release(); super.dispose(); }
}

// A process-lifetime empty source. Each preview table installs/removes its own listener.
class _EmptyCanvasDataTableSource extends DataTableSource {
  static final instance=_EmptyCanvasDataTableSource();
  @override DataRow? getRow(int index)=>null;
  @override int get rowCount=>0;
  @override int get selectedRowCount=>0;
  @override bool get isRowCountApproximate=>false;
}

String _dateRangePickerPreviewMessage(CanvasNode node) =>
    'DateRangePickerDialog ${node.id}: design-only preview, pointer input and autofocus disabled. '
    'Save/OK returns DateTimeRange through Navigator in Run/Debug; cancel returns null. '
    'Project dates/ranges/calendars show a preview-unavailable placeholder; predicates are not executed and typed keyboard uses datetime. '
    'Initial range/mode are remounted only when the Designer model changes.';
String _timePickerDialogPreviewMessage(CanvasNode node) =>
    'TimePickerDialog ${node.id}: design-only dialog; pointer input, focus and dismissal are suppressed. '
    'Confirm returns TimeOfDay through Navigator in Run/Debug; cancel returns null. Only entry-mode changes are constructor Events. '
    'Project initialTime cannot be previewed; project callbacks are not executed. Initial state is remounted on Designer edits.';
String _inputDatePickerFormFieldPreviewMessage(CanvasNode node) =>
    'InputDatePickerFormField ${node.id}: design-only date text field; editing, autofocus and focus are disabled. '
    'Project dates/calendar delegates show an unavailable placeholder; predicates, focus nodes and callbacks are not executed. '
    'Typed keyboard sources use datetime in preview. Use Run/Debug for submit, Form.save, locale parsing and validation. '
    'acceptEmptyDate allows empty validation but does not emit null. Designer model edits remount the preview.';
String _calendarDatePickerPreviewMessage(CanvasNode node) =>
    'CalendarDatePicker ${node.id}: inline calendar, not a dialog. Design-only preview: pointer input and focus disabled. '
    'Project dates/calendar delegates show an unavailable placeholder; predicates and callbacks are not executed. '
    'Use Run/Debug for selection/month Events and native same-key state. Designer model edits remount the preview.';
String _datePickerPreviewMessage(CanvasNode node) {
  final sources=node.properties.entries.where((e)=>e.value.kind=='dartObjectReferencePresence').map((e)=>e.key).toList();
  return sources.isEmpty ? 'Dialog preview: interactions and route dismissal are suppressed; use Run/Debug to select a date and receive the Navigator result.'
      : 'Project values are not executed: ${sources.join(', ')}. Project dates/calendar delegates cannot be previewed; predicates are omitted. Verify date bounds, custom calendar and route result with Run/Debug.';
}

String _dataTablePreviewMessage(CanvasNode root) {
  final messages=<String>[];
  if(root.type==canvasPaginatedDataTableType)messages.add('DataTableSource is not executed: preview has zero data rows; source counts, loading, sorting and selection must be tested with Run/Debug.');
  void visit(CanvasNode n) {
    final refs=n.properties.entries.where((e)=>e.value.kind=='dartObjectReferencePresence').map((e)=>e.key).toList();
    if(refs.isNotEmpty)messages.add('${n.type} ${n.id}: ${refs.join(', ')}');
    for (final slot in n.slots.values) {
      for (final child in slot.children) {
        if (isCanvasDataDescriptor(child.type)) visit(child);
      }
    }
  }
  visit(root);
  return messages.isEmpty?'':'Source-owned values are not executed in Canvas. Native defaults, private row keys and inert callbacks are used; sorting and selection remain controlled. ${messages.join('; ')}';
}

class _CanvasNodeView extends StatelessWidget implements PreferredSizeWidget {
  const _CanvasNodeView({
    required this.node,
    required this.imageResources,
    required this.onImageError,
    required this.selectedWidgetId,
    required this.onSelected,
    required this.nodeKey,
    required this.designerFocusParent,
    required this.overlayScale,
    required this.inlineTextEditEnabled,
    required this.inlineTextEditingWidgetId,
    required this.onBeginInlineTextEdit,
    required this.onCommitInlineTextEdit,
    required this.onCancelInlineTextEdit,
    this.suppressDesignerSemantics = false,
  });

  final CanvasNode node;
  final CanvasImageResourceBundle imageResources;
  final CanvasImageErrorReporter? onImageError;
  final String? selectedWidgetId;
  final ValueChanged<String> onSelected;
  final GlobalKey Function(String id) nodeKey;
  final FocusNode designerFocusParent;
  final double overlayScale;
  final bool inlineTextEditEnabled;
  final String? inlineTextEditingWidgetId;
  final bool Function(String) onBeginInlineTextEdit;
  final bool Function(String, String, bool) onCommitInlineTextEdit;
  final VoidCallback onCancelInlineTextEdit;
  final bool suppressDesignerSemantics;

  @override
  Size get preferredSize => node.type == 'flutter.material.AppBar'
      ? AppBar(
          toolbarHeight: _number('toolbarHeight'),
          bottom: _preferredSizeSingle('bottom'),
        ).preferredSize
      : node.type == 'flutter.widgets.PreferredSize'
        ? Size((node.properties['preferredSize']!.value as CanvasSizeValue).width,
            (node.properties['preferredSize']!.value as CanvasSizeValue).height)
        : Size.zero;

  @override
  Widget build(BuildContext context) {
    final paddingGeometry = switch (node.type) {
      'flutter.widgets.Padding' => _paddingGeometry(),
      'flutter.widgets.Container' => _edgeInsetsGeometry('padding'),
      _ => null,
    };
    final marginGeometry = node.type == 'flutter.widgets.Container'
        ? _edgeInsetsGeometry('margin')
        : null;
    final editing = inlineTextEditingWidgetId == node.id;
    final sdkChild = switch (node.type) {
      'flutter.material.Scaffold' => _scaffold(context),
      'flutter.material.AppBar' => _appBar(context),
      'flutter.material.FlexibleSpaceBar' => _flexibleSpaceBar(context),
      'flutter.material.FlexibleSpaceBarSettings' => FlexibleSpaceBarSettings(
        toolbarOpacity: _number('toolbarOpacity')!,
        minExtent: _number('minExtent')!, maxExtent: _number('maxExtent')!, currentExtent: _number('currentExtent')!,
        isScrolledUnder: _boolean('isScrolledUnder'), hasLeading: _boolean('hasLeading'),
        child: _single('child')!,
      ),
      'flutter.material.SliverAppBar' || 'flutter.material.SliverAppBar.medium' || 'flutter.material.SliverAppBar.large' => _sliverAppBar(context),
      'flutter.material.NavigationBar' => _navigationBar(context),
      'flutter.material.NavigationRail' => _navigationRail(context),
      'flutter.material.NavigationDrawer' => _navigationDrawer(context),
      'flutter.material.Drawer' => _drawer(context),
      'flutter.material.BottomAppBar' => _bottomAppBar(context),
      'flutter.material.BottomNavigationBar' => _bottomNavigationBar(context),
      'flutter.material.Material' => _material(context),
      'flutter.material.Scrollbar' => _scrollbar(),
      canvasBottomSheetType => _TextButtonPreview(message: _bottomSheetPreviewMessage(node), child: _bottomSheet(context)),
      canvasSnackBarType => _TextButtonPreview(message:_snackBarPreviewMessage(node),child:_snackBar(context)),
      canvasSnackBarActionType => _TextButtonPreview(message:_snackBarPreviewMessage(node),
        child:ExcludeFocus(child:AbsorbPointer(child:_snackBarAction(context)))),
      canvasSimpleDialogType || canvasSimpleDialogOptionType => _TextButtonPreview(message: _simpleDialogPreviewMessage(node), child: _simpleDialog(context)),
      canvasAlertDialogType || canvasAdaptiveAlertDialogType => _TextButtonPreview(message:_alertDialogPreviewMessage(node),child:_alertDialog(context)),
      canvasDialogType || canvasFullscreenDialogType => _TextButtonPreview(message:_dialogPreviewMessage(node) ?? 'Dialog surface: child widgets own Events; showDialog/DialogRoute owns dismissal and result.',
        child:ExcludeSemantics(excluding:!const {'none','dialog','alertDialog'}.contains(_enum('semanticsRole')??'dialog'),child:_dialog(context))),
      'flutter.material.Card' => _card(context),
      'flutter.material.Badge' => _badge(context),
      'flutter.material.CircleAvatar' => _circleAvatar(context),
      'flutter.material.RefreshIndicator' => _CanvasRefreshIndicatorPreview(
        node: node,
        color: _resolvedColor(context, 'color'),
        backgroundColor: _resolvedColor(context, 'backgroundColor'),
        child: _single('child')!,
      ),
      'flutter.material.LinearProgressIndicator' => _linearProgressIndicator(
        context,
      ),
      'flutter.material.CircularProgressIndicator' =>
        _circularProgressIndicator(context),
      'flutter.material.RefreshProgressIndicator' => _refreshProgressIndicator(
        context,
      ),
      'flutter.material.Divider' => Divider(
        height: _number('height'),
        thickness: _number('thickness'),
        indent: _number('indent'),
        endIndent: _number('endIndent'),
        color: _resolvedColor(context, 'color'),
        radius: switch (node.properties['radius']?.value) {
          final CanvasBorderRadiusGeometryValue radius => _borderRadius(radius),
          _ => null,
        },
      ),
      'flutter.material.VerticalDivider' => VerticalDivider(
        width: _number('width'),
        thickness: _number('thickness'),
        indent: _number('indent'),
        endIndent: _number('endIndent'),
        color: _resolvedColor(context, 'color'),
        radius: switch (node.properties['radius']?.value) {
          final CanvasBorderRadiusGeometryValue radius => _borderRadius(radius),
          _ => null,
        },
      ),
      'flutter.material.ElevatedButton' => _elevatedButton(context),
      'flutter.material.TextButton' => _textButton(context),
      'flutter.material.MenuItemButton' => _menuItemButton(context),
      'flutter.material.MenuAnchor' ||
      'flutter.material.MenuBar' ||
      'flutter.material.SubmenuButton' => _MenuAnchorPreview(
        key: (context.findAncestorStateOfType<_SwitcherEntryViewState>()?._menus ??
            context.findAncestorStateOfType<_CanvasDocumentViewState>()?._menuAnchorPreviewKeys)
            ?.putIfAbsent(
              node.id,
              () => GlobalKey<_MenuAnchorPreviewState>(
                debugLabel: 'menu-anchor-${node.id}',
              ),
            ),
        owner: this,
        menuBar: node.type == 'flutter.material.MenuBar',
      ),
      'flutter.material.OutlinedButton' => _textButton(context),
      'flutter.material.FilledButton' => _textButton(context),
      'flutter.material.IconButton' => _iconButton(context),
      'flutter.material.Checkbox' => _checkbox(context),
      'flutter.material.Switch' => _switch(context),
      'flutter.material.Radio' => _radio(context),
      'flutter.material.RadioListTile' => _radioListTile(context),
      'flutter.material.ExpansionTile' => _expansionTile(context),
      'flutter.material.Tooltip' => _tooltip(context),
      'flutter.material.TooltipVisibility' => TooltipVisibility(
        visible: _boolean('visible')!,
        child: _single('child')!,
      ),
      'flutter.material.TooltipTheme' => _tooltipTheme(context),
      'flutter.material.ListTile' => _listTile(context),
      'flutter.material.CheckboxListTile' => _checkboxListTile(context),
      'flutter.material.SwitchListTile' => _switchListTile(context),
      'flutter.widgets.RadioGroup' => _RadioGroupPreview(
        node: node,
        child: _single('child')!,
      ),
      'flutter.material.RangeSlider' => _rangeSlider(context),
      'flutter.material.Slider' => _slider(context),
      'flutter.material.FloatingActionButton' => _floatingActionButton(context),
      'flutter.widgets.Column' => _column(),
      'flutter.widgets.Row' => _row(),
      'flutter.widgets.Wrap' => _wrap(),
      'flutter.widgets.ListBody' => _listBody(),
      'flutter.widgets.OverflowBar' => _overflowBar(),
      'flutter.widgets.ListView' => _listView(),
      'flutter.widgets.GridView' => _gridView(),
      'flutter.widgets.GridView.extent' => _gridView(extent: true),
      'flutter.widgets.SingleChildScrollView' => _singleChildScrollView(),
      'flutter.widgets.PageView' => _pageView(),
      'flutter.widgets.ListWheelScrollView' => _listWheelScrollView(),
      'flutter.widgets.CustomScrollView' => _customScrollView(),
      'flutter.widgets.SliverMainAxisGroup' => SliverMainAxisGroup(slivers: _children('slivers')),
      'flutter.widgets.SliverCrossAxisGroup' => SliverCrossAxisGroup(slivers: _children('slivers')),
      'flutter.widgets.SliverConstrainedCrossAxis' => SliverConstrainedCrossAxis(
        maxExtent: node.properties['maxExtent']!.value is CanvasEnumValue
            ? double.infinity : _number('maxExtent')!,
        sliver: _single('sliver')!),
      'flutter.widgets.SliverCrossAxisExpanded' => SliverCrossAxisExpanded(flex: _integer('flex')!, sliver: _single('sliver')!),
      'flutter.widgets.SliverToBoxAdapter' => _sliverToBoxAdapter(context),
      'flutter.widgets.SliverVisibility' => _CanvasSliverVisibility(
        visibility: SliverVisibility(
          visible: _boolean('visible') ?? true,
          maintainState: _boolean('maintainState') ?? false,
          maintainAnimation: _boolean('maintainAnimation') ?? false,
          maintainSize: _boolean('maintainSize') ?? false,
          maintainSemantics: _boolean('maintainSemantics') ?? false,
          maintainInteractivity: _boolean('maintainInteractivity') ?? false,
          sliver: _single('sliver')!,
          replacementSliver: _single('replacementSliver') ?? const SliverToBoxAdapter(),
        ),
      ),
      'flutter.widgets.SliverVisibility.maintain' => _CanvasSliverVisibility(
        visibility: SliverVisibility.maintain(
          visible: _boolean('visible') ?? true,
          sliver: _single('sliver')!,
          replacementSliver: _single('replacementSliver') ?? const SliverToBoxAdapter(),
        ),
      ),
      'flutter.widgets.SliverSafeArea' => SliverSafeArea(
        left: _boolean('left') ?? true,
        top: _boolean('top') ?? true,
        right: _boolean('right') ?? true,
        bottom: _boolean('bottom') ?? true,
        minimum: _physicalEdgeInsets('minimum') ?? EdgeInsets.zero,
        sliver: _single('sliver')!,
      ),
      'flutter.widgets.SliverOffstage' => SliverOffstage(
        offstage: _boolean('offstage') ?? true,
        sliver: _single('sliver') ?? const SliverToBoxAdapter(),
      ),
      'flutter.widgets.SliverIgnorePointer' => SliverIgnorePointer(
        ignoring: _boolean('ignoring') ?? true,
        // Retained for complete pinned-SDK compatibility; omission/null uses modern semantics.
        // ignore: deprecated_member_use
        ignoringSemantics: _boolean('ignoringSemantics'),
        sliver: _single('sliver') ?? const SliverToBoxAdapter(),
      ),
      'flutter.widgets.SliverFloatingHeader' => SliverFloatingHeader(
        animationStyle: _floatingHeaderAnimationStyle(),
        snapMode: _enum('snapMode') == 'scroll' ? FloatingHeaderSnapMode.scroll
            : _enum('snapMode') == 'overlay' ? FloatingHeaderSnapMode.overlay : null,
        child: _single('child')!,
      ),
      'flutter.widgets.PinnedHeaderSliver' => PinnedHeaderSliver(child: _single('child')),
      'flutter.widgets.SliverResizingHeader' => SliverResizingHeader(
        minExtentPrototype: _headerPrototype('minExtentPrototype'),
        maxExtentPrototype: _headerPrototype('maxExtentPrototype'),
        child: _single('child'),
      ),
      'flutter.widgets.SliverPersistentHeader' => SliverPersistentHeader(
        delegate: const _PersistentHeaderPreviewDelegate(),
        pinned: _boolean('pinned') ?? false,
        floating: _boolean('floating') ?? false,
      ),
      'flutter.widgets.ValueListenableBuilder.sliver' => ValueListenableBuilder<Object?>(
        key: ValueKey('canvas-value-listenable-builder-${node.id}'),
        valueListenable: _valueListenablePreviewSource(node),
        builder: (context, value, child) => child ?? const SliverToBoxAdapter(),
        child: _single('child'),
      ),
      'flutter.widgets.TweenAnimationBuilder.sliver' => TweenAnimationBuilder<double>(
        key: ValueKey('canvas-tween-animation-builder-${node.id}'),
        tween: Tween<double>(begin: 0.0, end: 1.0),
        duration: Duration(microseconds: _integer('durationUs') ?? 300000),
        curve: _expansionCurves[_string('curve')] ?? Curves.linear,
        builder: (context, value, child) => child ?? const SliverToBoxAdapter(),
        child: _single('child'),
      ),
      'flutter.widgets.AnimatedBuilder.sliver' => AnimatedBuilder(
        key: ValueKey('canvas-animated-builder-${node.id}'),
        animation: const AlwaysStoppedAnimation<double>(0.0),
        builder: (context, child) => child ?? const SliverToBoxAdapter(),
        child: _single('child'),
      ),
      'flutter.widgets.ListenableBuilder.sliver' => ListenableBuilder(
        key: ValueKey('canvas-listenable-builder-${node.id}'),
        listenable: const AlwaysStoppedAnimation<double>(0.0),
        builder: (context, child) => child ?? const SliverToBoxAdapter(),
        child: _single('child'),
      ),
      'flutter.widgets.DeviceOrientationBuilder.sliver' => DeviceOrientationBuilder(
        key: ValueKey('canvas-device-orientation-builder-${node.id}'),
        builder: (context, orientation) => const SliverToBoxAdapter(),
      ),
      'flutter.widgets.SliverLayoutBuilder' => SliverLayoutBuilder(
        builder: (context, constraints) => const SliverToBoxAdapter(),
      ),
      'flutter.widgets.SliverFadeTransition' => SliverFadeTransition(
        opacity: AlwaysStoppedAnimation<double>(_number('opacity') ?? 1),
        alwaysIncludeSemantics: _boolean('alwaysIncludeSemantics') ?? false,
        sliver: _single('sliver') ?? const SliverToBoxAdapter(),
      ),
      'flutter.widgets.SliverAnimatedOpacity' => SliverAnimatedOpacity(
        opacity: _number('opacity')!,
        curve: _expansionCurves[_string('curve')] ?? Curves.linear,
        duration: Duration(microseconds: _integer('durationUs') ?? 300000),
        // Project callbacks are preserved in source but never executed in Canvas.
        onEnd: null,
        alwaysIncludeSemantics: _boolean('alwaysIncludeSemantics') ?? false,
        sliver: _single('sliver') ?? const SliverToBoxAdapter(),
      ),
      'flutter.widgets.SliverOpacity' => SliverOpacity(
        opacity: _number('opacity')!,
        alwaysIncludeSemantics: _boolean('alwaysIncludeSemantics') ?? false,
        // Match generated Dart: RenderProxySliver requires a non-null child at layout.
        sliver: _single('sliver') ?? const SliverToBoxAdapter(),
      ),
      'flutter.widgets.SliverPadding' => _sliverPadding(),
      'flutter.widgets.SliverFillViewport' || 'flutter.widgets.SliverFillViewport.delegate' => _sliverFillViewport(),
      'flutter.widgets.SliverFillRemaining' => SliverFillRemaining(
        hasScrollBody: _boolean('hasScrollBody') ?? true,
        fillOverscroll: _boolean('fillOverscroll') ?? false,
        child: _single('child'),
      ),
      'flutter.widgets.SliverList.builder' ||
      'flutter.widgets.SliverList.separated' ||
      'flutter.widgets.SliverList.delegate' ||
      'flutter.widgets.SliverGrid.builder' ||
      'flutter.widgets.SliverGrid.list' ||
      'flutter.widgets.SliverGrid.delegate' ||
      'flutter.widgets.SliverVariedExtentList' ||
      'flutter.widgets.SliverVariedExtentList.builder' ||
      'flutter.widgets.SliverVariedExtentList.delegate' ||
      'flutter.widgets.SliverFixedExtentList' ||
      'flutter.widgets.SliverFixedExtentList.builder' ||
      'flutter.widgets.SliverFixedExtentList.delegate' ||
      'flutter.widgets.SliverPrototypeExtentList' ||
      'flutter.widgets.SliverPrototypeExtentList.builder' ||
      'flutter.widgets.SliverPrototypeExtentList.delegate' => _dynamicSliver(),
      'flutter.widgets.SliverList' => SliverList.list(
        addAutomaticKeepAlives: _boolean('addAutomaticKeepAlives') ?? true,
        addRepaintBoundaries: _boolean('addRepaintBoundaries') ?? true,
        addSemanticIndexes: _boolean('addSemanticIndexes') ?? true,
        children: _children('children'),
      ),
      'flutter.widgets.SliverGrid' => SliverGrid.count(
        crossAxisCount: _integer('crossAxisCount') ?? 2,
        mainAxisSpacing: _number('mainAxisSpacing') ?? 0,
        crossAxisSpacing: _number('crossAxisSpacing') ?? 0,
        childAspectRatio: _number('childAspectRatio') ?? 1,
        children: _children('children'),
      ),
      'flutter.widgets.SliverGrid.extent' => SliverGrid.extent(
        maxCrossAxisExtent: _number('maxCrossAxisExtent') ?? 200,
        mainAxisSpacing: _number('mainAxisSpacing') ?? 0,
        crossAxisSpacing: _number('crossAxisSpacing') ?? 0,
        childAspectRatio: _number('childAspectRatio') ?? 1,
        children: _children('children'),
      ),
      'flutter.widgets.Stack' => _stack(),
      'flutter.widgets.IndexedStack' => _indexedStack(),
      'flutter.widgets.AnimatedPositioned' || 'flutter.widgets.AnimatedPositioned.fromRect' ||
      'flutter.widgets.AnimatedPositionedDirectional' || 'flutter.widgets.PositionedTransition'
      || 'flutter.widgets.RelativePositionedTransition' => _single('child')!,
      'flutter.widgets.Expanded' => _single('child')!,
      'flutter.widgets.Flexible' => _single('child')!,
      'flutter.widgets.SafeArea' => _safeArea(),
      'flutter.widgets.Directionality' => _directionality(),
      'flutter.widgets.ExcludeFocus' => ExcludeFocus(
        excluding: _boolean('excluding') ?? true,
        child: _single('child')!,
      ),
      'flutter.widgets.ExcludeFocusTraversal' => ExcludeFocusTraversal(
        excluding: _boolean('excluding') ?? true,
        child: _single('child')!,
      ),
      'flutter.widgets.TickerMode' => TickerMode(
        enabled: _boolean('enabled')!,
        forceFrames: _boolean('forceFrames') ?? false,
        child: _single('child')!,
      ),
      'flutter.widgets.DefaultTextHeightBehavior' => DefaultTextHeightBehavior(
        textHeightBehavior: _textHeightBehavior() ?? const TextHeightBehavior(),
        child: _single('child')!,
      ),
      'flutter.widgets.DefaultSelectionStyle' => _defaultSelectionStyle(
        context,
      ),
      'flutter.widgets.IconTheme' => _iconThemeWidget(context),
      'flutter.widgets.Visibility' => _CanvasVisibility(
        visibility: Visibility(
          visible: _boolean('visible') ?? true,
          maintainState: _boolean('maintainState') ?? false,
          maintainAnimation: _boolean('maintainAnimation') ?? false,
          maintainSize: _boolean('maintainSize') ?? false,
          maintainSemantics: _boolean('maintainSemantics') ?? false,
          maintainInteractivity: _boolean('maintainInteractivity') ?? false,
          maintainFocusability: _boolean('maintainFocusability') ?? false,
          replacement: _single('replacement') ?? const SizedBox.shrink(),
          child: _single('child')!,
        ),
      ),
      'flutter.widgets.Spacer' => Spacer(flex: _integer('flex') ?? 1),
      'flutter.widgets.Padding' => _padding(paddingGeometry!),
      'flutter.widgets.Align' => _align(),
      'flutter.widgets.AspectRatio' => _aspectRatio(),
      'flutter.widgets.Baseline' => _baseline(),
      'flutter.widgets.IntrinsicHeight' => _intrinsicHeight(),
      'flutter.widgets.IntrinsicWidth' => _intrinsicWidth(),
      'flutter.widgets.Offstage' => _offstage(),
      'flutter.widgets.RotatedBox' => _rotatedBox(),
      'flutter.widgets.PreferredSize' => _preferredSize(),
      'flutter.widgets.Builder' => _builder(),
      'flutter.widgets.LayoutBuilder' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: LayoutBuilder(
          key: ValueKey('canvas-layout-builder-${node.id}'),
          builder: (context, constraints) => const SizedBox.shrink(),
        ),
      ),
      'flutter.widgets.OrientationBuilder' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: OrientationBuilder(
          key: ValueKey('canvas-orientation-builder-${node.id}'),
          builder: (context, orientation) => const SizedBox.shrink(),
        ),
      ),
      'flutter.widgets.ValueListenableBuilder' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: ValueListenableBuilder<Object?>(
          key: ValueKey('canvas-value-listenable-builder-${node.id}'),
          valueListenable: _valueListenablePreviewSource(node),
          builder: (context, value, child) => child ?? const SizedBox.shrink(),
          child: _single('child'),
        ),
      ),
      'flutter.widgets.TweenAnimationBuilder' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: TweenAnimationBuilder<double>(
          key: ValueKey('canvas-tween-animation-builder-${node.id}'),
          tween: Tween<double>(begin: 0.0, end: 1.0),
          duration: Duration(microseconds: _integer('durationUs') ?? 300000),
          curve: _expansionCurves[_string('curve')] ?? Curves.linear,
          builder: (context, value, child) => child ?? const SizedBox.shrink(),
          child: _single('child'),
        ),
      ),
      'flutter.widgets.AnimatedBuilder' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: AnimatedBuilder(
          key: ValueKey('canvas-animated-builder-${node.id}'),
          animation: const AlwaysStoppedAnimation<double>(0.0),
          builder: (context, child) => child ?? const SizedBox.shrink(),
          child: _single('child'),
        ),
      ),
      'flutter.widgets.ListenableBuilder' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: ListenableBuilder(
          key: ValueKey('canvas-listenable-builder-${node.id}'),
          listenable: const AlwaysStoppedAnimation<double>(0.0),
          builder: (context, child) => child ?? const SizedBox.shrink(),
          child: _single('child'),
        ),
      ),
      'flutter.widgets.DeviceOrientationBuilder' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: DeviceOrientationBuilder(
          key: ValueKey('canvas-device-orientation-builder-${node.id}'),
          builder: (context, orientation) => const SizedBox.shrink(),
        ),
      ),
      'flutter.widgets.SizedOverflowBox' => _sizedOverflowBox(),
      'flutter.widgets.Transform' =>
        _single('child') ?? const SizedBox.shrink(),
      'flutter.widgets.Center' => _center(),
      'flutter.widgets.ConstrainedBox' => _constrainedBox(),
      'flutter.widgets.UnconstrainedBox' => _unconstrainedBox(),
      'flutter.widgets.LimitedBox' => _limitedBox(),
      'flutter.widgets.OverflowBox' => _overflowBox(),
      'flutter.widgets.Placeholder' => _placeholder(context),
      'flutter.widgets.ClipOval' => _clipOval(),
      'flutter.widgets.ClipRRect' => _clipRRect(context),
      'flutter.widgets.ClipRSuperellipse' => _clipRSuperellipse(context),
      'flutter.widgets.ClipPath' => _clipPath(),
      'flutter.widgets.ClipRect' => _clipRect(),
      'flutter.widgets.ColoredBox' => _coloredBox(context),
      'flutter.widgets.AnimatedPhysicalModel' => _animatedPhysicalModel(context),
      'flutter.widgets.PhysicalModel' => _physicalModel(context),
      'flutter.widgets.PhysicalShape' => _physicalShape(context),
      'flutter.widgets.Container' => _container(context),
      'flutter.widgets.DecoratedBox' => _decoratedBox(context),
      'flutter.widgets.AlignTransition' => _AlignTransitionPreview(
        node: node,
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: AlignTransition(
          alignment: AlwaysStoppedAnimation<AlignmentGeometry>(_alignmentGeometry('alignment') ?? Alignment.center),
          widthFactor: _number('widthFactor'), heightFactor: _number('heightFactor'), child: _single('child')!,
        ),
      ),
      'flutter.widgets.DecoratedBoxTransition' => _decoratedBoxTransition(context),
      'flutter.widgets.ExcludeSemantics' => _excludeSemantics(),
      'flutter.widgets.IgnorePointer' => _ignorePointer(),
      'flutter.widgets.GestureDetector' => _gestureDetector(),
      'flutter.widgets.Listener' => _listener(),
      'flutter.widgets.MouseRegion' => _mouseRegion(),
      'flutter.widgets.NotificationListener' =>
        _CanvasNotificationListenerPreview(
          node: node,
          child: _single('child')!,
        ),
      'flutter.widgets.Focus' => _CanvasFocusPreview(
        node: node,
        child: _single('child')!,
      ),
      'flutter.widgets.AbsorbPointer' => _absorbPointer(),
      'flutter.widgets.BlockSemantics' => _blockSemantics(),
      'flutter.widgets.MergeSemantics' => MergeSemantics(
        child: _single('child'),
      ),
      'flutter.widgets.IndexedSemantics' => IndexedSemantics(
        index: _integer('index')!,
        child: _single('child'),
      ),
      'flutter.widgets.RepaintBoundary' => RepaintBoundary(
        child: _single('child'),
      ),
      'flutter.widgets.FittedBox' => _fittedBox(),
      'flutter.widgets.FractionallySizedBox' => _fractionallySizedBox(),
      'flutter.widgets.AnimatedDefaultTextStyle' => _animatedDefaultTextStyle(context),
      'flutter.widgets.DefaultTextStyle' => _defaultTextStyle(context, false),
      'flutter.widgets.DefaultTextStyle.merge' => _defaultTextStyle(context, true),
      'flutter.widgets.DefaultTextStyleTransition' => _defaultTextStyleTransition(context),
      'flutter.material.Theme' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: Theme(data: _materialThemeData(), child: _single('child')!),
      ),
      'flutter.material.AnimatedTheme' => _TextButtonPreview(
        message:_customClipperPreviewUnavailableMessageForNode(node)??'',
        child:AnimatedTheme(
          data:_materialThemeData(),
          curve:_expansionCurves[_string('curve')]??Curves.linear,
          duration:Duration(microseconds:_integer('durationUs')??200000),
          onEnd:null,
          child:_single('child')!,
        ),
      ),
      'flutter.widgets.AnimatedSwitcher' => _animatedSwitcher(context),
      'flutter.widgets.AnimatedCrossFade' => _animatedCrossFade(context),
      'flutter.widgets.AnimatedSize' => _animatedSize(context),
      'flutter.widgets.AnimatedContainer' => _animatedContainer(context),
      'flutter.widgets.AnimatedRotation' => _AnimatedMatrixPreview(
        node: node,
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: AnimatedRotation(
          turns: node.properties['turns']?.kind == 'dartObjectReferencePresence' ? 0 : _number('turns') ?? 0,
          alignment: _alignmentGeometry('alignment') as Alignment? ?? Alignment.center,
          filterQuality: _enum('filterQuality') == null ? null : _filterQuality(_enum('filterQuality')!),
          curve: _expansionCurves[_string('curve')] ?? Curves.linear,
          duration: Duration(microseconds: _integer('durationUs') ?? 300000),
          // Project-owned callbacks never run in Canvas.
          onEnd: null,
          child: _single('child'),
        ),
      ),
      'flutter.widgets.AnimatedScale' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: AnimatedScale(
          scale: node.properties['scale']?.kind == 'dartObjectReferencePresence' ? 1 : _number('scale') ?? 1,
          alignment: _alignmentGeometry('alignment') as Alignment? ?? Alignment.center,
          filterQuality: _enum('filterQuality') == null ? null : _filterQuality(_enum('filterQuality')!),
          curve: _expansionCurves[_string('curve')] ?? Curves.linear,
          duration: Duration(microseconds: _integer('durationUs') ?? 300000),
          // Project-owned callbacks never run in Canvas.
          onEnd: null,
          child: _single('child'),
        ),
      ),
      'flutter.widgets.AnimatedSlide' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: AnimatedSlide(
          offset: _offset('offset') ?? Offset.zero,
          curve: _expansionCurves[_string('curve')] ?? Curves.linear,
          duration: Duration(microseconds: _integer('durationUs') ?? 300000),
          // Project-owned callbacks never run in Canvas.
          onEnd: null,
          child: _single('child'),
        ),
      ),
      'flutter.widgets.AnimatedPadding' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: AnimatedPadding(
          padding: _edgeInsetsGeometry('padding') ?? const EdgeInsets.all(16),
          curve: _expansionCurves[_string('curve')] ?? Curves.linear,
          duration: Duration(microseconds: _integer('durationUs') ?? 300000),
          // Project-owned callbacks never run in Canvas.
          onEnd: null,
          child: _single('child'),
        ),
      ),
      'flutter.widgets.AnimatedFractionallySizedBox' => _AnimatedFractionalPreview(
        target: AnimatedFractionallySizedBox(
          alignment: _alignmentGeometry('alignment') ?? Alignment.center,
          widthFactor: _number('widthFactor'), heightFactor: _number('heightFactor'),
          curve: _expansionCurves[_string('curve')] ?? Curves.linear,
          duration: Duration(microseconds: _integer('durationUs') ?? 300000),
          onEnd: null, child: _single('child'),
        ),
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
      ),
      'flutter.widgets.AnimatedAlign' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: AnimatedAlign(
          alignment: _alignmentGeometry('alignment') ?? Alignment.center,
          widthFactor: _number('widthFactor'),
          heightFactor: _number('heightFactor'),
          curve: _expansionCurves[_string('curve')] ?? Curves.linear,
          duration: Duration(microseconds: _integer('durationUs') ?? 300000),
          // Project-owned callbacks never run in Canvas.
          onEnd: null,
          child: _single('child'),
        ),
      ),
      'flutter.widgets.AnimatedModalBarrier' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: LayoutBuilder(builder: (context, constraints) {
        if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
          return Tooltip(message: 'AnimatedModalBarrier ${node.id}: preview unavailable because '
              '${!constraints.hasBoundedWidth ? 'width' : ''}${!constraints.hasBoundedWidth && !constraints.hasBoundedHeight ? ' and ' : ''}${!constraints.hasBoundedHeight ? 'height' : ''} is unbounded. '
              'Use a bounded parent such as SizedBox or Stack. No dimensions are written to the model.',
            child: const SizedBox.shrink());
        }
        return AbsorbPointer(child: AnimatedModalBarrier(
          key: ValueKey('canvas-animated-modal-barrier-${node.id}'),
          color: AlwaysStoppedAnimation<Color?>(_resolvedColor(context, 'color')),
          dismissible: _boolean('dismissible') ?? true,
          // No application callbacks, Navigator.maybePop or SDK alert sounds in Canvas.
          onDismiss: () {},
          semanticsLabel: _string('semanticsLabel'),
          barrierSemanticsDismissible: node.properties['barrierSemanticsDismissible']?.kind == 'null'
              ? null : _boolean('barrierSemanticsDismissible'),
          clipDetailsNotifier: null,
          semanticsOnTapHint: _string('semanticsOnTapHint'),
        ));
      })),
      'flutter.widgets.ModalBarrier' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: LayoutBuilder(builder: (context, constraints) {
        if (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight) {
          return Tooltip(message: 'ModalBarrier ${node.id}: preview unavailable because '
              '${!constraints.hasBoundedWidth ? 'width' : ''}${!constraints.hasBoundedWidth && !constraints.hasBoundedHeight ? ' and ' : ''}${!constraints.hasBoundedHeight ? 'height' : ''} is unbounded. '
              'Use a bounded parent such as SizedBox or Stack. No dimensions are written to the model.',
            child: const SizedBox.shrink());
        }
        return AbsorbPointer(child: ModalBarrier(
          key: ValueKey('canvas-modal-barrier-${node.id}'),
          color: _resolvedColor(context, 'color'),
          dismissible: _boolean('dismissible') ?? true,
          // No application callbacks, Navigator.maybePop or SDK alert sounds in Canvas.
          onDismiss: () {},
          semanticsLabel: _string('semanticsLabel'),
          barrierSemanticsDismissible: node.properties['barrierSemanticsDismissible']?.kind == 'null'
              ? null : _boolean('barrierSemanticsDismissible') ?? true,
          clipDetailsNotifier: null,
          semanticsOnTapHint: _string('semanticsOnTapHint'),
        ));
      })),
      'flutter.widgets.MatrixTransition' => _AnimatedMatrixPreview(
        node: node,
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: MatrixTransition(
          animation: AlwaysStoppedAnimation<double>(_number('animation') ?? 0),
          // Never execute a project callback; each local invocation gets a fresh matrix.
          onTransform: (_) => _matrix4('onTransform') ?? Matrix4.identity(),
          alignment: _alignmentGeometry('alignment') as Alignment? ?? Alignment.center,
          filterQuality: _enum('filterQuality') == null ? null : _filterQuality(_enum('filterQuality')!),
          child: _single('child'),
        ),
      ),
      'flutter.widgets.RotationTransition' => _AnimatedMatrixPreview(
        node: node,
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: RotationTransition(
          turns: AlwaysStoppedAnimation<double>(_number('turns') ?? 0),
          alignment: _alignmentGeometry('alignment') as Alignment? ?? Alignment.center,
          filterQuality: _enum('filterQuality') == null ? null : _filterQuality(_enum('filterQuality')!),
          child: _single('child'),
        ),
      ),
      'flutter.widgets.SizeTransition' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: SizeTransition(
          axis: _enum('axis') == 'horizontal' ? Axis.horizontal : Axis.vertical,
          sizeFactor: AlwaysStoppedAnimation<double>(_number('sizeFactor') ?? 1),
          // Deliberately retain the pinned SDK's deprecated constructor branch.
          // ignore: deprecated_member_use
          axisAlignment: _number('axisAlignment'),
          alignment: _alignmentGeometry('alignment'),
          fixedCrossAxisSizeFactor: _number('fixedCrossAxisSizeFactor'),
          child: _single('child'),
        ),
      ),
      'flutter.widgets.ScaleTransition' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: ScaleTransition(
          scale: AlwaysStoppedAnimation<double>(_number('scale') ?? 1),
          alignment: _alignmentGeometry('alignment') as Alignment? ?? Alignment.center,
          filterQuality: _enum('filterQuality') == null ? null : _filterQuality(_enum('filterQuality')!),
          child: _single('child'),
        ),
      ),
      'flutter.widgets.SlideTransition' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: SlideTransition(
          position: AlwaysStoppedAnimation<Offset>(_offset('position') ?? Offset.zero),
          transformHitTests: _boolean('transformHitTests') ?? true,
          textDirection: switch (_enum('textDirection')) {
            'ltr' => TextDirection.ltr,
            'rtl' => TextDirection.rtl,
            _ => null,
          },
          child: _single('child'),
        ),
      ),
      'flutter.widgets.FadeTransition' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: FadeTransition(
          opacity: AlwaysStoppedAnimation<double>(_number('opacity') ?? 1),
          alwaysIncludeSemantics: _boolean('alwaysIncludeSemantics') ?? false,
          child: _single('child'),
        ),
      ),
      'flutter.widgets.AnimatedOpacity' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: AnimatedOpacity(
          opacity: _number('opacity')!,
          curve: _expansionCurves[_string('curve')] ?? Curves.linear,
          duration: Duration(microseconds: _integer('durationUs') ?? 300000),
          // Project handlers are preserved in generated source, never invoked here.
          onEnd: null,
          alwaysIncludeSemantics: _boolean('alwaysIncludeSemantics') ?? false,
          child: _single('child'),
        ),
      ),
      'flutter.widgets.Opacity' => _opacity(),
      'flutter.widgets.SizedBox' => _sizedBox(),
      'flutter.material.AnimatedIcon' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: AnimatedIcon(
          key:ValueKey('canvas-animated-icon-${node.id}'),
          icon:switch(_string('icon')){'add_event'=>AnimatedIcons.add_event,'arrow_menu'=>AnimatedIcons.arrow_menu,'close_menu'=>AnimatedIcons.close_menu,'ellipsis_search'=>AnimatedIcons.ellipsis_search,'event_add'=>AnimatedIcons.event_add,'home_menu'=>AnimatedIcons.home_menu,'list_view'=>AnimatedIcons.list_view,'menu_arrow'=>AnimatedIcons.menu_arrow,'menu_close'=>AnimatedIcons.menu_close,'menu_home'=>AnimatedIcons.menu_home,'pause_play'=>AnimatedIcons.pause_play,'play_pause'=>AnimatedIcons.play_pause,'search_ellipsis'=>AnimatedIcons.search_ellipsis,'view_list'=>AnimatedIcons.view_list,_=>AnimatedIcons.menu_close},
          progress:AlwaysStoppedAnimation<double>(_number('progress')??0),
          color:_resolvedColor(context,'color'),size:_number('size'),
          semanticLabel:_string('semanticLabel'),textDirection:_enum('textDirection')=='rtl'?TextDirection.rtl:_enum('textDirection')=='ltr'?TextDirection.ltr:null,
        )),
      'flutter.widgets.Icon' => _icon(context),
      'flutter.widgets.Image' => _image(context),
      'flutter.material.DatePickerDialog' => _TextButtonPreview(message:_datePickerPreviewMessage(node),child:_datePickerDialog(context)),
      'flutter.material.CalendarDatePicker' => _TextButtonPreview(message:_calendarDatePickerPreviewMessage(node),child:_calendarDatePicker(context)),
      'flutter.material.InputDatePickerFormField' => _TextButtonPreview(message:_inputDatePickerFormFieldPreviewMessage(node),child:_inputDatePickerFormField(context)),
      'flutter.material.TimePickerDialog' => _TextButtonPreview(message:_timePickerDialogPreviewMessage(node),child:_timePickerDialog(context)),
      'flutter.material.DateRangePickerDialog' => _TextButtonPreview(message:_dateRangePickerPreviewMessage(node),child:_dateRangePickerDialog(context)),
      'flutter.material.DataTable' || 'flutter.material.PaginatedDataTable' => _TextButtonPreview(message:_dataTablePreviewMessage(node),child:_dataTable(context)),
      'flutter.material.DataColumn' || 'flutter.material.DataRow' || 'flutter.material.DataRow.byIndex'
          || 'flutter.material.DataCell' || 'flutter.material.DataCell.empty' =>
          throw StateError('Data table descriptor must be rendered by its native DataTable'),
      'flutter.widgets.Table' => _TextButtonPreview(message:_customClipperPreviewUnavailableMessageForNode(node)??'',child:_table(context)),
      'flutter.widgets.TableCell' => _single('child')!,
      'flutter.widgets.TableRow' => throw StateError('TableRow is a descriptor; render it through Table'),
      'flutter.widgets.Flow' || 'flutter.widgets.Flow.unwrapped' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: _flow()),
      'flutter.widgets.CustomMultiChildLayout' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: CustomMultiChildLayout(
          delegate: _MultiChildLayoutPreviewDelegate([for(final c in node.slot('children')?.children ?? <CanvasNode>[]) c.id]),
          // Key the outer component, above LayoutId and instrumentation, so
          // reordering preserves parent data and native child state together.
          children: [for(final c in node.slot('children')?.children ?? <CanvasNode>[])
            KeyedSubtree(key: ValueKey('canvas-layout-id-${c.id}'), child: _view(c))])),
      'flutter.widgets.LayoutId' => _single('child')!,
      'flutter.widgets.CustomSingleChildLayout' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: CustomSingleChildLayout(
          delegate: const _SingleChildLayoutPreviewDelegate(), child: _single('child'))),
      'flutter.widgets.CustomPaint' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: _customPaint()),
      'flutter.widgets.ShaderMask' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: _shaderMask(context)),
      'flutter.widgets.BackdropGroup' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: BackdropGroup(child: _single('child')!)),
      'flutter.widgets.BackdropFilter' || 'flutter.widgets.BackdropFilter.grouped' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: _backdropFilter()),
      'flutter.widgets.ImageFiltered' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: ImageFiltered(imageFilter: _localImageFilter(), enabled: _boolean('enabled') ?? true, child: _single('child'))),
      'flutter.widgets.ColorFiltered' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: ColorFiltered(colorFilter:_localColorFilter(context),child:_single('child'))),
      'flutter.widgets.RawImage' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '', child: _rawImage(context)),
      'flutter.widgets.FadeInImage' => _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '', child: _fadeInImage(context)),
      'flutter.widgets.ImageIcon' => _imageIcon(context),
      'flutter.material.TextField' => _textField(context),
      'flutter.widgets.Text' =>
        editing ? _inlineTextEditor(context) : _text(context),
      _ => const SizedBox.shrink(),
    };
    final tooltipAnchor =
        _TooltipAnchorScope.active(context) ||
        _MenuItemAnchorScope.active(context) ||
        node.type == 'flutter.material.MenuItemButton' ||
        _isSubmenuButton;
    final documentState = context
        .findAncestorStateOfType<_CanvasDocumentViewState>();
    final child = tooltipAnchor
        ? KeyedSubtree(
            key: documentState?._tooltipAnchorPreviewKeys.putIfAbsent(
              node.id,
              GlobalKey.new,
            ),
            child: sdkChild,
          )
        : sdkChild;
    if (node.type == canvasSpacerWidgetType) {
      // Every wrapper here is a component widget. A RenderObjectWidget between
      // Spacer's internal Expanded and the enclosing Flex would invalidate its
      // ParentData path; selection and outlines live in the surface overlay.
      return KeyedSubtree(
        key: ValueKey('canvas-widget-${node.id}'),
        child: KeyedSubtree(key: nodeKey(node.id), child: child),
      );
    }
    if (isCanvasSliverWidgetType(node.type)) {
      // Slivers participate in a RenderSliver viewport and cannot be wrapped
      // in the box-oriented Semantics/GestureDetector/CustomPaint stack used
      // by ordinary nodes. Their child remains fully instrumented; selection
      // and drop overlays are resolved from the owning CustomScrollView.
      return KeyedSubtree(
        key: ValueKey('canvas-widget-${node.id}'),
        child: KeyedSubtree(key: nodeKey(node.id), child: child),
      );
    }
    final selected = selectedWidgetId == node.id;
    final dark = Theme.of(context).brightness == Brightness.dark;
    final resolvedPadding = paddingGeometry?.resolve(
      Directionality.of(context),
    );
    final resolvedMargin = marginGeometry?.resolve(Directionality.of(context));
    final guidedChild = node.type == 'flutter.widgets.Container'
        ? resolvedPadding == null && resolvedMargin == null
              ? child
              : CustomPaint(
                  key: ValueKey('canvas-container-insets-guides-${node.id}'),
                  foregroundPainter: _CanvasContainerInsetsGuidesPainter(
                    padding: resolvedPadding ?? EdgeInsets.zero,
                    margin: resolvedMargin ?? EdgeInsets.zero,
                    visualScale: overlayScale,
                    paddingColor: dark
                        ? const Color(0xffffb74d)
                        : const Color(0xffd97706),
                    marginColor: dark
                        ? const Color(0xff80cbc4)
                        : const Color(0xff00897b),
                  ),
                  child: child,
                )
        : resolvedPadding == null
        ? child
        : CustomPaint(
            key: ValueKey('canvas-padding-guides-${node.id}'),
            foregroundPainter: _CanvasPaddingGuidesPainter(
              insets: resolvedPadding,
              visualScale: overlayScale,
              color: dark ? const Color(0xffffb74d) : const Color(0xffd97706),
            ),
            child: child,
          );
    final outlinedChild = CustomPaint(
      key: ValueKey('canvas-widget-outline-${node.id}'),
      foregroundPainter: _CanvasWidgetOutlinePainter(
        selected: selected,
        inflateEmptyLinearContainer: _isEmptyLinearContainer(node),
        visualScale: overlayScale,
        unselectedColor: dark
            ? const Color(0x99b0b8c1)
            : const Color(0x9974808a),
      ),
      child: guidedChild,
    );
    final suppressNodeDesignerSemantics =
        suppressDesignerSemantics ||
        node.type == 'flutter.widgets.IndexedSemantics';
    final selectable = KeyedSubtree(
      key: ValueKey('canvas-widget-${node.id}'),
      child: GestureDetector(
        key: nodeKey(node.id),
        excludeFromSemantics: suppressNodeDesignerSemantics,
        behavior: HitTestBehavior.translucent,
        onTap:
            editing ||
                tooltipAnchor ||
                node.type == 'flutter.material.Tooltip' ||
                _ignoresPointersForNode(node)
            ? null
            : () => onSelected(node.id),
        onDoubleTap:
            !editing &&
                inlineTextEditEnabled &&
                selected &&
                node.type == 'flutter.widgets.Text'
            ? () => Future<void>.microtask(() => onBeginInlineTextEdit(node.id))
            : null,
        child:
            tooltipAnchor ||
                (documentState?._tooltipAncestorIds.contains(node.id) ?? false)
            ? Stack(
                fit: StackFit.passthrough,
                children: [
                  outlinedChild,
                  if (selected)
                    Positioned.fill(
                      child: IgnoreBaseline(
                        child: IgnorePointer(
                          child: SizedBox(
                            key: ValueKey(
                              'canvas-selection-outline-${node.id}',
                            ),
                          ),
                        ),
                      ),
                    ),
                ],
              )
            : selected
            ? KeyedSubtree(
                key: ValueKey('canvas-selection-outline-${node.id}'),
                child: outlinedChild,
              )
            : outlinedChild,
      ),
    );
    final tooltipSelectable =
        tooltipAnchor || node.type == 'flutter.material.Tooltip'
        ? Listener(
            onPointerDown: !editing && !_ignoresPointersForNode(node)
                ? (event) => documentState?._selectTooltipAnchor(event, node.id)
                : null,
            onPointerUp: documentState?._finishTooltipAnchorPointer,
            onPointerCancel: documentState?._finishTooltipAnchorPointer,
            child: selectable,
          )
        : selectable;
    final applicationMouseRegion =
        tooltipAnchor ||
        node.type == 'flutter.material.Tooltip' ||
        node.type == 'flutter.widgets.MouseRegion' ||
        _CanvasMouseRegionCursorScope.active(context);
    final instrumented = Semantics(
      // Synthetic Designer labels, selected states and tap actions must not
      // contaminate merged nodes or consume an IndexedSemantics annotation.
      // Keep actionable preview diagnostics and pointer/keyboard editing.
      label: suppressNodeDesignerSemantics
          ? _imageStatusSemantics()
          : '${_displayType(node.type)} ${node.id}${_imageStatusSemantics()}',
      selected: suppressNodeDesignerSemantics ? null : selected,
      child: applicationMouseRegion
          ? tooltipSelectable
          : MouseRegion(
              cursor: editing
                  ? SystemMouseCursors.text
                  : SystemMouseCursors.click,
              opaque: !_ignoresPointersForNode(node),
              hitTestBehavior: _ignoresPointersForNode(node)
                  ? HitTestBehavior.deferToChild
                  : null,
              child: tooltipSelectable,
            ),
    );
    return switch (node.type) {
      'flutter.widgets.AnimatedPositioned' || 'flutter.widgets.AnimatedPositioned.fromRect' ||
      'flutter.widgets.AnimatedPositionedDirectional' => _animatedPositioned(context, instrumented),
      'flutter.widgets.PositionedTransition' => _positionedTransition(instrumented),
      'flutter.widgets.RelativePositionedTransition' => _relativePositionedTransition(instrumented),
      'flutter.widgets.Transform' => _transform(instrumented),
      'flutter.widgets.TableCell' => TableCell(verticalAlignment:_enum('verticalAlignment')==null ? null
          : TableCellVerticalAlignment.values.byName(_enum('verticalAlignment')!),child:instrumented),
      'flutter.widgets.LayoutId' => LayoutId(id: node.id, child: instrumented),
      'flutter.widgets.Expanded' => Expanded(
        flex: _integer('flex') ?? 1,
        child: instrumented,
      ),
      'flutter.widgets.Flexible' => Flexible(
        flex: _integer('flex') ?? 1,
        fit: _enum('fit') == 'tight' ? FlexFit.tight : FlexFit.loose,
        child: instrumented,
      ),
      _ => instrumented,
    };
  }

  Widget _scaffold(BuildContext context) {
    return _TextButtonPreview(
      message: _scaffoldScrimPreviewMessage(node) ?? '',
      child: Scaffold(
        key: ValueKey('canvas-scaffold-${node.id}'),
        appBar: _preferredSizeSingle('appBar'),
        body: _single('body') ?? const SizedBox.expand(),
        floatingActionButton: _single('floatingActionButton'),
        floatingActionButtonLocation: _scaffoldFloatingActionButtonLocation(),
        floatingActionButtonAnimator: _scaffoldFloatingActionButtonAnimator(),
        persistentFooterAlignment:
            _scaffoldPersistentFooterAlignment() ??
            AlignmentDirectional.centerEnd,
        onDrawerChanged: _callbackPresent('onDrawerChanged') ? (_) {} : null,
        onEndDrawerChanged: _callbackPresent('onEndDrawerChanged')
            ? (_) {}
            : null,
        backgroundColor: _resolvedColor(context, 'backgroundColor'),
        resizeToAvoidBottomInset: _boolean('resizeToAvoidBottomInset'),
        primary: _boolean('primary') ?? true,
        drawerDragStartBehavior: _scaffoldDrawerDragStartBehavior(),
        extendBody: _boolean('extendBody') ?? false,
        drawerBarrierDismissible: _boolean('drawerBarrierDismissible') ?? true,
        extendBodyBehindAppBar: _boolean('extendBodyBehindAppBar') ?? false,
        drawerScrimColor: _resolvedColor(context, 'drawerScrimColor'),
        drawerEdgeDragWidth: _number('drawerEdgeDragWidth'),
        drawerEnableOpenDragGesture:
            _boolean('drawerEnableOpenDragGesture') ?? true,
        endDrawerEnableOpenDragGesture:
            _boolean('endDrawerEnableOpenDragGesture') ?? true,
        restorationId: _string('restorationId'),
      ),
    );
  }

  FloatingActionButtonLocation? _scaffoldFloatingActionButtonLocation() =>
      switch (_string('floatingActionButtonLocation')) {
        'startTop' => FloatingActionButtonLocation.startTop,
        'miniStartTop' => FloatingActionButtonLocation.miniStartTop,
        'centerTop' => FloatingActionButtonLocation.centerTop,
        'miniCenterTop' => FloatingActionButtonLocation.miniCenterTop,
        'endTop' => FloatingActionButtonLocation.endTop,
        'miniEndTop' => FloatingActionButtonLocation.miniEndTop,
        'startFloat' => FloatingActionButtonLocation.startFloat,
        'miniStartFloat' => FloatingActionButtonLocation.miniStartFloat,
        'centerFloat' => FloatingActionButtonLocation.centerFloat,
        'miniCenterFloat' => FloatingActionButtonLocation.miniCenterFloat,
        'endFloat' => FloatingActionButtonLocation.endFloat,
        'miniEndFloat' => FloatingActionButtonLocation.miniEndFloat,
        'startDocked' => FloatingActionButtonLocation.startDocked,
        'miniStartDocked' => FloatingActionButtonLocation.miniStartDocked,
        'centerDocked' => FloatingActionButtonLocation.centerDocked,
        'miniCenterDocked' => FloatingActionButtonLocation.miniCenterDocked,
        'endDocked' => FloatingActionButtonLocation.endDocked,
        'miniEndDocked' => FloatingActionButtonLocation.miniEndDocked,
        'endContained' => FloatingActionButtonLocation.endContained,
        _ => null,
      };

  FloatingActionButtonAnimator? _scaffoldFloatingActionButtonAnimator() =>
      switch (_string('floatingActionButtonAnimator')) {
        'scaling' => FloatingActionButtonAnimator.scaling,
        'noAnimation' => FloatingActionButtonAnimator.noAnimation,
        _ => null,
      };

  AlignmentDirectional? _scaffoldPersistentFooterAlignment() =>
      switch (_string('persistentFooterAlignment')) {
        'topStart' => AlignmentDirectional.topStart,
        'topCenter' => AlignmentDirectional.topCenter,
        'topEnd' => AlignmentDirectional.topEnd,
        'centerStart' => AlignmentDirectional.centerStart,
        'center' => AlignmentDirectional.center,
        'centerEnd' => AlignmentDirectional.centerEnd,
        'bottomStart' => AlignmentDirectional.bottomStart,
        'bottomCenter' => AlignmentDirectional.bottomCenter,
        'bottomEnd' => AlignmentDirectional.bottomEnd,
        _ => null,
      };

  DragStartBehavior _scaffoldDrawerDragStartBehavior() =>
      switch (_enum('drawerDragStartBehavior')) {
        'down' => DragStartBehavior.down,
        _ => DragStartBehavior.start,
      };

  Widget _column() => Column(
    mainAxisAlignment: _mainAxisAlignment(),
    mainAxisSize: _mainAxisSize(),
    crossAxisAlignment: _crossAxisAlignment(),
    textDirection: _textDirection(),
    verticalDirection: _verticalDirection(),
    textBaseline: _textBaseline(),
    spacing: _number('spacing') ?? 0.0,
    children: _children('children'),
  );

  Widget _sliverAppBar(BuildContext context) {
    final modelTitle = node.slot('title')?.child;
    // Medium/large mount the title twice; isolate the mirror's model keys.
    final title = modelTitle == null ? null
      : node.type != 'flutter.material.SliverAppBar' && node.slot('flexibleSpace')?.child == null
        ? _CanvasSliverAppBarTitle(builder: (keys) => _view(modelTitle, keyProvider: keys))
        : _view(modelTitle);
    return switch (node.type) {
      'flutter.material.SliverAppBar' => SliverAppBar(
      key: ValueKey('canvas-sliver-app-bar-${node.id}-${_number('stretchTriggerOffset')}-${_callbackPresent('onStretchTrigger')}'),
      leading: _single('leading'), title: title, actions: node.slots.containsKey('actions') ? _children('actions') : null,
      flexibleSpace: _single('flexibleSpace'), bottom: _preferredSizeSingle('bottom'),
      automaticallyImplyLeading: _boolean('automaticallyImplyLeading') ?? true,
      automaticallyImplyActions: _boolean('automaticallyImplyActions') ?? true,
      elevation: _number('elevation'), scrolledUnderElevation: _number('scrolledUnderElevation'),
      shadowColor: _resolvedColor(context, 'shadowColor'), surfaceTintColor: _resolvedColor(context, 'surfaceTintColor'),
      forceElevated: _boolean('forceElevated') ?? false,
      backgroundColor: _resolvedColor(context, 'backgroundColor'), foregroundColor: _resolvedColor(context, 'foregroundColor'),
      iconTheme: _iconTheme(context, 'iconTheme'), actionsIconTheme: _iconTheme(context, 'actionsIconTheme'),
      primary: _boolean('primary') ?? true, centerTitle: _boolean('centerTitle'),
      excludeHeaderSemantics: _boolean('excludeHeaderSemantics') ?? false,
      titleSpacing: _number('titleSpacing'), collapsedHeight: _number('collapsedHeight'), expandedHeight: _number('expandedHeight'),
      floating: _boolean('floating') ?? false, pinned: _boolean('pinned') ?? false,
      snap: _boolean('snap') ?? false, stretch: _boolean('stretch') ?? false,
      stretchTriggerOffset: _number('stretchTriggerOffset') ?? 100,
      onStretchTrigger: _callbackPresent('onStretchTrigger') ? () async {} : null,
      shape: _appBarShape(context), toolbarHeight: _number('toolbarHeight') ?? 56, leadingWidth: _number('leadingWidth'),
      toolbarTextStyle: _textStyle(context, 'toolbarTextStyle'), titleTextStyle: _textStyle(context, 'titleTextStyle'),
      systemOverlayStyle: _systemOverlayStyle(context), forceMaterialTransparency: _boolean('forceMaterialTransparency') ?? false,
      useDefaultSemanticsOrder: _boolean('useDefaultSemanticsOrder') ?? true, clipBehavior: _clipBehavior(),
      actionsPadding: _edgeInsetsGeometry('actionsPadding'),
      ),
      'flutter.material.SliverAppBar.medium' => SliverAppBar.medium(
      key: ValueKey('canvas-sliver-app-bar-${node.id}-${_number('stretchTriggerOffset')}-${_callbackPresent('onStretchTrigger')}'),
      leading: _single('leading'), title: title, actions: node.slots.containsKey('actions') ? _children('actions') : null,
      flexibleSpace: _single('flexibleSpace'), bottom: _preferredSizeSingle('bottom'),
      automaticallyImplyLeading: _boolean('automaticallyImplyLeading') ?? true,
      automaticallyImplyActions: _boolean('automaticallyImplyActions') ?? true,
      elevation: _number('elevation'), scrolledUnderElevation: _number('scrolledUnderElevation'),
      shadowColor: _resolvedColor(context, 'shadowColor'), surfaceTintColor: _resolvedColor(context, 'surfaceTintColor'),
      forceElevated: _boolean('forceElevated') ?? false,
      backgroundColor: _resolvedColor(context, 'backgroundColor'), foregroundColor: _resolvedColor(context, 'foregroundColor'),
      iconTheme: _iconTheme(context, 'iconTheme'), actionsIconTheme: _iconTheme(context, 'actionsIconTheme'),
      primary: _boolean('primary') ?? true, centerTitle: _boolean('centerTitle'),
      excludeHeaderSemantics: _boolean('excludeHeaderSemantics') ?? false,
      titleSpacing: _number('titleSpacing'), collapsedHeight: _number('collapsedHeight'), expandedHeight: _number('expandedHeight'),
      floating: _boolean('floating') ?? false, pinned: _boolean('pinned') ?? true,
      snap: _boolean('snap') ?? false, stretch: _boolean('stretch') ?? false,
      stretchTriggerOffset: _number('stretchTriggerOffset') ?? 100,
      onStretchTrigger: _callbackPresent('onStretchTrigger') ? () async {} : null,
      shape: _appBarShape(context), toolbarHeight: _number('toolbarHeight') ?? 64, leadingWidth: _number('leadingWidth'),
      toolbarTextStyle: _textStyle(context, 'toolbarTextStyle'), titleTextStyle: _textStyle(context, 'titleTextStyle'),
      systemOverlayStyle: _systemOverlayStyle(context), forceMaterialTransparency: _boolean('forceMaterialTransparency') ?? false,
      useDefaultSemanticsOrder: _boolean('useDefaultSemanticsOrder') ?? true, clipBehavior: _clipBehavior(),
      actionsPadding: _edgeInsetsGeometry('actionsPadding'),
      ),
      'flutter.material.SliverAppBar.large' => SliverAppBar.large(
      key: ValueKey('canvas-sliver-app-bar-${node.id}-${_number('stretchTriggerOffset')}-${_callbackPresent('onStretchTrigger')}'),
      leading: _single('leading'), title: title, actions: node.slots.containsKey('actions') ? _children('actions') : null,
      flexibleSpace: _single('flexibleSpace'), bottom: _preferredSizeSingle('bottom'),
      automaticallyImplyLeading: _boolean('automaticallyImplyLeading') ?? true,
      automaticallyImplyActions: _boolean('automaticallyImplyActions') ?? true,
      elevation: _number('elevation'), scrolledUnderElevation: _number('scrolledUnderElevation'),
      shadowColor: _resolvedColor(context, 'shadowColor'), surfaceTintColor: _resolvedColor(context, 'surfaceTintColor'),
      forceElevated: _boolean('forceElevated') ?? false,
      backgroundColor: _resolvedColor(context, 'backgroundColor'), foregroundColor: _resolvedColor(context, 'foregroundColor'),
      iconTheme: _iconTheme(context, 'iconTheme'), actionsIconTheme: _iconTheme(context, 'actionsIconTheme'),
      primary: _boolean('primary') ?? true, centerTitle: _boolean('centerTitle'),
      excludeHeaderSemantics: _boolean('excludeHeaderSemantics') ?? false,
      titleSpacing: _number('titleSpacing'), collapsedHeight: _number('collapsedHeight'), expandedHeight: _number('expandedHeight'),
      floating: _boolean('floating') ?? false, pinned: _boolean('pinned') ?? true,
      snap: _boolean('snap') ?? false, stretch: _boolean('stretch') ?? false,
      stretchTriggerOffset: _number('stretchTriggerOffset') ?? 100,
      onStretchTrigger: _callbackPresent('onStretchTrigger') ? () async {} : null,
      shape: _appBarShape(context), toolbarHeight: _number('toolbarHeight') ?? 64, leadingWidth: _number('leadingWidth'),
      toolbarTextStyle: _textStyle(context, 'toolbarTextStyle'), titleTextStyle: _textStyle(context, 'titleTextStyle'),
      systemOverlayStyle: _systemOverlayStyle(context), forceMaterialTransparency: _boolean('forceMaterialTransparency') ?? false,
      useDefaultSemanticsOrder: _boolean('useDefaultSemanticsOrder') ?? true, clipBehavior: _clipBehavior(),
      actionsPadding: _edgeInsetsGeometry('actionsPadding'),
      ),
      _ => throw StateError('Unreviewed SliverAppBar variant'),
    };
  }

  Widget _appBar(BuildContext context) => _TextButtonPreview(
    message: _appBarPredicatePreviewMessage(node) ?? '',
    child: AppBar(
      key: ValueKey('canvas-app-bar-${node.id}'),
      leading: _single('leading'),
      automaticallyImplyLeading: _boolean('automaticallyImplyLeading') ?? true,
      title: _single('title'),
      actions: node.slots.containsKey('actions') ? _children('actions') : null,
      automaticallyImplyActions: _boolean('automaticallyImplyActions') ?? true,
      flexibleSpace: _single('flexibleSpace'),
      bottom: _preferredSizeSingle('bottom'),
      elevation: _number('elevation'),
      scrolledUnderElevation: _number('scrolledUnderElevation'),
      notificationPredicate: _notificationPredicate(),
      shadowColor: _resolvedColor(context, 'shadowColor'),
      surfaceTintColor: _resolvedColor(context, 'surfaceTintColor'),
      shape: _appBarShape(context),
      backgroundColor: _resolvedColor(context, 'backgroundColor'),
      foregroundColor: _resolvedColor(context, 'foregroundColor'),
      iconTheme: _iconTheme(context, 'iconTheme'),
      actionsIconTheme: _iconTheme(context, 'actionsIconTheme'),
      primary: _boolean('primary') ?? true,
      centerTitle: _boolean('centerTitle'),
      excludeHeaderSemantics: _boolean('excludeHeaderSemantics') ?? false,
      titleSpacing: _number('titleSpacing'),
      toolbarOpacity: _number('toolbarOpacity') ?? 1.0,
      bottomOpacity: _number('bottomOpacity') ?? 1.0,
      toolbarHeight: _number('toolbarHeight'),
      leadingWidth: _number('leadingWidth'),
      toolbarTextStyle: _textStyle(context, 'toolbarTextStyle'),
      titleTextStyle: _textStyle(context, 'titleTextStyle'),
      systemOverlayStyle: _systemOverlayStyle(context),
      forceMaterialTransparency: _boolean('forceMaterialTransparency') ?? false,
      useDefaultSemanticsOrder: _boolean('useDefaultSemanticsOrder') ?? true,
      clipBehavior: _clipBehavior(),
      actionsPadding: _edgeInsetsGeometry('actionsPadding'),
      animateColor: _boolean('animateColor') ?? false,
    ),
  );

  Widget _navigationBar(BuildContext context) {
    final destinations = _children('destinations');
    final rawSelectedIndex = _integer('selectedIndex') ?? 0;
    final selectedIndex = destinations.isEmpty
        ? 0
        : rawSelectedIndex >= 0 && rawSelectedIndex < destinations.length
        ? rawSelectedIndex
        : 0;
    if (destinations.length < 2) {
      return _TextButtonPreview(
        message: _navigationBarPreviewMessage(node) ?? '',
        child: Container(
          key: ValueKey('canvas-navigation-bar-empty-${node.id}'),
          height: _number('height') ?? 80,
          alignment: Alignment.center,
          color:
              _resolvedColor(context, 'backgroundColor') ??
              Theme.of(context).colorScheme.surfaceContainer,
          child: const Text('Add at least two destinations'),
        ),
      );
    }
    final animationDuration =
        switch (node.properties['animationDurationUs']?.value) {
          final int value => Duration(microseconds: value),
          _ => null,
        };
    final labelBehavior = switch (_enumOrString('labelBehavior')) {
      'alwaysShow' => NavigationDestinationLabelBehavior.alwaysShow,
      'onlyShowSelected' => NavigationDestinationLabelBehavior.onlyShowSelected,
      'alwaysHide' => NavigationDestinationLabelBehavior.alwaysHide,
      _ => null,
    };
    return _TextButtonPreview(
      message: _navigationBarPreviewMessage(node) ?? '',
      child: NavigationBar(
        key: ValueKey('canvas-navigation-bar-${node.id}'),
        animationDuration: animationDuration ?? kThemeChangeDuration,
        selectedIndex: selectedIndex,
        destinations: destinations,
        onDestinationSelected: _callbackPresent('onDestinationSelected')
            ? (_) {}
            : null,
        backgroundColor: _resolvedColor(context, 'backgroundColor'),
        elevation: _number('elevation'),
        shadowColor: _resolvedColor(context, 'shadowColor'),
        surfaceTintColor: _resolvedColor(context, 'surfaceTintColor'),
        indicatorColor: _resolvedColor(context, 'indicatorColor'),
        // A ShapeBorder reference is intentionally not executed by Canvas.
        indicatorShape: null,
        height: _number('height'),
        labelBehavior: labelBehavior,
        // WidgetStateProperty references are application-owned and therefore
        // use the SDK/theme fallback in the isolated preview.
        overlayColor: null,
        labelTextStyle: null,
        labelPadding: _edgeInsetsGeometry('labelPadding'),
        maintainBottomViewPadding:
            _boolean('maintainBottomViewPadding') ?? false,
      ),
    );
  }

  Widget _navigationRail(BuildContext context) {
    final destinationWidgets = _children('destinations');
    final destinations = [
      for (final (index, child) in destinationWidgets.indexed)
        NavigationRailDestination(
          icon: child,
          label: Text('Destination ${index + 1}'),
        ),
    ];
    final rawSelectedIndex = _integer('selectedIndex');
    final selectedIndex = rawSelectedIndex == null
        ? null
        : rawSelectedIndex >= 0 && rawSelectedIndex < destinations.length
        ? rawSelectedIndex
        : null;
    final labelType = switch (_enumOrString('labelType')) {
      'none' => NavigationRailLabelType.none,
      'selected' => NavigationRailLabelType.selected,
      'all' => NavigationRailLabelType.all,
      _ => null,
    };
    final extended = _boolean('extended') ?? false;
    final safeExtended =
        extended &&
            labelType != null &&
            labelType != NavigationRailLabelType.none
        ? false
        : extended;
    final minWidth = _number('minWidth');
    final minExtendedWidth = _number('minExtendedWidth');
    final safeMinExtendedWidth =
        minWidth != null &&
            minExtendedWidth != null &&
            minExtendedWidth < minWidth
        ? null
        : minExtendedWidth;
    return _TextButtonPreview(
      message: _navigationRailPreviewMessage(node) ?? '',
      child: NavigationRail(
        key: ValueKey('canvas-navigation-rail-${node.id}'),
        backgroundColor: _resolvedColor(context, 'backgroundColor'),
        extended: safeExtended,
        leading: _single('leading'),
        trailing: _single('trailing'),
        destinations: destinations,
        selectedIndex: selectedIndex,
        onDestinationSelected: _callbackPresent('onDestinationSelected')
            ? (_) {}
            : null,
        elevation: _number('elevation'),
        groupAlignment: _number('groupAlignment'),
        labelType: labelType,
        // TextStyle and IconThemeData references are application-owned and
        // therefore use NavigationRailTheme/SDK fallbacks in isolated Canvas.
        unselectedLabelTextStyle: null,
        selectedLabelTextStyle: null,
        unselectedIconTheme: null,
        selectedIconTheme: null,
        minWidth: minWidth,
        minExtendedWidth: safeMinExtendedWidth,
        useIndicator: _boolean('useIndicator'),
        indicatorColor: _resolvedColor(context, 'indicatorColor'),
        indicatorShape: null,
        leadingAtTop: _boolean('leadingAtTop') ?? true,
        trailingAtBottom: _boolean('trailingAtBottom') ?? false,
        scrollable: _boolean('scrollable') ?? false,
        mainAxisAlignment: _mainAxisAlignment(),
      ),
    );
  }

  Widget _navigationDrawer(BuildContext context) {
    final childWidgets = _children('children');
    final children = [
      for (final (index, child) in childWidgets.indexed)
        NavigationDrawerDestination(
          // NavigationDrawer lays out its icon as a non-flex Row child. A
          // designer widget can otherwise report the full canvas width (the
          // selection/semantics wrappers are intentionally unconstrained),
          // overflowing the drawer before the synthetic label is rendered.
          // Keep the adapted icon in the SDK's compact icon slot while
          // preserving the original widget subtree and its identity.
          icon: SizedBox(width: 24.0, height: 24.0, child: child),
          label: Text('Destination ${index + 1}'),
        ),
    ];
    final rawSelectedIndex = _integer('selectedIndex');
    final selectedIndex = rawSelectedIndex == null
        ? null
        : rawSelectedIndex >= 0 && rawSelectedIndex < children.length
        ? rawSelectedIndex
        : null;
    return _TextButtonPreview(
      message: _navigationDrawerPreviewMessage(node) ?? '',
      child: NavigationDrawer(
        key: ValueKey('canvas-navigation-drawer-${node.id}'),
        header: _single('header'),
        footer: _single('footer'),
        backgroundColor: _resolvedColor(context, 'backgroundColor'),
        shadowColor: _resolvedColor(context, 'shadowColor'),
        surfaceTintColor: _resolvedColor(context, 'surfaceTintColor'),
        elevation: _number('elevation'),
        indicatorColor: _resolvedColor(context, 'indicatorColor'),
        // A ShapeBorder reference is intentionally not executed by Canvas.
        indicatorShape: null,
        onDestinationSelected: _callbackPresent('onDestinationSelected')
            ? (_) {}
            : null,
        selectedIndex: selectedIndex,
        tilePadding:
            _edgeInsetsGeometry('tilePadding') ??
            const EdgeInsets.symmetric(horizontal: 12.0),
        children: children,
      ),
    );
  }

  Widget _drawer(BuildContext context) {
    return _TextButtonPreview(
      message: _drawerPreviewMessage(node) ?? '',
      child: Drawer(
        key: ValueKey('canvas-drawer-${node.id}'),
        backgroundColor: _resolvedColor(context, 'backgroundColor'),
        elevation: _number('elevation'),
        shadowColor: _resolvedColor(context, 'shadowColor'),
        surfaceTintColor: _resolvedColor(context, 'surfaceTintColor'),
        // ShapeBorder references are application-owned and intentionally not
        // executed in the isolated Canvas preview.
        shape: null,
        width: _number('width'),
        semanticLabel: _string('semanticLabel'),
        clipBehavior: _clipBehavior(),
        child: _single('child'),
      ),
    );
  }

  Widget _bottomAppBar(BuildContext context) {
    return _TextButtonPreview(
      message: _bottomAppBarPreviewMessage(node) ?? '',
      child: BottomAppBar(
        key: ValueKey('canvas-bottom-app-bar-${node.id}'),
        color: _resolvedColor(context, 'color'),
        elevation: _number('elevation'),
        // NotchedShape references are application-owned and intentionally not
        // executed in the isolated Canvas preview.
        shape: null,
        clipBehavior: _clipBehavior() ?? Clip.none,
        notchMargin: _number('notchMargin') ?? 4.0,
        padding: _edgeInsetsGeometry('padding'),
        surfaceTintColor: _resolvedColor(context, 'surfaceTintColor'),
        shadowColor: _resolvedColor(context, 'shadowColor'),
        height: _number('height'),
        child: _single('child'),
      ),
    );
  }

  Widget _bottomNavigationBar(BuildContext context) {
    final itemWidgets = _children('items');
    final items = [
      for (final (index, child) in itemWidgets.indexed)
        BottomNavigationBarItem(icon: child, label: 'Item ${index + 1}'),
    ];
    if (items.length < 2) {
      return _TextButtonPreview(
        message: _bottomNavigationBarPreviewMessage(node) ?? '',
        child: Container(
          key: ValueKey('canvas-bottom-navigation-bar-empty-${node.id}'),
          height: 56,
          alignment: Alignment.center,
          color:
              _resolvedColor(context, 'backgroundColor') ??
              Theme.of(context).colorScheme.surface,
          child: const Text('Add at least two items'),
        ),
      );
    }
    final rawCurrentIndex = _integer('currentIndex') ?? 0;
    final currentIndex = rawCurrentIndex >= 0 && rawCurrentIndex < items.length
        ? rawCurrentIndex
        : 0;
    final type = switch (_enumOrString('barType')) {
      'fixed' => BottomNavigationBarType.fixed,
      'shifting' => BottomNavigationBarType.shifting,
      _ => null,
    };
    final landscapeLayout = switch (_enumOrString('landscapeLayout')) {
      'spread' => BottomNavigationBarLandscapeLayout.spread,
      'centered' => BottomNavigationBarLandscapeLayout.centered,
      'linear' => BottomNavigationBarLandscapeLayout.linear,
      _ => null,
    };
    return _TextButtonPreview(
      message: _bottomNavigationBarPreviewMessage(node) ?? '',
      child: BottomNavigationBar(
        key: ValueKey('canvas-bottom-navigation-bar-${node.id}'),
        items: items,
        onTap: _callbackPresent('onTap') ? (_) {} : null,
        currentIndex: currentIndex,
        elevation: _number('elevation'),
        type: type,
        backgroundColor: _resolvedColor(context, 'backgroundColor'),
        iconSize: _number('iconSize') ?? 24.0,
        selectedItemColor: _resolvedColor(context, 'selectedItemColor'),
        unselectedItemColor: _resolvedColor(context, 'unselectedItemColor'),
        // Application-owned theme/style/cursor references are not executed.
        selectedIconTheme: null,
        unselectedIconTheme: null,
        selectedFontSize: _number('selectedFontSize') ?? 14.0,
        unselectedFontSize: _number('unselectedFontSize') ?? 12.0,
        selectedLabelStyle: null,
        unselectedLabelStyle: null,
        showSelectedLabels: _boolean('showSelectedLabels'),
        showUnselectedLabels: _boolean('showUnselectedLabels'),
        mouseCursor: null,
        enableFeedback: _boolean('enableFeedback'),
        landscapeLayout: landscapeLayout,
        useLegacyColorScheme: _boolean('useLegacyColorScheme') ?? true,
      ),
    );
  }

  Widget _material(BuildContext context) {
    final materialType = _materialType();
    final circle = materialType == MaterialType.circle;
    final shapeReference =
        node.properties['shape']?.kind == 'dartObjectReferencePresence';
    final radius = switch (node.properties['borderRadius']?.value) {
      final CanvasBorderRadiusGeometryValue value
          when !circle && !shapeReference =>
        _borderRadius(value),
      _ => null,
    };
    final animationDuration =
        switch (node.properties['animationDurationUs']?.value) {
          final int value => Duration(microseconds: value),
          _ => null,
        };
    return _TextButtonPreview(
      message: _materialPreviewMessage(node) ?? '',
      child: Material(
        key: ValueKey('canvas-material-${node.id}'),
        type: materialType ?? MaterialType.canvas,
        elevation: _number('elevation') ?? 0.0,
        color: _resolvedColor(context, 'color'),
        shadowColor: _resolvedColor(context, 'shadowColor'),
        surfaceTintColor: _resolvedColor(context, 'surfaceTintColor'),
        // TextStyle and ShapeBorder references are application-owned and are
        // intentionally not executed in the isolated Canvas preview.
        textStyle: null,
        borderRadius: radius,
        shape: null,
        borderOnForeground: _boolean('borderOnForeground') ?? true,
        clipBehavior: _clipBehavior() ?? Clip.none,
        animationDuration: animationDuration ?? kThemeChangeDuration,
        animateColor: _boolean('animateColor') ?? false,
        child: _single('child'),
      ),
    );
  }

  Widget _elevatedButton(BuildContext context) {
    final enabled = _boolean('enabled') ?? true;
    final onPressedPresent = _callbackPresent('onPressed');
    final onLongPressPresent = _callbackPresent('onLongPress');
    final onHoverPresent = _callbackPresent('onHover');
    final onFocusChangePresent = _callbackPresent('onFocusChange');
    return _TextButtonPreview(
      message: _textButtonReferenceMessage(node) ?? '',
      child: ElevatedButton(
        key: ValueKey('canvas-elevated-button-${node.id}'),
        onPressed: enabled && (onPressedPresent || !onLongPressPresent)
            ? () {}
            : null,
        onLongPress: enabled && onLongPressPresent ? () {} : null,
        onHover: onHoverPresent ? (_) {} : null,
        onFocusChange: onFocusChangePresent ? (_) {} : null,
        autofocus: _boolean('autofocus') ?? false,
        clipBehavior: _clipBehavior(),
        style: _elevatedButtonStyle(context),
        child: _single('child'),
      ),
    );
  }

  bool get _isOutlinedButton => node.type == 'flutter.material.OutlinedButton';
  bool get _isFilledButton => node.type == 'flutter.material.FilledButton';
  bool get _isIconButton => node.type == 'flutter.material.IconButton';
  bool get _isMenuItemButton => node.type == 'flutter.material.MenuItemButton';
  bool get _isSubmenuButton => node.type == 'flutter.material.SubmenuButton';
  bool get _buttonIconVariant =>
      {'icon', 'tonalIcon'}.contains(_string('variant'));
  bool get _usesExtendedButtonStyle =>
      node.type == 'flutter.material.TextButton' ||
      _isOutlinedButton ||
      _isFilledButton ||
      _isIconButton ||
      node.type == 'flutter.material.MenuAnchor' ||
      _isMenuItemButton ||
      _isSubmenuButton;

  bool _buttonReferencePresent(String name) =>
      node.properties[name]?.kind == 'dartObjectReferencePresence';

  IconAlignment? _buttonIconAlignment(String name) =>
      switch (_enumOrString(name)) {
        'start' => IconAlignment.start,
        'end' => IconAlignment.end,
        _ => null,
      };

  double? _iconButtonNumber(String name) =>
      switch (node.properties[name]?.value) {
        CanvasEnumValue(value: 'infinity') => double.infinity,
        _ => _number(name),
      };

  VisualDensity? _iconButtonDirectDensity(BuildContext context) {
    final horizontal = _number('visualDensityHorizontal');
    final vertical = _number('visualDensityVertical');
    if (horizontal == null && vertical == null) return null;
    return VisualDensity(horizontal: horizontal ?? 0, vertical: vertical ?? 0);
  }

  ButtonStyle _iconButtonCompoundTheme(BuildContext context) {
    final theme = _iconButtonThemeStyle(context);
    final constraints = _boxConstraints('constraints');
    // Deliberate partial-composite completion: the missing dimension/axis
    // inherits constructor values before theme/defaults. The resulting local
    // WidgetStateProperty still shadows the entire direct property in the SDK,
    // including states where the local property resolves null.
    return theme.copyWith(
      minimumSize: constraints == null
          ? null
          : WidgetStatePropertyAll(
              Size(constraints.minWidth, constraints.minHeight),
            ),
      maximumSize: constraints == null
          ? null
          : WidgetStatePropertyAll(
              Size(constraints.maxWidth, constraints.maxHeight),
            ),
      visualDensity: _iconButtonDirectDensity(context),
    );
  }

  WidgetStateProperty<Color?>? _checkboxStateColor(
    BuildContext context,
    String family,
  ) {
    final entries = <WidgetStatesConstraint, Color?>{};
    for (final entry in _checkboxStateLayers.entries) {
      final name = '$family${entry.value}';
      // Presence, not a non-null resolved color, defines an explicit map entry.
      if (node.properties.containsKey(name)) {
        entries[entry.key] = _resolvedColor(context, name);
      }
    }
    if (node.properties.containsKey('${family}Default')) {
      entries[WidgetState.any] = _resolvedColor(context, '${family}Default');
    }
    return entries.isEmpty
        ? null
        : WidgetStateProperty<Color?>.fromMap(entries);
  }

  bool _checkboxHasSideDetails(String prefix) => [
    'Color',
    'Width',
    'Style',
    'StrokeAlign',
  ].any((suffix) => node.properties.containsKey('$prefix$suffix'));

  BorderSide _checkboxLocalSide(
    BuildContext context,
    String prefix,
  ) => BorderSide(
    color: _resolvedColor(context, '${prefix}Color') ?? const Color(0xff000000),
    width: _number('${prefix}Width') ?? 1,
    style: _enum('${prefix}Style') == 'none'
        ? BorderStyle.none
        : BorderStyle.solid,
    strokeAlign:
        _number('${prefix}StrokeAlign') ?? BorderSide.strokeAlignInside,
  );

  BorderSide? _checkboxSide(BuildContext context, {String family = 'side'}) {
    final base = _checkboxHasSideDetails(family)
        ? _checkboxLocalSide(context, family)
        : null;
    if (_boolean('${family}Stateful') != true) return base;
    final entries = <WidgetStatesConstraint, BorderSide?>{};
    for (final entry in _checkboxStateLayers.entries) {
      final prefix = '$family${entry.value}';
      final mode = _string('${prefix}Mode');
      if (mode == 'inherit') {
        entries[entry.key] = null;
      } else if (mode == 'border' || _checkboxHasSideDetails(prefix)) {
        entries[entry.key] = _checkboxLocalSide(context, prefix);
      }
    }
    entries[WidgetState.any] = base;
    return WidgetStateBorderSide.fromMap(entries);
  }

  WidgetStateProperty<double?>? _switchStateWidth(BuildContext context) {
    final entries = <WidgetStatesConstraint, double?>{};
    for (final entry in {
      ..._checkboxStateLayers,
      WidgetState.any: 'Default',
    }.entries) {
      final name = 'trackOutlineWidth${entry.value}';
      if (node.properties.containsKey(name)) {
        entries[entry.key] = node.properties[name]!.kind == 'enum'
            ? double.infinity
            : _number(name);
      }
    }
    final local = entries.isEmpty
        ? null
        : WidgetStateProperty<double?>.fromMap(entries);
    if (_switchWidthMessage(node, context) == null) return local;
    return WidgetStateProperty.resolveWith(
      (states) => _switchWidthPairUnsafe(node, context, states)
          ? 2.0
          : local?.resolve(states),
    );
  }

  WidgetStateProperty<Icon?>? _switchStateIcon(BuildContext context) {
    final entries = <WidgetStatesConstraint, Icon?>{};
    for (final entry in {
      ..._checkboxStateLayers,
      WidgetState.any: 'Default',
    }.entries) {
      final prefix = 'thumbIcon${entry.value}';
      final fields = node.properties.keys.any(
        (name) => name.startsWith(prefix),
      );
      if (!fields) continue;
      if (_string('${prefix}Mode') == 'inherit') {
        entries[entry.key] = null;
        continue;
      }
      final data =
          node.properties['${prefix}Data']?.value as CanvasIconDataValue?;
      final icon = data?.codePoint == null
          ? null
          : IconData(
              // Closed, reviewed icon metadata, never a project Dart expression.
              // ignore: non_const_argument_for_const_parameter
              data!.codePoint!,
              // ignore: non_const_argument_for_const_parameter
              fontFamily: data.fontFamily,
              // ignore: non_const_argument_for_const_parameter
              fontPackage: data.fontPackage,
              matchTextDirection: data.matchTextDirection,
              fontFamilyFallback: data.fontFamilyFallback.isEmpty
                  ? null
                  : data.fontFamilyFallback,
            );
      entries[entry.key] = Icon(
        icon,
        size: _number('${prefix}Size'),
        fill: _number('${prefix}Fill'),
        weight: _number('${prefix}Weight'),
        grade: _number('${prefix}Grade'),
        opticalSize: _number('${prefix}OpticalSize'),
        color: _resolvedColor(context, '${prefix}Color'),
        shadows: _shadows(context, '${prefix}Shadows'),
        semanticLabel: _string('${prefix}SemanticLabel'),
        textDirection: switch (_enum('${prefix}TextDirection')) {
          'ltr' => TextDirection.ltr,
          'rtl' => TextDirection.rtl,
          _ => null,
        },
        applyTextScaling: _boolean('${prefix}ApplyTextScaling'),
        blendMode: _optionalBlendMode('${prefix}BlendMode'),
        fontWeight: _fontWeight('${prefix}FontWeight'),
      );
    }
    return entries.isEmpty ? null : WidgetStateProperty<Icon?>.fromMap(entries);
  }

  Widget _listTile(BuildContext context) {
    Color? color(String name) {
      final entries = <WidgetStatesConstraint, Color>{};
      for (final entry in {
        ..._checkboxStateLayers,
        WidgetState.any: 'Default',
      }.entries) {
        final value = _resolvedColor(context, '$name${entry.value}');
        if (value != null) entries[entry.key] = value;
      }
      return entries.isEmpty
          ? _resolvedColor(context, name)
          : WidgetStateColor.fromMap(entries);
    }

    final cursorEntries = <WidgetStatesConstraint, MouseCursor>{};
    var unresolvedCursor = false;
    for (final entry in {
      ..._checkboxStateLayers,
      WidgetState.any: 'Default',
    }.entries) {
      final name = 'mouseCursor${entry.value}';
      if (node.properties[name]?.kind == 'dartObjectReferencePresence') {
        unresolvedCursor = true;
      }
      final cursor = _mouseCursor(name);
      if (cursor != null) cursorEntries[entry.key] = cursor;
    }
    final cursor = unresolvedCursor
        ? null
        : cursorEntries.isEmpty
        ? _mouseCursor('mouseCursor')
        : WidgetStateMouseCursor.fromMap(cursorEntries);
    final horizontal = _number('visualDensityHorizontal'),
        vertical = _number('visualDensityVertical');
    final density = horizontal == null && vertical == null
        ? null
        : VisualDensity(horizontal: horizontal ?? 0, vertical: vertical ?? 0);
    TextStyle? textStyle(String prefix) =>
        node.properties[prefix]?.kind == 'dartObjectReferencePresence'
        ? null
        : _textStyle(context, prefix);
    final tileTheme = ListTileTheme.of(context);
    final height =
        _listTileNumber(node, 'minTileHeight') ?? tileTheme.minTileHeight;
    final padding =
        _listTileNumber(node, 'minVerticalPadding') ??
        tileTheme.minVerticalPadding ??
        (Theme.of(context).useMaterial3 ? 8 : 4);
    return _ListTilePreview(
      node: node,
      message: _listTileStaticMessage(node, context),
      riskyUnboundedHeight:
          (height != null &&
              !height.isFinite &&
              height != double.negativeInfinity) ||
          !(padding * 2).isFinite && padding != double.negativeInfinity,
      hasMaterial: context.findAncestorWidgetOfExactType<Material>() != null,
      slots: {
        for (final name in ['leading', 'title', 'subtitle', 'trailing'])
          if (_single(name) case final Widget child) name: child,
      },
      builder: (slots) => ListTile(
        leading: slots['leading'],
        title: slots['title'],
        subtitle: slots['subtitle'],
        trailing: slots['trailing'],
        isThreeLine: _boolean('isThreeLine'),
        dense: _boolean('dense'),
        visualDensity: density,
        shape:
            _cardShapePreviewUnavailableMessage(node, widgetName: 'ListTile') ==
                null
            ? _cardShape(context)
            : null,
        style: switch (_enum('style')) {
          'list' => ListTileStyle.list,
          'drawer' => ListTileStyle.drawer,
          _ => null,
        },
        selectedColor: _resolvedColor(context, 'selectedColor'),
        iconColor: color('iconColor'),
        textColor: color('textColor'),
        titleTextStyle: textStyle('titleTextStyle'),
        subtitleTextStyle: textStyle('subtitleTextStyle'),
        leadingAndTrailingTextStyle: textStyle('leadingAndTrailingTextStyle'),
        contentPadding:
            node.properties['contentPadding']?.kind ==
                'dartObjectReferencePresence'
            ? null
            : _edgeInsetsGeometry('contentPadding'),
        enabled: _boolean('enabled') ?? true,
        onTap: _listTileHasCallback(node, 'onTap')
            ? () => onSelected(node.id)
            : null,
        onLongPress: _listTileHasCallback(node, 'onLongPress')
            ? () => onSelected(node.id)
            : null,
        onFocusChange: _listTileHasCallback(node, 'onFocusChange')
            ? (_) {}
            : null,
        mouseCursor: cursor,
        selected: _boolean('selected') ?? false,
        focusColor: _resolvedColor(context, 'focusColor'),
        hoverColor: _resolvedColor(context, 'hoverColor'),
        splashColor: _resolvedColor(context, 'splashColor'),
        autofocus: _boolean('autofocus') ?? false,
        tileColor: _resolvedColor(context, 'tileColor'),
        selectedTileColor: _resolvedColor(context, 'selectedTileColor'),
        enableFeedback: _boolean('enableFeedback'),
        horizontalTitleGap: _listTileNumber(node, 'horizontalTitleGap'),
        minVerticalPadding: _listTileNumber(node, 'minVerticalPadding'),
        minLeadingWidth: _listTileNumber(node, 'minLeadingWidth'),
        minTileHeight: _listTileNumber(node, 'minTileHeight'),
        titleAlignment: switch (_enum('titleAlignment')) {
          'threeLine' => ListTileTitleAlignment.threeLine,
          'titleHeight' => ListTileTitleAlignment.titleHeight,
          'top' => ListTileTitleAlignment.top,
          'center' => ListTileTitleAlignment.center,
          'bottom' => ListTileTitleAlignment.bottom,
          _ => null,
        },
        internalAddSemanticForOnTap:
            _boolean('internalAddSemanticForOnTap') ?? true,
      ),
    );
  }

  Widget _switchListTile(BuildContext context) => _SwitchPreview(
    cupertino: _checkboxUsesCupertino(node, context),
    widgetName: 'SwitchListTile',
    message: '',
    builder: (focusNode) {
      final tileTheme = ListTileTheme.of(context);
      final height =
          _listTileNumber(node, 'minTileHeight') ?? tileTheme.minTileHeight;
      final padding =
          _listTileNumber(node, 'minVerticalPadding') ??
          tileTheme.minVerticalPadding ??
          (Theme.of(context).useMaterial3 ? 8 : 4);
      return _ListTilePreview(
        node: node,
        widgetName: 'SwitchListTile',
        message: _switchListTileStaticMessage(node, context),
        riskyUnboundedHeight:
            (height != null &&
                !height.isFinite &&
                height != double.negativeInfinity) ||
            !(padding * 2).isFinite && padding != double.negativeInfinity,
        hasMaterial: context.findAncestorWidgetOfExactType<Material>() != null,
        slots: {
          for (final name in ['title', 'subtitle', 'secondary'])
            if (_single(name) case final Widget child) name: child,
        },
        builder: (slots) => _switchListTileWithSlots(context, slots, focusNode),
      );
    },
  );

  Widget _switchListTileWithSlots(
    BuildContext context,
    Map<String, Widget> slots,
    FocusNode focusNode,
  ) {
    final cursors = <WidgetStatesConstraint, MouseCursor>{};
    var unresolved = false;
    for (final entry in {
      ..._checkboxStateLayers,
      WidgetState.any: 'Default',
    }.entries) {
      final name = 'mouseCursor${entry.value}';
      if (node.properties[name]?.kind == 'dartObjectReferencePresence') {
        unresolved = true;
      }
      if (_mouseCursor(name) case final MouseCursor cursor) {
        cursors[entry.key] = cursor;
      }
    }
    final cursor = unresolved
        ? null
        : cursors.isEmpty
        ? _mouseCursor('mouseCursor')
        : WidgetStateMouseCursor.fromMap(cursors);
    ({ImageProvider<Object>? provider, ImageErrorListener? onError}) image(
      String name,
    ) {
      final value = node.properties[name]?.value;
      if (value is! CanvasImageProviderValue) {
        return (provider: null, onError: null);
      }
      final binding = _imageProvider(value);
      if (binding.placeholder) return (provider: null, onError: null);
      final resource = binding.resolution as CanvasResolvedImageValue;
      return (
        provider: binding.provider,
        onError: (error, stack) =>
            onImageError?.call(resource.resourceId, error, stack),
      );
    }

    final active = image('activeThumbImage');
    final inactive = image('inactiveThumbImage');
    // Only the public SDK tile shell changes at the known Cupertino painter
    // configuration boundary. The outer focus owner and keyed slot children survive.
    final key = ValueKey(
      'canvas-switch-list-tile-${node.id}-${_checkboxUsesCupertino(node, context)}',
    );
    return _string('variant') == 'adaptive'
        ? SwitchListTile.adaptive(
            key: key,
            value: _boolean('value')!,
            onChanged: _listTileHasCallback(node, 'onChanged')
                ? (_) => onSelected(node.id)
                : null,
            // ignore: deprecated_member_use
            activeColor: _resolvedColor(context, 'activeColor'),
            activeThumbColor: _resolvedColor(context, 'activeThumbColor'),
            activeTrackColor: _resolvedColor(context, 'activeTrackColor'),
            inactiveThumbColor: _resolvedColor(context, 'inactiveThumbColor'),
            inactiveTrackColor: _resolvedColor(context, 'inactiveTrackColor'),
            activeThumbImage: active.provider,
            onActiveThumbImageError: active.onError,
            inactiveThumbImage: inactive.provider,
            onInactiveThumbImageError: inactive.onError,
            thumbColor: _checkboxStateColor(context, 'thumbColor'),
            trackColor: _checkboxStateColor(context, 'trackColor'),
            trackOutlineColor: _checkboxStateColor(
              context,
              'trackOutlineColor',
            ),
            thumbIcon: _switchStateIcon(context),
            materialTapTargetSize: switch (_enum('materialTapTargetSize')) {
              'padded' => MaterialTapTargetSize.padded,
              'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
              _ => null,
            },
            dragStartBehavior: _dragStartBehavior(),
            mouseCursor: cursor,
            overlayColor: _checkboxStateColor(context, 'overlayColor'),
            splashRadius: switch (_listTileNumber(node, 'splashRadius')) {
              final double radius when !radius.isFinite => 0,
              final radius => radius,
            },
            focusNode: focusNode,
            statesController: null,
            onFocusChange: _listTileHasCallback(node, 'onFocusChange')
                ? (_) {}
                : null,
            autofocus: _boolean('autofocus') ?? false,
            tileColor: _resolvedColor(context, 'tileColor'),
            title: slots['title'],
            subtitle: slots['subtitle'],
            isThreeLine: _boolean('isThreeLine'),
            dense: _boolean('dense'),
            contentPadding:
                node.properties['contentPadding']?.kind ==
                    'dartObjectReferencePresence'
                ? null
                : _edgeInsetsGeometry('contentPadding'),
            secondary: slots['secondary'],
            selected: _boolean('selected') ?? false,
            controlAffinity: switch (_enum('controlAffinity')) {
              'leading' => ListTileControlAffinity.leading,
              'trailing' => ListTileControlAffinity.trailing,
              'platform' => ListTileControlAffinity.platform,
              _ => null,
            },
            shape:
                _cardShapePreviewUnavailableMessage(
                      node,
                      widgetName: 'SwitchListTile',
                    ) ==
                    null
                ? _cardShape(context)
                : null,
            selectedTileColor: _resolvedColor(context, 'selectedTileColor'),
            visualDensity: switch ((
              _number('visualDensityHorizontal'),
              _number('visualDensityVertical'),
            )) {
              (null, null) => null,
              (final h, final v) => VisualDensity(
                horizontal: h ?? 0,
                vertical: v ?? 0,
              ),
            },
            enableFeedback: _boolean('enableFeedback'),
            horizontalTitleGap: _listTileNumber(node, 'horizontalTitleGap'),
            minVerticalPadding: _listTileNumber(node, 'minVerticalPadding'),
            minLeadingWidth: _listTileNumber(node, 'minLeadingWidth'),
            minTileHeight: _listTileNumber(node, 'minTileHeight'),
            hoverColor: _resolvedColor(context, 'hoverColor'),
            internalAddSemanticForOnTap:
                _boolean('internalAddSemanticForOnTap') ?? false,
            applyCupertinoTheme: _boolean('applyCupertinoTheme'),
          )
        : SwitchListTile(
            key: key,
            value: _boolean('value')!,
            onChanged: _listTileHasCallback(node, 'onChanged')
                ? (_) => onSelected(node.id)
                : null,
            // ignore: deprecated_member_use
            activeColor: _resolvedColor(context, 'activeColor'),
            activeThumbColor: _resolvedColor(context, 'activeThumbColor'),
            activeTrackColor: _resolvedColor(context, 'activeTrackColor'),
            inactiveThumbColor: _resolvedColor(context, 'inactiveThumbColor'),
            inactiveTrackColor: _resolvedColor(context, 'inactiveTrackColor'),
            activeThumbImage: active.provider,
            onActiveThumbImageError: active.onError,
            inactiveThumbImage: inactive.provider,
            onInactiveThumbImageError: inactive.onError,
            thumbColor: _checkboxStateColor(context, 'thumbColor'),
            trackColor: _checkboxStateColor(context, 'trackColor'),
            trackOutlineColor: _checkboxStateColor(
              context,
              'trackOutlineColor',
            ),
            thumbIcon: _switchStateIcon(context),
            materialTapTargetSize: switch (_enum('materialTapTargetSize')) {
              'padded' => MaterialTapTargetSize.padded,
              'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
              _ => null,
            },
            dragStartBehavior: _dragStartBehavior(),
            mouseCursor: cursor,
            overlayColor: _checkboxStateColor(context, 'overlayColor'),
            splashRadius: switch (_listTileNumber(node, 'splashRadius')) {
              final double radius when !radius.isFinite => 0,
              final radius => radius,
            },
            focusNode: focusNode,
            statesController: null,
            onFocusChange: _listTileHasCallback(node, 'onFocusChange')
                ? (_) {}
                : null,
            autofocus: _boolean('autofocus') ?? false,
            tileColor: _resolvedColor(context, 'tileColor'),
            title: slots['title'],
            subtitle: slots['subtitle'],
            isThreeLine: _boolean('isThreeLine'),
            dense: _boolean('dense'),
            contentPadding:
                node.properties['contentPadding']?.kind ==
                    'dartObjectReferencePresence'
                ? null
                : _edgeInsetsGeometry('contentPadding'),
            secondary: slots['secondary'],
            selected: _boolean('selected') ?? false,
            controlAffinity: switch (_enum('controlAffinity')) {
              'leading' => ListTileControlAffinity.leading,
              'trailing' => ListTileControlAffinity.trailing,
              'platform' => ListTileControlAffinity.platform,
              _ => null,
            },
            shape:
                _cardShapePreviewUnavailableMessage(
                      node,
                      widgetName: 'SwitchListTile',
                    ) ==
                    null
                ? _cardShape(context)
                : null,
            selectedTileColor: _resolvedColor(context, 'selectedTileColor'),
            visualDensity: switch ((
              _number('visualDensityHorizontal'),
              _number('visualDensityVertical'),
            )) {
              (null, null) => null,
              (final h, final v) => VisualDensity(
                horizontal: h ?? 0,
                vertical: v ?? 0,
              ),
            },
            enableFeedback: _boolean('enableFeedback'),
            horizontalTitleGap: _listTileNumber(node, 'horizontalTitleGap'),
            minVerticalPadding: _listTileNumber(node, 'minVerticalPadding'),
            minLeadingWidth: _listTileNumber(node, 'minLeadingWidth'),
            minTileHeight: _listTileNumber(node, 'minTileHeight'),
            hoverColor: _resolvedColor(context, 'hoverColor'),
            internalAddSemanticForOnTap:
                _boolean('internalAddSemanticForOnTap') ?? false,
          );
  }

  Widget _checkboxListTile(BuildContext context) {
    final tileTheme = ListTileTheme.of(context);
    final height =
        _listTileNumber(node, 'minTileHeight') ?? tileTheme.minTileHeight;
    final padding =
        _listTileNumber(node, 'minVerticalPadding') ??
        tileTheme.minVerticalPadding ??
        (Theme.of(context).useMaterial3 ? 8 : 4);
    return _ListTilePreview(
      node: node,
      widgetName: 'CheckboxListTile',
      message: _checkboxListTileStaticMessage(node, context),
      riskyUnboundedHeight:
          (height != null &&
              !height.isFinite &&
              height != double.negativeInfinity) ||
          !(padding * 2).isFinite && padding != double.negativeInfinity,
      hasMaterial: context.findAncestorWidgetOfExactType<Material>() != null,
      slots: {
        for (final name in ['title', 'subtitle', 'secondary'])
          if (_single(name) case final Widget child) name: child,
      },
      builder: (slots) => _checkboxListTileWithSlots(context, slots),
    );
  }

  Widget _checkboxListTileWithSlots(
    BuildContext context,
    Map<String, Widget> slots,
  ) {
    final create = _string('variant') == 'adaptive'
        ? CheckboxListTile.adaptive
        : CheckboxListTile.new;
    final cursorEntries = <WidgetStatesConstraint, MouseCursor>{};
    var unresolvedCursor = false;
    for (final entry in {
      ..._checkboxStateLayers,
      WidgetState.any: 'Default',
    }.entries) {
      final name = 'mouseCursor${entry.value}';
      if (node.properties[name]?.kind == 'dartObjectReferencePresence') {
        unresolvedCursor = true;
      }
      final cursor = _mouseCursor(name);
      if (cursor != null) cursorEntries[entry.key] = cursor;
    }
    final cursor = unresolvedCursor
        ? null
        : cursorEntries.isEmpty
        ? _mouseCursor('mouseCursor')
        : WidgetStateMouseCursor.fromMap(cursorEntries);
    final tileUnavailable = _cardShapePreviewUnavailableMessage(
      node,
      widgetName: 'CheckboxListTile',
    );
    final checkboxUnavailable = _cardShapePreviewUnavailableMessage(
      node,
      widgetName: 'CheckboxListTile',
      prefix: 'checkboxShape',
      expectedType: 'OutlinedBorder',
    );
    final checkboxDirection = _checkboxShapeDirectionMessage(
      node,
      context,
      family: 'checkboxShape',
      owner: 'CheckboxListTile.checkboxShape',
    );
    return create(
      value: _boolean('value'),
      onChanged:
          _listTileHasCallback(node, 'onChanged') &&
              _boolean('enabled') != false
          ? (_) => onSelected(node.id)
          : null,
      mouseCursor: cursor,
      activeColor: _resolvedColor(context, 'activeColor'),
      fillColor: _checkboxStateColor(context, 'fillColor'),
      checkColor: _resolvedColor(context, 'checkColor'),
      hoverColor: _resolvedColor(context, 'hoverColor'),
      overlayColor: _checkboxStateColor(context, 'overlayColor'),
      splashRadius: _listTileNumber(node, 'splashRadius'),
      materialTapTargetSize: switch (_enum('materialTapTargetSize')) {
        'padded' => MaterialTapTargetSize.padded,
        'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
        _ => null,
      },
      visualDensity: switch ((
        _number('visualDensityHorizontal'),
        _number('visualDensityVertical'),
      )) {
        (null, null) => null,
        (final h, final v) => VisualDensity(
          horizontal: h ?? 0,
          vertical: v ?? 0,
        ),
      },
      focusNode: null,
      statesController: null,
      autofocus: _boolean('autofocus') ?? false,
      shape: tileUnavailable == null ? _cardShape(context) : null,
      side: _checkboxSide(context),
      isError: _boolean('isError') ?? false,
      enabled: _boolean('enabled'),
      tileColor: _resolvedColor(context, 'tileColor'),
      title: slots['title'],
      subtitle: slots['subtitle'],
      isThreeLine: _boolean('isThreeLine'),
      dense: _boolean('dense'),
      secondary: slots['secondary'],
      selected: _boolean('selected') ?? false,
      controlAffinity: switch (_enum('controlAffinity')) {
        'leading' => ListTileControlAffinity.leading,
        'trailing' => ListTileControlAffinity.trailing,
        'platform' => ListTileControlAffinity.platform,
        _ => null,
      },
      contentPadding: _edgeInsetsGeometry('contentPadding'),
      tristate: _boolean('tristate') ?? false,
      checkboxShape: checkboxUnavailable == null && checkboxDirection == null
          ? _cardShapeForFamily(context, 'checkboxShape') as OutlinedBorder?
          : null,
      selectedTileColor: _resolvedColor(context, 'selectedTileColor'),
      onFocusChange: _listTileHasCallback(node, 'onFocusChange')
          ? (_) {}
          : null,
      enableFeedback: _boolean('enableFeedback'),
      horizontalTitleGap: _listTileNumber(node, 'horizontalTitleGap'),
      minVerticalPadding: _listTileNumber(node, 'minVerticalPadding'),
      minLeadingWidth: _listTileNumber(node, 'minLeadingWidth'),
      minTileHeight: _listTileNumber(node, 'minTileHeight'),
      checkboxSemanticLabel: _string('checkboxSemanticLabel'),
      checkboxScaleFactor: _listTileNumber(node, 'checkboxScaleFactor') ?? 1,
      titleAlignment: switch (_enum('titleAlignment')) {
        'threeLine' => ListTileTitleAlignment.threeLine,
        'titleHeight' => ListTileTitleAlignment.titleHeight,
        'top' => ListTileTitleAlignment.top,
        'center' => ListTileTitleAlignment.center,
        'bottom' => ListTileTitleAlignment.bottom,
        _ => null,
      },
      internalAddSemanticForOnTap:
          _boolean('internalAddSemanticForOnTap') ?? false,
    );
  }

  Widget _tooltipTheme(BuildContext context) {
    final messages = <String>[];
    final references = node.properties.entries
        .where((entry) => entry.value.kind == 'dartObjectReferencePresence')
        .map((entry) => entry.key)
        .toList();
    if (references.isNotEmpty) {
      messages.add(
        'TooltipTheme ${node.id} preview limitation for ${references.join(', ')}: isolated Canvas never executes project Dart. Unknown whole data uses an explicit empty nearest TooltipThemeData; unknown local values use null SDK defaults, not values from an outer TooltipTheme. Stored values and generated Dart remain exact.',
      );
    }
    EdgeInsetsGeometry? insets(String name) {
      final local = node.properties[name]?.kind == 'dartObjectReferencePresence'
          ? null
          : _edgeInsetsGeometry(name);
      final resolved = local?.resolve(Directionality.of(context));
      if (resolved != null &&
          (!resolved.isNonNegative ||
              !resolved.horizontal.isFinite ||
              !resolved.vertical.isFinite)) {
        messages.add(
          'TooltipTheme ${node.id}.$name preview limitation: negative or overflowing overlay Container insets cannot be mounted safely; this inset uses zero.',
        );
        return EdgeInsets.zero;
      }
      return local;
    }

    var height = _listTileNumber(node, 'height');
    if (height != null && (height.isNaN || height < 0)) {
      messages.add(
        'TooltipTheme ${node.id}.height preview limitation: negative or NaN minimum height uses zero.',
      );
      height = 0;
    }
    var verticalOffset = _listTileNumber(node, 'verticalOffset');
    if (verticalOffset != null &&
        (verticalOffset.isNaN || verticalOffset == double.negativeInfinity)) {
      messages.add(
        'TooltipTheme ${node.id}.verticalOffset preview limitation: nonfinite overlay position uses the SDK default 24; positive infinity remains exact.',
      );
      verticalOffset = 24;
    }
    Duration? duration(String name) => switch (_integer(name)) {
      final int value => Duration(microseconds: value),
      _ => null,
    };
    final padding = insets('padding');
    final margin = insets('margin');
    final localStyle =
        node.properties.keys.any(
          (name) => name.startsWith('textStyle') && name != 'textStyle',
        )
        ? _textStyle(context, 'textStyle')
        : null;
    // Fresh construction is deliberate: the pinned copyWith drops exitDuration
    // and lerp drops all five duration/trigger/feedback behavior fields.
    final data = TooltipThemeData(
      // ignore: deprecated_member_use
      height: height,
      constraints: _boxConstraints('constraints'),
      padding: padding,
      margin: margin,
      verticalOffset: verticalOffset,
      preferBelow: _boolean('preferBelow'),
      excludeFromSemantics: _boolean('excludeFromSemantics'),
      decoration: _boxDecoration(context, 'decoration'),
      textStyle: localStyle,
      textAlign: _textAlign(),
      waitDuration: duration('waitDurationUs'),
      showDuration: duration('showDurationUs'),
      exitDuration: duration('exitDurationUs'),
      triggerMode: switch (_enum('triggerMode')) {
        'manual' => TooltipTriggerMode.manual,
        'tap' => TooltipTriggerMode.tap,
        'longPress' => TooltipTriggerMode.longPress,
        _ => null,
      },
      enableFeedback: _boolean('enableFeedback'),
    );
    return _TextButtonPreview(
      message: messages.join(' '),
      stableDiagnostic: true,
      child: TooltipTheme(data: data, child: _single('child')!),
    );
  }

  Widget _tooltip(BuildContext context) {
    final previewKey = (context.findAncestorStateOfType<_SwitcherEntryViewState>()?._tooltips ??
        context.findAncestorStateOfType<_CanvasDocumentViewState>()?._tooltipPreviewKeys)
        ?.putIfAbsent(node.id, GlobalKey.new);
    return _TooltipPreview(
      key: previewKey,
      node: node,
      builder: (sdkKey, anchorKey) {
        final theme = TooltipTheme.of(context);
        final plainMessage = _string('message');
        final rich =
            node.properties['richMessage']?.kind ==
            'dartObjectReferencePresence';
        final active = rich || (plainMessage?.isNotEmpty ?? false);
        final messages = <String>[];
        final refs = node.properties.entries
            .where((entry) => entry.value.kind == 'dartObjectReferencePresence')
            .map((entry) => entry.key)
            .toList();
        if (active && refs.isNotEmpty) {
          messages.add(
            'Tooltip ${node.id} preview limitation for ${refs.join(', ')}: isolated Canvas never executes project Dart. '
            'Unknown appearance, durations and position use SDK theme/default approximations; project callbacks are benign no-ops. '
            'Project InlineSpan content, WidgetSpan interaction and semantics cannot be reproduced and are explicitly labeled unavailable. Stored values and generated Dart remain exact.',
          );
        }
        EdgeInsetsGeometry? padding(
          String name,
          EdgeInsetsGeometry? inherited,
        ) {
          final local =
              node.properties[name]?.kind == 'dartObjectReferencePresence'
              ? null
              : _edgeInsetsGeometry(name);
          final resolved = (local ?? inherited)?.resolve(
            Directionality.of(context),
          );
          if (active &&
              resolved != null &&
              (!resolved.isNonNegative ||
                  !resolved.horizontal.isFinite ||
                  !resolved.vertical.isFinite)) {
            messages.add(
              'Tooltip ${node.id}.$name preview limitation: negative or overflowing Container insets cannot be mounted safely; only this inset uses an explicit zero approximation.',
            );
            return EdgeInsets.zero;
          }
          return local;
        }

        var height = _listTileNumber(node, 'height');
        // The deprecated constructor field remains part of the reviewed SDK.
        // ignore: deprecated_member_use
        final effectiveHeight = height ?? theme.height;
        if (active &&
            effectiveHeight != null &&
            (effectiveHeight.isNaN || effectiveHeight < 0)) {
          messages.add(
            'Tooltip ${node.id}.height preview limitation: negative or NaN minimum height produces invalid SDK constraints; only this height uses zero.',
          );
          height = 0;
        }
        var constraints = _boxConstraints('constraints');
        final effectiveConstraints = constraints ?? theme.constraints;
        if (active &&
            effectiveConstraints != null &&
            !effectiveConstraints.isNormalized) {
          messages.add(
            'Tooltip ${node.id}.constraints preview limitation: invalid theme constraints use unconstrained SDK-default geometry without changing stored values.',
          );
          constraints = const BoxConstraints();
        }
        // Height is deprecated but still supplied exactly when legal. SDK allows
        // explicit null next to constraints; never create two nonnull arguments.
        if (constraints != null) height = null;
        var verticalOffset = _listTileNumber(node, 'verticalOffset');
        final effectiveOffset = verticalOffset ?? theme.verticalOffset;
        if (active &&
            effectiveOffset != null &&
            (effectiveOffset.isNaN ||
                effectiveOffset == double.negativeInfinity)) {
          messages.add(
            'Tooltip ${node.id}.verticalOffset preview limitation: the default delegate produces a nonfinite position; only this offset uses the SDK default 24. Positive infinity remains exact.',
          );
          verticalOffset = 24;
        }
        final localPadding = padding('padding', theme.padding);
        final localMargin = padding('margin', theme.margin);
        Duration? duration(String name) => switch (_integer(name)) {
          final int value => Duration(microseconds: value),
          _ => null,
        };
        final anchor = _single('child');
        final message = messages.isEmpty ? null : messages.join(' ');
        final state = previewKey?.currentState;
        if (state is _TooltipPreviewState) state.message = message;
        final localStyle =
            node.properties.keys.any(
              (name) => name.startsWith('textStyle') && name != 'textStyle',
            )
            ? _textStyle(context, 'textStyle')
            : null;
        return Stack(
          fit: StackFit.passthrough,
          clipBehavior: Clip.none,
          children: [
            Tooltip(
              key: sdkKey,
              message: plainMessage,
              richMessage: rich
                  ? const TextSpan(
                      text: '[Preview unavailable: project InlineSpan]',
                    )
                  : null,
              // ignore: deprecated_member_use
              height: height,
              constraints: constraints,
              padding: localPadding,
              margin: localMargin,
              verticalOffset: verticalOffset,
              preferBelow: _boolean('preferBelow'),
              excludeFromSemantics: _boolean('excludeFromSemantics'),
              decoration: _boxDecoration(context, 'decoration'),
              textStyle: localStyle,
              textAlign: _textAlign(),
              waitDuration: duration('waitDurationUs'),
              showDuration: duration('showDurationUs'),
              exitDuration: duration('exitDurationUs'),
              enableTapToDismiss: _boolean('enableTapToDismiss') ?? true,
              triggerMode: switch (_enum('triggerMode')) {
                'manual' => TooltipTriggerMode.manual,
                'tap' => TooltipTriggerMode.tap,
                'longPress' => TooltipTriggerMode.longPress,
                _ => null,
              },
              enableFeedback: _boolean('enableFeedback'),
              onTriggered: _listTileHasCallback(node, 'onTriggered')
                  ? () {}
                  : null,
              mouseCursor: _mouseCursor('mouseCursor'),
              ignorePointer: _boolean('ignorePointer'),
              positionDelegate: null,
              child: anchor == null
                  ? null
                  : _TooltipAnchorScope(
                      child: KeyedSubtree(key: anchorKey, child: anchor),
                    ),
            ),
            if (message != null)
              Positioned(
                top: 0,
                right: 0,
                child: IgnoreBaseline(
                  child: Tooltip(
                    key: ValueKey('canvas-tooltip-diagnostic-${node.id}'),
                    message: message,
                    child: const ColoredBox(
                      color: Color(0xfffef3c7),
                      child: Icon(
                        Icons.info_outline,
                        size: 12,
                        color: Color(0xff92400e),
                      ),
                    ),
                  ),
                ),
              ),
          ],
        );
      },
    );
  }

  Widget _expansionTile(BuildContext context) => _ExpansionTilePreview(
    // Selection adds/removes an outline wrapper below the node's outer key.
    // Retain the complete SDK subtree across that editor-only reparenting.
    key: context
        .findAncestorStateOfType<_CanvasDocumentViewState>()
        ?._expansionTilePreviewKeys
        .putIfAbsent(node.id, GlobalKey.new),
    node: node,
    builder: (controller, sdkKey) {
      final theme = ExpansionTileTheme.of(context);
      final messages = <String>[];
      final refs = node.properties.entries
          .where((entry) => entry.value.kind == 'dartObjectReferencePresence')
          .map((entry) => entry.key)
          .where((name) => name != 'expansionAnimationStyleReverseDurationUs')
          .toList();
      if (refs.isNotEmpty) {
        messages.add(
          'ExpansionTile ${node.id} preview limitation for ${refs.join(', ')}: isolated Canvas never executes project Dart. '
          'Appearance, padding and animation references use the actual SDK theme/default as an explicit approximation; callbacks are benign no-ops. '
          'The preview owns a local ExpansibleController and SDK header states, not referenced project controllers. Stored values and generated Dart remain unchanged.',
        );
      }
      ShapeBorder? shape(String family) {
        final message = _cardShapePreviewUnavailableMessage(
          node,
          widgetName: 'ExpansionTile',
          prefix: family,
        );
        if (message != null) messages.add(message);
        return message == null ? _cardShapeForFamily(context, family) : null;
      }

      final expandedShape = shape('shape');
      final collapsedShape = shape('collapsedShape');
      EdgeInsetsGeometry? padding(String name, EdgeInsetsGeometry? inherited) {
        final local =
            node.properties[name]?.kind == 'dartObjectReferencePresence'
            ? null
            : _edgeInsetsGeometry(name);
        final effective = (local ?? inherited)?.resolve(
          Directionality.of(context),
        );
        if (effective != null &&
            (!effective.isNonNegative ||
                !effective.horizontal.isFinite ||
                !effective.vertical.isFinite)) {
          messages.add(
            'ExpansionTile ${node.id}.$name preview limitation: the resolved negative or overflowing Padding geometry cannot be mounted safely. '
            'Only this padding uses an explicit zero approximation; actual children and stored values are unchanged.',
          );
          return EdgeInsets.zero;
        }
        return local;
      }

      final tilePadding = padding('tilePadding', theme.tilePadding);
      final childrenPadding = padding('childrenPadding', theme.childrenPadding);
      Duration? duration(String name) => switch (_integer(name)) {
        final int value => Duration(microseconds: value),
        _ => null,
      };
      AnimationStyle? style;
      if (_string('expansionAnimationStyle') == 'noAnimation') {
        style = AnimationStyle.noAnimation;
      } else if (node.properties.keys.any(
        (name) =>
            name.startsWith('expansionAnimationStyle') &&
            name != 'expansionAnimationStyle',
      )) {
        style = AnimationStyle(
          duration: duration('expansionAnimationStyleDurationUs'),
          reverseDuration: duration('expansionAnimationStyleReverseDurationUs'),
          curve: _expansionCurves[_string('expansionAnimationStyleCurve')],
          reverseCurve:
              _expansionCurves[_string('expansionAnimationStyleReverseCurve')],
        );
      }
      var effectiveDuration =
          style?.duration ??
          theme.expansionAnimationStyle?.duration ??
          const Duration(milliseconds: 200);
      if (effectiveDuration.isNegative) {
        messages.add(
          'ExpansionTile ${node.id} expansionAnimationStyle.duration preview limitation: negative duration is accepted by the constructor but the SDK animation asserts when started. '
          'Only the preview duration uses Duration.zero; stored signed microseconds remain exact.',
        );
        style = (style ?? const AnimationStyle()).copyWith(
          duration: Duration.zero,
        );
        effectiveDuration = Duration.zero;
      }
      bool undershoots(Curve? curve) => const [
        Curves.easeInBack,
        Curves.easeInOutBack,
        Curves.elasticIn,
        Curves.elasticInOut,
      ].contains(curve);
      if (effectiveDuration != Duration.zero) {
        final forward =
            style?.curve ??
            theme.expansionAnimationStyle?.curve ??
            Curves.easeIn;
        final reverse =
            style?.reverseCurve ?? theme.expansionAnimationStyle?.reverseCurve;
        if (undershoots(forward) || undershoots(reverse)) {
          messages.add(
            'ExpansionTile ${node.id} expansion curve preview limitation: the configured SDK curve has negative height factors, which Expansible Align rejects during animation. '
            'Only affected preview height curves use Curves.easeIn as an explicit approximation; stored curve presets and generated Dart are unchanged.',
          );
          style = (style ?? const AnimationStyle()).copyWith(
            curve: undershoots(forward) ? Curves.easeIn : null,
            reverseCurve: undershoots(reverse) ? Curves.easeIn : null,
          );
        }
      }
      // reverseDuration is intentionally not used for safety decisions or timing:
      // the pinned ExpansionTile and Expansible ignore that AnimationStyle field.
      final background = _resolvedColor(context, 'backgroundColor');
      final collapsedBackground = _resolvedColor(
        context,
        'collapsedBackgroundColor',
      );
      final hasShape =
          expandedShape != null ||
          collapsedShape != null ||
          theme.shape != null ||
          theme.collapsedShape != null;
      final activeBackground = controller.isExpanded
          ? background ?? theme.backgroundColor
          : collapsedBackground ?? theme.collapsedBackgroundColor;
      var hasModelMaterial = false;
      final rootId = context
          .findAncestorWidgetOfExactType<CanvasDocumentView>()
          ?.model
          .root
          .id;
      if (node.id != rootId) {
        context.visitAncestorElements((element) {
          // The editor chrome's Scaffold sits behind the viewport decoration;
          // it is not a valid painted Material ancestor in the designed tree.
          if (element.widget is CanvasDocumentView ||
              (element.widget is _CanvasNodeView &&
                  (element.widget as _CanvasNodeView).node.id == rootId)) {
            return false;
          }
          if (element.widget is Material) {
            hasModelMaterial = true;
            return false;
          }
          return true;
        });
      }
      final hasMaterial =
          hasModelMaterial || hasShape || (activeBackground?.a ?? 0) > 0;
      if (!hasMaterial) {
        messages.add(
          'ExpansionTile ${node.id} preview unavailable: no Material ancestor or actual SDK shape/background Material branch is available. No synthetic Material is inserted.',
        );
      }
      final height =
          _listTileNumber(node, 'minTileHeight') ??
          ListTileTheme.of(context).minTileHeight;
      final unsafeHeight =
          height != null &&
          !height.isFinite &&
          height != double.negativeInfinity;
      if (unsafeHeight) {
        messages.add(
          'ExpansionTile ${node.id}.minTileHeight preview limitation: its SDK Column gives the header unbounded height, so positive infinity or NaN cannot be laid out. '
          'Only this minimum uses an explicit zero approximation; actual header/body State and stored values are retained.',
        );
      }
      return _ListTilePreview(
        node: node,
        widgetName: 'ExpansionTile',
        message: messages.isEmpty ? null : messages.join(' '),
        hasMaterial: hasMaterial,
        riskyUnboundedHeight:
            height != null &&
            !height.isFinite &&
            height != double.negativeInfinity,
        slots: {
          for (final name in ['title', 'leading', 'subtitle', 'trailing'])
            if (_single(name) case final Widget child) name: child,
        },
        builder: (slots) => ExpansionTile(
          key: sdkKey,
          controller: controller,
          title: slots['title']!,
          leading: slots['leading'],
          subtitle: slots['subtitle'],
          trailing: slots['trailing'],
          onExpansionChanged: _listTileHasCallback(node, 'onExpansionChanged')
              ? (_) {}
              : null,
          showTrailingIcon: _boolean('showTrailingIcon') ?? true,
          initiallyExpanded: _boolean('initiallyExpanded') ?? false,
          maintainState: _boolean('maintainState') ?? false,
          tilePadding: tilePadding,
          expandedCrossAxisAlignment: switch (_enum(
            'expandedCrossAxisAlignment',
          )) {
            'start' => CrossAxisAlignment.start,
            'end' => CrossAxisAlignment.end,
            'center' => CrossAxisAlignment.center,
            'stretch' => CrossAxisAlignment.stretch,
            _ => null,
          },
          expandedAlignment: _alignmentGeometry('expandedAlignment'),
          childrenPadding: childrenPadding,
          backgroundColor: background,
          collapsedBackgroundColor: collapsedBackground,
          textColor: _resolvedColor(context, 'textColor'),
          collapsedTextColor: _resolvedColor(context, 'collapsedTextColor'),
          iconColor: _resolvedColor(context, 'iconColor'),
          collapsedIconColor: _resolvedColor(context, 'collapsedIconColor'),
          shape: expandedShape,
          collapsedShape: collapsedShape,
          clipBehavior: _clipBehavior(),
          controlAffinity: switch (_enum('controlAffinity')) {
            'leading' => ListTileControlAffinity.leading,
            'trailing' => ListTileControlAffinity.trailing,
            'platform' => ListTileControlAffinity.platform,
            _ => null,
          },
          dense: _boolean('dense'),
          splashColor: _resolvedColor(context, 'splashColor'),
          visualDensity: switch ((
            _number('visualDensityHorizontal'),
            _number('visualDensityVertical'),
          )) {
            (null, null) => null,
            (final h, final v) => VisualDensity(
              horizontal: h ?? 0,
              vertical: v ?? 0,
            ),
          },
          minTileHeight: unsafeHeight
              ? 0
              : _listTileNumber(node, 'minTileHeight'),
          enableFeedback: node.properties.containsKey('enableFeedback')
              ? _boolean('enableFeedback')
              : true,
          enabled: _boolean('enabled') ?? true,
          expansionAnimationStyle: style,
          internalAddSemanticForOnTap:
              _boolean('internalAddSemanticForOnTap') ?? false,
          statesController: null,
          children: _children('children'),
        ),
      );
    },
  );

  Widget _radioListTile(BuildContext context) {
    final tileTheme = ListTileTheme.of(context);
    final height =
        _listTileNumber(node, 'minTileHeight') ?? tileTheme.minTileHeight;
    final padding =
        _listTileNumber(node, 'minVerticalPadding') ??
        tileTheme.minVerticalPadding ??
        (Theme.of(context).useMaterial3 ? 8 : 4);
    return _ListTilePreview(
      node: node,
      widgetName: 'RadioListTile',
      message: _radioListTileStaticMessage(node, context),
      riskyUnboundedHeight:
          (height != null &&
              !height.isFinite &&
              height != double.negativeInfinity) ||
          !(padding * 2).isFinite && padding != double.negativeInfinity,
      hasMaterial: context.findAncestorWidgetOfExactType<Material>() != null,
      slots: {
        for (final name in ['title', 'subtitle', 'secondary'])
          if (_single(name) case final Widget child) name: child,
      },
      builder: (slots) {
        if (_radioListTileUnavailableMessage(node, context) != null) {
          // Preserve the actual slot children without inventing a radio value,
          // checked state or activation. This is explicitly unavailable layout.
          return ExcludeFocus(
            child: IgnorePointer(
              child: ExcludeSemantics(
                child: ListTile(
                  title: slots['title'],
                  subtitle: slots['subtitle'],
                  trailing: slots['secondary'],
                  enabled: false,
                  internalAddSemanticForOnTap: false,
                ),
              ),
            ),
          );
        }
        Widget build<T>() => _typedRadioListTile<T>(context, slots);
        return switch (_radioTypeKey(node)) {
          'String' => build<String>(),
          'String?' => build<String?>(),
          'int' => build<int>(),
          'int?' => build<int?>(),
          'double' => build<double>(),
          'double?' => build<double?>(),
          'num' => build<num>(),
          'num?' => build<num?>(),
          'bool' => build<bool>(),
          'bool?' => build<bool?>(),
          'Object?' => build<Object?>(),
          _ => build<Object>(),
        };
      },
    );
  }

  Widget _typedRadioListTile<T>(
    BuildContext context,
    Map<String, Widget> slots,
  ) {
    final cursors = <WidgetStatesConstraint, MouseCursor>{};
    var unresolvedCursor = false;
    final radii = <WidgetStatesConstraint, double?>{};
    for (final entry in {
      ..._checkboxStateLayers,
      WidgetState.any: 'Default',
    }.entries) {
      final name = 'mouseCursor${entry.value}';
      if (node.properties[name]?.kind == 'dartObjectReferencePresence') {
        unresolvedCursor = true;
      }
      if (_mouseCursor(name) case final MouseCursor cursor) {
        cursors[entry.key] = cursor;
      }
      final radiusName = 'radioInnerRadius${entry.value}';
      if (node.properties.containsKey(radiusName)) {
        radii[entry.key] = _sliderNumber(node, radiusName);
      }
    }
    final cursor = unresolvedCursor
        ? null
        : cursors.isEmpty
        ? _mouseCursor('mouseCursor')
        : WidgetStateMouseCursor.fromMap(cursors);
    final legacyValue =
        node.properties['groupValue']?.kind == 'dartObjectReferencePresence'
        ? null
        : _radioIdentityValue(node, 'groupValue') as T?;
    final control = _string('variant') == 'adaptive'
        ? RadioListTile<T>.adaptive(
            value: _radioIdentityValue(node, 'value') as T,
            // A null modern group value genuinely falls back to this legacy value.
            // ignore: deprecated_member_use
            groupValue: legacyValue,
            // ignore: deprecated_member_use
            onChanged: _radioHasCallback(node)
                ? (_) => onSelected(node.id)
                : null,
            enabled: _boolean('enabled'),
            mouseCursor: cursor,
            toggleable: _boolean('toggleable') ?? false,
            activeColor: _resolvedColor(context, 'activeColor'),
            fillColor: _checkboxStateColor(context, 'fillColor'),
            hoverColor: _resolvedColor(context, 'hoverColor'),
            overlayColor: _checkboxStateColor(context, 'overlayColor'),
            splashRadius: switch (_listTileNumber(node, 'splashRadius')) {
              final double radius when !radius.isFinite => 0,
              final radius => radius,
            },
            materialTapTargetSize: switch (_enum('materialTapTargetSize')) {
              'padded' => MaterialTapTargetSize.padded,
              'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
              _ => null,
            },
            title: slots['title'],
            subtitle: slots['subtitle'],
            isThreeLine: _boolean('isThreeLine'),
            dense: _boolean('dense'),
            secondary: slots['secondary'],
            selected: _boolean('selected') ?? false,
            controlAffinity: switch (_enum('controlAffinity')) {
              'leading' => ListTileControlAffinity.leading,
              'trailing' => ListTileControlAffinity.trailing,
              'platform' => ListTileControlAffinity.platform,
              _ => null,
            },
            autofocus: _boolean('autofocus') ?? false,
            contentPadding:
                node.properties['contentPadding']?.kind ==
                    'dartObjectReferencePresence'
                ? null
                : _edgeInsetsGeometry('contentPadding'),
            shape:
                _cardShapePreviewUnavailableMessage(
                      node,
                      widgetName: 'RadioListTile',
                    ) ==
                    null
                ? _cardShape(context)
                : null,
            tileColor: _resolvedColor(context, 'tileColor'),
            selectedTileColor: _resolvedColor(context, 'selectedTileColor'),
            visualDensity: switch ((
              _number('visualDensityHorizontal'),
              _number('visualDensityVertical'),
            )) {
              (null, null) => null,
              (final h, final v) => VisualDensity(
                horizontal: h ?? 0,
                vertical: v ?? 0,
              ),
            },
            focusNode: null,
            statesController: null,
            onFocusChange: _listTileHasCallback(node, 'onFocusChange')
                ? (_) {}
                : null,
            enableFeedback: _boolean('enableFeedback'),
            horizontalTitleGap: _listTileNumber(node, 'horizontalTitleGap'),
            minVerticalPadding: _listTileNumber(node, 'minVerticalPadding'),
            minLeadingWidth: _listTileNumber(node, 'minLeadingWidth'),
            minTileHeight: _listTileNumber(node, 'minTileHeight'),
            radioScaleFactor: switch (_listTileNumber(
              node,
              'radioScaleFactor',
            )) {
              final double scale when !scale.isFinite => 0,
              final scale => scale ?? 1,
            },
            titleAlignment: switch (_enum('titleAlignment')) {
              'threeLine' => ListTileTitleAlignment.threeLine,
              'titleHeight' => ListTileTitleAlignment.titleHeight,
              'top' => ListTileTitleAlignment.top,
              'center' => ListTileTitleAlignment.center,
              'bottom' => ListTileTitleAlignment.bottom,
              _ => null,
            },
            internalAddSemanticForOnTap:
                _boolean('internalAddSemanticForOnTap') ?? false,
            radioBackgroundColor: _checkboxStateColor(
              context,
              'radioBackgroundColor',
            ),
            radioSide: _checkboxSide(context, family: 'radioSide'),
            radioInnerRadius: radii.isEmpty
                ? null
                : WidgetStateProperty<double?>.fromMap(radii),
            useCupertinoCheckmarkStyle:
                _boolean('useCupertinoCheckmarkStyle') ?? false,
          )
        : RadioListTile<T>(
            value: _radioIdentityValue(node, 'value') as T,
            // A null modern group value genuinely falls back to this legacy value.
            // ignore: deprecated_member_use
            groupValue: legacyValue,
            // ignore: deprecated_member_use
            onChanged: _radioHasCallback(node)
                ? (_) => onSelected(node.id)
                : null,
            enabled: _boolean('enabled'),
            mouseCursor: cursor,
            toggleable: _boolean('toggleable') ?? false,
            activeColor: _resolvedColor(context, 'activeColor'),
            fillColor: _checkboxStateColor(context, 'fillColor'),
            hoverColor: _resolvedColor(context, 'hoverColor'),
            overlayColor: _checkboxStateColor(context, 'overlayColor'),
            splashRadius: switch (_listTileNumber(node, 'splashRadius')) {
              final double radius when !radius.isFinite => 0,
              final radius => radius,
            },
            materialTapTargetSize: switch (_enum('materialTapTargetSize')) {
              'padded' => MaterialTapTargetSize.padded,
              'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
              _ => null,
            },
            title: slots['title'],
            subtitle: slots['subtitle'],
            isThreeLine: _boolean('isThreeLine'),
            dense: _boolean('dense'),
            secondary: slots['secondary'],
            selected: _boolean('selected') ?? false,
            controlAffinity: switch (_enum('controlAffinity')) {
              'leading' => ListTileControlAffinity.leading,
              'trailing' => ListTileControlAffinity.trailing,
              'platform' => ListTileControlAffinity.platform,
              _ => null,
            },
            autofocus: _boolean('autofocus') ?? false,
            contentPadding:
                node.properties['contentPadding']?.kind ==
                    'dartObjectReferencePresence'
                ? null
                : _edgeInsetsGeometry('contentPadding'),
            shape:
                _cardShapePreviewUnavailableMessage(
                      node,
                      widgetName: 'RadioListTile',
                    ) ==
                    null
                ? _cardShape(context)
                : null,
            tileColor: _resolvedColor(context, 'tileColor'),
            selectedTileColor: _resolvedColor(context, 'selectedTileColor'),
            visualDensity: switch ((
              _number('visualDensityHorizontal'),
              _number('visualDensityVertical'),
            )) {
              (null, null) => null,
              (final h, final v) => VisualDensity(
                horizontal: h ?? 0,
                vertical: v ?? 0,
              ),
            },
            focusNode: null,
            statesController: null,
            onFocusChange: _listTileHasCallback(node, 'onFocusChange')
                ? (_) {}
                : null,
            enableFeedback: _boolean('enableFeedback'),
            horizontalTitleGap: _listTileNumber(node, 'horizontalTitleGap'),
            minVerticalPadding: _listTileNumber(node, 'minVerticalPadding'),
            minLeadingWidth: _listTileNumber(node, 'minLeadingWidth'),
            minTileHeight: _listTileNumber(node, 'minTileHeight'),
            radioScaleFactor: switch (_listTileNumber(
              node,
              'radioScaleFactor',
            )) {
              final double scale when !scale.isFinite => 0,
              final scale => scale ?? 1,
            },
            titleAlignment: switch (_enum('titleAlignment')) {
              'threeLine' => ListTileTitleAlignment.threeLine,
              'titleHeight' => ListTileTitleAlignment.titleHeight,
              'top' => ListTileTitleAlignment.top,
              'center' => ListTileTitleAlignment.center,
              'bottom' => ListTileTitleAlignment.bottom,
              _ => null,
            },
            internalAddSemanticForOnTap:
                _boolean('internalAddSemanticForOnTap') ?? false,
            radioBackgroundColor: _checkboxStateColor(
              context,
              'radioBackgroundColor',
            ),
            radioSide: _checkboxSide(context, family: 'radioSide'),
            radioInnerRadius: radii.isEmpty
                ? null
                : WidgetStateProperty<double?>.fromMap(radii),
          );
    final scoped = _canvasRadioScope(node, context)?.registry;
    return scoped == null
        ? control
        : _RadioListTileRegistration<T>(
            registry: (scoped as _RadioGroupForwardingRegistry<T>).forRadio(
              node.id,
            ),
            child: control,
          );
  }

  Widget _radio(BuildContext context) {
    final unavailable = _radioUnavailableMessage(node, context);
    if (unavailable != null) {
      return _customClipperPreviewUnavailable(
        widgetName: 'Radio',
        expectedType: 'known generic values and group selection',
        previewLabel: 'Radio\npreview unavailable',
        messageOverride: unavailable,
      );
    }
    final nullable = _boolean('nullableValueType') == true;
    final control = switch (_string('valueType')) {
      'String' =>
        nullable ? _typedRadio<String?>(context) : _typedRadio<String>(context),
      'int' =>
        nullable ? _typedRadio<int?>(context) : _typedRadio<int>(context),
      'double' =>
        nullable ? _typedRadio<double?>(context) : _typedRadio<double>(context),
      'num' =>
        nullable ? _typedRadio<num?>(context) : _typedRadio<num>(context),
      'bool' =>
        nullable ? _typedRadio<bool?>(context) : _typedRadio<bool>(context),
      _ =>
        nullable ? _typedRadio<Object?>(context) : _typedRadio<Object>(context),
    };
    return _TextButtonPreview(
      message: _radioReferenceMessage(node, context) ?? '',
      child: Listener(
        behavior: HitTestBehavior.translucent,
        onPointerDown: (_) => onSelected(node.id),
        child: control,
      ),
    );
  }

  Widget _typedRadio<T>(BuildContext context) {
    final value = _radioIdentityValue(node, 'value') as T;
    final scoped = _canvasRadioScope(node, context)?.registry;
    final registry = scoped == null
        ? null
        : (scoped as _RadioGroupForwardingRegistry<T>).forRadio(node.id);
    final groupValue = _radioHasInheritedRegistry(node, context)
        ? null
        : _radioIdentityValue(node, 'groupValue') as T?;
    final ValueChanged<T?>? onChanged = _radioHasCallback(node)
        ? (_) => onSelected(node.id)
        : null;
    final horizontal = _number('visualDensityHorizontal');
    final vertical = _number('visualDensityVertical');
    final density = horizontal == null && vertical == null
        ? null
        : VisualDensity(horizontal: horizontal ?? 0, vertical: vertical ?? 0);
    final radiusEntries = <WidgetStatesConstraint, double?>{};
    for (final entry in {
      ..._checkboxStateLayers,
      WidgetState.any: 'Default',
    }.entries) {
      final name = 'innerRadius${entry.value}';
      if (node.properties.containsKey(name)) {
        radiusEntries[entry.key] = _sliderNumber(node, name);
      }
    }
    final innerRadius = radiusEntries.isEmpty
        ? null
        : WidgetStateProperty<double?>.fromMap(radiusEntries);
    final tapTarget = switch (_enum('materialTapTargetSize')) {
      'padded' => MaterialTapTargetSize.padded,
      'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
      _ => null,
    };
    return _string('variant') == 'adaptive'
        ? Radio<T>.adaptive(
            value: value,
            groupRegistry: registry,
            // These still-existing SDK arguments preserve the full legacy branch.
            // ignore: deprecated_member_use
            groupValue: groupValue,
            // ignore: deprecated_member_use
            onChanged: onChanged,
            enabled: _boolean('enabled'),
            mouseCursor: _mouseCursor('mouseCursor'),
            toggleable: _boolean('toggleable') ?? false,
            activeColor: _resolvedColor(context, 'activeColor'),
            fillColor: _checkboxStateColor(context, 'fillColor'),
            focusColor: _resolvedColor(context, 'focusColor'),
            hoverColor: _resolvedColor(context, 'hoverColor'),
            overlayColor: _checkboxStateColor(context, 'overlayColor'),
            splashRadius: _sliderNumber(node, 'splashRadius'),
            materialTapTargetSize: tapTarget,
            visualDensity: density,
            autofocus: _boolean('autofocus') ?? false,
            useCupertinoCheckmarkStyle:
                _boolean('useCupertinoCheckmarkStyle') ?? false,
            backgroundColor: _checkboxStateColor(context, 'backgroundColor'),
            side: _checkboxSide(context),
            innerRadius: innerRadius,
          )
        : Radio<T>(
            value: value,
            groupRegistry: registry,
            // ignore: deprecated_member_use
            groupValue: groupValue,
            // ignore: deprecated_member_use
            onChanged: onChanged,
            enabled: _boolean('enabled'),
            mouseCursor: _mouseCursor('mouseCursor'),
            toggleable: _boolean('toggleable') ?? false,
            activeColor: _resolvedColor(context, 'activeColor'),
            fillColor: _checkboxStateColor(context, 'fillColor'),
            focusColor: _resolvedColor(context, 'focusColor'),
            hoverColor: _resolvedColor(context, 'hoverColor'),
            overlayColor: _checkboxStateColor(context, 'overlayColor'),
            splashRadius: _sliderNumber(node, 'splashRadius'),
            materialTapTargetSize: tapTarget,
            visualDensity: density,
            autofocus: _boolean('autofocus') ?? false,
            backgroundColor: _checkboxStateColor(context, 'backgroundColor'),
            side: _checkboxSide(context),
            innerRadius: innerRadius,
          );
  }

  Widget _rangeSlider(BuildContext context) => _TextButtonPreview(
    message: _rangeSliderReferenceMessage(node) ?? '',
    child: _RefreshLayoutObserver(
      builder: (context, constraints) {
        final geometry = _rangeSliderGeometryMessage(
          node,
          context,
          constraints,
        );
        if (geometry != null) {
          return _customClipperPreviewUnavailable(
            widgetName: 'RangeSlider',
            expectedType: 'safe SDK range slider geometry',
            previewLabel: 'RangeSlider\npreview unavailable',
            messageOverride: geometry,
          );
        }
        final cursorEntries = <WidgetStatesConstraint, MouseCursor?>{};
        for (final entry in _checkboxStateLayers.entries) {
          final name = 'mouseCursor${entry.value}';
          if (node.properties.containsKey(name)) {
            cursorEntries[entry.key] = _mouseCursor(name);
          }
        }
        if (node.properties.containsKey('mouseCursorDefault')) {
          cursorEntries[WidgetState.any] = _mouseCursor('mouseCursorDefault');
        }
        // Resolve only the outer property. Returned WidgetStateMouseCursor
        // presets retain the SDK's own cursor-session semantics.
        return Listener(
          behavior: HitTestBehavior.translucent,
          onPointerDown: (_) => onSelected(node.id),
          child: RangeSlider(
            values: RangeValues(
              _sliderNumber(node, 'valuesStart')!,
              _sliderNumber(node, 'valuesEnd')!,
            ),
            onChanged: _boolean('enabled') == false
                ? null
                : (_) => onSelected(node.id),
            onChangeStart: node.properties.containsKey('onChangeStart')
                ? (_) {}
                : null,
            onChangeEnd: node.properties.containsKey('onChangeEnd')
                ? (_) {}
                : null,
            min: _sliderNumber(node, 'min') ?? 0,
            max: _sliderNumber(node, 'max') ?? 1,
            divisions: _integer('divisions'),
            labels:
                node.properties.containsKey('labelsStart') ||
                    node.properties.containsKey('labelsEnd')
                ? RangeLabels(
                    _string('labelsStart') ?? '',
                    _string('labelsEnd') ?? '',
                  )
                : null,
            activeColor: _resolvedColor(context, 'activeColor'),
            inactiveColor: _resolvedColor(context, 'inactiveColor'),
            overlayColor: _checkboxStateColor(context, 'overlayColor'),
            mouseCursor: cursorEntries.isEmpty
                ? null
                : WidgetStateProperty<MouseCursor?>.fromMap(cursorEntries),
            padding: _edgeInsetsGeometry('padding'),
            // ignore: deprecated_member_use
            year2023: _boolean('year2023'),
          ),
        );
      },
    ),
  );

  Widget _slider(BuildContext context) => _SliderPreview(
    cupertino: _checkboxUsesCupertino(node, context),
    message: _sliderReferenceMessage(node, context) ?? '',
    builder: (focusNode) => _RefreshLayoutObserver(
      builder: (context, constraints) =>
          _buildSlider(context, focusNode, constraints),
    ),
  );

  Widget _buildSlider(
    BuildContext context,
    FocusNode focusNode,
    BoxConstraints constraints,
  ) {
    final geometry = _sliderGeometryMessage(node, context, constraints);
    if (geometry != null) {
      return _customClipperPreviewUnavailable(
        widgetName: 'Slider',
        expectedType: 'safe SDK slider geometry',
        previewLabel: 'Slider\npreview unavailable',
        messageOverride: geometry,
      );
    }
    final adaptive = _string('variant') == 'adaptive';
    final allowed = switch (_enum('allowedInteraction')) {
      'tapAndSlide' => SliderInteraction.tapAndSlide,
      'tapOnly' => SliderInteraction.tapOnly,
      'slideOnly' => SliderInteraction.slideOnly,
      'slideThumb' => SliderInteraction.slideThumb,
      _ => null,
    };
    final indicator = switch (_enum('showValueIndicator')) {
      'onlyForDiscrete' => ShowValueIndicator.onlyForDiscrete,
      'onlyForContinuous' => ShowValueIndicator.onlyForContinuous,
      // Preserve the SDK deprecated enum member as a distinct stored value.
      // ignore: deprecated_member_use
      'always' => ShowValueIndicator.always,
      'onDrag' => ShowValueIndicator.onDrag,
      'alwaysVisible' => ShowValueIndicator.alwaysVisible,
      'never' => ShowValueIndicator.never,
      _ => null,
    };
    final Widget control = adaptive
        ? Slider.adaptive(
            key: ValueKey(_checkboxUsesCupertino(node, context)),
            value: _sliderNumber(node, 'value')!,
            secondaryTrackValue: _sliderNumber(node, 'secondaryTrackValue'),
            onChanged: _boolean('enabled') == false
                ? null
                : (_) => onSelected(node.id),
            onChangeStart: node.properties.containsKey('onChangeStart')
                ? (_) {}
                : null,
            onChangeEnd: node.properties.containsKey('onChangeEnd')
                ? (_) {}
                : null,
            min: _sliderNumber(node, 'min') ?? 0,
            max: _sliderNumber(node, 'max') ?? 1,
            divisions: _integer('divisions'),
            label: _string('label'),
            activeColor: _resolvedColor(context, 'activeColor'),
            inactiveColor: _resolvedColor(context, 'inactiveColor'),
            secondaryActiveColor: _resolvedColor(
              context,
              'secondaryActiveColor',
            ),
            thumbColor: _resolvedColor(context, 'thumbColor'),
            overlayColor: _checkboxStateColor(context, 'overlayColor'),
            mouseCursor: _mouseCursor('mouseCursor'),
            focusNode: focusNode,
            autofocus: _boolean('autofocus') ?? false,
            allowedInteraction: allowed,
            showValueIndicator: indicator,
            // ignore: deprecated_member_use
            year2023: _boolean('year2023'),
          )
        : Slider(
            key: ValueKey(_checkboxUsesCupertino(node, context)),
            value: _sliderNumber(node, 'value')!,
            secondaryTrackValue: _sliderNumber(node, 'secondaryTrackValue'),
            onChanged: _boolean('enabled') == false
                ? null
                : (_) => onSelected(node.id),
            onChangeStart: node.properties.containsKey('onChangeStart')
                ? (_) {}
                : null,
            onChangeEnd: node.properties.containsKey('onChangeEnd')
                ? (_) {}
                : null,
            min: _sliderNumber(node, 'min') ?? 0,
            max: _sliderNumber(node, 'max') ?? 1,
            divisions: _integer('divisions'),
            label: _string('label'),
            activeColor: _resolvedColor(context, 'activeColor'),
            inactiveColor: _resolvedColor(context, 'inactiveColor'),
            secondaryActiveColor: _resolvedColor(
              context,
              'secondaryActiveColor',
            ),
            thumbColor: _resolvedColor(context, 'thumbColor'),
            overlayColor: _checkboxStateColor(context, 'overlayColor'),
            mouseCursor: _mouseCursor('mouseCursor'),
            focusNode: focusNode,
            autofocus: _boolean('autofocus') ?? false,
            allowedInteraction: allowed,
            padding: _edgeInsetsGeometry('padding'),
            showValueIndicator: indicator,
            // ignore: deprecated_member_use
            year2023: _boolean('year2023'),
          );
    return Listener(
      behavior: HitTestBehavior.translucent,
      onPointerDown: (_) => onSelected(node.id),
      child: control,
    );
  }

  Widget _switch(BuildContext context) => _SwitchPreview(
    cupertino: _checkboxUsesCupertino(node, context),
    message: _switchPreviewMessage(node, context) ?? '',
    builder: (focusNode) => _RefreshLayoutObserver(
      builder: (context, constraints) =>
          _buildSwitch(context, focusNode, constraints),
    ),
  );

  Widget _buildSwitch(
    BuildContext context,
    FocusNode focusNode,
    BoxConstraints constraints,
  ) {
    final paddingMessage = _switchPaddingMessage(node, context, constraints);
    if (paddingMessage != null) {
      return _customClipperPreviewUnavailable(
        widgetName: 'Switch.padding',
        expectedType: 'finite layout extent',
        previewLabel: 'Switch padding\npreview unavailable',
        messageOverride: paddingMessage,
      );
    }
    ({ImageProvider<Object>? provider, ImageErrorListener? onError}) image(
      String name,
    ) {
      final value = node.properties[name]?.value;
      if (value is! CanvasImageProviderValue) {
        return (provider: null, onError: null);
      }
      final binding = _imageProvider(value);
      // Keep the actual SDK thumb/color/icon fallback rather than cover it
      // with a synthetic image. Property-specific resource status is exposed.
      if (binding.placeholder) return (provider: null, onError: null);
      final resource = binding.resolution as CanvasResolvedImageValue;
      return (
        provider: binding.provider,
        onError: (error, stack) =>
            onImageError?.call(resource.resourceId, error, stack),
      );
    }

    final active = image('activeThumbImage');
    final inactive = image('inactiveThumbImage');
    final control = _string('variant') == 'adaptive'
        ? Switch.adaptive(
            // Public shell boundary only: SDK 3.44.8 never clears its
            // Cupertino painter flag. The outer owner preserves focus.
            key: ValueKey(_checkboxUsesCupertino(node, context)),
            focusNode: focusNode,
            value: _boolean('value')!,
            onChanged: _boolean('enabled') == false
                ? null
                : (_) => onSelected(node.id),
            // Preserve the SDK's still-supported legacy color alias exactly.
            // ignore: deprecated_member_use
            activeColor: _resolvedColor(context, 'activeColor'),
            activeThumbColor: _resolvedColor(context, 'activeThumbColor'),
            activeTrackColor: _resolvedColor(context, 'activeTrackColor'),
            inactiveThumbColor: _resolvedColor(context, 'inactiveThumbColor'),
            inactiveTrackColor: _resolvedColor(context, 'inactiveTrackColor'),
            activeThumbImage: active.provider,
            onActiveThumbImageError: active.onError,
            inactiveThumbImage: inactive.provider,
            onInactiveThumbImageError: inactive.onError,
            thumbColor: _checkboxStateColor(context, 'thumbColor'),
            trackColor: _checkboxStateColor(context, 'trackColor'),
            trackOutlineColor: _checkboxStateColor(
              context,
              'trackOutlineColor',
            ),
            trackOutlineWidth: _switchStateWidth(context),
            thumbIcon: _switchStateIcon(context),
            materialTapTargetSize: switch (_enum('materialTapTargetSize')) {
              'padded' => MaterialTapTargetSize.padded,
              'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
              _ => null,
            },
            dragStartBehavior: _dragStartBehavior(),
            mouseCursor: _mouseCursor('mouseCursor'),
            focusColor: _resolvedColor(context, 'focusColor'),
            hoverColor: _resolvedColor(context, 'hoverColor'),
            overlayColor: _checkboxStateColor(context, 'overlayColor'),
            splashRadius: node.properties['splashRadius']?.kind == 'enum'
                ? double.infinity
                : _number('splashRadius'),
            onFocusChange: node.properties.containsKey('onFocusChange')
                ? (_) {}
                : null,
            autofocus: _boolean('autofocus') ?? false,
            padding: _edgeInsetsGeometry('padding'),
            applyCupertinoTheme: _boolean('applyCupertinoTheme'),
          )
        : Switch(
            key: ValueKey(_checkboxUsesCupertino(node, context)),
            focusNode: focusNode,
            value: _boolean('value')!,
            onChanged: _boolean('enabled') == false
                ? null
                : (_) => onSelected(node.id),
            // Preserve the SDK's still-supported legacy color alias exactly.
            // ignore: deprecated_member_use
            activeColor: _resolvedColor(context, 'activeColor'),
            activeThumbColor: _resolvedColor(context, 'activeThumbColor'),
            activeTrackColor: _resolvedColor(context, 'activeTrackColor'),
            inactiveThumbColor: _resolvedColor(context, 'inactiveThumbColor'),
            inactiveTrackColor: _resolvedColor(context, 'inactiveTrackColor'),
            activeThumbImage: active.provider,
            onActiveThumbImageError: active.onError,
            inactiveThumbImage: inactive.provider,
            onInactiveThumbImageError: inactive.onError,
            thumbColor: _checkboxStateColor(context, 'thumbColor'),
            trackColor: _checkboxStateColor(context, 'trackColor'),
            trackOutlineColor: _checkboxStateColor(
              context,
              'trackOutlineColor',
            ),
            trackOutlineWidth: _switchStateWidth(context),
            thumbIcon: _switchStateIcon(context),
            materialTapTargetSize: switch (_enum('materialTapTargetSize')) {
              'padded' => MaterialTapTargetSize.padded,
              'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
              _ => null,
            },
            dragStartBehavior: _dragStartBehavior(),
            mouseCursor: _mouseCursor('mouseCursor'),
            focusColor: _resolvedColor(context, 'focusColor'),
            hoverColor: _resolvedColor(context, 'hoverColor'),
            overlayColor: _checkboxStateColor(context, 'overlayColor'),
            splashRadius: node.properties['splashRadius']?.kind == 'enum'
                ? double.infinity
                : _number('splashRadius'),
            onFocusChange: node.properties.containsKey('onFocusChange')
                ? (_) {}
                : null,
            autofocus: _boolean('autofocus') ?? false,
            padding: _edgeInsetsGeometry('padding'),
          );
    return control;
  }

  Widget _checkbox(BuildContext context) {
    final create = _string('variant') == 'adaptive'
        ? Checkbox.adaptive
        : Checkbox.new;
    final horizontal = _number('visualDensityHorizontal');
    final vertical = _number('visualDensityVertical');
    final shapeUnavailable =
        _cardShapePreviewUnavailableMessage(node, widgetName: 'Checkbox') !=
        null;
    final missingDirection =
        _checkboxShapeDirectionMessage(node, context) != null;
    final control = create(
      value: _boolean('value'),
      tristate: _boolean('tristate') ?? false,
      onChanged: _boolean('enabled') == false ? null : (_) {},
      mouseCursor: _mouseCursor('mouseCursor'),
      activeColor: _resolvedColor(context, 'activeColor'),
      fillColor: _checkboxStateColor(context, 'fillColor'),
      checkColor: _resolvedColor(context, 'checkColor'),
      focusColor: _resolvedColor(context, 'focusColor'),
      hoverColor: _resolvedColor(context, 'hoverColor'),
      overlayColor: _checkboxStateColor(context, 'overlayColor'),
      splashRadius: node.properties['splashRadius']?.kind == 'enum'
          ? double.infinity
          : _number('splashRadius'),
      materialTapTargetSize: switch (_enum('materialTapTargetSize')) {
        'padded' => MaterialTapTargetSize.padded,
        'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
        _ => null,
      },
      visualDensity: horizontal == null && vertical == null
          ? null
          : VisualDensity(horizontal: horizontal ?? 0, vertical: vertical ?? 0),
      autofocus: _boolean('autofocus') ?? false,
      shape: missingDirection
          ? RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(
                _checkboxUsesCupertino(node, context)
                    ? 4
                    : Theme.of(context).useMaterial3
                    ? 2
                    : 1,
              ),
            )
          : shapeUnavailable
          ? null
          : _cardShape(context) as OutlinedBorder?,
      side: _checkboxSide(context),
      isError: _boolean('isError') ?? false,
      semanticLabel: _string('semanticLabel'),
    );
    return _TextButtonPreview(
      message: _checkboxPreviewMessage(node, context) ?? '',
      child: control,
    );
  }

  Widget _iconButton(BuildContext context) => _RefreshLayoutObserver(
    builder: (context, constraints) => _buildIconButton(context, constraints),
  );

  Widget _buildIconButton(BuildContext context, BoxConstraints constraints) {
    final material3 = Theme.of(context).useMaterial3;
    final style = material3 ? _elevatedButtonStyle(context) : null;
    final geometryMessage =
        _iconButtonConstraintsMessage(context, constraints, style) ??
        _iconButtonM2GeometryMessage(node, context, constraints);
    if (geometryMessage != null) {
      return _customClipperPreviewUnavailable(
        widgetName: 'IconButton',
        expectedType: 'valid button geometry',
        previewLabel: 'Button geometry\npreview unavailable',
        messageOverride: geometryMessage,
        preservedChild: material3 && _boolean('isSelected') == true
            ? _single('selectedIcon') ?? _single('icon')
            : _single('icon'),
      );
    }
    final enabled = _boolean('enabled') ?? true;
    final longPressed = _buttonReferencePresent('onLongPress');
    final VoidCallback? onPressed = enabled ? () {} : null;
    final VoidCallback? onLongPress = enabled && longPressed ? () {} : null;
    final ValueChanged<bool>? onHover = _buttonReferencePresent('onHover')
        ? (_) {}
        : null;
    final icon = _single('icon')!;
    final selectedIcon = _single('selectedIcon');
    final visualDensity = _iconButtonDirectDensity(context);
    final message = _iconButtonPreviewMessage(node, context) ?? '';
    final button = switch (_string('variant')) {
      'filled' => IconButton.filled(
        iconSize: _iconButtonNumber('iconSize'),
        visualDensity: visualDensity,
        padding: _edgeInsetsGeometry('padding'),
        alignment: _alignmentGeometry('alignment'),
        splashRadius: _iconButtonNumber('splashRadius'),
        color: _resolvedColor(context, 'color'),
        focusColor: _resolvedColor(context, 'focusColor'),
        hoverColor: _resolvedColor(context, 'hoverColor'),
        highlightColor: _resolvedColor(context, 'highlightColor'),
        splashColor: _resolvedColor(context, 'splashColor'),
        disabledColor: _resolvedColor(context, 'disabledColor'),
        onPressed: onPressed,
        onHover: onHover,
        onLongPress: onLongPress,
        mouseCursor: _mouseCursor('mouseCursor'),
        autofocus: _boolean('autofocus') ?? false,
        tooltip: _string('tooltip'),
        enableFeedback: _boolean('enableFeedback'),
        constraints: _boxConstraints('constraints'),
        style: style,
        isSelected: _boolean('isSelected'),
        selectedIcon: selectedIcon,
        icon: icon,
      ),
      'filledTonal' => IconButton.filledTonal(
        iconSize: _iconButtonNumber('iconSize'),
        visualDensity: visualDensity,
        padding: _edgeInsetsGeometry('padding'),
        alignment: _alignmentGeometry('alignment'),
        splashRadius: _iconButtonNumber('splashRadius'),
        color: _resolvedColor(context, 'color'),
        focusColor: _resolvedColor(context, 'focusColor'),
        hoverColor: _resolvedColor(context, 'hoverColor'),
        highlightColor: _resolvedColor(context, 'highlightColor'),
        splashColor: _resolvedColor(context, 'splashColor'),
        disabledColor: _resolvedColor(context, 'disabledColor'),
        onPressed: onPressed,
        onHover: onHover,
        onLongPress: onLongPress,
        mouseCursor: _mouseCursor('mouseCursor'),
        autofocus: _boolean('autofocus') ?? false,
        tooltip: _string('tooltip'),
        enableFeedback: _boolean('enableFeedback'),
        constraints: _boxConstraints('constraints'),
        style: style,
        isSelected: _boolean('isSelected'),
        selectedIcon: selectedIcon,
        icon: icon,
      ),
      'outlined' => IconButton.outlined(
        iconSize: _iconButtonNumber('iconSize'),
        visualDensity: visualDensity,
        padding: _edgeInsetsGeometry('padding'),
        alignment: _alignmentGeometry('alignment'),
        splashRadius: _iconButtonNumber('splashRadius'),
        color: _resolvedColor(context, 'color'),
        focusColor: _resolvedColor(context, 'focusColor'),
        hoverColor: _resolvedColor(context, 'hoverColor'),
        highlightColor: _resolvedColor(context, 'highlightColor'),
        splashColor: _resolvedColor(context, 'splashColor'),
        disabledColor: _resolvedColor(context, 'disabledColor'),
        onPressed: onPressed,
        onHover: onHover,
        onLongPress: onLongPress,
        mouseCursor: _mouseCursor('mouseCursor'),
        autofocus: _boolean('autofocus') ?? false,
        tooltip: _string('tooltip'),
        enableFeedback: _boolean('enableFeedback'),
        constraints: _boxConstraints('constraints'),
        style: style,
        isSelected: _boolean('isSelected'),
        selectedIcon: selectedIcon,
        icon: icon,
      ),
      _ => IconButton(
        iconSize: _iconButtonNumber('iconSize'),
        visualDensity: visualDensity,
        padding: _edgeInsetsGeometry('padding'),
        alignment: _alignmentGeometry('alignment'),
        splashRadius: _iconButtonNumber('splashRadius'),
        color: _resolvedColor(context, 'color'),
        focusColor: _resolvedColor(context, 'focusColor'),
        hoverColor: _resolvedColor(context, 'hoverColor'),
        highlightColor: _resolvedColor(context, 'highlightColor'),
        splashColor: _resolvedColor(context, 'splashColor'),
        disabledColor: _resolvedColor(context, 'disabledColor'),
        onPressed: onPressed,
        onHover: onHover,
        onLongPress: onLongPress,
        mouseCursor: _mouseCursor('mouseCursor'),
        autofocus: _boolean('autofocus') ?? false,
        tooltip: _string('tooltip'),
        enableFeedback: _boolean('enableFeedback'),
        constraints: _boxConstraints('constraints'),
        style: style,
        isSelected: _boolean('isSelected'),
        selectedIcon: selectedIcon,
        icon: icon,
      ),
    };
    return _TextButtonPreview(message: message, child: button);
  }

  String? _iconButtonConstraintsMessage(
    BuildContext context,
    BoxConstraints incoming,
    ButtonStyle? local,
  ) {
    if (incoming.hasBoundedWidth && incoming.hasBoundedHeight) return null;
    final direct = _boxConstraints('constraints');
    final material3 = Theme.of(context).useMaterial3;
    final theme = material3 ? _iconButtonThemeStyle(context) : null;
    final minimum =
        local?.minimumSize ??
        (direct == null
            ? null
            : WidgetStatePropertyAll(Size(direct.minWidth, direct.minHeight)));
    final disabled = _boolean('enabled') == false;
    // Only local SDK-reachable states: no external controller/project code is
    // executed. A disabled button cannot start a hover/focus/press ink cycle.
    for (var bits = 0; bits < (material3 && !disabled ? 8 : 1); bits++) {
      final states = <WidgetState>{
        if (disabled) WidgetState.disabled,
        if (_boolean('isSelected') == true) WidgetState.selected,
        if ((bits & 1) != 0) WidgetState.hovered,
        if ((bits & 2) != 0) WidgetState.focused,
        if ((bits & 4) != 0) WidgetState.pressed,
      };
      final size =
          minimum?.resolve(states) ??
          theme?.minimumSize?.resolve(states) ??
          Size.square(material3 ? 40 : 48);
      if ((!incoming.hasBoundedWidth && !size.width.isFinite) ||
          (!incoming.hasBoundedHeight && !size.height.isFinite)) {
        return 'Render IconButton ${node.id}: constraints preview unavailable because the resolved minimum size has an infinite unbounded layout axis. Stored values and generated Dart are unchanged.';
      }
    }
    return null;
  }

  _CanvasNodeView _withProperties(Map<String, CanvasValue> properties) =>
      _CanvasNodeView(
        node: CanvasNode(
          id: node.id,
          type: node.type,
          properties: properties,
          slots: node.slots,
        ),
        imageResources: imageResources,
        onImageError: onImageError,
        selectedWidgetId: selectedWidgetId,
        onSelected: onSelected,
        nodeKey: nodeKey,
        designerFocusParent: designerFocusParent,
        overlayScale: overlayScale,
        inlineTextEditEnabled: inlineTextEditEnabled,
        inlineTextEditingWidgetId: inlineTextEditingWidgetId,
        onBeginInlineTextEdit: onBeginInlineTextEdit,
        onCommitInlineTextEdit: onCommitInlineTextEdit,
        onCancelInlineTextEdit: onCancelInlineTextEdit,
        suppressDesignerSemantics: suppressDesignerSemantics,
      );

  MenuStyle? _submenuMenuStyle(BuildContext context) {
    final projected = _withProperties({
      for (final entry in node.properties.entries)
        if (entry.key.startsWith('menuStyle'))
          'style${entry.key.substring(9)}': entry.value,
    });
    final style = projected._menuAnchorStyle(context);
    final localPadding = style?.padding;
    if (localPadding == null) return style;
    // Native SubmenuButton resolves the chosen padding property once and
    // force-unwraps it before building the menu. Sparse Designer locals retry
    // MenuTheme then the reviewed native vertical-8 default, as generated Dart.
    return style!.copyWith(
      padding: WidgetStateProperty.resolveWith(
        (states) =>
            localPadding.resolve(states) ??
            MenuTheme.of(context).style?.padding?.resolve(states) ??
            const EdgeInsetsDirectional.symmetric(vertical: 8),
      ),
    );
  }

  WidgetStateProperty<Widget?>? _submenuIcon() {
    const buckets = {
      WidgetState.disabled: 'Disabled',
      WidgetState.hovered: 'Hovered',
      WidgetState.focused: 'Focused',
    };
    if (!node.properties.keys.any(
      (name) => name != 'submenuIcon' && name.startsWith('submenuIcon'),
    )) {
      return null;
    }
    return WidgetStateProperty.resolveWith((states) {
      CanvasValue? value;
      for (final entry in buckets.entries) {
        if (states.contains(entry.key) &&
            node.properties.containsKey('submenuIcon${entry.value}')) {
          value = node.properties['submenuIcon${entry.value}'];
          break;
        }
      }
      value ??= node.properties['submenuIconDefault'];
      if (value?.kind != 'iconData') return null;
      final data = value!.value as CanvasIconDataValue;
      return Icon(
        data.codePoint == null
            ? null
            : IconData(
                // Closed reviewed icon data; no project widget expressions are run.
                // ignore: non_const_argument_for_const_parameter
                data.codePoint!,
                // ignore: non_const_argument_for_const_parameter
                fontFamily: data.fontFamily,
                // ignore: non_const_argument_for_const_parameter
                fontPackage: data.fontPackage,
                matchTextDirection: data.matchTextDirection,
                fontFamilyFallback: data.fontFamilyFallback.isEmpty
                    ? null
                    : data.fontFamilyFallback,
              ),
      );
    });
  }

  MenuStyle? _menuAnchorStyle(BuildContext context) {
    if (!node.properties.keys.any(
      (name) => name.startsWith('style') && name != 'style',
    )) {
      return null;
    }
    // MenuAnchor has its own MenuTheme and pinned defaults. Only generic
    // sparse-state/compound assembly is shared with buttons, never ButtonStyle.
    final theme = MenuTheme.of(context).style;
    const defaultShape = WidgetStatePropertyAll<OutlinedBorder>(
      RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(4)),
      ),
    );
    final sizes = _buttonConstraintSizeStateProperties(
      themeMinimum: theme?.minimumSize,
      defaultMinimum: null,
      themeMaximum: theme?.maximumSize,
      defaultMaximum: null,
    );
    return MenuStyle(
      backgroundColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}BackgroundColor'),
      ),
      shadowColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}ShadowColor'),
      ),
      surfaceTintColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}SurfaceTintColor'),
      ),
      elevation: _buttonStateProperty<double>(
        (prefix) => _number('${prefix}Elevation'),
      ),
      padding: _buttonStateProperty<EdgeInsetsGeometry>(
        (prefix) => _edgeInsetsGeometry('${prefix}Padding'),
      ),
      minimumSize: sizes.minimum,
      maximumSize: sizes.maximum,
      fixedSize: _buttonSizeStateProperty(
        'Fixed',
        missingDimension: double.infinity,
        themeValue: theme?.fixedSize,
        defaultValue: null,
      ),
      side: _buttonBorderSideStateProperty(
        context,
        theme?.side,
        null,
        theme?.shape,
        defaultShape,
      ),
      shape: _buttonShapeStateProperty(context, theme?.shape, defaultShape),
      mouseCursor: _buttonStateProperty<MouseCursor>(
        (prefix) => _mouseCursor('${prefix}MouseCursor'),
      ),
      visualDensity:
          _number('styleVisualDensityHorizontal') == null &&
              _number('styleVisualDensityVertical') == null
          ? null
          : VisualDensity(
              horizontal: _number('styleVisualDensityHorizontal') ?? 0,
              vertical: _number('styleVisualDensityVertical') ?? 0,
            ),
      alignment: _buttonAlignment(),
    );
  }

  Widget _menuItemButton(BuildContext context) {
    final trigger = _enum('shortcutTrigger');
    final character = _string('shortcutCharacter');
    // CharacterActivator accepts any String, but the pinned native menu hint
    // serializer requires exactly one UTF-16 code unit. Keep the model exact
    // and preserve the native button when that hint cannot be rendered.
    final unavailableCharacter = character != null && character.length != 1;
    final MenuSerializableShortcut? shortcut = trigger != null
        ? SingleActivator(
            LogicalKeyboardKey.findKeyByKeyId(
              canvasMenuShortcutKeyIds[trigger]!,
            )!,
            control: _boolean('shortcutControl') ?? false,
            shift: _boolean('shortcutShift') ?? false,
            alt: _boolean('shortcutAlt') ?? false,
            meta: _boolean('shortcutMeta') ?? false,
            numLock: switch (_enum('shortcutNumLock')) {
              'locked' => LockState.locked,
              'unlocked' => LockState.unlocked,
              _ => LockState.ignored,
            },
            includeRepeats: _boolean('shortcutIncludeRepeats') ?? true,
          )
        : character != null && !unavailableCharacter
        ? CharacterActivator(
            character,
            control: _boolean('shortcutControl') ?? false,
            alt: _boolean('shortcutAlt') ?? false,
            meta: _boolean('shortcutMeta') ?? false,
            includeRepeats: _boolean('shortcutIncludeRepeats') ?? true,
          )
        : null;
    return _MenuItemPreview(
      node: node,
      message: [
        ?_textButtonReferenceMessage(node),
        if (unavailableCharacter)
          'MenuItemButton shortcut preview unavailable: the pinned SDK menu hint serializer requires exactly one UTF-16 code unit. The stored CharacterActivator string is unchanged; only its preview hint is omitted.',
      ].join('\n'),
      child: _MenuItemAnchorScope(
        child: MenuItemButton(
          onPressed: (_boolean('enabled') ?? true) ? () {} : null,
          onHover: _buttonReferencePresent('onHover') ? (_) {} : null,
          requestFocusOnHover: _boolean('requestFocusOnHover') ?? true,
          onFocusChange: _buttonReferencePresent('onFocusChange')
              ? (_) {}
              : null,
          autofocus: _boolean('autofocus') ?? false,
          shortcut: shortcut,
          semanticsLabel: _string('semanticsLabel'),
          style: _elevatedButtonStyle(context),
          clipBehavior: _clipBehavior() ?? Clip.none,
          leadingIcon: _single('leadingIcon'),
          trailingIcon: _single('trailingIcon'),
          closeOnActivate: _boolean('closeOnActivate') ?? true,
          overflowAxis: _enum('overflowAxis') == 'vertical'
              ? Axis.vertical
              : Axis.horizontal,
          child: _single('child'),
        ),
      ),
    );
  }

  Widget _textButton(BuildContext context) {
    final enabled = _boolean('enabled') ?? true;
    final pressed = _buttonReferencePresent('onPressed');
    final longPressed = _buttonReferencePresent('onLongPress');
    final onPressed = enabled && (pressed || !longPressed) ? () {} : null;
    final onLongPress = enabled && longPressed ? () {} : null;
    final style = _elevatedButtonStyle(context);
    final child = _single('child');
    final iconVariant = _buttonIconVariant;
    final clip = node.properties['clipBehavior'];
    final clipBehavior = clip == null
        ? (_isFilledButton || (iconVariant && !_isOutlinedButton)
              ? Clip.none
              : null)
        : clip.kind == 'null'
        ? null
        : _clipBehavior();
    if (_isFilledButton) {
      return _textButtonPreview(
        _filledButton(
          onPressed: onPressed,
          onLongPress: onLongPress,
          onHover: _buttonReferencePresent('onHover') ? (_) {} : null,
          onFocusChange: _buttonReferencePresent('onFocusChange')
              ? (_) {}
              : null,
          autofocus: _boolean('autofocus') ?? false,
          clipBehavior: clipBehavior,
          style: style,
          icon: _single('icon'),
          child: child,
          iconAlignment: _buttonIconAlignment('iconAlignment'),
        ),
      );
    }
    if (_isOutlinedButton) {
      return _textButtonPreview(
        iconVariant
            ? OutlinedButton.icon(
                onPressed: onPressed,
                onLongPress: onLongPress,
                onHover: _buttonReferencePresent('onHover') ? (_) {} : null,
                onFocusChange: _buttonReferencePresent('onFocusChange')
                    ? (_) {}
                    : null,
                autofocus: _boolean('autofocus') ?? false,
                clipBehavior: clipBehavior,
                style: style,
                iconAlignment: _buttonIconAlignment('iconAlignment'),
                icon: _single('icon'),
                label: child!,
              )
            : OutlinedButton(
                onPressed: onPressed,
                onLongPress: onLongPress,
                onHover: _buttonReferencePresent('onHover') ? (_) {} : null,
                onFocusChange: _buttonReferencePresent('onFocusChange')
                    ? (_) {}
                    : null,
                autofocus: _boolean('autofocus') ?? false,
                clipBehavior: clipBehavior,
                style: style,
                child: child!,
              ),
      );
    }
    if (iconVariant) {
      return _textButtonPreview(
        TextButton.icon(
          onPressed: onPressed,
          onLongPress: onLongPress,
          onHover: _buttonReferencePresent('onHover') ? (_) {} : null,
          onFocusChange: _buttonReferencePresent('onFocusChange')
              ? (_) {}
              : null,
          autofocus: _boolean('autofocus') ?? false,
          clipBehavior: clipBehavior,
          style: style,
          iconAlignment: _buttonIconAlignment('iconAlignment'),
          icon: _single('icon'),
          label: child!,
        ),
      );
    }
    return _textButtonPreview(
      TextButton(
        onPressed: onPressed,
        onLongPress: onLongPress,
        onHover: _buttonReferencePresent('onHover') ? (_) {} : null,
        onFocusChange: _buttonReferencePresent('onFocusChange') ? (_) {} : null,
        autofocus: _boolean('autofocus') ?? false,
        clipBehavior: clipBehavior,
        isSemanticButton: node.properties.containsKey('isSemanticButton')
            ? _boolean('isSemanticButton')
            : true,
        style: style,
        child: child!,
      ),
    );
  }

  Widget _textButtonPreview(Widget button) {
    final message = _textButtonReferenceMessage(node) ?? '';
    return _TextButtonPreview(message: message, child: button);
  }

  FilledButton _filledButton({
    required VoidCallback? onPressed,
    VoidCallback? onLongPress,
    ValueChanged<bool>? onHover,
    ValueChanged<bool>? onFocusChange,
    bool autofocus = false,
    Clip? clipBehavior = Clip.none,
    ButtonStyle? style,
    Widget? icon,
    required Widget? child,
    IconAlignment? iconAlignment,
  }) => switch (_string('variant')) {
    'icon' => FilledButton.icon(
      onPressed: onPressed,
      onLongPress: onLongPress,
      onHover: onHover,
      onFocusChange: onFocusChange,
      autofocus: autofocus,
      clipBehavior: clipBehavior,
      style: style,
      icon: icon,
      label: child!,
      iconAlignment: iconAlignment,
    ),
    'tonalIcon' => FilledButton.tonalIcon(
      onPressed: onPressed,
      onLongPress: onLongPress,
      onHover: onHover,
      onFocusChange: onFocusChange,
      autofocus: autofocus,
      clipBehavior: clipBehavior,
      style: style,
      icon: icon,
      label: child!,
      iconAlignment: iconAlignment,
    ),
    'tonal' => FilledButton.tonal(
      onPressed: onPressed,
      onLongPress: onLongPress,
      onHover: onHover,
      onFocusChange: onFocusChange,
      autofocus: autofocus,
      clipBehavior: clipBehavior,
      style: style,
      child: child,
    ),
    _ => FilledButton(
      onPressed: onPressed,
      onLongPress: onLongPress,
      onHover: onHover,
      onFocusChange: onFocusChange,
      autofocus: autofocus,
      clipBehavior: clipBehavior,
      style: style,
      child: child,
    ),
  };

  ButtonStyle? _elevatedButtonStyle(BuildContext context) {
    // Project ButtonStyle code is never evaluated in the isolated runner.
    // The per-node diagnostic explicitly labels this as a default preview.
    if (_usesExtendedButtonStyle && _buttonReferencePresent('style')) {
      return null;
    }
    if ((_isMenuItemButton || _isSubmenuButton) &&
        node.properties['style']?.kind == 'null') {
      return null;
    }
    if (!node.properties.keys.any((name) => name.startsWith('style'))) {
      return null;
    }
    final themeStyle = (_isMenuItemButton || _isSubmenuButton)
        ? MenuButtonTheme.of(context).style
        : _isIconButton
        ? _iconButtonCompoundTheme(context)
        : _isFilledButton
        ? FilledButtonTheme.of(context).style
        : _isOutlinedButton
        ? OutlinedButtonTheme.of(context).style
        : _usesExtendedButtonStyle
        ? TextButtonTheme.of(context).style
        : ElevatedButtonTheme.of(context).style;
    final ButtonStyleButton defaultButton = _isFilledButton
        ? _filledButton(
            onPressed: () {},
            child: const SizedBox.shrink(),
            icon: (node.slot('icon')?.children.isNotEmpty ?? false)
                ? const SizedBox.shrink()
                : null,
          )
        : _isOutlinedButton
        ? _string('variant') == 'icon'
              ? OutlinedButton.icon(
                  onPressed: () {},
                  icon: (node.slot('icon')?.children.isNotEmpty ?? false)
                      ? const SizedBox.shrink()
                      : null,
                  label: const SizedBox.shrink(),
                )
              : OutlinedButton(onPressed: () {}, child: const SizedBox.shrink())
        : !_usesExtendedButtonStyle
        ? ElevatedButton(onPressed: () {}, child: null)
        : _string('variant') == 'icon'
        ? TextButton.icon(
            onPressed: () {},
            icon: (node.slot('icon')?.children.isNotEmpty ?? false)
                ? const SizedBox.shrink()
                : null,
            label: const SizedBox.shrink(),
          )
        : TextButton(onPressed: () {}, child: const SizedBox.shrink());
    final defaultStyle = _isMenuItemButton
        ? const MenuItemButton().defaultStyleOf(context)
        : _isSubmenuButton
        ? const SubmenuButton(
            menuChildren: [],
            child: null,
          ).defaultStyleOf(context)
        : _isIconButton
        ? _iconButtonCompoundDefaults(context, _string('variant') ?? 'standard')
        : switch (defaultButton) {
            FilledButton button => button.defaultStyleOf(context),
            OutlinedButton button => button.defaultStyleOf(context),
            TextButton button => button.defaultStyleOf(context),
            _ => (defaultButton as ElevatedButton).defaultStyleOf(context),
          };
    final constraintSizes = _buttonConstraintSizeStateProperties(
      themeMinimum: themeStyle?.minimumSize,
      defaultMinimum: defaultStyle.minimumSize,
      themeMaximum: themeStyle?.maximumSize,
      defaultMaximum: defaultStyle.maximumSize,
    );
    return ButtonStyle(
      textStyle: _buttonTextStyleStateProperty(
        context,
        themeStyle?.textStyle,
        defaultStyle.textStyle,
      ),
      backgroundColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}BackgroundColor'),
      ),
      foregroundColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}ForegroundColor'),
      ),
      overlayColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}OverlayColor'),
      ),
      shadowColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}ShadowColor'),
      ),
      surfaceTintColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}SurfaceTintColor'),
      ),
      elevation: _buttonStateProperty<double>(
        (prefix) => _number('${prefix}Elevation'),
      ),
      padding: _buttonStateProperty<EdgeInsetsGeometry>(
        (prefix) => _edgeInsetsGeometry('${prefix}Padding'),
      ),
      minimumSize: constraintSizes.minimum,
      fixedSize: _buttonSizeStateProperty(
        'Fixed',
        missingDimension: double.infinity,
        themeValue: themeStyle?.fixedSize,
        defaultValue: defaultStyle.fixedSize,
      ),
      maximumSize: constraintSizes.maximum,
      iconColor: _buttonStateProperty<Color>(
        (prefix) => _resolvedColor(context, '${prefix}IconColor'),
      ),
      iconSize: _buttonStateProperty<double>(
        (prefix) => _number('${prefix}IconSize'),
      ),
      side: _buttonBorderSideStateProperty(
        context,
        themeStyle?.side,
        defaultStyle.side,
        themeStyle?.shape,
        defaultStyle.shape,
      ),
      shape: _buttonShapeStateProperty(
        context,
        themeStyle?.shape,
        defaultStyle.shape,
      ),
      mouseCursor: _buttonStateProperty<MouseCursor>(
        (prefix) => _mouseCursor('${prefix}MouseCursor'),
      ),
      visualDensity: _buttonVisualDensity(
        themeStyle?.visualDensity,
        defaultStyle.visualDensity,
      ),
      tapTargetSize: _buttonTapTargetSize(),
      animationDuration: _buttonAnimationDuration(),
      enableFeedback: _boolean('styleEnableFeedback'),
      alignment: _buttonAlignment(),
      splashFactory: _buttonSplashFactory(),
      iconAlignment: _usesExtendedButtonStyle
          ? _buttonIconAlignment('styleIconAlignment')
          : null,
      backgroundBuilder: _buttonReferencePresent('styleBackgroundBuilder')
          ? (_, _, child) => child ?? const SizedBox.shrink()
          : null,
      foregroundBuilder: _buttonReferencePresent('styleForegroundBuilder')
          ? (_, _, child) => child ?? const SizedBox.shrink()
          : null,
    );
  }

  WidgetStateProperty<T?>? _buttonStateProperty<T>(
    T? Function(String prefix) resolve,
  ) {
    if (_usesExtendedButtonStyle) {
      final values = {
        for (final entry in _textButtonStateLayers.entries)
          entry.key: resolve(entry.value),
      };
      if (values.values.every((value) => value == null) &&
          resolve('style') == null) {
        return null;
      }
      return WidgetStateProperty<T?>.fromMap({
        WidgetState.disabled: values[WidgetState.disabled],
        for (final entry in values.entries)
          if (entry.key != WidgetState.disabled && entry.value != null)
            entry.key: entry.value,
        WidgetState.any: ?resolve('style'),
      });
    }
    final disabled = resolve('styleDisabled');
    final pressed = resolve('stylePressed');
    final hovered = resolve('styleHovered');
    final focused = resolve('styleFocused');
    final fallback = resolve('style');
    if (disabled == null &&
        pressed == null &&
        hovered == null &&
        focused == null &&
        fallback == null) {
      return null;
    }
    return WidgetStateProperty<T?>.fromMap(<WidgetStatesConstraint, T?>{
      WidgetState.disabled: disabled,
      WidgetState.pressed: ?pressed,
      WidgetState.hovered: ?hovered,
      WidgetState.focused: ?focused,
      WidgetState.any: ?fallback,
    });
  }

  WidgetStateProperty<TextStyle?>? _buttonTextStyleStateProperty(
    BuildContext context,
    WidgetStateProperty<TextStyle?>? themeValue,
    WidgetStateProperty<TextStyle?>? defaultValue,
  ) {
    if (!_buttonHasAnyStateGroup(
      (prefix) => _hasPropertyPrefix('${prefix}Text'),
    )) {
      return null;
    }
    return WidgetStateProperty.resolveWith<TextStyle?>((states) {
      final layers = _buttonActiveStateLayers(
        states,
        (candidate) => _hasPropertyPrefix('${candidate}Text'),
      );
      if (layers.isEmpty) {
        return null;
      }
      TextStyle? effective =
          themeValue?.resolve(states) ?? defaultValue?.resolve(states);
      for (final prefix in layers) {
        final local = _buttonLocalTextStyle(context, prefix, effective);
        effective = _mergeButtonTextStyle(
          effective,
          local,
          inheritExplicit:
              _themeTextStyle(context, '${prefix}TextTheme') != null ||
              node.properties.containsKey('${prefix}TextInherit'),
        );
        final package = _string('${prefix}TextPackage');
        if (package != null && effective != null) {
          effective = _buttonApplyTextPackage(effective, package);
        }
      }
      return effective;
    });
  }

  TextStyle? _buttonLocalTextStyle(
    BuildContext context,
    String prefix,
    TextStyle? inherited,
  ) {
    final textPrefix = '${prefix}Text';
    if (!_hasPropertyPrefix(textPrefix)) {
      return null;
    }
    final themeBase = _buttonNormalizedThemeTextStyle(
      _themeTextStyle(context, '${textPrefix}Theme'),
    );
    final hasOverrides = node.properties.keys.any(
      (name) => name.startsWith(textPrefix) && name != '${textPrefix}Theme',
    );
    if (!hasOverrides) {
      return themeBase;
    }
    final backgroundColor = _resolvedColor(
      context,
      '${textPrefix}BackgroundColor',
    );
    final configuredBackground = _paint(context, '${textPrefix}Background');
    final background =
        configuredBackground ??
        (backgroundColor == null ? null : (Paint()..color = backgroundColor));
    final explicitInherit = _boolean('${textPrefix}Inherit');
    final structuredBase = _mergeButtonTextStyle(
      explicitInherit == false ? null : inherited,
      themeBase,
      inheritExplicit: themeBase != null,
    );
    final familyName = '${textPrefix}FontFamily';
    final fallbackName = '${textPrefix}FontFamilyFallback';
    final fontFamily = node.properties.containsKey(familyName)
        ? _string(familyName)
        : null;
    final fontFamilyFallback = node.properties.containsKey(fallbackName)
        ? _newlineList(fallbackName)
        : null;
    final localBase = themeBase ?? const TextStyle();
    return localBase.copyWith(
      inherit: explicitInherit,
      fontSize: _number('${textPrefix}FontSize'),
      fontWeight: _fontWeight('${textPrefix}FontWeight'),
      fontStyle: _fontStyle('${textPrefix}FontStyle'),
      letterSpacing: _number('${textPrefix}LetterSpacing'),
      wordSpacing: _number('${textPrefix}WordSpacing'),
      textBaseline: _textBaseline('${textPrefix}TextBaseline'),
      height: _number('${textPrefix}Height'),
      leadingDistribution: _textLeadingDistribution(
        '${textPrefix}LeadingDistribution',
      ),
      locale: _buttonTextLocale(textPrefix, structuredBase?.locale),
      background: background,
      shadows: _shadows(context, '${textPrefix}Shadows'),
      fontFeatures: _fontFeatures('${textPrefix}FontFeatures'),
      fontVariations: _fontVariations('${textPrefix}FontVariations'),
      decoration: _buttonTextDecoration(textPrefix, structuredBase?.decoration),
      decorationColor: _resolvedColor(context, '${textPrefix}DecorationColor'),
      decorationStyle: _textDecorationStyle('${textPrefix}DecorationStyle'),
      decorationThickness: _number('${textPrefix}DecorationThickness'),
      fontFamily: fontFamily,
      fontFamilyFallback: fontFamilyFallback,
      overflow: _textOverflow('${textPrefix}Overflow'),
    );
  }

  TextStyle _buttonApplyTextPackage(TextStyle value, String package) {
    final family = value.fontFamily;
    if (family != null && !_buttonSyntheticNullFontFamily(family)) {
      return value.copyWith(package: package);
    }
    final fallback = value.fontFamilyFallback;
    if (fallback == null || fallback.isEmpty) {
      throw StateError(
        'Validated Canvas button TextPackage has no effective font reference.',
      );
    }
    final packageFree = _buttonTextStyleWithoutPrivatePackage(
      value,
      includeFontReferences: false,
    );
    return packageFree.copyWith(
      fontFamilyFallback: fallback
          .map(_buttonRawFontFamily)
          .map((family) => 'packages/$package/$family')
          .toList(growable: false),
    );
  }

  String _buttonRawFontFamily(String value) {
    const prefix = 'packages/';
    if (!value.startsWith(prefix)) {
      return value;
    }
    final separator = value.indexOf('/', prefix.length);
    return separator < 0 ? value : value.substring(separator + 1);
  }

  bool _buttonSyntheticNullFontFamily(String value) =>
      value.startsWith('packages/') && value.endsWith('/null');

  TextStyle? _buttonNormalizedThemeTextStyle(TextStyle? value) {
    final backgroundColor = value?.backgroundColor;
    if (value == null || backgroundColor == null || value.background != null) {
      return value;
    }
    return value.copyWith(background: Paint()..color = backgroundColor);
  }

  TextStyle _buttonTextStyleWithoutPrivatePackage(
    TextStyle value, {
    bool includeFontReferences = true,
  }) => TextStyle(
    inherit: value.inherit,
    color: value.color,
    backgroundColor: value.backgroundColor,
    fontSize: value.fontSize,
    fontWeight: value.fontWeight,
    fontStyle: value.fontStyle,
    letterSpacing: value.letterSpacing,
    wordSpacing: value.wordSpacing,
    textBaseline: value.textBaseline,
    height: value.height,
    leadingDistribution: value.leadingDistribution,
    locale: value.locale,
    foreground: value.foreground,
    background: value.background,
    shadows: value.shadows,
    fontFeatures: value.fontFeatures,
    fontVariations: value.fontVariations,
    decoration: value.decoration,
    decorationColor: value.decorationColor,
    decorationStyle: value.decorationStyle,
    decorationThickness: value.decorationThickness,
    debugLabel: value.debugLabel,
    fontFamily: includeFontReferences ? value.fontFamily : null,
    fontFamilyFallback: includeFontReferences ? value.fontFamilyFallback : null,
    overflow: value.overflow,
  );

  TextStyle? _mergeButtonTextStyle(
    TextStyle? inherited,
    TextStyle? local, {
    required bool inheritExplicit,
  }) {
    if (local == null) {
      return inherited;
    }
    if (inherited == null || (inheritExplicit && !local.inherit)) {
      return local;
    }
    return inherited
        .merge(local)
        .copyWith(inherit: inheritExplicit ? local.inherit : inherited.inherit);
  }

  Locale? _buttonTextLocale(String textPrefix, Locale? inherited) {
    final languageName = '${textPrefix}LocaleLanguageCode';
    final scriptName = '${textPrefix}LocaleScriptCode';
    final countryName = '${textPrefix}LocaleCountryCode';
    if (!node.properties.containsKey(languageName) &&
        !node.properties.containsKey(scriptName) &&
        !node.properties.containsKey(countryName)) {
      return null;
    }
    return Locale.fromSubtags(
      languageCode: _string(languageName) ?? inherited?.languageCode ?? 'und',
      scriptCode: _string(scriptName) ?? inherited?.scriptCode,
      countryCode: _string(countryName) ?? inherited?.countryCode,
    );
  }

  TextDecoration? _buttonTextDecoration(
    String textPrefix,
    TextDecoration? inherited,
  ) {
    final underlineName = '${textPrefix}DecorationUnderline';
    final overlineName = '${textPrefix}DecorationOverline';
    final lineThroughName = '${textPrefix}DecorationLineThrough';
    if (!node.properties.containsKey(underlineName) &&
        !node.properties.containsKey(overlineName) &&
        !node.properties.containsKey(lineThroughName)) {
      return null;
    }
    final decorations = <TextDecoration>[
      if (_boolean(underlineName) ??
          (inherited?.contains(TextDecoration.underline) ?? false))
        TextDecoration.underline,
      if (_boolean(overlineName) ??
          (inherited?.contains(TextDecoration.overline) ?? false))
        TextDecoration.overline,
      if (_boolean(lineThroughName) ??
          (inherited?.contains(TextDecoration.lineThrough) ?? false))
        TextDecoration.lineThrough,
    ];
    return switch (decorations.length) {
      0 => TextDecoration.none,
      1 => decorations.single,
      _ => TextDecoration.combine(decorations),
    };
  }

  WidgetStateProperty<Size?>? _buttonSizeStateProperty(
    String role, {
    required double missingDimension,
    required WidgetStateProperty<Size?>? themeValue,
    required WidgetStateProperty<Size?>? defaultValue,
  }) {
    bool hasGroup(String prefix) =>
        node.properties.containsKey('$prefix${role}Width') ||
        node.properties.containsKey('$prefix${role}Height');
    if (!_buttonHasAnyStateGroup(hasGroup)) {
      return null;
    }
    return WidgetStateProperty.resolveWith<Size?>((states) {
      final layers = _buttonActiveStateLayers(states, hasGroup);
      if (layers.isEmpty) {
        return null;
      }
      final inherited =
          themeValue?.resolve(states) ?? defaultValue?.resolve(states);
      var width = inherited?.width ?? missingDimension;
      var height = inherited?.height ?? missingDimension;
      for (final prefix in layers) {
        width = _number('$prefix${role}Width') ?? width;
        height = _number('$prefix${role}Height') ?? height;
      }
      return Size(width, height);
    });
  }

  ({WidgetStateProperty<Size?>? minimum, WidgetStateProperty<Size?>? maximum})
  _buttonConstraintSizeStateProperties({
    required WidgetStateProperty<Size?>? themeMinimum,
    required WidgetStateProperty<Size?>? defaultMinimum,
    required WidgetStateProperty<Size?>? themeMaximum,
    required WidgetStateProperty<Size?>? defaultMaximum,
  }) {
    bool hasGroup(String prefix) =>
        node.properties.containsKey('${prefix}MinimumWidth') ||
        node.properties.containsKey('${prefix}MinimumHeight') ||
        node.properties.containsKey('${prefix}MaximumWidth') ||
        node.properties.containsKey('${prefix}MaximumHeight');
    if (!_buttonHasAnyStateGroup(hasGroup)) {
      return (minimum: null, maximum: null);
    }

    ({Size minimum, Size maximum})? resolve(Set<WidgetState> states) {
      final layers = _buttonActiveStateLayers(states, hasGroup);
      if (layers.isEmpty) {
        return null;
      }
      final inheritedMinimum =
          themeMinimum?.resolve(states) ?? defaultMinimum?.resolve(states);
      final inheritedMaximum =
          themeMaximum?.resolve(states) ?? defaultMaximum?.resolve(states);
      var minimumWidth = inheritedMinimum?.width ?? 0;
      var minimumHeight = inheritedMinimum?.height ?? 0;
      var maximumWidth = inheritedMaximum?.width ?? double.infinity;
      var maximumHeight = inheritedMaximum?.height ?? double.infinity;
      for (final prefix in layers) {
        minimumWidth = _number('${prefix}MinimumWidth') ?? minimumWidth;
        minimumHeight = _number('${prefix}MinimumHeight') ?? minimumHeight;
        maximumWidth = _number('${prefix}MaximumWidth') ?? maximumWidth;
        maximumHeight = _number('${prefix}MaximumHeight') ?? maximumHeight;
      }
      maximumWidth = math.max(maximumWidth, minimumWidth);
      maximumHeight = math.max(maximumHeight, minimumHeight);
      return (
        minimum: Size(minimumWidth, minimumHeight),
        maximum: Size(maximumWidth, maximumHeight),
      );
    }

    return (
      minimum: WidgetStateProperty.resolveWith<Size?>(
        (states) => resolve(states)?.minimum,
      ),
      maximum: WidgetStateProperty.resolveWith<Size?>(
        (states) => resolve(states)?.maximum,
      ),
    );
  }

  WidgetStateProperty<BorderSide?>? _buttonBorderSideStateProperty(
    BuildContext context,
    WidgetStateProperty<BorderSide?>? themeValue,
    WidgetStateProperty<BorderSide?>? defaultValue,
    WidgetStateProperty<OutlinedBorder?>? themeShape,
    WidgetStateProperty<OutlinedBorder?>? defaultShape,
  ) {
    bool hasGroup(String prefix) => _hasPropertyPrefix('${prefix}Side');
    if (!_buttonHasAnyStateGroup(hasGroup)) {
      return null;
    }
    return WidgetStateProperty.resolveWith<BorderSide?>((states) {
      final layers = _buttonActiveStateLayers(states, hasGroup);
      if (layers.isEmpty) {
        return null;
      }
      final inheritedShape =
          themeShape?.resolve(states) ?? defaultShape?.resolve(states);
      final inherited =
          themeValue?.resolve(states) ??
          defaultValue?.resolve(states) ??
          inheritedShape?.side ??
          BorderSide.none;
      var color = inherited.color;
      var width = inherited.width;
      var style = inherited.style;
      var strokeAlign = inherited.strokeAlign;
      for (final prefix in layers) {
        final sidePrefix = '${prefix}Side';
        color = _resolvedColor(context, '${sidePrefix}Color') ?? color;
        width = _number('${sidePrefix}Width') ?? width;
        final localStyle = _enumOrString('${sidePrefix}Style');
        if (localStyle != null) {
          style = localStyle == 'none' ? BorderStyle.none : BorderStyle.solid;
        }
        strokeAlign = _number('${sidePrefix}StrokeAlign') ?? strokeAlign;
      }
      return BorderSide(
        color: color,
        width: width,
        style: style,
        strokeAlign: strokeAlign,
      );
    });
  }

  WidgetStateProperty<OutlinedBorder?>? _buttonShapeStateProperty(
    BuildContext context,
    WidgetStateProperty<OutlinedBorder?>? themeValue,
    WidgetStateProperty<OutlinedBorder?>? defaultValue,
  ) {
    bool hasGroup(String prefix) => _hasPropertyPrefix('${prefix}Shape');
    if (!_buttonHasAnyStateGroup(hasGroup)) {
      return null;
    }
    return WidgetStateProperty.resolveWith<OutlinedBorder?>((states) {
      final layers = _buttonActiveStateLayers(states, hasGroup);
      if (layers.isEmpty) {
        return null;
      }
      final inherited =
          themeValue?.resolve(states) ?? defaultValue?.resolve(states);
      final direction = Directionality.of(context);
      final inheritedRadius = _buttonShapeRadius(inherited, direction);
      var kind = _buttonShapeKind(inherited);
      var topLeft = inheritedRadius?.topLeft.x ?? 0;
      var topRight = inheritedRadius?.topRight.x ?? 0;
      var bottomRight = inheritedRadius?.bottomRight.x ?? 0;
      var bottomLeft = inheritedRadius?.bottomLeft.x ?? 0;
      var eccentricity = inherited is CircleBorder
          ? inherited.eccentricity
          : 0.0;
      for (final prefix in layers) {
        kind = _enumOrString('${prefix}ShapeKind') ?? kind;
        topLeft = _number('${prefix}ShapeRadiusTopLeft') ?? topLeft;
        topRight = _number('${prefix}ShapeRadiusTopRight') ?? topRight;
        bottomRight = _number('${prefix}ShapeRadiusBottomRight') ?? bottomRight;
        bottomLeft = _number('${prefix}ShapeRadiusBottomLeft') ?? bottomLeft;
        eccentricity =
            _number('${prefix}ShapeCircleEccentricity') ?? eccentricity;
      }
      if (kind == null) {
        return null;
      }
      final radiusValue = BorderRadius.only(
        topLeft: Radius.circular(topLeft),
        topRight: Radius.circular(topRight),
        bottomRight: Radius.circular(bottomRight),
        bottomLeft: Radius.circular(bottomLeft),
      );
      final side = inherited?.side ?? BorderSide.none;
      return switch (kind) {
        'roundedRectangle' => RoundedRectangleBorder(
          side: side,
          borderRadius: radiusValue,
        ),
        'roundedSuperellipse' => RoundedSuperellipseBorder(
          side: side,
          borderRadius: radiusValue,
        ),
        'stadium' => StadiumBorder(side: side),
        'circle' => CircleBorder(side: side, eccentricity: eccentricity),
        'beveledRectangle' => BeveledRectangleBorder(
          side: side,
          borderRadius: radiusValue,
        ),
        'continuousRectangle' => ContinuousRectangleBorder(
          side: side,
          borderRadius: radiusValue,
        ),
        _ => throw StateError('Unreviewed Canvas button shape kind: $kind'),
      };
    });
  }

  BorderRadius? _buttonShapeRadius(
    OutlinedBorder? shape,
    TextDirection direction,
  ) => switch (shape) {
    RoundedRectangleBorder value => value.borderRadius.resolve(direction),
    RoundedSuperellipseBorder value => value.borderRadius.resolve(direction),
    BeveledRectangleBorder value => value.borderRadius.resolve(direction),
    ContinuousRectangleBorder value => value.borderRadius.resolve(direction),
    _ => null,
  };

  String? _buttonShapeKind(OutlinedBorder? shape) => switch (shape) {
    RoundedRectangleBorder() => 'roundedRectangle',
    RoundedSuperellipseBorder() => 'roundedSuperellipse',
    StadiumBorder() => 'stadium',
    CircleBorder() => 'circle',
    BeveledRectangleBorder() => 'beveledRectangle',
    ContinuousRectangleBorder() => 'continuousRectangle',
    _ => null,
  };

  bool _buttonHasAnyStateGroup(bool Function(String prefix) hasGroup) =>
      (_usesExtendedButtonStyle
              ? <String>[..._textButtonStateLayers.values, 'style']
              : const <String>[
                  'styleDisabled',
                  'stylePressed',
                  'styleHovered',
                  'styleFocused',
                  'style',
                ])
          .any(hasGroup);

  List<String> _buttonActiveStateLayers(
    Set<WidgetState> states,
    bool Function(String prefix) hasGroup,
  ) {
    if (states.contains(WidgetState.disabled)) {
      return hasGroup('styleDisabled')
          ? const <String>['styleDisabled']
          : const <String>[];
    }
    return <String>[
      if (hasGroup('style')) 'style',
      if (_usesExtendedButtonStyle)
        for (final entry in _textButtonStateLayers.entries.toList().reversed)
          if (entry.key != WidgetState.disabled &&
              states.contains(entry.key) &&
              hasGroup(entry.value))
            entry.value,
      if (!_usesExtendedButtonStyle) ...[
        if (states.contains(WidgetState.focused) && hasGroup('styleFocused'))
          'styleFocused',
        if (states.contains(WidgetState.hovered) && hasGroup('styleHovered'))
          'styleHovered',
        if (states.contains(WidgetState.pressed) && hasGroup('stylePressed'))
          'stylePressed',
      ],
    ];
  }

  VisualDensity? _buttonVisualDensity(
    VisualDensity? themeValue,
    VisualDensity? defaultValue,
  ) {
    final horizontal = _number('styleVisualDensityHorizontal');
    final vertical = _number('styleVisualDensityVertical');
    if (horizontal == null && vertical == null) {
      return null;
    }
    final inherited = themeValue ?? defaultValue;
    return VisualDensity(
      horizontal: horizontal ?? inherited?.horizontal ?? 0,
      vertical: vertical ?? inherited?.vertical ?? 0,
    );
  }

  MaterialTapTargetSize? _buttonTapTargetSize() =>
      switch (_enumOrString('styleTapTargetSize')) {
        'padded' => MaterialTapTargetSize.padded,
        'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
        _ => null,
      };

  Duration? _buttonAnimationDuration() {
    final milliseconds = _number('styleAnimationDurationMs');
    return milliseconds == null
        ? null
        : Duration(microseconds: (milliseconds * 1000).round());
  }

  AlignmentGeometry? _buttonAlignment() {
    final kind = _enumOrString('styleAlignmentKind');
    final x = _number('styleAlignmentX');
    final y = _number('styleAlignmentY');
    if (kind == null && x == null && y == null) {
      return null;
    }
    return kind == 'directional'
        ? AlignmentDirectional(x ?? 0, y ?? 0)
        : Alignment(x ?? 0, y ?? 0);
  }

  InteractiveInkFeatureFactory? _buttonSplashFactory() =>
      switch (_enumOrString('styleSplashFactory')) {
        'inkRipple' => InkRipple.splashFactory,
        'inkSplash' => InkSplash.splashFactory,
        'inkSparkle' => InkSparkle.splashFactory,
        'none' || 'noSplash' => NoSplash.splashFactory,
        _ => null,
      };

  static bool _isEmptyLinearContainer(CanvasNode node) =>
      (node.type == 'flutter.widgets.Row' ||
          node.type == 'flutter.widgets.Column' ||
          node.type == 'flutter.widgets.Wrap' ||
          node.type == 'flutter.widgets.ListBody' ||
          node.type == 'flutter.widgets.OverflowBar' ||
          node.type == 'flutter.widgets.ListView' ||
          node.type == 'flutter.widgets.GridView' ||
          node.type == 'flutter.widgets.GridView.extent' ||
          node.type == 'flutter.widgets.PageView' ||
          node.type == 'flutter.widgets.ListWheelScrollView')
      ? (node.slot('children')?.children.isEmpty ?? false)
      : node.type == 'flutter.widgets.CustomScrollView' &&
            (node.slot('slivers')?.children.isEmpty ?? false);

  Widget _row() => Row(
    mainAxisAlignment: _mainAxisAlignment(),
    mainAxisSize: _mainAxisSize(),
    crossAxisAlignment: _crossAxisAlignment(),
    textDirection: _textDirection(),
    verticalDirection: _verticalDirection(),
    textBaseline: _textBaseline(),
    spacing: _number('spacing') ?? 0.0,
    children: _children('children'),
  );

  Widget _wrap() => Wrap(
    direction: _enum('direction') == 'vertical'
        ? Axis.vertical
        : Axis.horizontal,
    alignment: _wrapAlignment('alignment'),
    spacing: _number('spacing') ?? 0.0,
    runAlignment: _wrapAlignment('runAlignment'),
    runSpacing: _number('runSpacing') ?? 0.0,
    crossAxisAlignment: _wrapCrossAxisAlignment(),
    textDirection: _textDirection(),
    verticalDirection: _verticalDirection(),
    clipBehavior: _clipBehavior() ?? Clip.none,
    children: _children('children'),
  );

  Widget _listBody() {
    final mainAxis = _enum('mainAxis') == 'horizontal'
        ? Axis.horizontal
        : Axis.vertical;
    final reverse = _boolean('reverse') ?? false;

    Widget buildListBody() => ListBody(
      mainAxis: mainAxis,
      reverse: reverse,
      children: _children('children'),
    );

    return LayoutBuilder(
      builder: (context, constraints) {
        final fallbackWidth =
            mainAxis == Axis.vertical && !constraints.hasBoundedWidth
            ? 240.0
            : null;
        final fallbackHeight =
            mainAxis == Axis.horizontal && !constraints.hasBoundedHeight
            ? 120.0
            : null;
        // The viewport and RenderListBody intentionally share one axis
        // direction. `reverse` changes the viewport's scroll origin and the
        // ListBody's child placement; it does not reorder the child list twice.
        Widget viewport = SingleChildScrollView(
          scrollDirection: mainAxis,
          reverse: reverse,
          primary: false,
          child: buildListBody(),
        );
        if (node.slot('children')?.children.isEmpty ?? true) {
          viewport = ConstrainedBox(
            constraints: mainAxis == Axis.vertical
                ? const BoxConstraints(minHeight: 36)
                : const BoxConstraints(minWidth: 36),
            child: viewport,
          );
        }
        if (fallbackWidth != null || fallbackHeight != null) {
          return SizedBox(
            width: fallbackWidth,
            height: fallbackHeight,
            child: viewport,
          );
        }
        return viewport;
      },
    );
  }

  Widget _overflowBar() {
    Widget buildOverflowBar() => OverflowBar(
      spacing: _number('spacing') ?? 0.0,
      alignment: _overflowBarMainAxisAlignment(),
      overflowSpacing: _number('overflowSpacing') ?? 0.0,
      overflowAlignment: _overflowBarAlignment(),
      overflowDirection: _enum('overflowDirection') == 'up'
          ? VerticalDirection.up
          : VerticalDirection.down,
      textDirection: _textDirection(),
      children: _children('children'),
    );

    return LayoutBuilder(
      builder: (context, constraints) {
        final empty = node.slot('children')?.children.isEmpty ?? true;
        if (!empty &&
            (constraints.hasBoundedWidth ||
                _overflowBarMainAxisAlignment() == null)) {
          return buildOverflowBar();
        }
        // RenderOverflowBar needs a finite width whenever a non-null
        // alignment expands the horizontal layout. The cap is Canvas-only:
        // generated Dart retains the user's bare OverflowBar constructor.
        return ConstrainedBox(
          constraints: BoxConstraints(
            minWidth: empty ? 36 : 0,
            minHeight: empty ? 36 : 0,
            maxWidth: constraints.hasBoundedWidth ? double.infinity : 240,
          ),
          child: buildOverflowBar(),
        );
      },
    );
  }

  Widget _listView() {
    final scrollDirection = _enum('scrollDirection') == 'horizontal'
        ? Axis.horizontal
        : Axis.vertical;
    final shrinkWrap = _boolean('shrinkWrap') ?? false;

    Widget buildListView() => ListView(
      key: ValueKey('canvas-list-view-${node.id}'),
      scrollDirection: scrollDirection,
      reverse: _boolean('reverse') ?? false,
      primary: _boolean('primary'),
      physics: _scrollPhysics(),
      shrinkWrap: shrinkWrap,
      padding: _edgeInsetsGeometry('padding'),
      itemExtent: _number('itemExtent'),
      // Unknown project extents and out-of-range results cannot be reproduced.
      // Omit itemExtentBuilder so the labeled approximation uses natural sizing.
      addAutomaticKeepAlives: _boolean('addAutomaticKeepAlives') ?? true,
      addRepaintBoundaries: _boolean('addRepaintBoundaries') ?? true,
      addSemanticIndexes: _boolean('addSemanticIndexes') ?? true,
      scrollCacheExtent: _scrollCacheExtent(),
      semanticChildCount: _integer('semanticChildCount'),
      dragStartBehavior: _dragStartBehavior(),
      keyboardDismissBehavior: _scrollKeyboardDismissBehavior(),
      restorationId: _string('restorationId'),
      clipBehavior: _clipBehavior() ?? Clip.hardEdge,
      hitTestBehavior: _scrollHitTestBehavior(),
      children: _children('children'),
    );

    return _TextButtonPreview(
      message: _listViewExtentPreviewMessage(node) ?? '',
      child: LayoutBuilder(
        builder: (context, constraints) {
          final fallbackWidth =
              !constraints.hasBoundedWidth &&
                  (scrollDirection == Axis.vertical || !shrinkWrap)
              ? 240.0
              : null;
          final fallbackHeight =
              !constraints.hasBoundedHeight &&
                  (scrollDirection == Axis.horizontal || !shrinkWrap)
              ? 120.0
              : null;
          if (fallbackWidth != null || fallbackHeight != null) {
            return SizedBox(
              width: fallbackWidth,
              height: fallbackHeight,
              child: buildListView(),
            );
          }
          return buildListView();
        },
      ),
    );
  }

  Widget _gridView({bool extent = false}) {
    final scrollDirection = _enum('scrollDirection') == 'horizontal'
        ? Axis.horizontal
        : Axis.vertical;
    final shrinkWrap = _boolean('shrinkWrap') ?? false;

    Widget buildGridView() {
      final commonChildren = _children('children');
      if (extent) {
        return GridView.extent(
          scrollDirection: scrollDirection,
          reverse: _boolean('reverse') ?? false,
          primary: _boolean('primary'),
          physics: _scrollPhysics(),
          shrinkWrap: shrinkWrap,
          padding: _edgeInsetsGeometry('padding'),
          maxCrossAxisExtent: _number('maxCrossAxisExtent') ?? 200.0,
          mainAxisSpacing: _number('mainAxisSpacing') ?? 0.0,
          crossAxisSpacing: _number('crossAxisSpacing') ?? 0.0,
          childAspectRatio: _number('childAspectRatio') ?? 1.0,
          mainAxisExtent: _number('mainAxisExtent'),
          addAutomaticKeepAlives: _boolean('addAutomaticKeepAlives') ?? true,
          addRepaintBoundaries: _boolean('addRepaintBoundaries') ?? true,
          addSemanticIndexes: _boolean('addSemanticIndexes') ?? true,
          scrollCacheExtent: _scrollCacheExtent(),
          semanticChildCount: _integer('semanticChildCount'),
          dragStartBehavior: _dragStartBehavior(),
          keyboardDismissBehavior: _scrollKeyboardDismissBehavior(),
          restorationId: _string('restorationId'),
          clipBehavior: _clipBehavior() ?? Clip.hardEdge,
          hitTestBehavior: _scrollHitTestBehavior(),
          children: commonChildren,
        );
      }
      return GridView.count(
        scrollDirection: scrollDirection,
        reverse: _boolean('reverse') ?? false,
        primary: _boolean('primary'),
        physics: _scrollPhysics(),
        shrinkWrap: shrinkWrap,
        padding: _edgeInsetsGeometry('padding'),
        crossAxisCount: _integer('crossAxisCount') ?? 2,
        mainAxisSpacing: _number('mainAxisSpacing') ?? 0.0,
        crossAxisSpacing: _number('crossAxisSpacing') ?? 0.0,
        childAspectRatio: _number('childAspectRatio') ?? 1.0,
        mainAxisExtent: _number('mainAxisExtent'),
        addAutomaticKeepAlives: _boolean('addAutomaticKeepAlives') ?? true,
        addRepaintBoundaries: _boolean('addRepaintBoundaries') ?? true,
        addSemanticIndexes: _boolean('addSemanticIndexes') ?? true,
        scrollCacheExtent: _scrollCacheExtent(),
        semanticChildCount: _integer('semanticChildCount'),
        dragStartBehavior: _dragStartBehavior(),
        keyboardDismissBehavior: _scrollKeyboardDismissBehavior(),
        restorationId: _string('restorationId'),
        clipBehavior: _clipBehavior() ?? Clip.hardEdge,
        hitTestBehavior: _scrollHitTestBehavior(),
        children: commonChildren,
      );
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        // A grid always needs a bounded cross axis. A non-shrink-wrapped
        // viewport additionally needs a bounded main axis. These finite caps
        // mirror the generated application's shared constraint guard.
        final fallbackWidth =
            !constraints.hasBoundedWidth &&
                (scrollDirection == Axis.vertical || !shrinkWrap)
            ? 240.0
            : null;
        final fallbackHeight =
            !constraints.hasBoundedHeight &&
                (scrollDirection == Axis.horizontal || !shrinkWrap)
            ? 120.0
            : null;
        if (fallbackWidth != null || fallbackHeight != null) {
          return SizedBox(
            width: fallbackWidth,
            height: fallbackHeight,
            child: buildGridView(),
          );
        }
        return buildGridView();
      },
    );
  }

  Widget _customScrollView() {
    final scrollDirection = _enum('scrollDirection') == 'horizontal'
        ? Axis.horizontal
        : Axis.vertical;
    final shrinkWrap = _boolean('shrinkWrap') ?? false;

    Widget buildCustomScrollView() => CustomScrollView(
      key: ValueKey('canvas-custom-scroll-view-${node.id}'),
      scrollDirection: scrollDirection,
      reverse: _boolean('reverse') ?? false,
      controller: null,
      primary: _boolean('primary'),
      physics: _scrollPhysics(),
      shrinkWrap: shrinkWrap,
      anchor: _number('anchor') ?? 0.0,
      scrollCacheExtent: _scrollCacheExtent(),
      paintOrder: _enum('paintOrder') == 'lastIsTop'
          ? SliverPaintOrder.lastIsTop
          : SliverPaintOrder.firstIsTop,
      slivers: _children('slivers'),
      semanticChildCount: _integer('semanticChildCount'),
      dragStartBehavior: _dragStartBehavior(),
      keyboardDismissBehavior: _scrollKeyboardDismissBehavior(),
      restorationId: _string('restorationId'),
      clipBehavior: _clipBehavior() ?? Clip.hardEdge,
      hitTestBehavior: _scrollHitTestBehavior(),
    );

    return LayoutBuilder(
      builder: (context, constraints) {
        final fallbackWidth =
            !constraints.hasBoundedWidth &&
                (scrollDirection == Axis.vertical || !shrinkWrap)
            ? 240.0
            : null;
        final fallbackHeight =
            !constraints.hasBoundedHeight &&
                (scrollDirection == Axis.horizontal || !shrinkWrap)
            ? 120.0
            : null;
        if (fallbackWidth != null || fallbackHeight != null) {
          return SizedBox(
            width: fallbackWidth,
            height: fallbackHeight,
            child: buildCustomScrollView(),
          );
        }
        return buildCustomScrollView();
      },
    );
  }

  Widget _sliverFillViewport() {
    final sliver = SliverFillViewport(
      viewportFraction: _number('viewportFraction') ?? 1,
      padEnds: _boolean('padEnds') ?? true,
      allowImplicitScrolling: _boolean('allowImplicitScrolling') ?? true,
      delegate: SliverChildListDelegate(
        node.type.endsWith('.delegate') ? const [] : _children('children'),
        addAutomaticKeepAlives: _boolean('addAutomaticKeepAlives') ?? true,
        addRepaintBoundaries: _boolean('addRepaintBoundaries') ?? true,
        addSemanticIndexes: _boolean('addSemanticIndexes') ?? true,
        semanticIndexOffset: _integer('semanticIndexOffset') ?? 0,
      ),
    );
    final message = _viewportSliverPreviewMessage(node);
    if (message == null) return sliver;
    return SliverMainAxisGroup(slivers: [
      SliverToBoxAdapter(child: SizedBox(width: 280, height: 96,
        child: ColoredBox(color: const Color(0xfffff3cd),
          child: Padding(padding: const EdgeInsets.all(8),
            child: Text(message, style: const TextStyle(color: Color(0xff563d00), fontSize: 12)))))),
      sliver,
    ]);
  }

  Widget _dynamicSliver() {
    final grid = _string('gridDelegate') == 'maxExtent'
        ? const SliverGridDelegateWithMaxCrossAxisExtent(maxCrossAxisExtent: 200)
        : const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2);
    final count = _integer('itemCount');
    final keep = _boolean('addAutomaticKeepAlives') ?? true;
    final repaint = _boolean('addRepaintBoundaries') ?? true;
    final semantics = _boolean('addSemanticIndexes') ?? true;
    final offset = _integer('semanticIndexOffset') ?? 0;
    final customExtent = node.properties['itemExtentBuilder']?.kind == 'dartObjectReferencePresence';
    final Widget sliver = switch (node.type) {
      'flutter.widgets.SliverVariedExtentList' => customExtent
        ? SliverList.list(addAutomaticKeepAlives: keep, addRepaintBoundaries: repaint,
            addSemanticIndexes: semantics, children: _children('children'))
        : SliverVariedExtentList.list(itemExtentBuilder: (index, dimensions) =>
            index >= 0 && index < (node.slots['children']?.children.length ?? 0) ? 48 : null,
            addAutomaticKeepAlives: keep, addRepaintBoundaries: repaint,
            addSemanticIndexes: semantics, children: _children('children')),
      'flutter.widgets.SliverVariedExtentList.builder' => customExtent
        ? SliverList.builder(itemBuilder: (_, index) => null, itemCount: count,
            addAutomaticKeepAlives: keep, addRepaintBoundaries: repaint, addSemanticIndexes: semantics)
        : SliverVariedExtentList.builder(itemExtentBuilder: (index, dimensions) =>
            index >= 0 && (count == null || index < count) ? 48 : null,
            itemBuilder: (_, index) => null, itemCount: count,
            addAutomaticKeepAlives: keep, addRepaintBoundaries: repaint, addSemanticIndexes: semantics),
      'flutter.widgets.SliverVariedExtentList.delegate' => customExtent
        ? SliverList(delegate: SliverChildListDelegate(const []))
        : SliverVariedExtentList(itemExtentBuilder: (index, dimensions) => index >= 0 ? 48 : null,
            delegate: SliverChildListDelegate(const [])),
      'flutter.widgets.SliverPrototypeExtentList' => SliverPrototypeExtentList.list(
        prototypeItem: _prototypeItem(), addAutomaticKeepAlives: keep,
        addRepaintBoundaries: repaint, addSemanticIndexes: semantics, children: _children('children')),
      'flutter.widgets.SliverPrototypeExtentList.builder' => SliverPrototypeExtentList.builder(
        prototypeItem: _prototypeItem(), itemBuilder: (_, index) => null,
        itemCount: count, addAutomaticKeepAlives: keep, addRepaintBoundaries: repaint,
        addSemanticIndexes: semantics),
      'flutter.widgets.SliverPrototypeExtentList.delegate' => SliverPrototypeExtentList(
        prototypeItem: _prototypeItem(), delegate: SliverChildListDelegate(const [])),
      'flutter.widgets.SliverFixedExtentList' => SliverFixedExtentList.list(
        itemExtent: _number('itemExtent')!, addAutomaticKeepAlives: keep,
        addRepaintBoundaries: repaint, addSemanticIndexes: semantics, children: _children('children')),
      'flutter.widgets.SliverFixedExtentList.builder' => SliverFixedExtentList.builder(
        itemExtent: _number('itemExtent')!, itemBuilder: (_, index) => null,
        itemCount: count, addAutomaticKeepAlives: keep, addRepaintBoundaries: repaint,
        addSemanticIndexes: semantics, semanticIndexOffset: offset),
      'flutter.widgets.SliverFixedExtentList.delegate' => SliverFixedExtentList(
        itemExtent: _number('itemExtent')!, delegate: SliverChildListDelegate(const [])),
      'flutter.widgets.SliverList.builder' => SliverList.builder(
        itemBuilder: (_, index) => null, itemCount: count,
        addAutomaticKeepAlives: keep, addRepaintBoundaries: repaint,
        addSemanticIndexes: semantics, semanticIndexOffset: offset),
      'flutter.widgets.SliverList.separated' => SliverList.separated(
        itemBuilder: (_, index) => null, separatorBuilder: (_, index) => const SizedBox.shrink(),
        itemCount: count, addAutomaticKeepAlives: keep,
        addRepaintBoundaries: repaint, addSemanticIndexes: semantics),
      'flutter.widgets.SliverList.delegate' => SliverList(delegate: SliverChildListDelegate(const [])),
      'flutter.widgets.SliverGrid.delegate' => SliverGrid(gridDelegate: grid, delegate: SliverChildListDelegate(const [])),
      'flutter.widgets.SliverGrid.builder' => SliverGrid.builder(
        gridDelegate: grid, itemBuilder: (_, index) => null, itemCount: count,
        addAutomaticKeepAlives: keep, addRepaintBoundaries: repaint,
        addSemanticIndexes: semantics, semanticIndexOffset: offset),
      'flutter.widgets.SliverGrid.list' => SliverGrid.list(
        gridDelegate: grid, addAutomaticKeepAlives: keep,
        addRepaintBoundaries: repaint, addSemanticIndexes: semantics,
        semanticIndexOffset: offset, children: _children('children')),
      _ => throw StateError('Unsupported dynamic sliver ${node.type}'),
    };
    final message = _dynamicSliverPreviewMessage(node);
    if (message == null) return sliver;
    // Project-owned functions/delegates never execute in the isolated process.
    // The banner is preview-only; no fabricated item is persisted or generated.
    return SliverMainAxisGroup(slivers: [
      SliverToBoxAdapter(child: SizedBox(width: 280, height: 96,
        child: ColoredBox(color: const Color(0xfffff3cd),
          child: Padding(padding: const EdgeInsets.all(8),
            child: Text(message, style: const TextStyle(color: Color(0xff563d00), fontSize: 12)))))),
      sliver,
    ]);
  }

  Widget _sliverPadding() {
    final message = _sliverPaddingPreviewMessage(node);
    final sliver = SliverPadding(
      padding: message == null ? _edgeInsetsGeometry('padding')! : EdgeInsets.zero,
      sliver: _single('sliver'),
    );
    if (message == null) return sliver;
    return SliverMainAxisGroup(slivers: [
      SliverToBoxAdapter(child: SizedBox(width: 280,
        child: ColoredBox(color: const Color(0xfffff3cd),
          child: Padding(padding: const EdgeInsets.all(8),
            child: Text(message, style: const TextStyle(color: Color(0xff563d00), fontSize: 12)))))),
      sliver,
    ]);
  }

  Widget _sliverToBoxAdapter(BuildContext context) {
    final child = _single('child');
    // A sliver is only valid below a viewport. Keep a palette-created sliver
    // visible and mountable when it is temporarily selected as a root node.
    if (context.findAncestorWidgetOfExactType<CustomScrollView>() == null) {
      return SizedBox(width: 240, height: 80, child: child);
    }
    return SliverToBoxAdapter(child: child);
  }

  Widget _singleChildScrollView() => SingleChildScrollView(
    scrollDirection: _enum('scrollDirection') == 'horizontal'
        ? Axis.horizontal
        : Axis.vertical,
    reverse: _boolean('reverse') ?? false,
    padding: _edgeInsetsGeometry('padding'),
    primary: _boolean('primary'),
    physics: _scrollPhysics(),
    dragStartBehavior: _dragStartBehavior(),
    clipBehavior: _clipBehavior() ?? Clip.hardEdge,
    hitTestBehavior: _scrollHitTestBehavior(),
    restorationId: _string('restorationId'),
    keyboardDismissBehavior: _scrollKeyboardDismissBehavior(),
    child: _single('child'),
  );

  Widget _pageView() {
    final scrollDirection = _enum('scrollDirection') == 'vertical'
        ? Axis.vertical
        : Axis.horizontal;

    Widget buildPageView() => PageView(
      key: ValueKey('canvas-page-view-${node.id}'),
      scrollDirection: scrollDirection,
      reverse: _boolean('reverse') ?? false,
      // Project controllers, ScrollBehaviors, and callbacks are deliberately
      // not executed by the isolated preview. Their persisted values remain
      // available to Dart generation and are surfaced in the diagnostic.
      controller: null,
      physics: _scrollPhysics(),
      pageSnapping: _boolean('pageSnapping') ?? true,
      onPageChanged: null,
      dragStartBehavior: _dragStartBehavior(),
      allowImplicitScrolling: _boolean('allowImplicitScrolling') ?? false,
      scrollCacheExtent: _scrollCacheExtent(),
      restorationId: _string('restorationId'),
      clipBehavior: _clipBehavior() ?? Clip.hardEdge,
      hitTestBehavior: _scrollHitTestBehavior(),
      scrollBehavior: null,
      padEnds: _boolean('padEnds') ?? true,
      children: _children('children'),
    );

    final refs = <String>[
      if (node.properties['controller']?.kind == 'dartObjectReferencePresence')
        'controller',
      if (node.properties['scrollBehavior']?.kind ==
          'dartObjectReferencePresence')
        'scrollBehavior',
      if (node.properties['onPageChanged']?.kind ==
          'dartObjectReferencePresence')
        'onPageChanged',
    ];
    final message = refs.isEmpty
        ? ''
        : 'PageView project ${refs.join(', ')} reference${refs.length == 1 ? '' : 's'} '
              'are not executed in isolated Canvas; native preview defaults are used. '
              'Stored values and generated Dart are preserved.';
    return _TextButtonPreview(
      message: message,
      child: LayoutBuilder(
        builder: (context, constraints) {
          final width = constraints.hasBoundedWidth ? null : 240.0;
          final height = constraints.hasBoundedHeight ? null : 120.0;
          if (width != null || height != null) {
            return SizedBox(
              width: width,
              height: height,
              child: buildPageView(),
            );
          }
          return buildPageView();
        },
      ),
    );
  }

  Widget _listWheelScrollView() {
    final renderOutside = _boolean('renderChildrenOutsideViewport') ?? false;
    final configuredClip = _clipBehavior() ?? Clip.hardEdge;
    // The SDK asserts that rendering outside the viewport is paired with
    // Clip.none. Keep the stored values intact while making the isolated
    // preview mountable when an older document contains the conflicting pair.
    final effectiveClip = renderOutside && configuredClip != Clip.none
        ? Clip.none
        : configuredClip;

    Widget buildWheel() => ListWheelScrollView(
      key: ValueKey('canvas-list-wheel-${node.id}'),
      controller: null,
      physics: _scrollPhysics(),
      diameterRatio: _number('diameterRatio') ?? 2.0,
      perspective: _number('perspective') ?? 0.003,
      offAxisFraction: _number('offAxisFraction') ?? 0.0,
      useMagnifier: _boolean('useMagnifier') ?? false,
      magnification: _number('magnification') ?? 1.0,
      overAndUnderCenterOpacity: _number('overAndUnderCenterOpacity') ?? 1.0,
      itemExtent: _number('itemExtent') ?? 50.0,
      squeeze: _number('squeeze') ?? 1.0,
      onSelectedItemChanged: null,
      renderChildrenOutsideViewport: renderOutside,
      clipBehavior: effectiveClip,
      hitTestBehavior: _scrollHitTestBehavior(),
      restorationId: _string('restorationId'),
      scrollBehavior: null,
      dragStartBehavior: _dragStartBehavior(),
      changeReportingBehavior: _enum('changeReportingBehavior') == 'onScrollEnd'
          ? ChangeReportingBehavior.onScrollEnd
          : ChangeReportingBehavior.onScrollUpdate,
      children: _children('children'),
    );

    return _TextButtonPreview(
      message: _listWheelPreviewMessage(node) ?? '',
      child: LayoutBuilder(
        builder: (context, constraints) {
          final width = constraints.hasBoundedWidth ? null : 240.0;
          final height = constraints.hasBoundedHeight ? null : 120.0;
          if (width != null || height != null) {
            return SizedBox(width: width, height: height, child: buildWheel());
          }
          return buildWheel();
        },
      ),
    );
  }

  ScrollPhysics? _scrollPhysics() => switch (_string('physics')) {
    'alwaysScrollable' => const AlwaysScrollableScrollPhysics(),
    'bouncing' => const BouncingScrollPhysics(),
    'clamping' => const ClampingScrollPhysics(),
    'neverScrollable' => const NeverScrollableScrollPhysics(),
    'page' => const PageScrollPhysics(),
    'rangeMaintaining' => const RangeMaintainingScrollPhysics(),
    _ => null,
  };

  ScrollCacheExtent? _scrollCacheExtent() {
    final pixels = _number('scrollCacheExtent');
    return pixels == null ? null : ScrollCacheExtent.pixels(pixels);
  }

  ScrollViewKeyboardDismissBehavior? _scrollKeyboardDismissBehavior() =>
      switch (_enum('keyboardDismissBehavior')) {
        'manual' => ScrollViewKeyboardDismissBehavior.manual,
        'onDrag' => ScrollViewKeyboardDismissBehavior.onDrag,
        _ => null,
      };

  HitTestBehavior _scrollHitTestBehavior() =>
      switch (_enum('hitTestBehavior')) {
        'deferToChild' => HitTestBehavior.deferToChild,
        'translucent' => HitTestBehavior.translucent,
        _ => HitTestBehavior.opaque,
      };

  Widget _stackNative() => Stack(
    alignment: _alignmentGeometry('alignment') ?? AlignmentDirectional.topStart,
    textDirection: _textDirection(),
    fit: _stackFit(),
    clipBehavior: _clipBehavior() ?? Clip.hardEdge,
    children: _children('children'),
  );

  Widget _stack() {
    final children = node.slots['children']!.children;
    if (!children.any((child) => isCanvasStackPositionedWidgetType(child.type))) {
      return _stackNative();
    }
    // Positioned-only Stack takes constraints.biggest. Do not feed infinite
    // extents to RenderStack or invent a document height in the preview.
    // Eager intrinsic forwarding preserves valid IntrinsicWidth/Height parents.
    return _RefreshLayoutObserver(builder: (context, constraints) {
      bool positioned(CanvasNode child) =>
          isCanvasStackPositionedWidgetType(child.type) &&
          (child.type == 'flutter.widgets.PositionedTransition' || child.type == 'flutter.widgets.RelativePositionedTransition' || child.type.endsWith('.fromRect') ||
              ['left', 'top', 'right', 'bottom', 'start', 'end', 'width', 'height']
                  .any((name) => const {'integer', 'double'}.contains(child.properties[name]?.kind)));
      if (children.every(positioned) &&
          (!constraints.hasBoundedWidth || !constraints.hasBoundedHeight)) {
        return const _TextButtonPreview(
          message: 'Stack preview unavailable: all children are positioned but parent bounds are unbounded. '
              'Constrain the Stack width and height (for example with SizedBox). Stored values and generated Dart are unchanged.',
          child: _PositionedStackFallback(),
        );
      }
      return _stackNative();
    });
  }

  Widget _indexedStack() {
    final indexValue = node.properties['index'];
    final index = indexValue == null
        ? 0
        : indexValue.kind == 'null'
        ? null
        : indexValue.value as int;
    return IndexedStack(
      alignment:
          _alignmentGeometry('alignment') ?? AlignmentDirectional.topStart,
      textDirection: _textDirection(),
      clipBehavior: _clipBehavior() ?? Clip.hardEdge,
      sizing: _stackFit('sizing'),
      index: index,
      children: _children('children'),
    );
  }

  Widget _safeArea() => SafeArea(
    left: _boolean('left') ?? true,
    top: _boolean('top') ?? true,
    right: _boolean('right') ?? true,
    bottom: _boolean('bottom') ?? true,
    minimum: _physicalEdgeInsets('minimum') ?? EdgeInsets.zero,
    maintainBottomViewPadding: _boolean('maintainBottomViewPadding') ?? false,
    child: _single('child')!,
  );

  Widget _scrollbar() {
    final orientation = switch (_enumOrString('scrollbarOrientation')) {
      'left' => ScrollbarOrientation.left,
      'right' => ScrollbarOrientation.right,
      'top' => ScrollbarOrientation.top,
      'bottom' => ScrollbarOrientation.bottom,
      _ => null,
    };
    final scrollDirection =
        orientation == ScrollbarOrientation.top ||
            orientation == ScrollbarOrientation.bottom
        ? Axis.horizontal
        : Axis.vertical;
    return _TextButtonPreview(
      message: _scrollbarPreviewMessage(node) ?? '',
      child: Scrollbar(
        key: ValueKey('canvas-scrollbar-${node.id}'),
        // Application-owned controllers, predicates and Radius objects are
        // intentionally not executed by the isolated preview.
        controller: null,
        thumbVisibility: _boolean('thumbVisibility'),
        trackVisibility: _boolean('trackVisibility'),
        thickness: _number('thickness'),
        radius: null,
        notificationPredicate: null,
        interactive: _boolean('interactive'),
        scrollbarOrientation: orientation,
        // Scrollbar reads a ScrollPosition during layout/painting.  The
        // designer model can contain an arbitrary required child (including
        // a non-scrollable Text), so provide an isolated SDK scroll position
        // without changing the persisted child or generated constructor.
        child: SingleChildScrollView(
          scrollDirection: scrollDirection,
          primary: scrollDirection == Axis.vertical,
          child: _single('child')!,
        ),
      ),
    );
  }

  Widget _directionality() => Directionality(
    textDirection: _textDirection()!,
    child: _single('child')!,
  );

  EdgeInsetsGeometry _paddingGeometry() {
    return _edgeInsetsGeometry('padding')!;
  }

  EdgeInsetsGeometry? _edgeInsetsGeometry(String name) {
    if (node.properties[name]?.kind == 'dartObjectReferencePresence') return null;
    final value = node.properties[name]?.value;
    if (value == null) {
      return null;
    }
    return switch (value) {
      CanvasEdgeInsets physical => EdgeInsets.fromLTRB(
        physical.left,
        physical.top,
        physical.right,
        physical.bottom,
      ),
      CanvasEdgeInsetsDirectional directional => EdgeInsetsDirectional.fromSTEB(
        directional.start,
        directional.top,
        directional.end,
        directional.bottom,
      ),
      _ => throw StateError('Unsupported Canvas EdgeInsets value.'),
    };
  }

  EdgeInsets? _physicalEdgeInsets(String name) {
    final value = node.properties[name]?.value;
    if (value == null) {
      return null;
    }
    if (value case CanvasEdgeInsets physical) {
      return EdgeInsets.fromLTRB(
        physical.left,
        physical.top,
        physical.right,
        physical.bottom,
      );
    }
    throw StateError('Unsupported physical Canvas EdgeInsets value.');
  }

  AlignmentGeometry? _alignmentGeometry(String name) {
    final value = node.properties[name]?.value;
    if (value is! CanvasAlignmentGeometryValue) {
      return null;
    }
    return value.basis == 'directional'
        ? AlignmentDirectional(value.horizontal, value.vertical)
        : Alignment(value.horizontal, value.vertical);
  }

  BoxConstraints? _boxConstraints(String name) {
    final value = node.properties[name]?.value;
    if (value is! CanvasBoxConstraintsValue) {
      return null;
    }
    return BoxConstraints(
      minWidth: value.minWidth ?? double.infinity,
      maxWidth: value.maxWidth ?? double.infinity,
      minHeight: value.minHeight ?? double.infinity,
      maxHeight: value.maxHeight ?? double.infinity,
    );
  }

  Matrix4? _matrix4(String name) {
    final value = node.properties[name]?.value;
    return value is CanvasMatrix4Value ? Matrix4.fromList(value.storage) : null;
  }

  Offset? _offset(String name) {
    final value = node.properties[name]?.value;
    return value is CanvasOffsetValue ? Offset(value.dx, value.dy) : null;
  }

  BoxDecoration? _boxDecoration(BuildContext context, String name, [CanvasNode? owner]) {
    final value = (owner ?? node).properties[name]?.value;
    if (value is! CanvasBoxDecorationValue) {
      return null;
    }
    return BoxDecoration(
      color: value.color == null ? null : _colorSource(context, value.color!),
      image: value.image == null
          ? null
          : _decorationImage(context, value.image!),
      border: value.border == null ? null : _boxBorder(context, value.border!),
      borderRadius: value.borderRadius == null
          ? null
          : _borderRadius(value.borderRadius!),
      boxShadow: [
        for (final shadow in value.boxShadow)
          BoxShadow(
            color: _colorSource(context, shadow.color),
            offset: Offset(shadow.offsetX, shadow.offsetY),
            blurRadius: shadow.blurRadius,
            spreadRadius: shadow.spreadRadius,
            blurStyle: _blurStyle(shadow.blurStyle),
          ),
      ],
      gradient: value.gradient == null
          ? null
          : _boxGradient(context, value.gradient!),
      backgroundBlendMode: value.backgroundBlendMode == null
          ? null
          : _blendMode(value.backgroundBlendMode!),
      shape: value.shape == 'circle' ? BoxShape.circle : BoxShape.rectangle,
    );
  }

  DecorationImage _decorationImage(
    BuildContext context,
    CanvasDecorationImageValue value,
  ) {
    final binding = _imageProvider(value.image);
    final resolution = binding.resolution;
    return DecorationImage(
      image: binding.provider,
      onError: value.onError && resolution is CanvasResolvedImageValue
          ? (error, stackTrace) =>
                onImageError?.call(resolution.resourceId, error, stackTrace)
          : null,
      colorFilter: value.colorFilter == null
          ? null
          : _colorFilter(context, value.colorFilter!),
      fit: value.fit == null ? null : _boxFit(value.fit!),
      alignment: _alignmentGeometryValue(value.alignment),
      // An unavailable image has no trustworthy intrinsic dimensions. The
      // status checker remains paint-safe while preserving every other visual
      // argument; a resolved image keeps the reviewed nine-patch contract.
      centerSlice: binding.placeholder || value.centerSlice == null
          ? null
          : Rect.fromLTRB(
              value.centerSlice!.left,
              value.centerSlice!.top,
              value.centerSlice!.right,
              value.centerSlice!.bottom,
            ),
      repeat: _imageRepeat(value.repeat),
      matchTextDirection: value.matchTextDirection,
      scale: value.scale,
      opacity: value.opacity,
      filterQuality: _filterQuality(value.filterQuality),
      invertColors: value.invertColors,
      isAntiAlias: value.isAntiAlias,
    );
  }

  ({
    ImageProvider<Object> provider,
    bool placeholder,
    CanvasImageResolutionValue resolution,
  })
  _imageProvider(CanvasImageProviderValue value) {
    final resolution = value.resolution;
    final resource = resolution is CanvasResolvedImageValue
        ? imageResources[resolution.resourceId]
        : null;
    final placeholder =
        resolution is! CanvasResolvedImageValue || resource == null;
    ImageProvider<Object> provider = placeholder
        ? MemoryImage(_unavailableImageBytes)
        : MemoryImage(resource.encodedBytes, scale: resolution.resolvedScale);
    final resize = value.resize;
    if (!placeholder && resize != null) {
      provider = ResizeImage(
        provider,
        width: resize.width,
        height: resize.height,
        policy: resize.policy == 'fit'
            ? ResizeImagePolicy.fit
            : ResizeImagePolicy.exact,
        allowUpscaling: resize.allowUpscaling,
      );
    }
    return (
      provider: provider,
      placeholder: placeholder,
      resolution: resolution,
    );
  }

  Widget _image(BuildContext context) {
    final provider = node.properties['image']!.value;
    if (provider is! CanvasImageProviderValue) {
      throw StateError('Canvas Image has no decoded image provider.');
    }
    final binding = _imageProvider(provider);
    final resolution = binding.resolution;
    final centerSlice = binding.placeholder ? null : _imageCenterSlice();
    final opacity = _number('opacity');
    return Image(
      image: binding.provider,
      frameBuilder: _callbackPresent('frameBuilder')
          ? (context, child, frame, wasSynchronouslyLoaded) => child
          : null,
      loadingBuilder: _callbackPresent('loadingBuilder')
          ? (context, child, loadingProgress) => child
          : null,
      errorBuilder: _callbackPresent('errorBuilder')
          ? (context, error, stackTrace) {
              if (resolution is CanvasResolvedImageValue) {
                onImageError?.call(resolution.resourceId, error, stackTrace);
              }
              return const SizedBox.shrink();
            }
          : null,
      semanticLabel: _string('semanticLabel'),
      excludeFromSemantics: _boolean('excludeFromSemantics') ?? false,
      width: _number('width'),
      height: _number('height'),
      color: _resolvedColor(context, 'color'),
      opacity: opacity == null ? null : AlwaysStoppedAnimation<double>(opacity),
      colorBlendMode: _enum('colorBlendMode') == null
          ? null
          : _blendMode(_enum('colorBlendMode')!),
      fit: _enum('fit') == null ? null : _boxFit(_enum('fit')!),
      alignment: _alignmentGeometry('alignment') ?? Alignment.center,
      repeat: _imageRepeat(_enum('repeat') ?? 'noRepeat'),
      centerSlice: centerSlice,
      matchTextDirection: _boolean('matchTextDirection') ?? false,
      gaplessPlayback: _boolean('gaplessPlayback') ?? false,
      isAntiAlias: _boolean('isAntiAlias') ?? false,
      filterQuality: _filterQuality(_enum('filterQuality') ?? 'medium'),
    );
  }

  bool _dataCallback(CanvasNode owner,String name)=>owner.properties[name]!=null&&owner.properties[name]!.kind!='null';

  WidgetStateProperty<T?>? _dataStates<T>(CanvasNode owner,String family,T? Function(String) resolve) {
    if(owner.properties[family]?.value!='local')return null;
    final values=<WidgetStatesConstraint,T?>{};
    for(final state in {..._checkboxStateLayers,WidgetState.any:'Default'}.entries) {
      final name='$family${state.value}';
      if(owner.properties.containsKey(name))values[state.key]=resolve(name);
    }
    // Even an empty local map is explicit; resolve to null in unmatched states.
    if(values.isEmpty)values[WidgetState.any]=null;
    return WidgetStateProperty<T?>.fromMap(values);
  }

  Widget _dateRangePickerDialog(BuildContext context) {
    if(['firstDate','lastDate','initialDateRange','currentDate','calendarDelegate'].any((name)=>node.properties[name]?.kind=='dartObjectReferencePresence')) {
      return const SizedBox(width:328,height:160,child:Center(child:Text('DateRangePickerDialog: project DateTime/range/calendar delegate preview unavailable. Run/Debug uses the saved source.')));
    }
    final icons=<String,Icon>{};
    for(final name in ['switchToInputEntryModeIcon','switchToCalendarEntryModeIcon']) {
      final children=node.slot(name)?.children??<CanvasNode>[];
      if(children.isNotEmpty) {
        final child=children.single, icon=_view(child)._icon(context,key:nodeKey(child.id));
        if(icon is! Icon)return icon;
        icons[name]=icon;
      }
    }
    DateTime? date(String name)=>_string(name)==null?null:canvasGregorianDate(_string(name)!);
    final range=_string('initialDateRange')==null?null:canvasGregorianDateRange(_string('initialDateRange')!);
    return KeyedSubtree(key:ValueKey(node),child:ExcludeFocus(child:AbsorbPointer(child:DateRangePickerDialog(
      firstDate:date('firstDate')!,lastDate:date('lastDate')!,currentDate:date('currentDate'),
      initialDateRange:range==null?null:DateTimeRange(start:range.$1,end:range.$2),
      initialEntryMode:DatePickerEntryMode.values.byName(_enum('initialEntryMode')??'calendar'),
      cancelText:_string('cancelText'),confirmText:_string('confirmText'),saveText:_string('saveText'),helpText:_string('helpText'),
      errorInvalidRangeText:_string('errorInvalidRangeText'),errorFormatText:_string('errorFormatText'),errorInvalidText:_string('errorInvalidText'),
      fieldStartHintText:_string('fieldStartHintText'),fieldEndHintText:_string('fieldEndHintText'),
      fieldStartLabelText:_string('fieldStartLabelText'),fieldEndLabelText:_string('fieldEndLabelText'),
      keyboardType:node.properties['keyboardType']?.kind=='dartObjectReferencePresence'||_string('keyboardType')==null?TextInputType.datetime:(_textInputType()??TextInputType.datetime),
      restorationId:_string('restorationId'),
      switchToInputEntryModeIcon:icons['switchToInputEntryModeIcon'],switchToCalendarEntryModeIcon:icons['switchToCalendarEntryModeIcon'],
    ))));
  }

  Widget _timePickerDialog(BuildContext context) {
    if(node.properties['initialTime']?.kind=='dartObjectReferencePresence') {
      return const SizedBox(width:312,height:100,child:Center(child:Text('TimePickerDialog: project TimeOfDay preview unavailable. Run/Debug uses the saved source.')));
    }
    final parts=_string('initialTime')!.split(':').map(int.parse).toList();
    final icons=<String,Icon>{};
    for(final name in ['switchToInputEntryModeIcon','switchToTimerEntryModeIcon']) {
      final children=node.slot(name)?.children??<CanvasNode>[];
      if(children.isNotEmpty) {
        final child=children.single,icon=_view(child)._icon(context,key:nodeKey(child.id));
        if(icon is! Icon){return icon;}
        icons[name]=icon;
      }
    }
    return KeyedSubtree(key:ValueKey(node),child:ExcludeFocus(child:AbsorbPointer(child:TimePickerDialog(
      initialTime:TimeOfDay(hour:parts[0],minute:parts[1]),
      initialEntryMode:TimePickerEntryMode.values.byName(_enum('initialEntryMode')??'dial'),
      orientation:_enum('orientation')==null?null:Orientation.values.byName(_enum('orientation')!),
      cancelText:_string('cancelText'),confirmText:_string('confirmText'),helpText:_string('helpText'),
      errorInvalidText:_string('errorInvalidText'),hourLabelText:_string('hourLabelText'),minuteLabelText:_string('minuteLabelText'),
      restorationId:_string('restorationId'),emptyInitialInput:_boolean('emptyInitialInput')??false,
      onEntryModeChanged:_dataCallback(node,'onEntryModeChanged')?(_){}:null,
      switchToInputEntryModeIcon:icons['switchToInputEntryModeIcon'],switchToTimerEntryModeIcon:icons['switchToTimerEntryModeIcon'],
    ))));
  }

  Widget _inputDatePickerFormField(BuildContext context) {
    if(['firstDate','lastDate','initialDate','calendarDelegate'].any((name)=>node.properties[name]?.kind=='dartObjectReferencePresence')) {
      return const SizedBox(width:328,height:80,child:Center(child:Text('InputDatePickerFormField: project DateTime/calendar delegate preview unavailable. Run/Debug uses the saved source.')));
    }
    DateTime? date(String name)=>_string(name)==null?null:canvasGregorianDate(_string(name)!);
    return KeyedSubtree(key:ValueKey(node),child:Material(type:MaterialType.transparency,
      child:ExcludeFocus(child:AbsorbPointer(child:InputDatePickerFormField(
        initialDate:date('initialDate'),firstDate:date('firstDate')!,lastDate:date('lastDate')!,
        onDateSubmitted:_dataCallback(node,'onDateSubmitted')?(_){}:null,
        onDateSaved:_dataCallback(node,'onDateSaved')?(_){}:null,
        errorFormatText:_string('errorFormatText'),errorInvalidText:_string('errorInvalidText'),
        fieldHintText:_string('fieldHintText'),fieldLabelText:_string('fieldLabelText'),
        keyboardType:node.properties['keyboardType']?.kind=='dartObjectReferencePresence'?null:_textInputType(),
        acceptEmptyDate:_boolean('acceptEmptyDate')??false,autofocus:false,
      )))));
  }

  Widget _calendarDatePicker(BuildContext context) {
    if(['firstDate','lastDate','initialDate','currentDate','calendarDelegate'].any((name)=>node.properties[name]?.kind=='dartObjectReferencePresence')) {
      return const SizedBox(width:328,height:160,child:Center(child:Text('CalendarDatePicker: project DateTime/calendar delegate preview unavailable. Run/Debug uses the saved source.')));
    }
    DateTime? date(String name)=>_string(name)==null?null:canvasGregorianDate(_string(name)!);
    // Applications retain same-key native state. Model edits deliberately remount this isolated preview.
    return KeyedSubtree(key:ValueKey(node),child:Material(type:MaterialType.transparency,
      child:ExcludeFocus(child:AbsorbPointer(child:CalendarDatePicker(
        initialDate:date('initialDate'),firstDate:date('firstDate')!,lastDate:date('lastDate')!,currentDate:date('currentDate'),
        initialCalendarMode:DatePickerMode.values.byName(_enum('initialCalendarMode')??'day'),
        onDateChanged:(_){},
        onDisplayedMonthChanged:_dataCallback(node,'onDisplayedMonthChanged')?(_){}:null,
      )))));
  }

  Widget _datePickerDialog(BuildContext context) {
    if(['firstDate','lastDate','initialDate','currentDate','calendarDelegate'].any((name)=>node.properties[name]?.kind=='dartObjectReferencePresence')) {
      return const SizedBox(width:328,height:160,child:Center(child:Text('DatePickerDialog: project DateTime/calendar delegate preview unavailable. Run/Debug uses the saved source.')));
    }
    final icons=<String,Icon>{};
    for(final name in ['switchToInputEntryModeIcon','switchToCalendarEntryModeIcon']) {
      final children=node.slot(name)?.children??<CanvasNode>[];
      if(children.isNotEmpty) {
        final child=children.single, icon=_view(child)._icon(context,key:nodeKey(child.id));
        if(icon is! Icon)return icon;
        icons[name]=icon;
      }
    }
    DateTime? date(String name)=>_string(name)==null?null:canvasGregorianDate(_string(name)!);
    // Initial/restoration state is native in applications; a new model must remount the preview.
    return KeyedSubtree(key:ValueKey(node),child:ExcludeFocus(child:AbsorbPointer(child:DatePickerDialog(
      firstDate:date('firstDate')!,lastDate:date('lastDate')!,initialDate:date('initialDate'),currentDate:date('currentDate'),
      initialEntryMode:DatePickerEntryMode.values.byName(_enum('initialEntryMode')??'calendar'),
      initialCalendarMode:DatePickerMode.values.byName(_enum('initialCalendarMode')??'day'),
      cancelText:_string('cancelText'),confirmText:_string('confirmText'),helpText:_string('helpText'),
      errorFormatText:_string('errorFormatText'),errorInvalidText:_string('errorInvalidText'),
      fieldHintText:_string('fieldHintText'),fieldLabelText:_string('fieldLabelText'),
      keyboardType:node.properties['keyboardType']?.kind=='dartObjectReferencePresence'?null:_textInputType(),
      restorationId:_string('restorationId'),
      onDatePickerModeChange:_dataCallback(node,'onDatePickerModeChange')?(_){}:null,
      switchToInputEntryModeIcon:icons['switchToInputEntryModeIcon'],switchToCalendarEntryModeIcon:icons['switchToCalendarEntryModeIcon'],
      insetPadding:node.properties['insetPadding']?.kind=='dartObjectReferencePresence'?const EdgeInsets.symmetric(horizontal:16,vertical:24):
        (_edgeInsetsGeometry('insetPadding') as EdgeInsets?)??const EdgeInsets.symmetric(horizontal:16,vertical:24),
    ))));
  }

  Widget _dataTable(BuildContext context) {
    final columns=node.slot('columns')!.children,rows=node.slot('rows')?.children??<CanvasNode>[];
    final columnValues=<DataColumn>[];
    for(final column in columns) {
      final own=_view(column);
      columnValues.add(DataColumn(
        label:_view(column.slot('label')!.children.single),
        columnWidth:own._string('columnWidth')==null?null:canvasTableWidth(own._string('columnWidth')!),
        tooltip:own._string('tooltip'),numeric:own._boolean('numeric')??false,
        onSort:_dataCallback(column,'onSort')?(_,_){}:null,
        mouseCursor:_dataStates<MouseCursor>(column,'mouseCursor',own._mouseCursor),
        headingRowAlignment:own._enum('headingRowAlignment')==null?null:MainAxisAlignment.values.byName(own._enum('headingRowAlignment')!),
      ));
    }
    final rowValues=<DataRow>[];
    for(final row in rows) {
      final own=_view(row);
      rowValues.add(DataRow(
        key:ValueKey('canvas-data-row-${row.id}'),
        selected:own._boolean('selected')??false,
        onSelectChanged:_dataCallback(row,'onSelectChanged')?(_){}:null,
        onLongPress:_dataCallback(row,'onLongPress')?(){}:null,
        onHover:_dataCallback(row,'onHover')?(_){}:null,
        color:_dataStates<Color>(row,'color',(n)=>own._resolvedColor(context,n)),
        mouseCursor:_dataStates<MouseCursor>(row,'mouseCursor',own._mouseCursor),
        cells:[for(final cell in row.slot('cells')!.children)
          if(cell.type=='flutter.material.DataCell.empty')DataCell.empty else DataCell(
            KeyedSubtree(key:ValueKey('canvas-data-cell-${cell.id}'),child:_view(cell.slot('child')!.children.single)),
            placeholder:_view(cell)._boolean('placeholder')??false,
            showEditIcon:_view(cell)._boolean('showEditIcon')??false,
            onTap:_dataCallback(cell,'onTap')?(){}:null,
            onDoubleTap:_dataCallback(cell,'onDoubleTap')?(){}:null,
            onLongPress:_dataCallback(cell,'onLongPress')?(){}:null,
            onTapDown:_dataCallback(cell,'onTapDown')?(_){}:null,
            onTapCancel:_dataCallback(cell,'onTapCancel')?(){}:null,
          )],
      ));
    }
    if(node.type==canvasPaginatedDataTableType) {
      if((_integer('rowsPerPage')??10)>1000) {
        return const SizedBox(width:320,height:80,child:Center(child:Text('PaginatedDataTable: Canvas preview is limited to 1,000 rows per page. Run/Debug uses the saved page size.')));
      }
      final header=node.slot('header')?.children??<CanvasNode>[];
      final actions=node.slot('actions')?.children??<CanvasNode>[];
      return PaginatedDataTable(
        // A changed initial index must remount the preview; app runtime retains native initial-only behavior.
        key:ValueKey('canvas-paginated-${node.id}-${_integer('initialFirstRowIndex')}'),
        columns:columnValues,source:_EmptyCanvasDataTableSource.instance,
        header:header.isEmpty?null:_view(header.single),
        actions:actions.isEmpty?null:[for(final action in actions)_view(action)],
        sortColumnIndex:_integer('sortColumnIndex'),sortAscending:_boolean('sortAscending')??true,
        onSelectAll:_dataCallback(node,'onSelectAll')?(_){}:null,
        // ignore: deprecated_member_use
        dataRowHeight:_number('dataRowHeight'),
        dataRowMinHeight:_number('dataRowMinHeight'),dataRowMaxHeight:_number('dataRowMaxHeight'),
        headingRowHeight:_number('headingRowHeight')??56,
        horizontalMargin:_number('horizontalMargin')??24,columnSpacing:_number('columnSpacing')??56,
        showCheckboxColumn:_boolean('showCheckboxColumn')??true,
        showFirstLastButtons:_boolean('showFirstLastButtons')??false,
        initialFirstRowIndex:_integer('initialFirstRowIndex'),
        onPageChanged:_dataCallback(node,'onPageChanged')?(_){}:null,
        rowsPerPage:_integer('rowsPerPage')??10,
        availableRowsPerPage:_string('availableRowsPerPage')==null?[10,20,50,100]:canvasPageSizes(_string('availableRowsPerPage')!),
        onRowsPerPageChanged:_dataCallback(node,'onRowsPerPageChanged')?(_){}:null,
        dragStartBehavior:_enum('dragStartBehavior')=='down'?DragStartBehavior.down:DragStartBehavior.start,
        arrowHeadColor:_resolvedColor(context,'arrowHeadColor'),
        checkboxHorizontalMargin:_number('checkboxHorizontalMargin'),primary:_boolean('primary'),
        headingRowColor:_dataStates<Color>(node,'headingRowColor',(n)=>_resolvedColor(context,n)),
        dividerThickness:_number('dividerThickness'),showEmptyRows:_boolean('showEmptyRows')??true,
      );
    }
    return DataTable(
      columns:columnValues,rows:rowValues,
      sortColumnIndex:_integer('sortColumnIndex'),sortAscending:_boolean('sortAscending')??true,
      onSelectAll:_dataCallback(node,'onSelectAll')?(_){}:null,
      decoration:_boxDecoration(context,'decoration'),
      dataRowColor:_dataStates<Color>(node,'dataRowColor',(n)=>_resolvedColor(context,n)),
      // ignore: deprecated_member_use
      dataRowHeight:_number('dataRowHeight'),
      dataRowMinHeight:_number('dataRowMinHeight'),dataRowMaxHeight:_number('dataRowMaxHeight'),
      dataTextStyle:_string('dataTextStyle')=='local'?_textStyle(context,'dataTextStyle'):null,
      headingRowColor:_dataStates<Color>(node,'headingRowColor',(n)=>_resolvedColor(context,n)),
      headingRowHeight:_number('headingRowHeight'),
      headingTextStyle:_string('headingTextStyle')=='local'?_textStyle(context,'headingTextStyle'):null,
      horizontalMargin:_number('horizontalMargin'),columnSpacing:_number('columnSpacing'),
      showCheckboxColumn:_boolean('showCheckboxColumn')??true,showBottomBorder:_boolean('showBottomBorder')??false,
      dividerThickness:_number('dividerThickness'),checkboxHorizontalMargin:_number('checkboxHorizontalMargin'),
      border:_tableBorder(context),clipBehavior:_clipBehavior()??Clip.none,
    );
  }

  Widget _table(BuildContext context) {
    final widths=node.properties['columnWidths'];
    final defaultWidth=node.properties['defaultColumnWidth'];
    final rows=node.slot('children')?.children??<CanvasNode>[];
    return Table(
      columnWidths:widths?.kind=='string'?canvasTableWidths(widths!.value as String):null,
      defaultColumnWidth:defaultWidth?.kind=='string'?canvasTableWidth(defaultWidth!.value as String):const FlexColumnWidth(),
      textDirection:_textDirection(),
      defaultVerticalAlignment:TableCellVerticalAlignment.values.byName(_enum('defaultVerticalAlignment')??'top'),
      textBaseline:_enum('textBaseline')==null?null:TextBaseline.values.byName(_enum('textBaseline')!),
      border:_tableBorder(context),
      children:[for(final row in rows) TableRow(
        key:ValueKey('canvas-table-row-${row.id}'),
        decoration:_boxDecoration(context,'decoration',row),
        children:[for(final cell in row.slot('children')!.children)
          KeyedSubtree(key:ValueKey('canvas-table-cell-${cell.id}'),child:_view(cell))],
      )],
    );
  }

  TableBorder? _tableBorder(BuildContext context) {
    final mode=_string('border');
    if(mode==null) return null;
    BorderSide side(String part) {
      final prefix='border${part[0].toUpperCase()}${part.substring(1)}';
      if(part!='all'&&!['Color','Width','Style','StrokeAlign'].any((s)=>node.properties.containsKey('$prefix$s'))) return BorderSide.none;
      return BorderSide(color:_resolvedColor(context,'${prefix}Color')??const Color(0xff000000),
        width:_number('${prefix}Width')??1,style:BorderStyle.values.byName(_enum('${prefix}Style')??'solid'),
        strokeAlign:_number('${prefix}StrokeAlign')??BorderSide.strokeAlignInside);
    }
    final value=node.properties['borderRadius']?.value;
    final radius=value is CanvasPhysicalBorderRadiusValue?_borderRadius(value) as BorderRadius:BorderRadius.zero;
    return TableBorder(
      top:side(mode=='all'?'all':mode=='symmetric'?'outside':'top'),
      right:side(mode=='all'?'all':mode=='symmetric'?'outside':'right'),
      bottom:side(mode=='all'?'all':mode=='symmetric'?'outside':'bottom'),
      left:side(mode=='all'?'all':mode=='symmetric'?'outside':'left'),
      horizontalInside:side(mode=='all'?'all':mode=='symmetric'?'inside':'horizontalInside'),
      verticalInside:side(mode=='all'?'all':mode=='symmetric'?'inside':'verticalInside'),borderRadius:radius);
  }

  Widget _flow() {
    // Identity belongs above instrumentation; preserve native child state on reorder.
    final children = [for (final c in node.slot('children')?.children ?? <CanvasNode>[])
      KeyedSubtree(key: ValueKey('canvas-flow-child-${c.id}'), child: _view(c))];
    return node.type.endsWith('.unwrapped')
        ? Flow.unwrapped(delegate: const _FlowPreviewDelegate(),
            clipBehavior: _clipBehavior() ?? Clip.hardEdge, children: children)
        : Flow(delegate: const _FlowPreviewDelegate(),
            clipBehavior: _clipBehavior() ?? Clip.hardEdge, children: children);
  }

  Widget _customPaint() {
    final size = node.properties['size']?.value;
    CustomPainter? painter(String name) => node.properties[name]?.kind == 'dartObjectReferencePresence'
        ? const _InertProjectPainter() : null;
    return CustomPaint(
      painter: painter('painter'),
      foregroundPainter: painter('foregroundPainter'),
      size: size is CanvasSizeValue ? Size(size.width, size.height) : Size.zero,
      isComplex: _boolean('isComplex') ?? false,
      willChange: _boolean('willChange') ?? false,
      child: _single('child'));
  }

  Widget _shaderMask(BuildContext context) {
    final local = node.properties['shaderCallback']?.value;
    final gradient = local is CanvasBoxGradientValue ? _boxGradient(context, local)
        : const LinearGradient(colors: [Color(0xffffffff), Color(0xffffffff)]);
    final direction = Directionality.maybeOf(context);
    return ShaderMask(
      shaderCallback: (bounds) => gradient.createShader(bounds, textDirection: direction),
      blendMode: BlendMode.values.byName(_enum('blendMode') ?? 'modulate'),
      child: _single('child'));
  }

  Widget _backdropFilter() {
    final value = node.properties['filterConfig'];
    final hasConfig = value != null && value.kind != 'null';
    final filter = hasConfig ? null : _localImageFilter('filter');
    final config = hasConfig ? _localBackdropConfig() : null;
    final blend = BlendMode.values.byName(_enum('blendMode') ?? 'srcOver');
    final enabled = _boolean('enabled') ?? true;
    return node.type.endsWith('.grouped')
        ? BackdropFilter.grouped(filter: filter, filterConfig: config, blendMode: blend, enabled: enabled, child: _single('child'))
        : BackdropFilter(filter: filter, filterConfig: config, blendMode: blend, enabled: enabled, child: _single('child'));
  }

  ImageFilterConfig _localBackdropConfig() => switch (_string('filterConfig')) {
    'wrap' => ImageFilterConfig(_localImageFilter('filter')),
    'blur' => ImageFilterConfig.blur(
      sigmaX: _number('configSigmaX') ?? 0, sigmaY: _number('configSigmaY') ?? 0,
      tileMode: TileMode.values.byName(_enum('configTileMode') ?? 'clamp'),
      bounded: _boolean('configBounded') ?? false),
    'compose' => const ImageFilterConfig.compose(inner: ImageFilterConfig.blur(), outer: ImageFilterConfig.blur()),
    _ => const ImageFilterConfig.blur(),
  };

  ui.ImageFilter _localImageFilter([String field = 'imageFilter']) {
    switch (_string(field)) {
      case 'blur':
        Rect? bounds;
        if (!node.properties.containsKey('bounds') &&
            ['boundsLeft', 'boundsTop', 'boundsWidth', 'boundsHeight'].any(node.properties.containsKey)) {
          bounds = Rect.fromLTWH(_number('boundsLeft') ?? 0, _number('boundsTop') ?? 0,
              _number('boundsWidth') ?? 0, _number('boundsHeight') ?? 0);
        }
        final tile = _enum('tileMode');
        return ui.ImageFilter.blur(sigmaX: _number('sigmaX') ?? 0, sigmaY: _number('sigmaY') ?? 0,
            tileMode: tile == null ? null : TileMode.values.byName(tile), bounds: bounds);
      case 'dilate':
        return ui.ImageFilter.dilate(radiusX: _number('radiusX') ?? 0, radiusY: _number('radiusY') ?? 0);
      case 'erode':
        return ui.ImageFilter.erode(radiusX: _number('radiusX') ?? 0, radiusY: _number('radiusY') ?? 0);
      case 'matrix':
        return ui.ImageFilter.matrix((_matrix4('matrix4') ?? Matrix4.identity()).storage,
            filterQuality: FilterQuality.values.byName(_enum('filterQuality') ?? 'medium'));
      case 'compose':
        return ui.ImageFilter.compose(inner: ui.ImageFilter.blur(), outer: ui.ImageFilter.blur());
      default:
        return ui.ImageFilter.blur();
    }
  }

  ColorFilter _localColorFilter(BuildContext context) {
    final preset=_string('colorFilter');
    if(preset=='mode') {
      return ColorFilter.mode(_resolvedColor(context,'color')??Colors.transparent,_blendMode(_enum('blendMode')??'srcOver'));
    }
    if(preset=='linearToSrgbGamma') return const ColorFilter.linearToSrgbGamma();
    if(preset=='srgbToLinearGamma') return const ColorFilter.srgbToLinearGamma();
    if(preset=='saturation') return ColorFilter.saturation(_number('saturation')??1);
    final project=node.properties['colorFilter']?.kind=='dartObjectReferencePresence';
    return ColorFilter.matrix([
      for(int row=0;row<4;row++) for(int col=0;col<5;col++)
        project ? (row==col?1.0:0.0) : (_number('m$row$col')??(row==col?1.0:0.0)),
    ]);
  }

  Widget _rawImage(BuildContext context) {
    final opacity = _number('opacity');
    return RawImage(
      // Project handles never cross the Canvas process boundary.
      image: null,
      debugImageLabel: _string('debugImageLabel'),
      width: _number('width'), height: _number('height'),
      scale: _number('scale') ?? 1,
      color: _resolvedColor(context, 'color'),
      opacity: opacity == null ? null : AlwaysStoppedAnimation<double>(opacity),
      colorBlendMode: _enum('colorBlendMode') == null ? null : _blendMode(_enum('colorBlendMode')!),
      fit: _enum('fit') == null ? null : _boxFit(_enum('fit')!),
      alignment: _alignmentGeometry('alignment') ?? Alignment.center,
      repeat: _imageRepeat(_enum('repeat') ?? 'noRepeat'),
      centerSlice: null,
      matchTextDirection: _boolean('matchTextDirection') ?? false,
      invertColors: _boolean('invertColors') ?? false,
      filterQuality: _filterQuality(_enum('filterQuality') ?? 'medium'),
      isAntiAlias: _boolean('isAntiAlias') ?? false,
    );
  }

  Widget _fadeInImage(BuildContext context) {
    ImageProvider<Object> provider(String name) {
      final value = node.properties[name]?.value;
      return value is CanvasImageProviderValue
          ? _imageProvider(value).provider : MemoryImage(_unavailableImageBytes);
    }
    Widget error(String name, Object failure, StackTrace? stack) {
      final value = node.properties[name]?.value;
      if (value is CanvasImageProviderValue && value.resolution is CanvasResolvedImageValue) {
        onImageError?.call((value.resolution as CanvasResolvedImageValue).resourceId, failure, stack);
      }
      return SizedBox(width: _number('width'), height: _number('height'));
    }
    return FadeInImage(
      placeholder: provider('placeholder'),
      placeholderErrorBuilder: (context, failure, stack) => error('placeholder', failure, stack),
      image: provider('image'),
      imageErrorBuilder: (context, failure, stack) => error('image', failure, stack),
      excludeFromSemantics: _boolean('excludeFromSemantics') ?? false,
      imageSemanticLabel: _string('imageSemanticLabel'),
      fadeOutDuration: Duration(microseconds: _integer('fadeOutDurationUs') ?? 300000),
      fadeOutCurve: _expansionCurves[_string('fadeOutCurve')] ?? Curves.easeOut,
      fadeInDuration: Duration(microseconds: _integer('fadeInDurationUs') ?? 700000),
      fadeInCurve: _expansionCurves[_string('fadeInCurve')] ?? Curves.easeIn,
      color: _resolvedColor(context, 'color'),
      colorBlendMode: _enum('colorBlendMode') == null ? null : _blendMode(_enum('colorBlendMode')!),
      placeholderColor: _resolvedColor(context, 'placeholderColor'),
      placeholderColorBlendMode: _enum('placeholderColorBlendMode') == null ? null : _blendMode(_enum('placeholderColorBlendMode')!),
      width: _number('width'), height: _number('height'),
      fit: _enum('fit') == null ? null : _boxFit(_enum('fit')!),
      placeholderFit: _enum('placeholderFit') == null ? null : _boxFit(_enum('placeholderFit')!),
      filterQuality: _filterQuality(_enum('filterQuality') ?? 'medium'),
      placeholderFilterQuality: _enum('placeholderFilterQuality') == null ? null : _filterQuality(_enum('placeholderFilterQuality')!),
      alignment: _alignmentGeometry('alignment') ?? Alignment.center,
      repeat: _imageRepeat(_enum('repeat') ?? 'noRepeat'),
      matchTextDirection: _boolean('matchTextDirection') ?? false,
    );
  }

  Widget _imageIcon(BuildContext context) {
    final value = node.properties['image']!.value;
    return ImageIcon(
      value is CanvasImageProviderValue ? _imageProvider(value).provider : null,
      size: _number('size'),
      color: _resolvedColor(context, 'color'),
      semanticLabel: _string('semanticLabel'),
    );
  }

  Rect? _imageCenterSlice() {
    final left = _number('centerSliceLeft');
    final top = _number('centerSliceTop');
    final right = _number('centerSliceRight');
    final bottom = _number('centerSliceBottom');
    if (left == null || top == null || right == null || bottom == null) {
      return null;
    }
    return Rect.fromLTRB(left, top, right, bottom);
  }

  Widget _textField(BuildContext context) {
    final expands = _boolean('expands') ?? false;
    final maxLength = _integer('maxLength');
    final cursorRadiusX = _number('cursorRadiusX');
    final cursorRadiusY = _number('cursorRadiusY');
    final scrollPaddingLeft = _number('scrollPaddingLeft');
    final scrollPaddingTop = _number('scrollPaddingTop');
    final scrollPaddingRight = _number('scrollPaddingRight');
    final scrollPaddingBottom = _number('scrollPaddingBottom');

    final field = IgnorePointer(
      ignoring: true,
      child: ExcludeFocus(
        excluding: true,
        child: Focus(
          canRequestFocus: false,
          skipTraversal: true,
          descendantsAreFocusable: false,
          descendantsAreTraversable: false,
          child: TextField(
            key: ValueKey('canvas-text-field-${node.id}'),
            keyboardType: _textInputType(),
            textInputAction: _textInputAction(),
            textCapitalization:
                _textCapitalization() ?? TextCapitalization.none,
            textAlign: _textAlign() ?? TextAlign.start,
            textAlignVertical: _textAlignVertical(),
            textDirection: _textDirection(),
            readOnly: _boolean('readOnly') ?? false,
            showCursor: _boolean('showCursor'),
            autofocus: _boolean('autofocus') ?? false,
            obscuringCharacter: _string('obscuringCharacter') ?? '•',
            obscureText: _boolean('obscureText') ?? false,
            autocorrect: _boolean('autocorrect'),
            smartDashesType: _smartDashesType(),
            smartQuotesType: _smartQuotesType(),
            enableSuggestions: _boolean('enableSuggestions') ?? true,
            maxLines: expands ? null : (_integer('maxLines') ?? 1),
            minLines: expands ? null : _integer('minLines'),
            expands: expands,
            maxLength: maxLength == -1 ? TextField.noMaxLength : maxLength,
            maxLengthEnforcement: _maxLengthEnforcement(),
            // Counter omission, explicit null and unknown project presence use
            // the SDK default. The limitation label never claims the unknown
            // project callback or its nullable return value was reproduced.
            contextMenuBuilder:
                node.properties['contextMenuBuilder']?.kind == 'null'
                ? null
                : const TextField().contextMenuBuilder,
            onChanged: _callbackPresent('onChanged') ? (value) {} : null,
            onEditingComplete: _callbackPresent('onEditingComplete')
                ? () {}
                : null,
            onSubmitted: _callbackPresent('onSubmitted') ? (value) {} : null,
            onAppPrivateCommand: _callbackPresent('onAppPrivateCommand')
                ? (action, data) {}
                : null,
            enabled: _boolean('enabled'),
            ignorePointers: _boolean('ignorePointers'),
            cursorWidth: _number('cursorWidth') ?? 2.0,
            cursorHeight: _number('cursorHeight'),
            cursorRadius: cursorRadiusX == null || cursorRadiusY == null
                ? null
                : Radius.elliptical(cursorRadiusX, cursorRadiusY),
            cursorOpacityAnimates: _boolean('cursorOpacityAnimates'),
            cursorColor: _resolvedColor(context, 'cursorColor'),
            cursorErrorColor: _resolvedColor(context, 'cursorErrorColor'),
            selectionHeightStyle: _boxHeightStyle(),
            selectionWidthStyle: _boxWidthStyle(),
            keyboardAppearance: _brightness('keyboardAppearance'),
            scrollPadding:
                scrollPaddingLeft == null ||
                    scrollPaddingTop == null ||
                    scrollPaddingRight == null ||
                    scrollPaddingBottom == null
                ? const EdgeInsets.all(20.0)
                : EdgeInsets.fromLTRB(
                    scrollPaddingLeft,
                    scrollPaddingTop,
                    scrollPaddingRight,
                    scrollPaddingBottom,
                  ),
            dragStartBehavior: _dragStartBehavior(),
            enableInteractiveSelection: _boolean('enableInteractiveSelection'),
            selectAllOnFocus: _boolean('selectAllOnFocus'),
            onTap: _callbackPresent('onTap') ? () {} : null,
            onTapAlwaysCalled: _boolean('onTapAlwaysCalled') ?? false,
            onTapOutside: _callbackPresent('onTapOutside') ? (event) {} : null,
            onTapUpOutside: _callbackPresent('onTapUpOutside')
                ? (event) {}
                : null,
            mouseCursor: _mouseCursor('mouseCursor'),
            clipBehavior: _clipBehavior() ?? Clip.hardEdge,
            restorationId: _string('restorationId'),
            stylusHandwritingEnabled:
                _boolean('stylusHandwritingEnabled') ??
                EditableText.defaultStylusHandwritingEnabled,
            enableIMEPersonalizedLearning:
                _boolean('enableIMEPersonalizedLearning') ?? true,
            enableInlinePrediction: _boolean('enableInlinePrediction'),
            canRequestFocus: _boolean('canRequestFocus') ?? true,
          ),
        ),
      ),
    );

    return _TextButtonPreview(
      message: _textFieldBuilderPreviewMessage(node) ?? '',
      child: LayoutBuilder(
        builder: (context, constraints) {
          final guardWidth = constraints.maxWidth.isInfinite;
          final guardHeight = expands && constraints.maxHeight.isInfinite;
          return SizedBox(
            width: guardWidth ? 240 : null,
            height: guardHeight ? 120 : null,
            child: field,
          );
        },
      ),
    );
  }

  ColorFilter _colorFilter(
    BuildContext context,
    CanvasColorFilterValue value,
  ) => switch (value) {
    CanvasModeColorFilterValue() => ColorFilter.mode(
      _colorSource(context, value.color),
      _blendMode(value.blendMode),
    ),
    CanvasMatrixColorFilterValue() => ColorFilter.matrix(value.values),
    CanvasLinearToSrgbGammaColorFilterValue() =>
      const ColorFilter.linearToSrgbGamma(),
    CanvasSrgbToLinearGammaColorFilterValue() =>
      const ColorFilter.srgbToLinearGamma(),
    CanvasSaturationColorFilterValue() => ColorFilter.saturation(value.value),
  };

  BoxFit _boxFit(String value) => switch (value) {
    'fill' => BoxFit.fill,
    'contain' => BoxFit.contain,
    'cover' => BoxFit.cover,
    'fitWidth' => BoxFit.fitWidth,
    'fitHeight' => BoxFit.fitHeight,
    'none' => BoxFit.none,
    _ => BoxFit.scaleDown,
  };

  ImageRepeat _imageRepeat(String value) => switch (value) {
    'repeat' => ImageRepeat.repeat,
    'repeatX' => ImageRepeat.repeatX,
    'repeatY' => ImageRepeat.repeatY,
    _ => ImageRepeat.noRepeat,
  };

  FilterQuality _filterQuality(String value) => switch (value) {
    'none' => FilterQuality.none,
    'low' => FilterQuality.low,
    'high' => FilterQuality.high,
    _ => FilterQuality.medium,
  };

  String _imageStatusSemantics() {
    final statuses = <String>[];
    if (node.type == 'flutter.material.CircleAvatar' ||
        node.type == 'flutter.material.Switch' ||
        node.type == 'flutter.material.SwitchListTile') {
      for (final name
          in node.type != 'flutter.material.CircleAvatar'
              ? const ['activeThumbImage', 'inactiveThumbImage']
              : const ['backgroundImage', 'foregroundImage']) {
        final provider = node.properties[name]?.value;
        if (provider is CanvasImageProviderValue) {
          _appendImageStatus(statuses, provider, propertyName: name);
        }
      }
    }
    if (node.type == 'flutter.widgets.FadeInImage') {
      final placeholder = node.properties['placeholder']?.value;
      if (placeholder is CanvasImageProviderValue) _appendImageStatus(statuses, placeholder, propertyName: 'placeholder');
    }
    final directProvider = node.properties['image']?.value;
    if (directProvider is CanvasImageProviderValue) {
      _appendImageStatus(statuses, directProvider);
    }
    for (final name in const ['decoration', 'foregroundDecoration']) {
      final decoration = node.properties[name]?.value;
      if (decoration is! CanvasBoxDecorationValue || decoration.image == null) {
        continue;
      }
      _appendImageStatus(statuses, decoration.image!.image);
    }
    return statuses.isEmpty ? '' : '. ${statuses.join('. ')}';
  }

  void _appendImageStatus(
    List<String> statuses,
    CanvasImageProviderValue provider, {
    String? propertyName,
  }) {
    final resolution = provider.resolution;
    final identity =
        '${propertyName == null ? '' : '$propertyName: '}'
        '${_imageProviderIdentity(provider)}';
    if (resolution is CanvasUnavailableImageValue) {
      statuses.add(
        'Image preview unavailable for $identity. '
        'Status ${resolution.code}. Reason: ${resolution.reason}',
      );
    } else if (resolution is CanvasResolvedImageValue &&
        imageResources[resolution.resourceId] == null) {
      final rejection = imageResources.rejection(resolution.resourceId);
      statuses.add(
        'Image preview unavailable for $identity. '
        'Status ${rejection?.code ?? 'missing'}. Reason: '
        '${rejection?.reason ?? 'the content-addressed resource is not bound to this revision'}',
      );
    }
  }

  String _imageProviderIdentity(CanvasImageProviderValue provider) {
    final packageName = provider.packageName;
    return packageName == null
        ? 'app:${provider.assetName}'
        : 'package:$packageName:${provider.assetName}';
  }

  BoxBorder _boxBorder(BuildContext context, CanvasBoxBorderValue value) =>
      switch (value) {
        CanvasPhysicalBoxBorderValue() => Border(
          top: _borderSide(context, value.top),
          right: _borderSide(context, value.right),
          bottom: _borderSide(context, value.bottom),
          left: _borderSide(context, value.left),
        ),
        CanvasDirectionalBoxBorderValue() => BorderDirectional(
          top: _borderSide(context, value.top),
          start: _borderSide(context, value.start),
          end: _borderSide(context, value.end),
          bottom: _borderSide(context, value.bottom),
        ),
      };

  BorderSide _borderSide(BuildContext context, CanvasBorderSideValue value) =>
      BorderSide(
        color: _colorSource(context, value.color),
        width: value.width,
        style: value.style == 'none' ? BorderStyle.none : BorderStyle.solid,
        strokeAlign: value.strokeAlign,
      );

  BorderRadiusGeometry _borderRadius(CanvasBorderRadiusGeometryValue value) =>
      switch (value) {
        CanvasPhysicalBorderRadiusValue() => BorderRadius.only(
          topLeft: _radius(value.topLeft),
          topRight: _radius(value.topRight),
          bottomRight: _radius(value.bottomRight),
          bottomLeft: _radius(value.bottomLeft),
        ),
        CanvasDirectionalBorderRadiusValue() => BorderRadiusDirectional.only(
          topStart: _radius(value.topStart),
          topEnd: _radius(value.topEnd),
          bottomEnd: _radius(value.bottomEnd),
          bottomStart: _radius(value.bottomStart),
        ),
      };

  Radius _radius(CanvasRadiusValue value) =>
      Radius.elliptical(value.x, value.y);

  Gradient _boxGradient(BuildContext context, CanvasBoxGradientValue value) {
    final colors = [
      for (final stop in value.stops) _colorSource(context, stop.color),
    ];
    final stops = [for (final stop in value.stops) stop.stop];
    final transform = value.rotationRadians == null
        ? null
        : GradientRotation(value.rotationRadians!);
    return switch (value) {
      CanvasLinearGradientValue() => LinearGradient(
        begin: _alignmentGeometryValue(value.begin),
        end: _alignmentGeometryValue(value.end),
        colors: colors,
        stops: stops,
        tileMode: _tileMode(value.tileMode),
        transform: transform,
      ),
      CanvasRadialGradientValue() => RadialGradient(
        center: _alignmentGeometryValue(value.center),
        radius: value.radius,
        colors: colors,
        stops: stops,
        tileMode: _tileMode(value.tileMode),
        focal: value.focal == null
            ? null
            : _alignmentGeometryValue(value.focal!),
        focalRadius: value.focalRadius,
        transform: transform,
      ),
      CanvasSweepGradientValue() => SweepGradient(
        center: _alignmentGeometryValue(value.center),
        startAngle: value.startAngle,
        endAngle: value.endAngle,
        colors: colors,
        stops: stops,
        tileMode: _tileMode(value.tileMode),
        transform: transform,
      ),
    };
  }

  AlignmentGeometry _alignmentGeometryValue(
    CanvasAlignmentGeometryValue value,
  ) => value.basis == 'directional'
      ? AlignmentDirectional(value.horizontal, value.vertical)
      : Alignment(value.horizontal, value.vertical);

  TileMode _tileMode(String value) => switch (value) {
    'repeated' => TileMode.repeated,
    'mirror' => TileMode.mirror,
    'decal' => TileMode.decal,
    _ => TileMode.clamp,
  };

  BlurStyle _blurStyle(String value) => switch (value) {
    'solid' => BlurStyle.solid,
    'outer' => BlurStyle.outer,
    'inner' => BlurStyle.inner,
    _ => BlurStyle.normal,
  };

  Widget _padding(EdgeInsetsGeometry insets) =>
      Padding(padding: insets, child: _single('child'));

  Widget _aspectRatio() => AspectRatio(
    aspectRatio: _number('aspectRatio')!,
    child: _single('child'),
  );

  Widget _baseline() => Baseline(
    baseline: _number('baseline')!,
    baselineType: _textBaseline('baselineType')!,
    child: _single('child'),
  );

  Widget _intrinsicHeight() => IntrinsicHeight(child: _single('child'));

  Widget _intrinsicWidth() => IntrinsicWidth(
    stepWidth: _number('stepWidth'),
    stepHeight: _number('stepHeight'),
    child: _single('child'),
  );

  Widget _offstage() =>
      Offstage(offstage: _boolean('offstage') ?? true, child: _single('child'));

  Widget _rotatedBox() => RotatedBox(
    quarterTurns: _integer('quarterTurns')!,
    child: _single('child'),
  );

  Widget _preferredSize() {
    final preferredSize =
        node.properties['preferredSize']!.value as CanvasSizeValue;
    return PreferredSize(
      preferredSize: Size(preferredSize.width, preferredSize.height),
      child: _single('child')!,
    );
  }

  Widget _builder() =>
      Builder(builder: (_) => const SizedBox(width: 48, height: 36));

  Widget _sizedOverflowBox() {
    final requestedSize = node.properties['size']!.value as CanvasSizeValue;
    return SizedOverflowBox(
      size: Size(requestedSize.width, requestedSize.height),
      alignment: _alignmentGeometry('alignment') ?? Alignment.center,
      child: _single('child'),
    );
  }

  Widget _transform(Widget child) {
    final filterQuality = _enum('filterQuality');
    return Transform(
      key: ValueKey('canvas-transform-${node.id}'),
      transform: _matrix4('transform')!,
      origin: _offset('origin'),
      alignment: _alignmentGeometry('alignment'),
      transformHitTests: _boolean('transformHitTests') ?? true,
      filterQuality: filterQuality == null
          ? null
          : _filterQuality(filterQuality),
      child: child,
    );
  }

  Widget _align() => Align(
    alignment: _alignmentGeometry('alignment') ?? Alignment.center,
    widthFactor: _number('widthFactor'),
    heightFactor: _number('heightFactor'),
    child: _single('child'),
  );

  Widget _fractionallySizedBox() => FractionallySizedBox(
    alignment: _alignmentGeometry('alignment') ?? Alignment.center,
    widthFactor: _number('widthFactor'),
    heightFactor: _number('heightFactor'),
    child: _single('child'),
  );

  Widget _fittedBox() => FittedBox(
    fit: _boxFit(_enum('fit') ?? 'contain'),
    alignment: _alignmentGeometry('alignment') ?? Alignment.center,
    clipBehavior: _clipBehavior() ?? Clip.none,
    child: _single('child'),
  );

  Widget _clipRect() {
    if (node.properties['clipper']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipRect',
        expectedType: 'CustomClipper<Rect>',
      );
    }
    return ClipRect(
      clipBehavior: _clipBehavior() ?? Clip.hardEdge,
      child: _single('child'),
    );
  }

  Widget _clipOval() {
    if (node.properties['clipper']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipOval',
        expectedType: 'CustomClipper<Rect>',
      );
    }
    return ClipOval(
      clipBehavior: _clipBehavior() ?? Clip.antiAlias,
      child: _single('child'),
    );
  }

  Widget _clipRRect(BuildContext context) {
    if (node.properties['clipper']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipRRect',
        expectedType: 'CustomClipper<RRect>',
      );
    }
    final radius = node.properties['borderRadius']?.value;
    return ClipRRect(
      borderRadius: radius is CanvasBorderRadiusGeometryValue
          ? _borderRadius(radius).resolve(Directionality.of(context))
          : BorderRadius.zero,
      clipBehavior: _clipBehavior() ?? Clip.antiAlias,
      child: _single('child'),
    );
  }

  Widget _clipRSuperellipse(BuildContext context) {
    if (node.properties['clipper']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipRSuperellipse',
        expectedType: 'CustomClipper<RSuperellipse>',
      );
    }
    final radius = node.properties['borderRadius']?.value;
    return ClipRSuperellipse(
      borderRadius: radius is CanvasBorderRadiusGeometryValue
          ? _borderRadius(radius).resolve(Directionality.of(context))
          : BorderRadius.zero,
      clipBehavior: _clipBehavior() ?? Clip.antiAlias,
      child: _single('child'),
    );
  }

  Widget _clipPath() {
    if (node.properties['shape']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipPath.shape',
        expectedType: 'ShapeBorder',
        previewLabel: 'Custom shape\npreview unavailable',
      );
    }
    if (node.properties['clipper']?.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'ClipPath',
        expectedType: 'CustomClipper<Path>',
      );
    }
    return ClipPath(
      clipBehavior: _clipBehavior() ?? Clip.antiAlias,
      child: _single('child'),
    );
  }

  Widget _refreshProgressIndicator(BuildContext context) => LayoutBuilder(
    builder: (context, constraints) {
      final failure = _refreshProgressUnavailableMessage(
        node,
        context,
        constraints: constraints,
      );
      if (failure != null) {
        return _customClipperPreviewUnavailable(
          widgetName: 'RefreshProgressIndicator',
          expectedType: 'Animation<Color?>',
          previewLabel: 'Refresh preview\nunavailable',
          messageOverride: failure,
        );
      }
      final color = node.properties['valueColor'];
      final animation = color == null
          ? null
          : AlwaysStoppedAnimation<Color?>(
              color.kind == 'null'
                  ? null
                  : _resolvedColor(context, 'valueColor'),
            );
      final cap = _enum('strokeCap');
      return RefreshProgressIndicator(
        value: _number('value'),
        backgroundColor: _resolvedColor(context, 'backgroundColor'),
        color: _resolvedColor(context, 'color'),
        valueColor: animation,
        // Explicit null inherits strokeWidth; omission retains the SDK's distinct
        // constructor default. Neither creates an explicit model property.
        strokeWidth: node.properties.containsKey('strokeWidth')
            ? _number('strokeWidth')
            : RefreshProgressIndicator.defaultStrokeWidth,
        strokeAlign: _number('strokeAlign'),
        strokeCap: cap == null
            ? null
            : StrokeCap.values.firstWhere((v) => v.name == cap),
        semanticsLabel: _string('semanticsLabel'),
        semanticsValue: _string('semanticsValue'),
        elevation: _number('elevation') ?? 2,
        indicatorMargin:
            _edgeInsetsGeometry('indicatorMargin') ?? const EdgeInsets.all(4),
        indicatorPadding:
            _edgeInsetsGeometry('indicatorPadding') ?? const EdgeInsets.all(12),
      );
    },
  );

  Widget _circularProgressIndicator(BuildContext context) {
    Widget unavailable(String message) => _customClipperPreviewUnavailable(
      widgetName: 'CircularProgressIndicator',
      expectedType: 'Animation',
      previewLabel: 'Progress preview\nunavailable',
      messageOverride: message,
    );
    double? number(String name) {
      final value = node.properties[name]?.value;
      return value is CanvasEnumValue
          ? double.infinity
          : (value as num?)?.toDouble();
    }

    return LayoutBuilder(
      builder: (context, constraints) {
        final failure = _circularProgressUnavailableMessage(
          node,
          context,
          constraints: constraints,
        );
        if (failure != null) return unavailable(failure);
        final property = node.properties['valueColor'];
        final Animation<Color?>? animation =
            property == null || property.kind == 'dartObjectReferencePresence'
            ? null
            : AlwaysStoppedAnimation<Color?>(
                property.kind == 'null'
                    ? null
                    : _resolvedColor(context, 'valueColor'),
              );
        final cap = _enum('strokeCap');
        final strokeCap = cap == null
            ? null
            : StrokeCap.values.firstWhere((value) => value.name == cap);
        // Cupertino ignores references; do not execute them or supply fake live
        // objects. The actual adaptive SDK constructor chooses the platform branch.
        if (_string('variant') == 'adaptive') {
          return CircularProgressIndicator.adaptive(
            value: _number('value'),
            backgroundColor: _resolvedColor(context, 'backgroundColor'),
            valueColor: animation,
            strokeWidth: number('strokeWidth'),
            strokeAlign: number('strokeAlign'),
            semanticsLabel: _string('semanticsLabel'),
            semanticsValue: _string('semanticsValue'),
            strokeCap: strokeCap,
            constraints: _boxConstraints('constraints'),
            trackGap: number('trackGap'),
            // ignore: deprecated_member_use
            year2023: _boolean('year2023'),
            padding: _edgeInsetsGeometry('padding'),
          );
        }
        return CircularProgressIndicator(
          value: _number('value'),
          backgroundColor: _resolvedColor(context, 'backgroundColor'),
          color: _resolvedColor(context, 'color'),
          valueColor: animation,
          strokeWidth: number('strokeWidth'),
          strokeAlign: number('strokeAlign'),
          semanticsLabel: _string('semanticsLabel'),
          semanticsValue: _string('semanticsValue'),
          strokeCap: strokeCap,
          constraints: _boxConstraints('constraints'),
          trackGap: number('trackGap'),
          // ignore: deprecated_member_use
          year2023: _boolean('year2023'),
          padding: _edgeInsetsGeometry('padding'),
        );
      },
    );
  }

  Widget _linearProgressIndicator(BuildContext context) {
    Widget unavailable(String message) => _customClipperPreviewUnavailable(
      widgetName: 'LinearProgressIndicator',
      expectedType: 'Animation',
      previewLabel: 'Progress preview\nunavailable',
      messageOverride: message,
    );
    double? number(String name) {
      final value = node.properties[name]?.value;
      return value is CanvasEnumValue
          ? double.infinity
          : (value as num?)?.toDouble();
    }

    final failure = _linearProgressUnavailableMessage(node, context: context);
    if (failure != null) return unavailable(failure);
    final valueColor = node.properties['valueColor'];
    final Animation<Color?>? animation = valueColor == null
        ? null
        : AlwaysStoppedAnimation<Color?>(
            valueColor.kind == 'null'
                ? null
                : _resolvedColor(context, 'valueColor'),
          );
    final radius = node.properties['borderRadius']?.value;
    return LayoutBuilder(
      builder: (context, constraints) {
        final failure = _linearProgressUnavailableMessage(
          node,
          context: context,
          constraints: constraints,
        );
        if (failure != null) return unavailable(failure);
        final minHeight = number('minHeight');
        return LinearProgressIndicator(
          value: _number('value'),
          backgroundColor: _resolvedColor(context, 'backgroundColor'),
          color: _resolvedColor(context, 'color'),
          valueColor: animation,
          minHeight: minHeight,
          semanticsLabel: _string('semanticsLabel'),
          semanticsValue: _string('semanticsValue'),
          borderRadius: radius is CanvasBorderRadiusGeometryValue
              ? _borderRadius(radius)
              : null,
          stopIndicatorColor: _resolvedColor(context, 'stopIndicatorColor'),
          stopIndicatorRadius: number('stopIndicatorRadius'),
          trackGap: number('trackGap'),
          // ignore: deprecated_member_use
          year2023: _boolean('year2023'),
        );
      },
    );
  }

  Widget _circleAvatar(BuildContext context) {
    ({ImageProvider<Object>? provider, ImageErrorListener? onError}) image(
      String name,
    ) {
      final value = node.properties[name]?.value;
      if (value is! CanvasImageProviderValue) {
        return (provider: null, onError: null);
      }
      final binding = _imageProvider(value);
      // A checkerboard foreground would hide the valid background/initials.
      // A missing/rejected layer is omitted with slot-specific diagnostics,
      // retaining CircleAvatar's actual SDK foreground/background fallback.
      if (binding.placeholder) return (provider: null, onError: null);
      final resource = binding.resolution as CanvasResolvedImageValue;
      return (
        provider: binding.provider,
        // This is a Canvas safety reporter, not execution of the model's Dart
        // callback reference. It also protects previews without a callback.
        onError: (error, stack) =>
            onImageError?.call(resource.resourceId, error, stack),
      );
    }

    double? radius(String name) {
      final value = node.properties[name]?.value;
      return value is CanvasEnumValue
          ? double.infinity
          : (value as num?)?.toDouble();
    }

    final fixed = radius('radius');
    final minimum = radius('minRadius');
    final maximum = radius('maxRadius');
    final defaults = fixed == null && minimum == null && maximum == null;
    final minDiameter = defaults ? 40.0 : 2 * (fixed ?? minimum ?? 0);
    final maxDiameter = defaults
        ? 40.0
        : 2 * (fixed ?? maximum ?? double.infinity);
    final background = image('backgroundImage');
    final foreground = image('foregroundImage');
    final avatar = CircleAvatar(
      // Flutter cannot interpolate finite and infinite BoxConstraints. Reset
      // only that SDK animation shell on this boundary; global Canvas child
      // keys preserve the existing subtree's state/focus across the reparent.
      key: ValueKey((
        'circle-avatar-constraint-finiteness',
        minDiameter.isInfinite,
        maxDiameter.isInfinite,
      )),
      backgroundColor: _resolvedColor(context, 'backgroundColor'),
      foregroundColor: _resolvedColor(context, 'foregroundColor'),
      backgroundImage: background.provider,
      foregroundImage: foreground.provider,
      onBackgroundImageError: background.onError,
      onForegroundImageError: foreground.onError,
      radius: fixed,
      minRadius: minimum,
      maxRadius: maximum,
      child: _single('child'),
    );
    // AnimatedContainer can reach zero after the model revision's first
    // layout. Refresh the surface-only selection handle when that animation
    // changes size, without inserting a visible child or changing SDK bounds.
    return NotificationListener<SizeChangedLayoutNotification>(
      onNotification: (_) {
        context
            .findAncestorStateOfType<_CanvasDocumentViewState>()
            ?._refreshZeroSizedWidgetTargetsAfterFrame();
        return false;
      },
      child: SizeChangedLayoutNotifier(child: avatar),
    );
  }

  Widget _floatingActionButton(BuildContext context) {
    final variant = _string('variant');
    final extended = variant == 'extended';
    final isExtended = _boolean('isExtended') ?? extended;
    double? number(String name) =>
        node.properties[name]?.kind == 'enum' ? double.infinity : _number(name);
    final spacing = number('extendedIconLabelSpacing');
    final unavailable = _fabLayoutMessage(node, context);
    if (unavailable != null) {
      return _customClipperPreviewUnavailable(
        widgetName: 'FloatingActionButton',
        expectedType: 'safe resolved SDK layout and elevation',
        previewLabel: 'FAB\npreview unavailable',
        messageOverride: unavailable,
      );
    }
    final onPressed = (_boolean('enabled') ?? true) ? () {} : null;
    final tooltip = _string('tooltip');
    final foreground = _resolvedColor(context, 'foregroundColor');
    final background = _resolvedColor(context, 'backgroundColor');
    final focus = _resolvedColor(context, 'focusColor');
    final hover = _resolvedColor(context, 'hoverColor');
    final splash = _resolvedColor(context, 'splashColor');
    final hero = _fabHeroTag(node);
    final elevation = number('elevation');
    final focusElevation = number('focusElevation');
    final hoverElevation = number('hoverElevation');
    final highlightElevation = number('highlightElevation');
    final disabledElevation = number('disabledElevation');
    final mouse = _mouseCursor('mouseCursor');
    final shape =
        _cardShapePreviewUnavailableMessage(
              node,
              widgetName: 'FloatingActionButton',
            ) ==
            null
        ? _cardShape(context)
        : null;
    final clip = _clipBehavior() ?? Clip.none;
    final autofocus = _boolean('autofocus') ?? false;
    final tapSize = switch (_enumOrString('materialTapTargetSize')) {
      'padded' => MaterialTapTargetSize.padded,
      'shrinkWrap' => MaterialTapTargetSize.shrinkWrap,
      _ => null,
    };
    final feedback = _boolean('enableFeedback');
    final child = _single('child');
    // SDK Material cannot interpolate infinity back to finite. All-infinite
    // states are safe; restart only that SDK subtree when crossing the boundary.
    // Model child GlobalKeys retain actual editing state across this reparent.
    final sdkKey = ValueKey((
      'fab-elevation-kind',
      _fabElevations(node, context).every((value) => value.isInfinite),
    ));
    final button = switch (variant) {
      'small' => FloatingActionButton.small(
        key: sdkKey,
        onPressed: onPressed,
        tooltip: tooltip,
        foregroundColor: foreground,
        backgroundColor: background,
        focusColor: focus,
        hoverColor: hover,
        splashColor: splash,
        heroTag: hero,
        elevation: elevation,
        focusElevation: focusElevation,
        hoverElevation: hoverElevation,
        highlightElevation: highlightElevation,
        disabledElevation: disabledElevation,
        mouseCursor: mouse,
        shape: shape,
        clipBehavior: clip,
        autofocus: autofocus,
        materialTapTargetSize: tapSize,
        enableFeedback: feedback,
        child: child,
      ),
      'large' => FloatingActionButton.large(
        key: sdkKey,
        onPressed: onPressed,
        tooltip: tooltip,
        foregroundColor: foreground,
        backgroundColor: background,
        focusColor: focus,
        hoverColor: hover,
        splashColor: splash,
        heroTag: hero,
        elevation: elevation,
        focusElevation: focusElevation,
        hoverElevation: hoverElevation,
        highlightElevation: highlightElevation,
        disabledElevation: disabledElevation,
        mouseCursor: mouse,
        shape: shape,
        clipBehavior: clip,
        autofocus: autofocus,
        materialTapTargetSize: tapSize,
        enableFeedback: feedback,
        child: child,
      ),
      'extended' => FloatingActionButton.extended(
        key: sdkKey,
        onPressed: onPressed,
        tooltip: tooltip,
        foregroundColor: foreground,
        backgroundColor: background,
        focusColor: focus,
        hoverColor: hover,
        splashColor: splash,
        heroTag: hero,
        elevation: elevation,
        focusElevation: focusElevation,
        hoverElevation: hoverElevation,
        highlightElevation: highlightElevation,
        disabledElevation: disabledElevation,
        mouseCursor: mouse,
        shape: shape,
        clipBehavior: clip,
        autofocus: autofocus,
        materialTapTargetSize: tapSize,
        enableFeedback: feedback,
        isExtended: isExtended,
        extendedIconLabelSpacing: spacing,
        extendedPadding: _edgeInsetsGeometry('extendedPadding'),
        extendedTextStyle: _textStyle(context, 'extendedTextStyle'),
        icon: _single('icon'),
        label: child!,
      ),
      _ => FloatingActionButton(
        key: sdkKey,
        onPressed: onPressed,
        tooltip: tooltip,
        foregroundColor: foreground,
        backgroundColor: background,
        focusColor: focus,
        hoverColor: hover,
        splashColor: splash,
        heroTag: hero,
        elevation: elevation,
        focusElevation: focusElevation,
        hoverElevation: hoverElevation,
        highlightElevation: highlightElevation,
        disabledElevation: disabledElevation,
        mouseCursor: mouse,
        shape: shape,
        clipBehavior: clip,
        autofocus: autofocus,
        materialTapTargetSize: tapSize,
        enableFeedback: feedback,
        mini: _boolean('mini') ?? false,
        isExtended: isExtended,
        child: child,
      ),
    };
    return _TextButtonPreview(
      message: _fabPreviewMessage(node, context) ?? '',
      child: button,
    );
  }

  Widget _badge(BuildContext context) {
    final backgroundColor = _resolvedColor(context, 'backgroundColor');
    final textColor = _resolvedColor(context, 'textColor');
    final smallSize = _number('smallSize');
    final largeSize = _number('largeSize');
    final textStyle = _textStyle(context, 'textStyle');
    final padding = _edgeInsetsGeometry('padding');
    final alignment = _alignmentGeometry('alignment');
    final offset = _offset('offset');
    final isLabelVisible = _boolean('isLabelVisible') ?? true;
    final child = _single('child');
    final count = _integer('count');
    // Flutter 3.44.8's _IntrinsicHorizontalStadium has no updateRenderObject:
    // its minSize otherwise remains stale after a local or BadgeTheme edit.
    // Recreate only the SDK Badge for a changed effective minimum. Existing
    // global node keys reparent actual child/label state, including edit focus.
    final hasLabel = count != null || node.slot('label')?.child != null;
    final sdkKey = ValueKey((
      'badge-stadium-minimum',
      isLabelVisible && hasLabel
          ? largeSize ?? BadgeTheme.of(context).largeSize ?? 16.0
          : null,
    ));
    if (count != null) {
      return Badge.count(
        key: sdkKey,
        backgroundColor: backgroundColor,
        textColor: textColor,
        smallSize: smallSize,
        largeSize: largeSize,
        textStyle: textStyle,
        padding: padding,
        alignment: alignment,
        offset: offset,
        count: count,
        maxCount: _integer('maxCount') ?? 999,
        isLabelVisible: isLabelVisible,
        child: child,
      );
    }
    return Badge(
      key: sdkKey,
      backgroundColor: backgroundColor,
      textColor: textColor,
      smallSize: smallSize,
      largeSize: largeSize,
      textStyle: textStyle,
      padding: padding,
      alignment: alignment,
      offset: offset,
      label: _single('label'),
      isLabelVisible: isLabelVisible,
      child: child,
    );
  }

  SnackBarAction _snackBarAction(BuildContext context, {Key? key}) => SnackBarAction(
    key:key ?? (_string('key') == null ? null : ValueKey<String>(_string('key')!)),
    label:_string('label') ?? 'Project label preview unavailable',
    onPressed:(){},
    textColor:_resolvedColor(context,'textColor'), disabledTextColor:_resolvedColor(context,'disabledTextColor'),
    backgroundColor:_resolvedColor(context,'backgroundColor'), disabledBackgroundColor:_resolvedColor(context,'disabledBackgroundColor'));

  Widget _snackBar(BuildContext context) {
    final unavailable = _cardShapePreviewUnavailableMessage(node,widgetName:'SnackBar');
    if (unavailable != null) {
      return _customClipperPreviewUnavailable(widgetName:'SnackBar.shape',expectedType:'ShapeBorder',
        previewLabel:'SnackBar shape preview unavailable',messageOverride:unavailable);
    }
    EdgeInsetsGeometry? insets(String name) => const {'edgeInsets','edgeInsetsDirectional'}.contains(node.properties[name]?.kind) ? _edgeInsetsGeometry(name) : null;
    final children = node.slot('action')?.children ?? <CanvasNode>[];
    final action = children.isEmpty ? null : _view(children.single)._snackBarAction(context,key:nodeKey(children.single.id));
    return ExcludeFocus(child:AbsorbPointer(child:SnackBar(
      key:_string('key') == null ? null : ValueKey<String>(_string('key')!),
      content:_single('content')!, action:action,
      backgroundColor:_resolvedColor(context,'backgroundColor'),elevation:_number('elevation'),
      margin:insets('margin'),padding:insets('padding'),width:_number('width'),shape:_cardShape(context),
      hitTestBehavior:_enum('hitTestBehavior')==null?null:HitTestBehavior.values.byName(_enum('hitTestBehavior')!),
      behavior:_enum('behavior')==null?null:SnackBarBehavior.values.byName(_enum('behavior')!),
      actionOverflowThreshold:_number('actionOverflowThreshold'),showCloseIcon:_boolean('showCloseIcon'),
      closeIconColor:_resolvedColor(context,'closeIconColor'),duration:Duration(microseconds:_integer('durationUs')??4000000),
      persist:_boolean('persist'),animation:const AlwaysStoppedAnimation<double>(1),onVisible:(){},
      dismissDirection:_enum('dismissDirection')==null?null:DismissDirection.values.byName(_enum('dismissDirection')!),
      clipBehavior:_clipBehavior()??Clip.hardEdge)));
  }

  Widget _bottomSheet(BuildContext context) {
    final unavailable = _cardShapePreviewUnavailableMessage(node, widgetName: 'BottomSheet');
    if (unavailable != null) {
      return _customClipperPreviewUnavailable(widgetName:'BottomSheet.shape', expectedType:'ShapeBorder',
        previewLabel:'BottomSheet shape preview unavailable',messageOverride:unavailable);
    }
    final size = node.properties['dragHandleSize']?.value;
    final child = node.properties['builder']?.kind == 'dartObjectReferencePresence'
        ? const SizedBox(width:240,height:64,child:Center(child:Text('BottomSheet builder preview unavailable')))
        : _single('child') ?? const SizedBox.shrink();
    return _BottomSheetPreview(
      sheetKey:_string('key') == null ? null : ValueKey<String>(_string('key')!),
      enableDrag:_boolean('enableDrag') ?? true, showDragHandle:_boolean('showDragHandle'),
      dragHandleColor:_resolvedColor(context,'dragHandleColor'),
      dragHandleSize:size is CanvasSizeValue ? Size(size.width,size.height) : null,
      backgroundColor:_resolvedColor(context,'backgroundColor'), shadowColor:_resolvedColor(context,'shadowColor'),
      elevation:_number('elevation'), shape:_cardShape(context), clipBehavior:_clipBehavior(),
      constraints:_boxConstraints('constraints'), child:child);
  }

  Widget _simpleDialog(BuildContext context) {
    if (node.type == canvasSimpleDialogOptionType) {
      return SimpleDialogOption(
        key: _string('key') == null ? null : ValueKey<String>(_string('key')!),
        onPressed: node.properties['onPressed']?.kind == 'dartObjectReferencePresence' ? () {} : null,
        padding: node.properties['padding']?.value is CanvasEdgeInsets ? _physicalEdgeInsets('padding') : null,
        child: _single('child'));
    }
    final unavailable = _cardShapePreviewUnavailableMessage(node, widgetName: 'SimpleDialog');
    if (unavailable != null) {
      return _customClipperPreviewUnavailable(widgetName: 'SimpleDialog.shape', expectedType: 'ShapeBorder',
        previewLabel: 'SimpleDialog shape\\npreview unavailable', messageOverride: unavailable);
    }
    EdgeInsetsGeometry? padding(String name) => const {'edgeInsets', 'edgeInsetsDirectional'}.contains(node.properties[name]?.kind)
        ? _edgeInsetsGeometry(name) : null;
    return SimpleDialog(
      key: _string('key') == null ? null : ValueKey<String>(_string('key')!),
      title: _single('title'),
      titlePadding: padding('titlePadding') ?? const EdgeInsets.fromLTRB(24, 24, 24, 0),
      contentPadding: padding('contentPadding') ?? const EdgeInsets.fromLTRB(0, 12, 0, 16),
      titleTextStyle: _textStyle(context, 'titleTextStyle'), contentTextStyle: _textStyle(context, 'contentTextStyle'),
      backgroundColor: _resolvedColor(context, 'backgroundColor'), elevation: _number('elevation'),
      shadowColor: _resolvedColor(context, 'shadowColor'), surfaceTintColor: _resolvedColor(context, 'surfaceTintColor'),
      semanticLabel: _string('semanticLabel'), clipBehavior: _clipBehavior(), shape: _cardShape(context),
      alignment: _alignmentGeometry('alignment'), constraints: _boxConstraints('constraints'),
      insetPadding: node.properties['insetPadding']?.value is CanvasEdgeInsets ? _physicalEdgeInsets('insetPadding') : null,
      children: node.slots.containsKey('children') ? _children('children') : null);
  }

  Widget _alertDialog(BuildContext context) {
    final adaptive=node.type==canvasAdaptiveAlertDialogType;
    final cupertino=adaptive && const {TargetPlatform.iOS,TargetPlatform.macOS}.contains(Theme.of(context).platform);
    final unavailable=cupertino?null:_cardShapePreviewUnavailableMessage(node,widgetName:'AlertDialog');
    if(unavailable!=null) {
      return _customClipperPreviewUnavailable(widgetName:'AlertDialog.shape',expectedType:'ShapeBorder',
        previewLabel:'AlertDialog shape\npreview unavailable',messageOverride:unavailable);
    }
    EdgeInsetsGeometry? padding(String name)=>const {'edgeInsets','edgeInsetsDirectional'}.contains(node.properties[name]?.kind)?_edgeInsetsGeometry(name):null;
    final inset=node.properties['insetPadding']?.value is CanvasEdgeInsets?_physicalEdgeInsets('insetPadding'):null;
    if(adaptive) {
      return AlertDialog.adaptive(key:_string('key')==null?null:ValueKey<String>(_string('key')!),
      icon:_single('icon'),title:_single('title'),content:_single('content'),actions:node.slots.containsKey('actions')?_children('actions'):null,
      iconPadding:padding('iconPadding'),iconColor:_resolvedColor(context,'iconColor'),
      titlePadding:padding('titlePadding'),titleTextStyle:_textStyle(context,'titleTextStyle'),
      contentPadding:padding('contentPadding'),contentTextStyle:_textStyle(context,'contentTextStyle'),
      actionsPadding:padding('actionsPadding'),buttonPadding:padding('buttonPadding'),
      actionsAlignment:_enum('actionsAlignment')==null?null:MainAxisAlignment.values.byName(_enum('actionsAlignment')!),
      actionsOverflowAlignment:_enum('actionsOverflowAlignment')==null?null:OverflowBarAlignment.values.byName(_enum('actionsOverflowAlignment')!),
      actionsOverflowDirection:_enum('actionsOverflowDirection')==null?null:VerticalDirection.values.byName(_enum('actionsOverflowDirection')!),
      actionsOverflowButtonSpacing:_number('actionsOverflowButtonSpacing'),
      backgroundColor:_resolvedColor(context,'backgroundColor'),elevation:_number('elevation'),
      shadowColor:_resolvedColor(context,'shadowColor'),surfaceTintColor:_resolvedColor(context,'surfaceTintColor'),
      semanticLabel:_string('semanticLabel'),clipBehavior:_clipBehavior(),shape:cupertino?null:_cardShape(context),
      alignment:_alignmentGeometry('alignment'),constraints:_boxConstraints('constraints'),scrollable:_boolean('scrollable')??false,
        insetPadding:inset??DialogTheme.of(context).insetPadding??const EdgeInsets.symmetric(horizontal:40,vertical:24),
        insetAnimationDuration:Duration(microseconds:_integer('insetAnimationDurationUs')??100000),
        insetAnimationCurve:_expansionCurves[_string('insetAnimationCurve')]??Curves.decelerate);
    }
    return AlertDialog(key:_string('key')==null?null:ValueKey<String>(_string('key')!),
      icon:_single('icon'),title:_single('title'),content:_single('content'),actions:node.slots.containsKey('actions')?_children('actions'):null,
      iconPadding:padding('iconPadding'),iconColor:_resolvedColor(context,'iconColor'),
      titlePadding:padding('titlePadding'),titleTextStyle:_textStyle(context,'titleTextStyle'),
      contentPadding:padding('contentPadding'),contentTextStyle:_textStyle(context,'contentTextStyle'),
      actionsPadding:padding('actionsPadding'),buttonPadding:padding('buttonPadding'),
      actionsAlignment:_enum('actionsAlignment')==null?null:MainAxisAlignment.values.byName(_enum('actionsAlignment')!),
      actionsOverflowAlignment:_enum('actionsOverflowAlignment')==null?null:OverflowBarAlignment.values.byName(_enum('actionsOverflowAlignment')!),
      actionsOverflowDirection:_enum('actionsOverflowDirection')==null?null:VerticalDirection.values.byName(_enum('actionsOverflowDirection')!),
      actionsOverflowButtonSpacing:_number('actionsOverflowButtonSpacing'),
      backgroundColor:_resolvedColor(context,'backgroundColor'),elevation:_number('elevation'),
      shadowColor:_resolvedColor(context,'shadowColor'),surfaceTintColor:_resolvedColor(context,'surfaceTintColor'),
      semanticLabel:_string('semanticLabel'),clipBehavior:_clipBehavior(),shape:cupertino?null:_cardShape(context),
      alignment:_alignmentGeometry('alignment'),constraints:_boxConstraints('constraints'),scrollable:_boolean('scrollable')??false,insetPadding:inset);
  }

  Widget _dialog(BuildContext context) {
    final fullscreen=node.type==canvasFullscreenDialogType;
    final unavailable=fullscreen?null:_cardShapePreviewUnavailableMessage(node,widgetName:'Dialog');
    if(unavailable!=null) {
      return _customClipperPreviewUnavailable(
        widgetName:'Dialog.shape',expectedType:'ShapeBorder',previewLabel:'Dialog shape\npreview unavailable',messageOverride:unavailable);
    }
    final duration=Duration(microseconds:_integer('insetAnimationDurationUs')??(fullscreen?0:100000));
    final curve=_expansionCurves[_string('insetAnimationCurve')]??Curves.decelerate;
    final role=SemanticsRole.values.byName(_enum('semanticsRole')??'dialog');
    final child=_single('child');
    if(fullscreen) {
      return Dialog.fullscreen(
        key:_string('key')==null?null:ValueKey<String>(_string('key')!),
        backgroundColor:_resolvedColor(context,'backgroundColor'),insetAnimationDuration:duration,
        insetAnimationCurve:curve,semanticsRole:role,child:child);
    }
    return Dialog(
      key:_string('key')==null?null:ValueKey<String>(_string('key')!),
      backgroundColor:_resolvedColor(context,'backgroundColor'),elevation:_number('elevation'),
      shadowColor:_resolvedColor(context,'shadowColor'),surfaceTintColor:_resolvedColor(context,'surfaceTintColor'),
      insetAnimationDuration:duration,insetAnimationCurve:curve,
      insetPadding:node.properties['insetPadding']?.value is CanvasEdgeInsets?_physicalEdgeInsets('insetPadding'):null,
      clipBehavior:_clipBehavior(),shape:_cardShape(context),alignment:_alignmentGeometry('alignment'),
      constraints:_boxConstraints('constraints'),semanticsRole:role,child:child);
  }

  Widget _card(BuildContext context) {
    final unavailable = _cardShapePreviewUnavailableMessage(node);
    if (unavailable != null) {
      return _customClipperPreviewUnavailable(
        widgetName: 'Card.shape',
        expectedType: 'ShapeBorder',
        previewLabel: 'Card shape\npreview unavailable',
        messageOverride: unavailable,
      );
    }
    final color = _resolvedColor(context, 'color');
    final shadowColor = _resolvedColor(context, 'shadowColor');
    final surfaceTintColor = _resolvedColor(context, 'surfaceTintColor');
    final elevation = _number('elevation');
    final shape = _cardShape(context);
    final borderOnForeground = _boolean('borderOnForeground') ?? true;
    final margin = _edgeInsetsGeometry('margin');
    final clipBehavior = _clipBehavior();
    final semanticContainer = _boolean('semanticContainer') ?? true;
    final child = _single('child');
    return switch (_string('variant')) {
      'elevated' => Card(
        color: color,
        shadowColor: shadowColor,
        surfaceTintColor: surfaceTintColor,
        elevation: elevation,
        shape: shape,
        borderOnForeground: borderOnForeground,
        margin: margin,
        clipBehavior: clipBehavior,
        semanticContainer: semanticContainer,
        child: child,
      ),
      'filled' => Card.filled(
        color: color,
        shadowColor: shadowColor,
        surfaceTintColor: surfaceTintColor,
        elevation: elevation,
        shape: shape,
        borderOnForeground: borderOnForeground,
        margin: margin,
        clipBehavior: clipBehavior,
        semanticContainer: semanticContainer,
        child: child,
      ),
      'outlined' => Card.outlined(
        color: color,
        shadowColor: shadowColor,
        surfaceTintColor: surfaceTintColor,
        elevation: elevation,
        shape: shape,
        borderOnForeground: borderOnForeground,
        margin: margin,
        clipBehavior: clipBehavior,
        semanticContainer: semanticContainer,
        child: child,
      ),
      _ => throw StateError('Unreviewed Canvas Card variant.'),
    };
  }

  ShapeBorder? _cardShape(BuildContext context) =>
      _cardShapeForFamily(context, 'shape');

  ShapeBorder? _cardShapeForFamily(BuildContext context, String family) {
    String property(String suffix) => '$family$suffix';
    final kind = _string(property('Kind'));
    if (kind == null) return null;
    final side = _shapeBorderSide(context, family);
    final value = node.properties[property('Radius')]?.value;
    final radius = value is CanvasBorderRadiusGeometryValue
        ? _borderRadius(value)
        : BorderRadius.zero;
    LinearBorderEdge? edge(String name) {
      final size = _number(property('${name}Size'));
      final alignment = _number(property('${name}Alignment'));
      return size == null && alignment == null
          ? null
          : LinearBorderEdge(size: size ?? 1, alignment: alignment ?? 0);
    }

    return switch (kind) {
      'roundedRectangle' => RoundedRectangleBorder(
        side: side,
        borderRadius: radius,
      ),
      'beveledRectangle' => BeveledRectangleBorder(
        side: side,
        borderRadius: radius,
      ),
      'continuousRectangle' => ContinuousRectangleBorder(
        side: side,
        borderRadius: radius,
      ),
      'roundedSuperellipse' => RoundedSuperellipseBorder(
        side: side,
        borderRadius: radius,
      ),
      'circle' => CircleBorder(
        side: side,
        eccentricity: _number(property('CircleEccentricity')) ?? 0,
      ),
      'oval' => OvalBorder(
        side: side,
        eccentricity: _number(property('CircleEccentricity')) ?? 1,
      ),
      'stadium' => StadiumBorder(side: side),
      'linear' => LinearBorder(
        side: side,
        start: edge('Start'),
        end: edge('End'),
        top: edge('Top'),
        bottom: edge('Bottom'),
      ),
      'star' => StarBorder(
        side: side,
        points: _number(property('Points')) ?? 5,
        innerRadiusRatio: _number(property('InnerRadiusRatio')) ?? .4,
        pointRounding: _number(property('PointRounding')) ?? 0,
        valleyRounding: _number(property('ValleyRounding')) ?? 0,
        rotation: _number(property('Rotation')) ?? 0,
        squash: _number(property('Squash')) ?? 0,
      ),
      'polygon' => StarBorder.polygon(
        side: side,
        sides: _number(property('Points')) ?? 5,
        pointRounding: _number(property('PointRounding')) ?? 0,
        rotation: _number(property('Rotation')) ?? 0,
        squash: _number(property('Squash')) ?? 0,
      ),
      _ => throw StateError('Unreviewed Canvas Card shape kind: $kind'),
    };
  }

  Widget _customClipperPreviewUnavailable({
    required String widgetName,
    required String expectedType,
    String previewLabel = 'Custom clipper\npreview unavailable',
    String? messageOverride,
    Widget? preservedChild,
  }) {
    final message =
        messageOverride ??
        _customClipperPreviewUnavailableMessage(
          widgetName: widgetName,
          expectedType: expectedType,
        );
    return Tooltip(
      message: message,
      excludeFromSemantics: true,
      child: Stack(
        fit: StackFit.passthrough,
        clipBehavior: Clip.none,
        children: [
          preservedChild ?? _single('child') ?? const SizedBox.shrink(),
          Positioned.fill(
            child: IgnorePointer(
              child: Semantics(
                key: ValueKey('canvas-custom-clipper-preview-${node.id}'),
                container: true,
                label: message,
                child: ExcludeSemantics(
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: Colors.amber.withValues(alpha: 0.22),
                      border: Border.all(
                        color: Colors.amber.shade800,
                        width: 1,
                      ),
                    ),
                    child: Center(
                      child: DecoratedBox(
                        decoration: BoxDecoration(
                          color: Colors.amber.shade100,
                          borderRadius: BorderRadius.circular(2),
                        ),
                        child: Padding(
                          padding: const EdgeInsets.all(3),
                          child: Text(
                            previewLabel,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            textAlign: TextAlign.center,
                            style: const TextStyle(
                              color: Colors.black87,
                              fontSize: 10,
                              fontWeight: FontWeight.w600,
                              height: 1.05,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _flexibleSpaceBar(BuildContext context) => LayoutBuilder(builder: (context, constraints) {
    final settings = context.dependOnInheritedWidgetOfExactType<FlexibleSpaceBarSettings>();
    final scale = _number('expandedTitleScale') ?? 1.5;
    final failure = settings == null
        ? 'FlexibleSpaceBar ${node.id} requires inherited FlexibleSpaceBarSettings. Place it in SliverAppBar.flexibleSpace, or in AppBar.flexibleSpace under Scaffold. Stored values and generated Dart are unchanged.'
        : settings.toolbarOpacity > 1 && node.slot('title')?.child != null
          ? 'FlexibleSpaceBar ${node.id} preview unavailable: inherited Toolbar opacity exceeds 1. Settings stores this native value, but the title color requires 0..1. Stored values and generated Dart are unchanged.'
        : !constraints.hasBoundedWidth || !constraints.hasBoundedHeight || !(scale * kToolbarHeight).isFinite
          ? 'FlexibleSpaceBar ${node.id} preview unavailable: finite app-bar bounds and finite scaled title geometry are required. Stored values and generated Dart are unchanged.'
          : null;
    if (failure != null) {
      return _customClipperPreviewUnavailable(
        widgetName: 'FlexibleSpaceBar', expectedType: 'FlexibleSpaceBarSettings',
        previewLabel: 'FlexibleSpaceBar\npreview unavailable', messageOverride: failure,
        preservedChild: SizedBox(
          width: constraints.hasBoundedWidth ? constraints.maxWidth : 240,
          height: constraints.hasBoundedHeight ? constraints.maxHeight : 120,
          child: Stack(fit: StackFit.expand, children: [
            if (node.slot('background')?.child != null) _single('background')!,
            if (node.slot('title')?.child != null) Align(alignment: Alignment.bottomCenter, child: _single('title')!),
          ]),
        ),
      );
    }
    final modes = _string('stretchModes') ?? 'zoomBackground';
    return _TextButtonPreview(
      message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
      child: FlexibleSpaceBar(
      title: _single('title'), background: _single('background'),
      centerTitle: _boolean('centerTitle'), titlePadding: node.properties['titlePadding']?.kind == 'dartObjectReferencePresence' ? null : _edgeInsetsGeometry('titlePadding'),
      collapseMode: switch (_enum('collapseMode')) {
        'pin' => CollapseMode.pin, 'none' => CollapseMode.none, _ => CollapseMode.parallax,
      },
      stretchModes: modes == 'none' ? const <StretchMode>[] : [
        for (final mode in modes.split(','))
          switch (mode) {
            'blurBackground' => StretchMode.blurBackground,
            'fadeTitle' => StretchMode.fadeTitle,
            _ => StretchMode.zoomBackground,
          },
      ],
      expandedTitleScale: scale,
    ));
  });

  Widget _opacity() => Opacity(
    opacity: _number('opacity')!,
    alwaysIncludeSemantics: _boolean('alwaysIncludeSemantics') ?? false,
    child: _single('child'),
  );

  Widget _placeholder(BuildContext context) => Placeholder(
    color: _resolvedColor(context, 'color') ?? const Color(0xFF455A64),
    strokeWidth: _number('strokeWidth') ?? 2.0,
    fallbackWidth: _number('fallbackWidth') ?? 400.0,
    fallbackHeight: _number('fallbackHeight') ?? 400.0,
    child: _single('child'),
  );

  Widget _coloredBox(BuildContext context) => ColoredBox(
    color: _resolvedColor(context, 'color')!,
    isAntiAlias: _boolean('isAntiAlias') ?? true,
    child: _single('child'),
  );

  Widget _animatedPhysicalModel(BuildContext context) {
    final radius=node.properties['borderRadius']?.value;
    return _AnimatedPhysicalPreview(
      message:_customClipperPreviewUnavailableMessageForNode(node)??'',
      target:AnimatedPhysicalModel(
        shape:_enum('shape')=='circle'?BoxShape.circle:BoxShape.rectangle,
        clipBehavior:_clipBehavior()??Clip.none,
        borderRadius:radius is CanvasPhysicalBorderRadiusValue?_borderRadius(radius) as BorderRadius:null,
        elevation:_number('elevation')??0,
        color:_resolvedColor(context,'color')??const Color(0xFF2196F3),
        shadowColor:_resolvedColor(context,'shadowColor')??const Color(0xFF000000),
        animateColor:_boolean('animateColor')??true, animateShadowColor:_boolean('animateShadowColor')??true,
        curve:_expansionCurves[_string('curve')]??Curves.linear,
        duration:Duration(microseconds:_integer('durationUs')??300000),child:_single('child')!,
      ));
  }

  Widget _physicalModel(BuildContext context) {
    final radius = node.properties['borderRadius']?.value;
    return PhysicalModel(
      shape: _enum('shape') == 'circle' ? BoxShape.circle : BoxShape.rectangle,
      clipBehavior: _clipBehavior() ?? Clip.none,
      borderRadius: radius is CanvasPhysicalBorderRadiusValue
          ? _borderRadius(radius) as BorderRadius
          : null,
      elevation: _number('elevation') ?? 0.0,
      color: _resolvedColor(context, 'color')!,
      shadowColor:
          _resolvedColor(context, 'shadowColor') ?? const Color(0xFF000000),
      child: _single('child'),
    );
  }

  Widget _physicalShape(BuildContext context) {
    final clipper = node.properties['clipper']!;
    if (clipper.kind == 'dartObjectReferencePresence') {
      return _customClipperPreviewUnavailable(
        widgetName: 'PhysicalShape',
        expectedType: 'CustomClipper<Path>',
      );
    }
    final value = clipper.value as CanvasShapeBorderClipperValue;
    final radius = _borderRadius(value.borderRadius);
    final ShapeBorder shape = switch (value.shape) {
      'roundedRectangle' => RoundedRectangleBorder(borderRadius: radius),
      'beveledRectangle' => BeveledRectangleBorder(borderRadius: radius),
      'continuousRectangle' => ContinuousRectangleBorder(borderRadius: radius),
      'roundedSuperellipse' => RoundedSuperellipseBorder(borderRadius: radius),
      'circle' => const CircleBorder(),
      'stadium' => const StadiumBorder(),
      _ => throw StateError('Unreviewed ShapeBorderClipper shape.'),
    };
    return PhysicalShape(
      clipper: ShapeBorderClipper(
        shape: shape,
        textDirection: switch (value.textDirection) {
          'ltr' => TextDirection.ltr,
          'rtl' => TextDirection.rtl,
          _ => null,
        },
      ),
      clipBehavior: _clipBehavior() ?? Clip.none,
      elevation: _number('elevation') ?? 0.0,
      color: _resolvedColor(context, 'color')!,
      shadowColor:
          _resolvedColor(context, 'shadowColor') ?? const Color(0xFF000000),
      child: _single('child'),
    );
  }

  Widget _center() => Center(
    widthFactor: _number('widthFactor'),
    heightFactor: _number('heightFactor'),
    child: _single('child'),
  );

  Widget _constrainedBox() => ConstrainedBox(
    constraints: _boxConstraints('constraints')!,
    child: _single('child'),
  );

  Widget _unconstrainedBox() => UnconstrainedBox(
    textDirection: _textDirection(),
    alignment: _alignmentGeometry('alignment') ?? Alignment.center,
    constrainedAxis: switch (_enum('constrainedAxis')) {
      'horizontal' => Axis.horizontal,
      'vertical' => Axis.vertical,
      _ => null,
    },
    clipBehavior: _clipBehavior() ?? Clip.none,
    child: _single('child'),
  );

  Widget _limitedBox() => LimitedBox(
    maxWidth: _number('maxWidth') ?? double.infinity,
    maxHeight: _number('maxHeight') ?? double.infinity,
    child: _single('child'),
  );

  Widget _overflowBox() => OverflowBox(
    alignment: _alignmentGeometry('alignment') ?? Alignment.center,
    minWidth: _number('minWidth'),
    maxWidth: _number('maxWidth'),
    minHeight: _number('minHeight'),
    maxHeight: _number('maxHeight'),
    fit: switch (_enum('fit')) {
      'deferToChild' => OverflowBoxFit.deferToChild,
      _ => OverflowBoxFit.max,
    },
    child: _single('child'),
  );

  Widget _relativePositionedTransition(Widget child) {
    final rect = node.properties['rect']?.kind == 'dartObjectReferencePresence' ? const Rect.fromLTWH(0,0,48,48)
        : _string('rect') == 'null' ? null
        : Rect.fromLTWH(_number('rectLeft')!, _number('rectTop')!, _number('rectWidth')!, _number('rectHeight')!);
    final size = node.properties['size']?.kind == 'dartObjectReferencePresence' ? const Size(48,48)
        : Size(_number('sizeWidth')!, _number('sizeHeight')!);
    return RelativePositionedTransition(
      rect: AlwaysStoppedAnimation<Rect?>(rect), size: size,
      child: _PositionedGeometryObserver(child: _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '', child: child)),
    );
  }

  Widget _positionedTransition(Widget child) {
    final rect = node.properties['rect']?.kind == 'dartObjectReferencePresence' ? RelativeRect.fill
        : RelativeRect.fromLTRB(_number('rectLeft')!, _number('rectTop')!, _number('rectRight')!, _number('rectBottom')!);
    return PositionedTransition(
      rect: AlwaysStoppedAnimation<RelativeRect>(rect),
      child: _PositionedGeometryObserver(child: _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '', child: child)),
    );
  }

  Widget _animatedPositioned(BuildContext context, Widget child) {
    final duration = Duration(microseconds: _integer('durationUs') ?? 300000);
    final curve = _expansionCurves[_string('curve')] ?? Curves.linear;
    final preview = _TextButtonPreview(
      message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
      child: child,
    );
    if (node.type.endsWith('.fromRect')) {
      final rect = node.properties['rect']?.kind == 'dartObjectReferencePresence'
          ? const Rect.fromLTWH(0,0,48,48)
          : Rect.fromLTWH(_number('rectLeft')!, _number('rectTop')!, _number('rectWidth')!, _number('rectHeight')!);
      return AnimatedPositioned.fromRect(rect: rect, duration: duration, curve: curve, child: _PositionedGeometryObserver(child: preview));
    }
    if (node.type.endsWith('Directional')) {
      return AnimatedPositionedDirectional(start: _number('start'), top: _number('top'), end: _number('end'),
        bottom: _number('bottom'), width: _number('width'), height: _number('height'),
        duration: duration, curve: curve, child: _PositionedGeometryObserver(child: preview));
    }
    return AnimatedPositioned(left: _number('left'), top: _number('top'), right: _number('right'),
      bottom: _number('bottom'), width: _number('width'), height: _number('height'),
      duration: duration, curve: curve, child: _PositionedGeometryObserver(child: preview));
  }

  Widget _defaultTextStyleTransition(BuildContext context) => _TextButtonPreview(
    message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
    child: DefaultTextStyleTransition(
      style: AlwaysStoppedAnimation<TextStyle>(
          node.properties['style']?.kind == 'dartObjectReferencePresence'
              ? const TextStyle() : _textStyle(context) ?? const TextStyle()),
      textAlign: _textAlign(),
      softWrap: _boolean('softWrap') ?? true,
      overflow: _textOverflow() ?? TextOverflow.clip,
      maxLines: _integer('maxLines'),
      child: _single('child')!,
    ),
  );

  Widget _defaultTextStyle(BuildContext context, bool merge) {
    final style = _string('style') == 'local' ? _textStyle(context) ?? const TextStyle() : null;
    final height = node.properties.containsKey('textHeightBehavior') ? null : _textHeightBehavior();
    final child = _single('child')!;
    return _TextButtonPreview(
      message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
      child: merge
          ? DefaultTextStyle.merge(style: style, textAlign: _textAlign(),
              softWrap: _boolean('softWrap'), overflow: _textOverflow(),
              maxLines: _integer('maxLines'), textWidthBasis: _textWidthBasis(),
              textHeightBehavior: height, child: child)
          : DefaultTextStyle(style: style ?? const TextStyle(), textAlign: _textAlign(),
              softWrap: _boolean('softWrap') ?? true, overflow: _textOverflow() ?? TextOverflow.clip,
              maxLines: _integer('maxLines'), textWidthBasis: _textWidthBasis() ?? TextWidthBasis.parent,
              textHeightBehavior: height, child: child),
    );
  }

  Widget _animatedDefaultTextStyle(BuildContext context) => _AnimatedTextStylePreview(
    message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
    target: AnimatedDefaultTextStyle(
      style: node.properties['style']?.kind == 'dartObjectReferencePresence'
          ? const TextStyle() : _textStyle(context) ?? const TextStyle(),
      textAlign: _textAlign(),
      softWrap: _boolean('softWrap') ?? true,
      overflow: _textOverflow() ?? TextOverflow.clip,
      maxLines: _integer('maxLines'),
      textWidthBasis: _textWidthBasis() ?? TextWidthBasis.parent,
      textHeightBehavior: node.properties.containsKey('textHeightBehavior') ? null : _textHeightBehavior(),
      curve: _expansionCurves[_string('curve')] ?? Curves.linear,
      duration: Duration(microseconds: _integer('durationUs') ?? 300000),
      child: _single('child')!,
    ),
  );

  ThemeData _materialThemeData() {
    final preset=_string('data')??'fallback';
    final material3=!preset.endsWith('M2');
    return switch(preset.replaceAll('M2','')) {
      'dark'=>ThemeData.dark(useMaterial3:material3),
      'light'=>ThemeData.light(useMaterial3:material3),
      _=>ThemeData.fallback(useMaterial3:material3),
    };
  }

  Widget _animatedSwitcher(BuildContext context) {
    final child = node.slot('child')?.child;
    return NotificationListener<SizeChangedLayoutNotification>(
      onNotification: (_) {
        context.findAncestorStateOfType<_CanvasDocumentViewState>()?._refreshZeroSizedWidgetTargetsAfterFrame();
        return false;
      },
      child: SizeChangedLayoutNotifier(
        child: _TextButtonPreview(
          message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
          child: AnimatedSwitcher(
            duration: Duration(microseconds: _integer('durationUs') ?? 300000),
            reverseDuration: _integer('reverseDurationUs') == null ? null : Duration(microseconds: _integer('reverseDurationUs')!),
            switchInCurve: _expansionCurves[_string('switchInCurve')] ?? Curves.linear,
            switchOutCurve: _expansionCurves[_string('switchOutCurve')] ?? Curves.linear,
            transitionBuilder: _switcherPreviewTransition,
            layoutBuilder: _switcherPreviewLayout,
            child: child == null ? null : _SwitcherEntryView(
              // The renderer itself has one wrapper type. Match the real unkeyed SDK child's type.
              key: ValueKey(_switcherRuntimeType(child)),
              node: child,
              builder: (keys) => _view(child, keyProvider: keys, allowInlineTextEdit: true),
            ),
          ),
        ),
      ),
    );
  }

  Widget _animatedCrossFade(BuildContext context) => NotificationListener<SizeChangedLayoutNotification>(
    onNotification: (_) {
      context.findAncestorStateOfType<_CanvasDocumentViewState>()?._refreshZeroSizedWidgetTargetsAfterFrame();
      return false;
    },
    child: SizeChangedLayoutNotifier(
      child: _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: AnimatedCrossFade(
          firstChild: _single('firstChild')!,
          secondChild: _single('secondChild')!,
          firstCurve: _expansionCurves[_string('firstCurve')] ?? Curves.linear,
          secondCurve: _expansionCurves[_string('secondCurve')] ?? Curves.linear,
          sizeCurve: _expansionCurves[_string('sizeCurve')] ?? Curves.linear,
          alignment: _alignmentGeometry('alignment') ?? Alignment.topCenter,
          crossFadeState: _enum('crossFadeState') == 'showSecond' ? CrossFadeState.showSecond : CrossFadeState.showFirst,
          duration: Duration(microseconds: _integer('durationUs') ?? 300000),
          reverseDuration: _integer('reverseDurationUs') == null ? null : Duration(microseconds: _integer('reverseDurationUs')!),
          excludeBottomFocus: _boolean('excludeBottomFocus') ?? true,
          // SDK default layout preserves both keyed subtrees. Never execute project code.
          onEnd: null,
        ),
      ),
    ),
  );

  Widget _animatedSize(BuildContext context) => NotificationListener<SizeChangedLayoutNotification>(
    onNotification: (_) {
      // Layout animation continues after the initial model-update frame.
      // Keep surface selection targets in sync, including crossing zero size.
      context.findAncestorStateOfType<_CanvasDocumentViewState>()
          ?._refreshZeroSizedWidgetTargetsAfterFrame();
      return false;
    },
    child: SizeChangedLayoutNotifier(
      child: _TextButtonPreview(
        message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
        child: AnimatedSize(
          alignment: _alignmentGeometry('alignment') ?? Alignment.center,
          curve: _expansionCurves[_string('curve')] ?? Curves.linear,
          duration: Duration(microseconds: _integer('durationUs') ?? 300000),
          reverseDuration: _integer('reverseDurationUs') == null ? null
              : Duration(microseconds: _integer('reverseDurationUs')!),
          clipBehavior: _clipBehavior() ?? Clip.hardEdge,
          // Project-owned callbacks never run in Canvas.
          onEnd: null,
          child: _single('child'),
        ),
      ),
    ),
  );

  Widget _animatedContainer(BuildContext context) {
    final color = _resolvedColor(context, 'color');
    final decoration = _boxDecoration(context, 'decoration');
    return _AnimatedMatrixPreview(
      node: node,
      message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
      child: AnimatedContainer(
        alignment: _alignmentGeometry('alignment'),
        padding: _edgeInsetsGeometry('padding'),
        color: color,
        decoration: decoration,
        foregroundDecoration: _boxDecoration(context, 'foregroundDecoration'),
        width: _enum('width') == 'infinity' ? double.infinity : _number('width'),
        height: _enum('height') == 'infinity' ? double.infinity : _number('height'),
        constraints: _boxConstraints('constraints'),
        margin: _edgeInsetsGeometry('margin'),
        transform: _matrix4('transform'),
        transformAlignment: _alignmentGeometry('transformAlignment'),
        clipBehavior: color == null && decoration == null ? Clip.none : _clipBehavior() ?? Clip.none,
        curve: _expansionCurves[_string('curve')] ?? Curves.linear,
        duration: Duration(microseconds: _integer('durationUs') ?? 300000),
        onEnd: null,
        child: _single('child'),
      ),
    );
  }

  Widget _container(BuildContext context) => Container(
    alignment: _alignmentGeometry('alignment'),
    padding: _edgeInsetsGeometry('padding'),
    color: _resolvedColor(context, 'color'),
    isAntiAlias: _boolean('isAntiAlias') ?? true,
    decoration: _boxDecoration(context, 'decoration'),
    foregroundDecoration: _boxDecoration(context, 'foregroundDecoration'),
    width: _number('width'),
    height: _number('height'),
    constraints: _boxConstraints('constraints'),
    margin: _edgeInsetsGeometry('margin'),
    transform: _matrix4('transform'),
    transformAlignment: _alignmentGeometry('transformAlignment'),
    clipBehavior: _clipBehavior() ?? Clip.none,
    child: _single('child'),
  );

  Widget _decoratedBoxTransition(BuildContext context) => _TextButtonPreview(
    message: _customClipperPreviewUnavailableMessageForNode(node) ?? '',
    child: DecoratedBoxTransition(
      decoration: AlwaysStoppedAnimation<Decoration>(_boxDecoration(context, 'decoration') ?? const BoxDecoration()),
      position: _enum('position') == 'foreground' ? DecorationPosition.foreground : DecorationPosition.background,
      child: _single('child')!,
    ),
  );

  Widget _decoratedBox(BuildContext context) => DecoratedBox(
    decoration: _boxDecoration(context, 'decoration')!,
    position: _enum('position') == 'foreground'
        ? DecorationPosition.foreground
        : DecorationPosition.background,
    child: _single('child'),
  );

  Widget _blockSemantics() => BlockSemantics(
    blocking: _boolean('blocking') ?? true,
    child: _single('child'),
  );

  Widget _mouseRegion() {
    final child = _single('child');
    final configuredCursor = _mouseCursor('cursor') ?? MouseCursor.defer;
    final cursor = configuredCursor is WidgetStateMouseCursor
        ? configuredCursor.resolve(const <WidgetState>{})
        : configuredCursor;
    return _TextButtonPreview(
      message: _mouseRegionPreviewMessage(node) ?? '',
      child: MouseRegion(
        key: ValueKey('canvas-mouse-region-${node.id}'),
        onEnter: _gestureCallbackPresent('onEnter') ? (_) {} : null,
        onExit: _gestureCallbackPresent('onExit') ? (_) {} : null,
        onHover: _gestureCallbackPresent('onHover') ? (_) {} : null,
        cursor: cursor,
        opaque: _boolean('opaque') ?? true,
        hitTestBehavior: switch (_enum('hitTestBehavior')) {
          'deferToChild' => HitTestBehavior.deferToChild,
          'opaque' => HitTestBehavior.opaque,
          'translucent' => HitTestBehavior.translucent,
          _ => null,
        },
        child: child == null
            ? null
            : _CanvasMouseRegionCursorScope(child: child),
      ),
    );
  }

  Widget _listener() => Listener(
    key: ValueKey('canvas-listener-${node.id}'),
    // Raw pointer notifications never execute application callbacks or alter
    // selection. The surrounding Designer gesture handlers remain responsible
    // for selecting the deepest child; hover, cancel and scroll must not steal it.
    onPointerDown: _gestureCallbackPresent('onPointerDown') ? (_) {} : null,
    onPointerMove: _gestureCallbackPresent('onPointerMove') ? (_) {} : null,
    onPointerUp: _gestureCallbackPresent('onPointerUp') ? (_) {} : null,
    onPointerHover: _gestureCallbackPresent('onPointerHover') ? (_) {} : null,
    onPointerCancel: _gestureCallbackPresent('onPointerCancel') ? (_) {} : null,
    onPointerPanZoomStart: _gestureCallbackPresent('onPointerPanZoomStart')
        ? (_) {}
        : null,
    onPointerPanZoomUpdate: _gestureCallbackPresent('onPointerPanZoomUpdate')
        ? (_) {}
        : null,
    onPointerPanZoomEnd: _gestureCallbackPresent('onPointerPanZoomEnd')
        ? (_) {}
        : null,
    onPointerSignal: _gestureCallbackPresent('onPointerSignal') ? (_) {} : null,
    behavior: switch (_enum('behavior')) {
      'opaque' => HitTestBehavior.opaque,
      'translucent' => HitTestBehavior.translucent,
      _ => HitTestBehavior.deferToChild,
    },
    child: _single('child'),
  );

  Widget _gestureDetector() => GestureDetector(
    // The projection contains presence only, never application code. Local
    // recognizers preserve semantics and select the Designer target; children
    // retain their own deeper Designer gesture handlers.
    key: ValueKey('canvas-gesture-detector-${node.id}'),
    onTapDown: _gestureCallbackPresent('onTapDown')
        ? (_) => onSelected(node.id)
        : null,
    onTapUp: _gestureCallbackPresent('onTapUp')
        ? (_) => onSelected(node.id)
        : null,
    onTap: _gestureCallbackPresent('onTap') ? () => onSelected(node.id) : null,
    onTapMove: _gestureCallbackPresent('onTapMove')
        ? (_) => onSelected(node.id)
        : null,
    onTapCancel: _gestureCallbackPresent('onTapCancel') ? () {} : null,
    onSecondaryTap: _gestureCallbackPresent('onSecondaryTap')
        ? () => onSelected(node.id)
        : null,
    onSecondaryTapDown: _gestureCallbackPresent('onSecondaryTapDown')
        ? (_) => onSelected(node.id)
        : null,
    onSecondaryTapUp: _gestureCallbackPresent('onSecondaryTapUp')
        ? (_) => onSelected(node.id)
        : null,
    onSecondaryTapCancel: _gestureCallbackPresent('onSecondaryTapCancel')
        ? () {}
        : null,
    onTertiaryTapDown: _gestureCallbackPresent('onTertiaryTapDown')
        ? (_) => onSelected(node.id)
        : null,
    onTertiaryTapUp: _gestureCallbackPresent('onTertiaryTapUp')
        ? (_) => onSelected(node.id)
        : null,
    onTertiaryTapCancel: _gestureCallbackPresent('onTertiaryTapCancel')
        ? () {}
        : null,
    onDoubleTapDown: _gestureCallbackPresent('onDoubleTapDown')
        ? (_) => onSelected(node.id)
        : null,
    onDoubleTap: _gestureCallbackPresent('onDoubleTap')
        ? () => onSelected(node.id)
        : null,
    onDoubleTapCancel: _gestureCallbackPresent('onDoubleTapCancel')
        ? () {}
        : null,
    onLongPressDown: _gestureCallbackPresent('onLongPressDown')
        ? (_) => onSelected(node.id)
        : null,
    onLongPressCancel: _gestureCallbackPresent('onLongPressCancel')
        ? () {}
        : null,
    onLongPress: _gestureCallbackPresent('onLongPress')
        ? () => onSelected(node.id)
        : null,
    onLongPressStart: _gestureCallbackPresent('onLongPressStart')
        ? (_) => onSelected(node.id)
        : null,
    onLongPressMoveUpdate: _gestureCallbackPresent('onLongPressMoveUpdate')
        ? (_) => onSelected(node.id)
        : null,
    onLongPressUp: _gestureCallbackPresent('onLongPressUp')
        ? () => onSelected(node.id)
        : null,
    onLongPressEnd: _gestureCallbackPresent('onLongPressEnd')
        ? (_) => onSelected(node.id)
        : null,
    onSecondaryLongPressDown:
        _gestureCallbackPresent('onSecondaryLongPressDown')
        ? (_) => onSelected(node.id)
        : null,
    onSecondaryLongPressCancel:
        _gestureCallbackPresent('onSecondaryLongPressCancel') ? () {} : null,
    onSecondaryLongPress: _gestureCallbackPresent('onSecondaryLongPress')
        ? () => onSelected(node.id)
        : null,
    onSecondaryLongPressStart:
        _gestureCallbackPresent('onSecondaryLongPressStart')
        ? (_) => onSelected(node.id)
        : null,
    onSecondaryLongPressMoveUpdate:
        _gestureCallbackPresent('onSecondaryLongPressMoveUpdate')
        ? (_) => onSelected(node.id)
        : null,
    onSecondaryLongPressUp: _gestureCallbackPresent('onSecondaryLongPressUp')
        ? () => onSelected(node.id)
        : null,
    onSecondaryLongPressEnd: _gestureCallbackPresent('onSecondaryLongPressEnd')
        ? (_) => onSelected(node.id)
        : null,
    onTertiaryLongPressDown: _gestureCallbackPresent('onTertiaryLongPressDown')
        ? (_) => onSelected(node.id)
        : null,
    onTertiaryLongPressCancel:
        _gestureCallbackPresent('onTertiaryLongPressCancel') ? () {} : null,
    onTertiaryLongPress: _gestureCallbackPresent('onTertiaryLongPress')
        ? () => onSelected(node.id)
        : null,
    onTertiaryLongPressStart:
        _gestureCallbackPresent('onTertiaryLongPressStart')
        ? (_) => onSelected(node.id)
        : null,
    onTertiaryLongPressMoveUpdate:
        _gestureCallbackPresent('onTertiaryLongPressMoveUpdate')
        ? (_) => onSelected(node.id)
        : null,
    onTertiaryLongPressUp: _gestureCallbackPresent('onTertiaryLongPressUp')
        ? () => onSelected(node.id)
        : null,
    onTertiaryLongPressEnd: _gestureCallbackPresent('onTertiaryLongPressEnd')
        ? (_) => onSelected(node.id)
        : null,
    onVerticalDragDown: _gestureCallbackPresent('onVerticalDragDown')
        ? (_) => onSelected(node.id)
        : null,
    onVerticalDragStart: _gestureCallbackPresent('onVerticalDragStart')
        ? (_) => onSelected(node.id)
        : null,
    onVerticalDragUpdate: _gestureCallbackPresent('onVerticalDragUpdate')
        ? (_) => onSelected(node.id)
        : null,
    onVerticalDragEnd: _gestureCallbackPresent('onVerticalDragEnd')
        ? (_) => onSelected(node.id)
        : null,
    onVerticalDragCancel: _gestureCallbackPresent('onVerticalDragCancel')
        ? () {}
        : null,
    onHorizontalDragDown: _gestureCallbackPresent('onHorizontalDragDown')
        ? (_) => onSelected(node.id)
        : null,
    onHorizontalDragStart: _gestureCallbackPresent('onHorizontalDragStart')
        ? (_) => onSelected(node.id)
        : null,
    onHorizontalDragUpdate: _gestureCallbackPresent('onHorizontalDragUpdate')
        ? (_) => onSelected(node.id)
        : null,
    onHorizontalDragEnd: _gestureCallbackPresent('onHorizontalDragEnd')
        ? (_) => onSelected(node.id)
        : null,
    onHorizontalDragCancel: _gestureCallbackPresent('onHorizontalDragCancel')
        ? () {}
        : null,
    onPanDown: _gestureCallbackPresent('onPanDown')
        ? (_) => onSelected(node.id)
        : null,
    onPanStart: _gestureCallbackPresent('onPanStart')
        ? (_) => onSelected(node.id)
        : null,
    onPanUpdate: _gestureCallbackPresent('onPanUpdate')
        ? (_) => onSelected(node.id)
        : null,
    onPanEnd: _gestureCallbackPresent('onPanEnd')
        ? (_) => onSelected(node.id)
        : null,
    onPanCancel: _gestureCallbackPresent('onPanCancel') ? () {} : null,
    onScaleStart: _gestureCallbackPresent('onScaleStart')
        ? (_) => onSelected(node.id)
        : null,
    onScaleUpdate: _gestureCallbackPresent('onScaleUpdate')
        ? (_) => onSelected(node.id)
        : null,
    onScaleEnd: _gestureCallbackPresent('onScaleEnd')
        ? (_) => onSelected(node.id)
        : null,
    onForcePressStart: _gestureCallbackPresent('onForcePressStart')
        ? (_) => onSelected(node.id)
        : null,
    onForcePressPeak: _gestureCallbackPresent('onForcePressPeak')
        ? (_) => onSelected(node.id)
        : null,
    onForcePressUpdate: _gestureCallbackPresent('onForcePressUpdate')
        ? (_) => onSelected(node.id)
        : null,
    onForcePressEnd: _gestureCallbackPresent('onForcePressEnd')
        ? (_) => onSelected(node.id)
        : null,
    behavior: switch (_enum('behavior')) {
      'opaque' => HitTestBehavior.opaque,
      'translucent' => HitTestBehavior.translucent,
      'deferToChild' => HitTestBehavior.deferToChild,
      _ => null,
    },
    excludeFromSemantics: _boolean('excludeFromSemantics') ?? false,
    dragStartBehavior: _enum('dragStartBehavior') == 'down'
        ? DragStartBehavior.down
        : DragStartBehavior.start,
    trackpadScrollCausesScale: _boolean('trackpadScrollCausesScale') ?? false,
    trackpadScrollToScaleFactor:
        _offset('trackpadScrollToScaleFactor') ??
        kDefaultTrackpadScrollToScaleFactor,
    supportedDevices:
        node.properties['supportedDevices']?.kind == 'pointerDeviceKindSet'
        ? (node.properties['supportedDevices']!.value as Set<String>)
              .map((name) => PointerDeviceKind.values.byName(name))
              .toSet()
        : null,
    child: _single('child'),
  );

  bool _gestureCallbackPresent(String name) =>
      node.properties[name]?.kind == 'callbackPresence' ||
      node.properties[name]?.kind == 'dartObjectReferencePresence';

  Widget _absorbPointer() => AbsorbPointer(
    absorbing: _boolean('absorbing') ?? true,
    // The pinned SDK still supports this deprecated compatibility branch.
    // ignore: deprecated_member_use
    ignoringSemantics: _boolean('ignoringSemantics'),
    child: _single('child'),
  );

  Widget _ignorePointer() => IgnorePointer(
    ignoring: _boolean('ignoring') ?? true,
    // The pinned SDK still supports this deprecated compatibility branch.
    // ignore: deprecated_member_use
    ignoringSemantics: _boolean('ignoringSemantics'),
    child: _single('child'),
  );

  Widget _excludeSemantics() => ExcludeSemantics(
    excluding: _boolean('excluding') ?? true,
    child: _single('child'),
  );

  Widget _sizedBox() => SizedBox(
    width: _number('width'),
    height: _number('height'),
    child: _single('child'),
  );

  Widget _icon(BuildContext context, {Key? key}) {
    final sizeMessage = _iconButtonMountedIconMessage(node, context);
    if (sizeMessage != null) {
      return _customClipperPreviewUnavailable(
        widgetName: 'IconButton.Icon',
        expectedType: 'finite iconSize',
        previewLabel: 'Icon size\npreview unavailable',
        messageOverride: sizeMessage,
      );
    }
    final value = node.properties['icon']!.value as CanvasIconDataValue;
    final icon = value.codePoint == null
        ? null
        : IconData(
            // The designer intentionally reconstructs reviewed wire metadata
            // at runtime; it never evaluates an arbitrary Dart expression.
            // ignore: non_const_argument_for_const_parameter
            value.codePoint!,
            // ignore: non_const_argument_for_const_parameter
            fontFamily: value.fontFamily,
            // ignore: non_const_argument_for_const_parameter
            fontPackage: value.fontPackage,
            matchTextDirection: value.matchTextDirection,
            fontFamilyFallback: value.fontFamilyFallback.isEmpty
                ? null
                : value.fontFamilyFallback,
          );
    return Icon(
      icon,
      key: key,
      size: _number('size'),
      fill: _number('fill'),
      weight: _number('weight'),
      grade: _number('grade'),
      opticalSize: _number('opticalSize'),
      color: _resolvedColor(context, 'color'),
      shadows: _shadows(context, 'shadows'),
      semanticLabel: _string('semanticLabel'),
      textDirection: _textDirection(),
      applyTextScaling: _boolean('applyTextScaling'),
      blendMode: _optionalBlendMode('blendMode'),
      fontWeight: _fontWeight('fontWeight'),
    );
  }

  ScrollNotificationPredicate _notificationPredicate() =>
      switch (_string('notificationPredicate')) {
        'depthZero' => (notification) => notification.depth == 0,
        'all' => (notification) => true,
        _ => defaultScrollNotificationPredicate,
      };

  IconThemeData? _iconTheme(BuildContext context, String prefix) {
    if (!_hasPropertyPrefix(prefix)) {
      return null;
    }
    return IconThemeData(
      size: _number('${prefix}Size'),
      fill: _number('${prefix}Fill'),
      weight: _number('${prefix}Weight'),
      grade: _number('${prefix}Grade'),
      opticalSize: _number('${prefix}OpticalSize'),
      color: _resolvedColor(context, '${prefix}Color'),
      opacity: _number('${prefix}Opacity'),
      shadows: _shadows(context, '${prefix}Shadows'),
      applyTextScaling: _boolean('${prefix}ApplyTextScaling'),
    );
  }

  Widget _iconThemeWidget(BuildContext context) {
    final data = IconThemeData(
      size: _number('size'),
      fill: _number('fill'),
      weight: _number('weight'),
      grade: _number('grade'),
      opticalSize: _number('opticalSize'),
      color: _resolvedColor(context, 'color'),
      opacity: _number('opacity'),
      shadows: _shadows(context, 'shadows'),
      applyTextScaling: _boolean('applyTextScaling'),
    );
    final child = _single('child')!;
    return _boolean('merge')!
        ? IconTheme.merge(data: data, child: child)
        : IconTheme(data: data, child: child);
  }

  ShapeBorder? _appBarShape(BuildContext context) {
    final kind = _string('shapeKind');
    if (kind == null) {
      return null;
    }
    final side = _appBarBorderSide(context);
    final borderRadius = BorderRadius.only(
      topLeft: Radius.circular(_number('shapeRadiusTopLeft') ?? 0),
      topRight: Radius.circular(_number('shapeRadiusTopRight') ?? 0),
      bottomRight: Radius.circular(_number('shapeRadiusBottomRight') ?? 0),
      bottomLeft: Radius.circular(_number('shapeRadiusBottomLeft') ?? 0),
    );
    return switch (kind) {
      'roundedRectangle' => RoundedRectangleBorder(
        side: side,
        borderRadius: borderRadius,
      ),
      'stadium' => StadiumBorder(side: side),
      'circle' => CircleBorder(
        side: side,
        eccentricity: _number('shapeCircleEccentricity') ?? 0,
      ),
      'beveledRectangle' => BeveledRectangleBorder(
        side: side,
        borderRadius: borderRadius,
      ),
      'continuousRectangle' => ContinuousRectangleBorder(
        side: side,
        borderRadius: borderRadius,
      ),
      _ => throw StateError('Unreviewed Canvas AppBar shape kind: $kind'),
    };
  }

  BorderSide _appBarBorderSide(BuildContext context) {
    return _shapeBorderSide(context, 'shape');
  }

  BorderSide _shapeBorderSide(BuildContext context, String family) {
    final prefix = '${family}Side';
    if (!node.properties.keys.any((name) => name.startsWith(prefix))) {
      return BorderSide.none;
    }
    return BorderSide(
      color:
          _resolvedColor(context, '${prefix}Color') ?? const Color(0xff000000),
      width: _number('${prefix}Width') ?? 1.0,
      style: _enum('${prefix}Style') == 'none'
          ? BorderStyle.none
          : BorderStyle.solid,
      strokeAlign:
          _number('${prefix}StrokeAlign') ?? BorderSide.strokeAlignInside,
    );
  }

  SystemUiOverlayStyle? _systemOverlayStyle(BuildContext context) {
    const prefix = 'systemOverlayStyle';
    if (!_hasPropertyPrefix(prefix)) {
      return null;
    }
    return SystemUiOverlayStyle(
      systemNavigationBarColor: _resolvedColor(
        context,
        '${prefix}SystemNavigationBarColor',
      ),
      systemNavigationBarDividerColor: _resolvedColor(
        context,
        '${prefix}SystemNavigationBarDividerColor',
      ),
      systemNavigationBarIconBrightness: _brightness(
        '${prefix}SystemNavigationBarIconBrightness',
      ),
      systemNavigationBarContrastEnforced: _boolean(
        '${prefix}SystemNavigationBarContrastEnforced',
      ),
      statusBarColor: _resolvedColor(context, '${prefix}StatusBarColor'),
      statusBarBrightness: _brightness('${prefix}StatusBarBrightness'),
      statusBarIconBrightness: _brightness('${prefix}StatusBarIconBrightness'),
      systemStatusBarContrastEnforced: _boolean(
        '${prefix}SystemStatusBarContrastEnforced',
      ),
    );
  }

  Widget _text(BuildContext context) => Text(
    _string('data')!,
    style: _textStyle(context),
    strutStyle: _strutStyle(),
    textAlign: _textAlign(),
    textDirection: _textDirection(),
    locale: _locale(
      languageCode: 'localeLanguageCode',
      scriptCode: 'localeScriptCode',
      countryCode: 'localeCountryCode',
    ),
    softWrap: _boolean('softWrap'),
    overflow: _textOverflow(),
    textScaler: _textScaler(),
    maxLines: _integer('maxLines'),
    semanticsLabel: _string('semanticsLabel'),
    semanticsIdentifier: _string('semanticsIdentifier'),
    textWidthBasis: _textWidthBasis(),
    textHeightBehavior: _textHeightBehavior(),
    selectionColor: _resolvedColor(context, 'selectionColor'),
  );

  Widget _inlineTextEditor(BuildContext context) => _CanvasInlineTextEditor(
    key: ValueKey('canvas-inline-text-editor-${node.id}'),
    widgetId: node.id,
    designerFocusParent: _MenuPanelPreviewScope.active(context)
        ? null
        : designerFocusParent,
    initialText: _string('data')!,
    style: _textStyle(context),
    strutStyle: _strutStyle(),
    textAlign: _textAlign() ?? TextAlign.start,
    textDirection: _textDirection(),
    maxLines: _integer('maxLines') ?? 8,
    cursorColor:
        _resolvedColor(context, 'selectionColor') ??
        Theme.of(context).colorScheme.primary,
    onCommit: onCommitInlineTextEdit,
    onCancel: onCancelInlineTextEdit,
  );

  TextStyle? _textStyle(BuildContext context, [String prefix = 'style']) {
    if (!_hasPropertyPrefix(prefix)) {
      return null;
    }
    final themeBase = _themeTextStyle(context, '${prefix}ThemeTextStyle');
    final hasOverrides = node.properties.keys.any(
      (name) => name.startsWith(prefix) && name != '${prefix}ThemeTextStyle',
    );
    if (!hasOverrides) {
      return themeBase;
    }
    return (themeBase ?? const TextStyle()).copyWith(
      inherit: _boolean('${prefix}Inherit'),
      color: _resolvedColor(context, '${prefix}Color'),
      backgroundColor: _resolvedColor(context, '${prefix}BackgroundColor'),
      fontSize: _number('${prefix}FontSize'),
      fontWeight: _fontWeight('${prefix}FontWeight'),
      fontStyle: _fontStyle('${prefix}FontStyle'),
      letterSpacing: _number('${prefix}LetterSpacing'),
      wordSpacing: _number('${prefix}WordSpacing'),
      textBaseline: _textBaseline('${prefix}TextBaseline'),
      height: _number('${prefix}Height'),
      leadingDistribution: _textLeadingDistribution(
        '${prefix}LeadingDistribution',
      ),
      locale: _locale(
        languageCode: '${prefix}LocaleLanguageCode',
        scriptCode: '${prefix}LocaleScriptCode',
        countryCode: '${prefix}LocaleCountryCode',
      ),
      foreground: _paint(context, '${prefix}Foreground'),
      background: _paint(context, '${prefix}Background'),
      shadows: _shadows(context, '${prefix}Shadows'),
      fontFeatures: _fontFeatures('${prefix}FontFeatures'),
      fontVariations: _fontVariations('${prefix}FontVariations'),
      decoration: _textDecoration(prefix),
      decorationColor: _resolvedColor(context, '${prefix}DecorationColor'),
      decorationStyle: _textDecorationStyle('${prefix}DecorationStyle'),
      decorationThickness: _number('${prefix}DecorationThickness'),
      debugLabel: _string('${prefix}DebugLabel'),
      fontFamily: _string('${prefix}FontFamily'),
      fontFamilyFallback: _newlineList('${prefix}FontFamilyFallback'),
      package: _string('${prefix}Package'),
      overflow: _textOverflow('${prefix}Overflow'),
    );
  }

  StrutStyle? _strutStyle() {
    if (!_hasPropertyPrefix('strut')) {
      return null;
    }
    return StrutStyle(
      fontFamily: _string('strutFontFamily'),
      fontFamilyFallback: _newlineList('strutFontFamilyFallback'),
      fontSize: _number('strutFontSize'),
      height: _number('strutHeight'),
      leadingDistribution: _textLeadingDistribution('strutLeadingDistribution'),
      leading: _number('strutLeading'),
      fontWeight: _fontWeight('strutFontWeight'),
      fontStyle: _fontStyle('strutFontStyle'),
      forceStrutHeight: _boolean('strutForceHeight'),
      debugLabel: _string('strutDebugLabel'),
      package: _string('strutPackage'),
    );
  }

  Locale? _locale({
    required String languageCode,
    required String scriptCode,
    required String countryCode,
  }) {
    if (!node.properties.containsKey(languageCode) &&
        !node.properties.containsKey(scriptCode) &&
        !node.properties.containsKey(countryCode)) {
      return null;
    }
    return Locale.fromSubtags(
      languageCode: _string(languageCode) ?? 'und',
      scriptCode: _string(scriptCode),
      countryCode: _string(countryCode),
    );
  }

  TextScaler? _textScaler() {
    final factor = _number('textScalerFactor');
    return factor == null ? null : TextScaler.linear(factor);
  }

  TextHeightBehavior? _textHeightBehavior() {
    if (!_hasPropertyPrefix('textHeight')) {
      return null;
    }
    return TextHeightBehavior(
      applyHeightToFirstAscent: _boolean('textHeightApplyFirstAscent') ?? true,
      applyHeightToLastDescent: _boolean('textHeightApplyLastDescent') ?? true,
      leadingDistribution:
          _textLeadingDistribution('textHeightLeadingDistribution') ??
          TextLeadingDistribution.proportional,
    );
  }

  Widget _defaultSelectionStyle(BuildContext context) {
    final cursorColor = _resolvedColor(context, 'cursorColor');
    final selectionColor = _resolvedColor(context, 'selectionColor');
    final mouseCursor = _mouseCursor('mouseCursor');
    final child = _single('child')!;
    return _boolean('merge')!
        ? DefaultSelectionStyle.merge(
            cursorColor: cursorColor,
            selectionColor: selectionColor,
            mouseCursor: mouseCursor,
            child: child,
          )
        : DefaultSelectionStyle(
            cursorColor: cursorColor,
            selectionColor: selectionColor,
            mouseCursor: mouseCursor,
            child: child,
          );
  }

  TextDecoration? _textDecoration(String prefix) {
    final names = {
      '${prefix}DecorationUnderline',
      '${prefix}DecorationOverline',
      '${prefix}DecorationLineThrough',
    };
    if (!names.any(node.properties.containsKey)) {
      return null;
    }
    final decorations = <TextDecoration>[
      if (_boolean('${prefix}DecorationUnderline') ?? false)
        TextDecoration.underline,
      if (_boolean('${prefix}DecorationOverline') ?? false)
        TextDecoration.overline,
      if (_boolean('${prefix}DecorationLineThrough') ?? false)
        TextDecoration.lineThrough,
    ];
    return switch (decorations.length) {
      0 => TextDecoration.none,
      1 => decorations.single,
      _ => TextDecoration.combine(decorations),
    };
  }

  List<String>? _newlineList(String name) {
    final value = _string(name);
    if (value == null) {
      return null;
    }
    return List.unmodifiable(
      value
          .split(RegExp(r'\r\n?|\n'))
          .map((entry) => entry.trim())
          .where((entry) => entry.isNotEmpty),
    );
  }

  bool _hasPropertyPrefix(String prefix) =>
      node.properties.keys.any((name) => name.startsWith(prefix));

  AnimationStyle? _floatingHeaderAnimationStyle() {
    if (_string('animationStyle') == 'noAnimation') return AnimationStyle.noAnimation;
    if (!node.properties.keys.any((name) => name.startsWith('animationStyle') && name != 'animationStyle')) return null;
    Duration? duration(String name) {
      final value = _integer(name);
      return value == null ? null : Duration(microseconds: math.max(0, value));
    }
    return AnimationStyle(duration: duration('animationStyleDurationUs'),
      reverseDuration: duration('animationStyleReverseDurationUs'),
      curve: _expansionCurves[_string('animationStyleCurve')],
      reverseCurve: _expansionCurves[_string('animationStyleReverseCurve')]);
  }

  Widget? _headerPrototype(String name) {
    final prototype = _single(name);
    return prototype == null ? null : _PrototypeMeasurement(child: prototype);
  }

  Widget _prototypeItem() => _PrototypeMeasurement(
    child: _single('prototypeItem') ?? const SizedBox(width: 48, height: 48),
  );

  Widget? _single(String name) {
    final child = node.slot(name)?.child;
    return child == null ? null : _view(child);
  }

  PreferredSizeWidget? _preferredSizeSingle(String name) {
    final child = node.slot(name)?.child;
    return child == null ? null : _view(child);
  }

  List<Widget> _children(String name) => [
    for (final child in node.slot(name)?.children ?? const <CanvasNode>[])
      _view(child),
  ];

  _CanvasNodeView _view(CanvasNode child, {GlobalKey Function(String)? keyProvider, bool allowInlineTextEdit = false}) => _CanvasNodeView(
    node: child,
    imageResources: imageResources,
    onImageError: onImageError,
    selectedWidgetId: selectedWidgetId,
    onSelected: onSelected,
    nodeKey: keyProvider ?? nodeKey,
    designerFocusParent: designerFocusParent,
    overlayScale: overlayScale,
    inlineTextEditEnabled: (keyProvider == null || allowInlineTextEdit) && inlineTextEditEnabled,
    inlineTextEditingWidgetId: inlineTextEditingWidgetId,
    onBeginInlineTextEdit: onBeginInlineTextEdit,
    onCommitInlineTextEdit: onCommitInlineTextEdit,
    onCancelInlineTextEdit: onCancelInlineTextEdit,
    suppressDesignerSemantics:
        suppressDesignerSemantics || isCanvasDataTable(node.type) || isCanvasDataDescriptor(node.type) ||
        node.type == 'flutter.widgets.MergeSemantics' ||
        node.type == 'flutter.widgets.IndexedSemantics',
  );

  String? _string(String name) {
    final property = node.properties[name];
    return property?.kind == 'string' ? property!.value as String : null;
  }

  bool? _boolean(String name) {
    final property = node.properties[name];
    return property?.kind == 'boolean' ? property!.value as bool : null;
  }

  bool _callbackPresent(String name) =>
      node.properties[name]?.kind == 'callbackPresence';

  int? _integer(String name) {
    final property = node.properties[name];
    return property?.kind == 'integer' ? property!.value as int : null;
  }

  double? _number(String name) {
    final property = node.properties[name];
    final value = property?.value;
    return value is num ? value.toDouble() : null;
  }

  Color? _resolvedColor(BuildContext context, String name) {
    final property = node.properties[name];
    if (property == null) {
      return null;
    }
    if (property.kind == 'color') {
      return Color(property.value as int);
    }
    final token = property.value;
    return token is CanvasThemeToken ? _themeColor(context, token) : null;
  }

  Color _colorSource(BuildContext context, CanvasColorSource source) =>
      switch (source) {
        CanvasLiteralColor(:final argb) => Color(argb),
        CanvasThemeColor(:final token) => _themeColor(context, token),
      };

  Color _themeColor(BuildContext context, CanvasThemeToken token) {
    final scheme = Theme.of(context).colorScheme;
    return switch (token.wireId) {
      'material.colorScheme.primary' => scheme.primary,
      'material.colorScheme.onPrimary' => scheme.onPrimary,
      'material.colorScheme.primaryContainer' => scheme.primaryContainer,
      'material.colorScheme.onPrimaryContainer' => scheme.onPrimaryContainer,
      'material.colorScheme.primaryFixed' => scheme.primaryFixed,
      'material.colorScheme.primaryFixedDim' => scheme.primaryFixedDim,
      'material.colorScheme.onPrimaryFixed' => scheme.onPrimaryFixed,
      'material.colorScheme.onPrimaryFixedVariant' =>
        scheme.onPrimaryFixedVariant,
      'material.colorScheme.secondary' => scheme.secondary,
      'material.colorScheme.onSecondary' => scheme.onSecondary,
      'material.colorScheme.secondaryContainer' => scheme.secondaryContainer,
      'material.colorScheme.onSecondaryContainer' =>
        scheme.onSecondaryContainer,
      'material.colorScheme.secondaryFixed' => scheme.secondaryFixed,
      'material.colorScheme.secondaryFixedDim' => scheme.secondaryFixedDim,
      'material.colorScheme.onSecondaryFixed' => scheme.onSecondaryFixed,
      'material.colorScheme.onSecondaryFixedVariant' =>
        scheme.onSecondaryFixedVariant,
      'material.colorScheme.tertiary' => scheme.tertiary,
      'material.colorScheme.onTertiary' => scheme.onTertiary,
      'material.colorScheme.tertiaryContainer' => scheme.tertiaryContainer,
      'material.colorScheme.onTertiaryContainer' => scheme.onTertiaryContainer,
      'material.colorScheme.tertiaryFixed' => scheme.tertiaryFixed,
      'material.colorScheme.tertiaryFixedDim' => scheme.tertiaryFixedDim,
      'material.colorScheme.onTertiaryFixed' => scheme.onTertiaryFixed,
      'material.colorScheme.onTertiaryFixedVariant' =>
        scheme.onTertiaryFixedVariant,
      'material.colorScheme.error' => scheme.error,
      'material.colorScheme.onError' => scheme.onError,
      'material.colorScheme.errorContainer' => scheme.errorContainer,
      'material.colorScheme.onErrorContainer' => scheme.onErrorContainer,
      'material.colorScheme.surface' => scheme.surface,
      'material.colorScheme.onSurface' => scheme.onSurface,
      'material.colorScheme.surfaceDim' => scheme.surfaceDim,
      'material.colorScheme.surfaceBright' => scheme.surfaceBright,
      'material.colorScheme.surfaceContainerLowest' =>
        scheme.surfaceContainerLowest,
      'material.colorScheme.surfaceContainerLow' => scheme.surfaceContainerLow,
      'material.colorScheme.surfaceContainer' => scheme.surfaceContainer,
      'material.colorScheme.surfaceContainerHigh' =>
        scheme.surfaceContainerHigh,
      'material.colorScheme.surfaceContainerHighest' =>
        scheme.surfaceContainerHighest,
      'material.colorScheme.onSurfaceVariant' => scheme.onSurfaceVariant,
      'material.colorScheme.outline' => scheme.outline,
      'material.colorScheme.outlineVariant' => scheme.outlineVariant,
      'material.colorScheme.shadow' => scheme.shadow,
      'material.colorScheme.scrim' => scheme.scrim,
      'material.colorScheme.inverseSurface' => scheme.inverseSurface,
      'material.colorScheme.onInverseSurface' => scheme.onInverseSurface,
      'material.colorScheme.inversePrimary' => scheme.inversePrimary,
      'material.colorScheme.surfaceTint' => scheme.surfaceTint,
      _ => throw StateError(
        'Unreviewed Canvas color theme token: ${token.wireId}',
      ),
    };
  }

  TextStyle? _themeTextStyle(BuildContext context, String propertyName) {
    final value = node.properties[propertyName]?.value;
    if (value is! CanvasThemeToken) {
      return null;
    }
    final textTheme = Theme.of(context).textTheme;
    return switch (value.wireId) {
      'material.textTheme.displayLarge' => textTheme.displayLarge,
      'material.textTheme.displayMedium' => textTheme.displayMedium,
      'material.textTheme.displaySmall' => textTheme.displaySmall,
      'material.textTheme.headlineLarge' => textTheme.headlineLarge,
      'material.textTheme.headlineMedium' => textTheme.headlineMedium,
      'material.textTheme.headlineSmall' => textTheme.headlineSmall,
      'material.textTheme.titleLarge' => textTheme.titleLarge,
      'material.textTheme.titleMedium' => textTheme.titleMedium,
      'material.textTheme.titleSmall' => textTheme.titleSmall,
      'material.textTheme.bodyLarge' => textTheme.bodyLarge,
      'material.textTheme.bodyMedium' => textTheme.bodyMedium,
      'material.textTheme.bodySmall' => textTheme.bodySmall,
      'material.textTheme.labelLarge' => textTheme.labelLarge,
      'material.textTheme.labelMedium' => textTheme.labelMedium,
      'material.textTheme.labelSmall' => textTheme.labelSmall,
      _ => throw StateError(
        'Unreviewed Canvas TextTheme token: ${value.wireId}',
      ),
    };
  }

  Paint? _paint(BuildContext context, String name) {
    final value = node.properties[name]?.value;
    if (value is! CanvasPaint) {
      return null;
    }
    return Paint()
      ..color = _colorSource(context, value.color)
      ..blendMode = _blendMode(value.blendMode)
      ..style = value.style == 'stroke'
          ? PaintingStyle.stroke
          : PaintingStyle.fill
      ..strokeWidth = value.strokeWidth
      ..strokeCap = switch (value.strokeCap) {
        'round' => StrokeCap.round,
        'square' => StrokeCap.square,
        _ => StrokeCap.butt,
      }
      ..strokeJoin = switch (value.strokeJoin) {
        'round' => StrokeJoin.round,
        'bevel' => StrokeJoin.bevel,
        _ => StrokeJoin.miter,
      }
      ..strokeMiterLimit = value.strokeMiterLimit
      ..isAntiAlias = value.antiAlias
      ..filterQuality = switch (value.filterQuality) {
        'low' => FilterQuality.low,
        'medium' => FilterQuality.medium,
        'high' => FilterQuality.high,
        _ => FilterQuality.none,
      }
      ..invertColors = value.invertColors
      ..maskFilter = value.maskFilter == null
          ? null
          : MaskFilter.blur(switch (value.maskFilter!.style) {
              'solid' => BlurStyle.solid,
              'outer' => BlurStyle.outer,
              'inner' => BlurStyle.inner,
              _ => BlurStyle.normal,
            }, value.maskFilter!.sigma);
  }

  List<Shadow>? _shadows(BuildContext context, String name) {
    final value = node.properties[name]?.value;
    if (value is! List<CanvasShadowValue>) {
      return null;
    }
    return List.unmodifiable([
      for (final shadow in value)
        Shadow(
          color: _colorSource(context, shadow.color),
          offset: Offset(shadow.offsetX, shadow.offsetY),
          blurRadius: shadow.blurRadius,
        ),
    ]);
  }

  List<FontFeature>? _fontFeatures(String name) {
    final value = node.properties[name]?.value;
    if (value is! List<CanvasFontFeatureValue>) {
      return null;
    }
    return List.unmodifiable([
      for (final feature in value) FontFeature(feature.tag, feature.value),
    ]);
  }

  List<FontVariation>? _fontVariations(String name) {
    final value = node.properties[name]?.value;
    if (value is! List<CanvasFontVariationValue>) {
      return null;
    }
    return List.unmodifiable([
      for (final variation in value)
        FontVariation(variation.axis, variation.value),
    ]);
  }

  BlendMode _blendMode(String name) => switch (name) {
    'clear' => BlendMode.clear,
    'src' => BlendMode.src,
    'dst' => BlendMode.dst,
    'dstOver' => BlendMode.dstOver,
    'srcIn' => BlendMode.srcIn,
    'dstIn' => BlendMode.dstIn,
    'srcOut' => BlendMode.srcOut,
    'dstOut' => BlendMode.dstOut,
    'srcATop' => BlendMode.srcATop,
    'dstATop' => BlendMode.dstATop,
    'xor' => BlendMode.xor,
    'plus' => BlendMode.plus,
    'modulate' => BlendMode.modulate,
    'screen' => BlendMode.screen,
    'overlay' => BlendMode.overlay,
    'darken' => BlendMode.darken,
    'lighten' => BlendMode.lighten,
    'colorDodge' => BlendMode.colorDodge,
    'colorBurn' => BlendMode.colorBurn,
    'hardLight' => BlendMode.hardLight,
    'softLight' => BlendMode.softLight,
    'difference' => BlendMode.difference,
    'exclusion' => BlendMode.exclusion,
    'multiply' => BlendMode.multiply,
    'hue' => BlendMode.hue,
    'saturation' => BlendMode.saturation,
    'color' => BlendMode.color,
    'luminosity' => BlendMode.luminosity,
    _ => BlendMode.srcOver,
  };

  BlendMode? _optionalBlendMode(String name) {
    final value = _enum(name);
    return value == null ? null : _blendMode(value);
  }

  String? _enum(String name) {
    final property = node.properties[name];
    final value = property?.value;
    return value is CanvasEnumValue ? value.value : null;
  }

  String? _enumOrString(String name) => _enum(name) ?? _string(name);

  MouseCursor? _mouseCursor(String name) => switch (_enumOrString(name)) {
    'defer' => MouseCursor.defer,
    'uncontrolled' => MouseCursor.uncontrolled,
    'clickable' => WidgetStateMouseCursor.clickable,
    'adaptiveClickable' => WidgetStateMouseCursor.adaptiveClickable,
    'textable' => WidgetStateMouseCursor.textable,
    'none' => SystemMouseCursors.none,
    'basic' => SystemMouseCursors.basic,
    'click' => SystemMouseCursors.click,
    'forbidden' => SystemMouseCursors.forbidden,
    'wait' => SystemMouseCursors.wait,
    'progress' => SystemMouseCursors.progress,
    'contextMenu' => SystemMouseCursors.contextMenu,
    'help' => SystemMouseCursors.help,
    'text' => SystemMouseCursors.text,
    'verticalText' => SystemMouseCursors.verticalText,
    'cell' => SystemMouseCursors.cell,
    'precise' => SystemMouseCursors.precise,
    'move' => SystemMouseCursors.move,
    'grab' => SystemMouseCursors.grab,
    'grabbing' => SystemMouseCursors.grabbing,
    'noDrop' => SystemMouseCursors.noDrop,
    'alias' => SystemMouseCursors.alias,
    'copy' => SystemMouseCursors.copy,
    'disappearing' => SystemMouseCursors.disappearing,
    'allScroll' => SystemMouseCursors.allScroll,
    'resizeLeftRight' => SystemMouseCursors.resizeLeftRight,
    'resizeUpDown' => SystemMouseCursors.resizeUpDown,
    'resizeUpLeftDownRight' => SystemMouseCursors.resizeUpLeftDownRight,
    'resizeUpRightDownLeft' => SystemMouseCursors.resizeUpRightDownLeft,
    'resizeUp' => SystemMouseCursors.resizeUp,
    'resizeDown' => SystemMouseCursors.resizeDown,
    'resizeLeft' => SystemMouseCursors.resizeLeft,
    'resizeRight' => SystemMouseCursors.resizeRight,
    'resizeUpLeft' => SystemMouseCursors.resizeUpLeft,
    'resizeUpRight' => SystemMouseCursors.resizeUpRight,
    'resizeDownLeft' => SystemMouseCursors.resizeDownLeft,
    'resizeDownRight' => SystemMouseCursors.resizeDownRight,
    'resizeColumn' => SystemMouseCursors.resizeColumn,
    'resizeRow' => SystemMouseCursors.resizeRow,
    'zoomIn' => SystemMouseCursors.zoomIn,
    'zoomOut' => SystemMouseCursors.zoomOut,
    _ => null,
  };

  MainAxisAlignment _mainAxisAlignment() =>
      switch (_enum('mainAxisAlignment')) {
        'end' => MainAxisAlignment.end,
        'center' => MainAxisAlignment.center,
        'spaceBetween' => MainAxisAlignment.spaceBetween,
        'spaceAround' => MainAxisAlignment.spaceAround,
        'spaceEvenly' => MainAxisAlignment.spaceEvenly,
        _ => MainAxisAlignment.start,
      };

  MainAxisAlignment? _overflowBarMainAxisAlignment() =>
      switch (_enum('alignment')) {
        'start' => MainAxisAlignment.start,
        'end' => MainAxisAlignment.end,
        'center' => MainAxisAlignment.center,
        'spaceBetween' => MainAxisAlignment.spaceBetween,
        'spaceAround' => MainAxisAlignment.spaceAround,
        'spaceEvenly' => MainAxisAlignment.spaceEvenly,
        _ => null,
      };

  OverflowBarAlignment _overflowBarAlignment() =>
      switch (_enum('overflowAlignment')) {
        'end' => OverflowBarAlignment.end,
        'center' => OverflowBarAlignment.center,
        _ => OverflowBarAlignment.start,
      };

  MainAxisSize _mainAxisSize() =>
      _enum('mainAxisSize') == 'min' ? MainAxisSize.min : MainAxisSize.max;

  CrossAxisAlignment _crossAxisAlignment() =>
      switch (_enum('crossAxisAlignment')) {
        'start' => CrossAxisAlignment.start,
        'end' => CrossAxisAlignment.end,
        'stretch' => CrossAxisAlignment.stretch,
        'baseline' => CrossAxisAlignment.baseline,
        _ => CrossAxisAlignment.center,
      };

  WrapAlignment _wrapAlignment(String name) => switch (_enum(name)) {
    'end' => WrapAlignment.end,
    'center' => WrapAlignment.center,
    'spaceBetween' => WrapAlignment.spaceBetween,
    'spaceAround' => WrapAlignment.spaceAround,
    'spaceEvenly' => WrapAlignment.spaceEvenly,
    _ => WrapAlignment.start,
  };

  WrapCrossAlignment _wrapCrossAxisAlignment() =>
      switch (_enum('crossAxisAlignment')) {
        'end' => WrapCrossAlignment.end,
        'center' => WrapCrossAlignment.center,
        _ => WrapCrossAlignment.start,
      };

  TextDirection? _textDirection() => switch (_enum('textDirection')) {
    'rtl' => TextDirection.rtl,
    'ltr' => TextDirection.ltr,
    _ => null,
  };

  VerticalDirection _verticalDirection() => _enum('verticalDirection') == 'up'
      ? VerticalDirection.up
      : VerticalDirection.down;

  StackFit _stackFit([String name = 'fit']) => switch (_enum(name)) {
    'expand' => StackFit.expand,
    'passthrough' => StackFit.passthrough,
    _ => StackFit.loose,
  };

  TextBaseline? _textBaseline([String name = 'textBaseline']) =>
      switch (_enum(name)) {
        'alphabetic' => TextBaseline.alphabetic,
        'ideographic' => TextBaseline.ideographic,
        _ => null,
      };

  FontWeight? _fontWeight(String name) => switch (_enum(name)) {
    'w100' => FontWeight.w100,
    'w200' => FontWeight.w200,
    'w300' => FontWeight.w300,
    'w400' => FontWeight.w400,
    'w500' => FontWeight.w500,
    'w600' => FontWeight.w600,
    'w700' => FontWeight.w700,
    'w800' => FontWeight.w800,
    'w900' => FontWeight.w900,
    _ => null,
  };

  FontStyle? _fontStyle(String name) => switch (_enum(name)) {
    'normal' => FontStyle.normal,
    'italic' => FontStyle.italic,
    _ => null,
  };

  TextLeadingDistribution? _textLeadingDistribution(String name) =>
      switch (_enum(name)) {
        'proportional' => TextLeadingDistribution.proportional,
        'even' => TextLeadingDistribution.even,
        _ => null,
      };

  TextDecorationStyle? _textDecorationStyle(String name) =>
      switch (_enum(name)) {
        'solid' => TextDecorationStyle.solid,
        'double' => TextDecorationStyle.double,
        'dotted' => TextDecorationStyle.dotted,
        'dashed' => TextDecorationStyle.dashed,
        'wavy' => TextDecorationStyle.wavy,
        _ => null,
      };

  Brightness? _brightness(String name) => switch (_enum(name)) {
    'light' => Brightness.light,
    'dark' => Brightness.dark,
    _ => null,
  };

  Clip? _clipBehavior() => switch (_enum('clipBehavior')) {
    'none' => Clip.none,
    'hardEdge' => Clip.hardEdge,
    'antiAlias' => Clip.antiAlias,
    'antiAliasWithSaveLayer' => Clip.antiAliasWithSaveLayer,
    _ => null,
  };

  MaterialType? _materialType() => switch (_enum('materialType')) {
    'canvas' => MaterialType.canvas,
    'card' => MaterialType.card,
    'circle' => MaterialType.circle,
    'button' => MaterialType.button,
    'transparency' => MaterialType.transparency,
    _ => null,
  };

  TextAlign? _textAlign() => switch (_enum('textAlign')) {
    'start' => TextAlign.start,
    'end' => TextAlign.end,
    'left' => TextAlign.left,
    'right' => TextAlign.right,
    'center' => TextAlign.center,
    'justify' => TextAlign.justify,
    _ => null,
  };

  TextInputType? _textInputType() => switch (_string('keyboardType')) {
    'text' => TextInputType.text,
    'multiline' => TextInputType.multiline,
    'number' => TextInputType.number,
    'numberSigned' => const TextInputType.numberWithOptions(signed: true),
    'numberDecimal' => const TextInputType.numberWithOptions(decimal: true),
    'numberSignedDecimal' => const TextInputType.numberWithOptions(
      signed: true,
      decimal: true,
    ),
    'phone' => TextInputType.phone,
    'datetime' => TextInputType.datetime,
    'emailAddress' => TextInputType.emailAddress,
    'url' => TextInputType.url,
    'visiblePassword' => TextInputType.visiblePassword,
    'name' => TextInputType.name,
    'streetAddress' => TextInputType.streetAddress,
    'none' => TextInputType.none,
    'webSearch' => TextInputType.webSearch,
    'twitter' => TextInputType.twitter,
    _ => null,
  };

  TextInputAction? _textInputAction() => switch (_enum('textInputAction')) {
    'none' => TextInputAction.none,
    'unspecified' => TextInputAction.unspecified,
    'done' => TextInputAction.done,
    'go' => TextInputAction.go,
    'search' => TextInputAction.search,
    'send' => TextInputAction.send,
    'next' => TextInputAction.next,
    'previous' => TextInputAction.previous,
    'continueAction' => TextInputAction.continueAction,
    'join' => TextInputAction.join,
    'route' => TextInputAction.route,
    'emergencyCall' => TextInputAction.emergencyCall,
    'newline' => TextInputAction.newline,
    _ => null,
  };

  TextCapitalization? _textCapitalization() =>
      switch (_enum('textCapitalization')) {
        'words' => TextCapitalization.words,
        'sentences' => TextCapitalization.sentences,
        'characters' => TextCapitalization.characters,
        'none' => TextCapitalization.none,
        _ => null,
      };

  TextAlignVertical? _textAlignVertical() =>
      switch (_string('textAlignVertical')) {
        'top' => TextAlignVertical.top,
        'center' => TextAlignVertical.center,
        'bottom' => TextAlignVertical.bottom,
        _ => null,
      };

  SmartDashesType? _smartDashesType() => switch (_enum('smartDashesType')) {
    'disabled' => SmartDashesType.disabled,
    'enabled' => SmartDashesType.enabled,
    _ => null,
  };

  SmartQuotesType? _smartQuotesType() => switch (_enum('smartQuotesType')) {
    'disabled' => SmartQuotesType.disabled,
    'enabled' => SmartQuotesType.enabled,
    _ => null,
  };

  MaxLengthEnforcement? _maxLengthEnforcement() =>
      switch (_enum('maxLengthEnforcement')) {
        'none' => MaxLengthEnforcement.none,
        'enforced' => MaxLengthEnforcement.enforced,
        'truncateAfterCompositionEnds' =>
          MaxLengthEnforcement.truncateAfterCompositionEnds,
        _ => null,
      };

  ui.BoxHeightStyle? _boxHeightStyle() => switch (_enum(
    'selectionHeightStyle',
  )) {
    'tight' => ui.BoxHeightStyle.tight,
    'max' => ui.BoxHeightStyle.max,
    'includeLineSpacingMiddle' => ui.BoxHeightStyle.includeLineSpacingMiddle,
    'includeLineSpacingTop' => ui.BoxHeightStyle.includeLineSpacingTop,
    'includeLineSpacingBottom' => ui.BoxHeightStyle.includeLineSpacingBottom,
    'strut' => ui.BoxHeightStyle.strut,
    _ => null,
  };

  ui.BoxWidthStyle? _boxWidthStyle() => switch (_enum('selectionWidthStyle')) {
    'tight' => ui.BoxWidthStyle.tight,
    'max' => ui.BoxWidthStyle.max,
    _ => null,
  };

  DragStartBehavior _dragStartBehavior() => _enum('dragStartBehavior') == 'down'
      ? DragStartBehavior.down
      : DragStartBehavior.start;

  TextOverflow? _textOverflow([String name = 'overflow']) =>
      switch (_enum(name)) {
        'clip' => TextOverflow.clip,
        'fade' => TextOverflow.fade,
        'ellipsis' => TextOverflow.ellipsis,
        'visible' => TextOverflow.visible,
        _ => null,
      };

  TextWidthBasis? _textWidthBasis() => switch (_enum('textWidthBasis')) {
    'parent' => TextWidthBasis.parent,
    'longestLine' => TextWidthBasis.longestLine,
    _ => null,
  };
}

class _CanvasInlineTextEditor extends StatefulWidget {
  const _CanvasInlineTextEditor({
    required this.widgetId,
    required this.designerFocusParent,
    required this.initialText,
    required this.style,
    required this.strutStyle,
    required this.textAlign,
    required this.textDirection,
    required this.maxLines,
    required this.cursorColor,
    required this.onCommit,
    required this.onCancel,
    super.key,
  });

  final String widgetId;
  final FocusNode? designerFocusParent;
  final String initialText;
  final TextStyle? style;
  final StrutStyle? strutStyle;
  final TextAlign textAlign;
  final TextDirection? textDirection;
  final int maxLines;
  final Color cursorColor;
  final bool Function(String, String, bool) onCommit;
  final VoidCallback onCancel;

  @override
  State<_CanvasInlineTextEditor> createState() =>
      _CanvasInlineTextEditorState();
}

class _CanvasInlineTextEditorState extends State<_CanvasInlineTextEditor> {
  late final TextEditingController _controller;
  late final FocusNode _focusNode;
  bool _composing = false;
  bool _compositionObserved = false;
  String? _validationError;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController.fromValue(
      TextEditingValue(
        text: widget.initialText,
        selection: TextSelection.collapsed(offset: widget.initialText.length),
      ),
    )..addListener(_onEditingValueChanged);
    _focusNode = FocusNode(debugLabel: 'canvas-inline-text-${widget.widgetId}');
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _focusNode.requestFocus();
      }
    });
  }

  @override
  void dispose() {
    _controller
      ..removeListener(_onEditingValueChanged)
      ..dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bindings = <ShortcutActivator, VoidCallback>{
      if (!_composing)
        const SingleActivator(LogicalKeyboardKey.enter, control: true): _commit,
      if (!_composing)
        const SingleActivator(LogicalKeyboardKey.escape): _cancel,
    };
    // This is Designer UI, not an application TextField. Keep its focus branch
    // under the outer Designer surface so application ExcludeFocus still gates
    // every real application descendant without disabling F2 editing.
    return Focus(
      parentNode: widget.designerFocusParent,
      canRequestFocus: false,
      skipTraversal: true,
      includeSemantics: false,
      child: CallbackShortcuts(
        bindings: bindings,
        child: TextField(
          controller: _controller,
          focusNode: _focusNode,
          autofocus: true,
          keyboardType: TextInputType.multiline,
          maxLines: widget.maxLines,
          style: widget.style,
          strutStyle: widget.strutStyle,
          textAlign: widget.textAlign,
          textDirection: widget.textDirection,
          cursorColor: widget.cursorColor,
          decoration: InputDecoration(
            isDense: true,
            contentPadding: EdgeInsets.zero,
            border: InputBorder.none,
            enabledBorder: InputBorder.none,
            focusedBorder: InputBorder.none,
            errorBorder: InputBorder.none,
            focusedErrorBorder: InputBorder.none,
            errorText: _validationError,
            errorStyle: const TextStyle(fontSize: 10, height: 1),
          ),
        ),
      ),
    );
  }

  void _onEditingValueChanged() {
    final composing = _hasActiveComposition(_controller.value);
    if (composing) {
      _compositionObserved = true;
    }
    final validationError = inlineTextValidationError(_controller.text);
    if ((composing != _composing || validationError != _validationError) &&
        mounted) {
      setState(() {
        _composing = composing;
        _validationError = validationError;
      });
    }
  }

  void _commit() {
    if (_hasActiveComposition(_controller.value)) {
      return;
    }
    if (_validationError != null) {
      return;
    }
    final accepted = widget.onCommit(
      widget.widgetId,
      _controller.text,
      _compositionObserved,
    );
    if (!accepted && mounted) {
      setState(() {
        _validationError =
            'The host rejected this edit because its Canvas authority changed.';
      });
      _focusNode.requestFocus();
    }
  }

  void _cancel() {
    if (_hasActiveComposition(_controller.value)) {
      return;
    }
    widget.onCancel();
  }

  bool _hasActiveComposition(TextEditingValue value) {
    final composing = value.composing;
    return composing.start >= 0 &&
        composing.start < composing.end &&
        composing.end <= value.text.length;
  }
}

bool _ignoresPointersForNode(CanvasNode node) =>
    node.type == 'flutter.widgets.IgnorePointer' &&
    (node.properties['ignoring']?.value as bool? ?? true);

class _ZeroSizedWidgetTargetGroup {
  const _ZeroSizedWidgetTargetGroup({
    required this.rect,
    required this.widgetIds,
    required this.widgetTypes,
    required this.previewUnavailableMessages,
  });

  final Rect rect;
  final List<String> widgetIds;
  final List<String> widgetTypes;
  final List<String?> previewUnavailableMessages;
}

class _ZeroSizedWidgetTarget extends StatelessWidget {
  const _ZeroSizedWidgetTarget({
    required this.widgetIds,
    required this.widgetTypes,
    required this.previewUnavailableMessages,
    required this.selectedWidgetId,
    required this.dark,
    required this.onSelected,
  }) : assert(widgetIds.length == widgetTypes.length),
       assert(widgetIds.length == previewUnavailableMessages.length);

  final List<String> widgetIds;
  final List<String> widgetTypes;
  final List<String?> previewUnavailableMessages;
  final String? selectedWidgetId;
  final bool dark;
  final ValueChanged<String> onSelected;

  bool get _grouped => widgetIds.length > 1;

  bool get _selected =>
      selectedWidgetId != null && widgetIds.contains(selectedWidgetId);

  String get _keySuffix =>
      _grouped ? 'group-${widgetIds.first}' : widgetIds.single;

  String get _cyclingMessage =>
      widgetTypes.every((type) => type == 'flutter.widgets.SizedBox')
      ? '${widgetIds.length} overlapping empty SizedBox widgets. '
            'Activate repeatedly to cycle selection.'
      : '${widgetIds.length} overlapping '
            '${(widgetTypes.contains('flutter.widgets.IgnorePointer') || widgetTypes.contains('flutter.widgets.SliverIgnorePointer')) ? 'Designer targets' : 'zero-size widgets'}. '
            'Activate repeatedly to cycle selection.';

  String? get _previewUnavailableMessage {
    final selectedIndex = selectedWidgetId == null
        ? -1
        : widgetIds.indexOf(selectedWidgetId!);
    if (selectedIndex >= 0) {
      final selectedMessage = previewUnavailableMessages[selectedIndex];
      if (selectedMessage != null) {
        return selectedMessage;
      }
    }
    for (final message in previewUnavailableMessages) {
      if (message != null) {
        return message;
      }
    }
    return null;
  }

  void _activate() {
    final selectedIndex = selectedWidgetId == null
        ? -1
        : widgetIds.indexOf(selectedWidgetId!);
    final nextIndex = selectedIndex < 0
        ? 0
        : (selectedIndex + 1) % widgetIds.length;
    onSelected(widgetIds[nextIndex]);
  }

  @override
  Widget build(BuildContext context) {
    final previewUnavailableMessage = _previewUnavailableMessage;
    Widget visual = CustomPaint(
      key: ValueKey('canvas-zero-size-widget-outline-$_keySuffix'),
      foregroundPainter: _CanvasWidgetOutlinePainter(
        selected: _selected,
        inflateEmptyLinearContainer: false,
        visualScale: 1,
        unselectedColor: dark
            ? const Color(0x99b0b8c1)
            : const Color(0x9974808a),
      ),
      child: const SizedBox.expand(),
    );
    if (previewUnavailableMessage != null) {
      visual = Stack(
        fit: StackFit.expand,
        children: [
          visual,
          IgnorePointer(
            child: Align(
              alignment: Alignment.topRight,
              child: SizedBox.square(
                dimension: 20,
                child: DecoratedBox(
                  key: ValueKey(
                    'canvas-zero-size-custom-clipper-warning-$_keySuffix',
                  ),
                  decoration: BoxDecoration(
                    color: Colors.amber.shade100,
                    border: Border.all(color: Colors.amber.shade800),
                    borderRadius: BorderRadius.circular(2),
                  ),
                  child: const Center(
                    child: Text(
                      '!',
                      style: TextStyle(
                        color: Colors.black87,
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        height: 1,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      );
    }
    final interaction = MouseRegion(
      cursor: SystemMouseCursors.click,
      child: GestureDetector(
        key: ValueKey('canvas-zero-size-widget-target-$_keySuffix'),
        behavior: HitTestBehavior.opaque,
        onTap: _activate,
        child: visual,
      ),
    );
    final baseLabel = _grouped
        ? _cyclingMessage
        : '${_displayType(widgetTypes.single)} ${widgetIds.single}';
    final semanticsLabel = previewUnavailableMessage == null
        ? baseLabel
        : '$baseLabel. $previewUnavailableMessage';
    final tooltipMessage = switch ((_grouped, previewUnavailableMessage)) {
      (true, final String warning) => '$_cyclingMessage $warning',
      (true, null) => _cyclingMessage,
      (false, final String warning) => warning,
      (false, null) when widgetTypes.single == 'flutter.widgets.SliverOffstage' =>
        'SliverOffstage Designer selection handle. Hidden content stays editable in the widget tree.',
      (false, null) =>
        (widgetTypes.single == 'flutter.widgets.IgnorePointer' || widgetTypes.single == 'flutter.widgets.SliverIgnorePointer')
            ? '${_displayType(widgetTypes.single)} Designer selection handle. '
                  'The widget body keeps the configured pointer behavior.'
            : null,
    };
    return Semantics(
      key: ValueKey('canvas-zero-size-widget-semantics-$_keySuffix'),
      label: semanticsLabel,
      selected: _selected,
      child: tooltipMessage == null
          ? interaction
          : Tooltip(
              message: tooltipMessage,
              excludeFromSemantics: true,
              child: interaction,
            ),
    );
  }
}

// Layout-only prototype descendants must never acquire Canvas hit/drop handles.
// A render marker preserves native sizing while avoiding invalid paint transforms
// through Flutter's special unpainted prototype render child.
class _PrototypeMeasurement extends SingleChildRenderObjectWidget {
  const _PrototypeMeasurement({required super.child});
  @override
  RenderObject createRenderObject(BuildContext context) => _PrototypeMeasurementRenderBox();
}
class _PrototypeMeasurementRenderBox extends RenderProxyBox {}

/// Reviewed finite-size layout preview. Never invokes project delegate methods.
class _MultiChildLayoutPreviewDelegate extends MultiChildLayoutDelegate {
  _MultiChildLayoutPreviewDelegate(this.ids);
  final List<Object> ids;
  @override
  Size getSize(BoxConstraints constraints) => constraints.constrain(const Size(256, 192));
  @override
  void performLayout(Size size) {
    final height = ids.isEmpty ? 0.0 : size.height / ids.length;
    for(var index = 0; index < ids.length; index++) {
      layoutChild(ids[index], BoxConstraints.loose(Size(size.width, height)));
      positionChild(ids[index], Offset(0, index * height));
    }
  }
  @override
  bool shouldRelayout(covariant _MultiChildLayoutPreviewDelegate oldDelegate) => true;
}

class _FlowPreviewDelegate extends FlowDelegate {
  const _FlowPreviewDelegate();
  @override
  Size getSize(BoxConstraints constraints) => constraints.constrain(const Size(256, 192));
  @override
  BoxConstraints getConstraintsForChild(int i, BoxConstraints constraints) {
    final size = getSize(constraints);
    return BoxConstraints.loose(Size(size.width.clamp(0.0, 48.0), size.height.clamp(0.0, 48.0)));
  }
  @override
  void paintChildren(FlowPaintingContext context) {
    var x = 0.0, y = 0.0, rowHeight = 0.0;
    for (var i = 0; i < context.childCount; i++) {
      final size = context.getChildSize(i)!;
      if (x > 0 && x + size.width > context.size.width) {
        x = 0;
        y += rowHeight + 8;
        rowHeight = 0;
      }
      context.paintChild(i, transform: Matrix4.translationValues(x, y, 0));
      x += size.width + 8;
      if (size.height > rowHeight) rowHeight = size.height;
    }
  }
  @override
  bool shouldRelayout(covariant _FlowPreviewDelegate oldDelegate) => false;
  @override
  bool shouldRepaint(covariant _FlowPreviewDelegate oldDelegate) => false;
}

class _SingleChildLayoutPreviewDelegate extends SingleChildLayoutDelegate {
  const _SingleChildLayoutPreviewDelegate();
  @override
  Size getSize(BoxConstraints constraints) => constraints.constrain(const Size(128, 96));
  @override
  BoxConstraints getConstraintsForChild(BoxConstraints constraints) =>
      BoxConstraints.loose(getSize(constraints));
  @override
  Offset getPositionForChild(Size size, Size childSize) =>
      Offset((size.width - childSize.width) / 2, (size.height - childSize.height) / 2);
  @override
  bool shouldRelayout(covariant _SingleChildLayoutPreviewDelegate oldDelegate) => false;
}

/// Keeps the native painter-presence/cache-hint contract without running project code.
/// It paints nothing, publishes no semantics and does not claim source hit behavior.
class _InertProjectPainter extends CustomPainter {
  const _InertProjectPainter();
  @override
  void paint(Canvas canvas, Size size) {}
  @override
  bool? hitTest(Offset position) => false;
  @override
  bool shouldRepaint(covariant _InertProjectPainter oldDelegate) => false;
}

class _CanvasWidgetOutlinePainter extends CustomPainter {
  const _CanvasWidgetOutlinePainter({
    required this.selected,
    required this.inflateEmptyLinearContainer,
    required this.visualScale,
    required this.unselectedColor,
  });

  final bool selected;
  final bool inflateEmptyLinearContainer;
  final double visualScale;
  final Color unselectedColor;

  double get debugStrokeWidth =>
      (selected ? 2 : 1) / math.max(visualScale, 0.000001);

  double get debugDashLength => 4 / math.max(visualScale, 0.000001);

  @override
  void paint(Canvas canvas, Size size) {
    const minimumExtent = 36.0;
    final width = inflateEmptyLinearContainer
        ? math.max(size.width, minimumExtent)
        : size.width;
    final height = inflateEmptyLinearContainer
        ? math.max(size.height, minimumExtent)
        : size.height;
    final rect = Rect.fromCenter(
      center: size.center(Offset.zero),
      width: width,
      height: height,
    );
    final safeScale = math.max(visualScale, 0.000001);
    final paint = Paint()
      ..color = selected ? const Color(0xff1a73e8) : unselectedColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = debugStrokeWidth
      ..strokeCap = StrokeCap.butt;
    if (selected) {
      canvas.drawRect(rect, paint);
    } else {
      _drawDashedRect(
        canvas,
        rect,
        paint,
        dashLength: debugDashLength,
        gapLength: 3 / safeScale,
      );
    }
  }

  @override
  bool shouldRepaint(_CanvasWidgetOutlinePainter oldDelegate) =>
      selected != oldDelegate.selected ||
      inflateEmptyLinearContainer != oldDelegate.inflateEmptyLinearContainer ||
      visualScale != oldDelegate.visualScale ||
      unselectedColor != oldDelegate.unselectedColor;
}

class _CanvasPaddingGuidesPainter extends CustomPainter {
  const _CanvasPaddingGuidesPainter({
    required this.insets,
    required this.visualScale,
    required this.color,
  });

  final EdgeInsets insets;
  final double visualScale;
  final Color color;

  double get debugStrokeWidth => 1 / math.max(visualScale, 0.000001);

  double get debugCapLength => 4 / math.max(visualScale, 0.000001);

  @override
  void paint(Canvas canvas, Size size) {
    if ((size.width <= 0 && size.height <= 0) || insets == EdgeInsets.zero) {
      return;
    }
    final outer = Offset.zero & size;
    final inner = insets.deflateRect(outer);
    final innerLeft = inner.left.clamp(outer.left, outer.right).toDouble();
    final innerTop = inner.top.clamp(outer.top, outer.bottom).toDouble();
    final innerRight = inner.right.clamp(outer.left, outer.right).toDouble();
    final innerBottom = inner.bottom.clamp(outer.top, outer.bottom).toDouble();
    final guideX = ((innerLeft + innerRight) / 2)
        .clamp(outer.left, outer.right)
        .toDouble();
    final guideY = ((innerTop + innerBottom) / 2)
        .clamp(outer.top, outer.bottom)
        .toDouble();
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.stroke
      ..strokeWidth = debugStrokeWidth
      ..strokeCap = StrokeCap.butt;
    final capLength = debugCapLength;

    _drawMeasurement(
      canvas,
      Offset(outer.left, guideY),
      Offset(innerLeft, guideY),
      paint,
      capLength,
    );
    _drawMeasurement(
      canvas,
      Offset(innerRight, guideY),
      Offset(outer.right, guideY),
      paint,
      capLength,
    );
    _drawMeasurement(
      canvas,
      Offset(guideX, outer.top),
      Offset(guideX, innerTop),
      paint,
      capLength,
    );
    _drawMeasurement(
      canvas,
      Offset(guideX, innerBottom),
      Offset(guideX, outer.bottom),
      paint,
      capLength,
    );
  }

  @override
  bool shouldRepaint(_CanvasPaddingGuidesPainter oldDelegate) =>
      insets != oldDelegate.insets ||
      visualScale != oldDelegate.visualScale ||
      color != oldDelegate.color;
}

class _CanvasContainerInsetsGuidesPainter extends CustomPainter {
  const _CanvasContainerInsetsGuidesPainter({
    required this.padding,
    required this.margin,
    required this.visualScale,
    required this.paddingColor,
    required this.marginColor,
  });

  final EdgeInsets padding;
  final EdgeInsets margin;
  final double visualScale;
  final Color paddingColor;
  final Color marginColor;

  double get debugStrokeWidth => 1 / math.max(visualScale, 0.000001);

  double get debugCapLength => 4 / math.max(visualScale, 0.000001);

  @override
  void paint(Canvas canvas, Size size) {
    if ((size.width <= 0 && size.height <= 0) ||
        (padding == EdgeInsets.zero && margin == EdgeInsets.zero)) {
      return;
    }
    final layoutBounds = Offset.zero & size;
    final decorationBounds = _designerDeflateRect(layoutBounds, margin);
    final contentBounds = _designerDeflateRect(decorationBounds, padding);
    final marginPaint = Paint()
      ..color = marginColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = debugStrokeWidth
      ..strokeCap = StrokeCap.butt;
    final paddingPaint = Paint()
      ..color = paddingColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = debugStrokeWidth
      ..strokeCap = StrokeCap.butt;
    final safeScale = math.max(visualScale, 0.000001);
    _drawInsetsMeasurements(
      canvas,
      layoutBounds,
      decorationBounds,
      marginPaint,
      debugCapLength,
      dashLength: 3 / safeScale,
      gapLength: 2 / safeScale,
    );
    _drawInsetsMeasurements(
      canvas,
      decorationBounds,
      contentBounds,
      paddingPaint,
      debugCapLength,
    );
  }

  @override
  bool shouldRepaint(_CanvasContainerInsetsGuidesPainter oldDelegate) =>
      padding != oldDelegate.padding ||
      margin != oldDelegate.margin ||
      visualScale != oldDelegate.visualScale ||
      paddingColor != oldDelegate.paddingColor ||
      marginColor != oldDelegate.marginColor;
}

Rect _designerDeflateRect(Rect outer, EdgeInsets insets) {
  final left = outer.left + insets.left;
  final top = outer.top + insets.top;
  final right = outer.right - insets.right;
  final bottom = outer.bottom - insets.bottom;
  final horizontalMiddle = (left + right) / 2;
  final verticalMiddle = (top + bottom) / 2;
  return Rect.fromLTRB(
    math.min(left, horizontalMiddle),
    math.min(top, verticalMiddle),
    math.max(right, horizontalMiddle),
    math.max(bottom, verticalMiddle),
  );
}

void _drawInsetsMeasurements(
  Canvas canvas,
  Rect outer,
  Rect inner,
  Paint paint,
  double capLength, {
  double? dashLength,
  double? gapLength,
}) {
  final guideX = (inner.left + inner.right) / 2;
  final guideY = (inner.top + inner.bottom) / 2;
  void draw(Offset start, Offset end) {
    if (dashLength == null || gapLength == null) {
      _drawMeasurement(canvas, start, end, paint, capLength);
    } else {
      _drawDashedMeasurement(
        canvas,
        start,
        end,
        paint,
        capLength,
        dashLength,
        gapLength,
      );
    }
  }

  draw(Offset(outer.left, guideY), Offset(inner.left, guideY));
  draw(Offset(inner.right, guideY), Offset(outer.right, guideY));
  draw(Offset(guideX, outer.top), Offset(guideX, inner.top));
  draw(Offset(guideX, inner.bottom), Offset(guideX, outer.bottom));
}

void _drawDashedRect(
  Canvas canvas,
  Rect rect,
  Paint paint, {
  required double dashLength,
  required double gapLength,
}) {
  _drawDashedLine(
    canvas,
    rect.topLeft,
    rect.topRight,
    paint,
    dashLength,
    gapLength,
  );
  _drawDashedLine(
    canvas,
    rect.topRight,
    rect.bottomRight,
    paint,
    dashLength,
    gapLength,
  );
  _drawDashedLine(
    canvas,
    rect.bottomRight,
    rect.bottomLeft,
    paint,
    dashLength,
    gapLength,
  );
  _drawDashedLine(
    canvas,
    rect.bottomLeft,
    rect.topLeft,
    paint,
    dashLength,
    gapLength,
  );
}

void _drawDashedLine(
  Canvas canvas,
  Offset start,
  Offset end,
  Paint paint,
  double dashLength,
  double gapLength,
) {
  final delta = end - start;
  final distance = delta.distance;
  if (distance <= 0) {
    return;
  }
  final direction = delta / distance;
  var offset = 0.0;
  while (offset < distance) {
    final dashEnd = math.min(offset + dashLength, distance);
    canvas.drawLine(
      start + direction * offset,
      start + direction * dashEnd,
      paint,
    );
    offset += dashLength + gapLength;
  }
}

void _drawMeasurement(
  Canvas canvas,
  Offset start,
  Offset end,
  Paint paint,
  double capLength,
) {
  final delta = end - start;
  if (delta.distance <= 0.000001) {
    return;
  }
  canvas.drawLine(start, end, paint);
  final halfCap = capLength / 2;
  if (delta.dx.abs() >= delta.dy.abs()) {
    canvas.drawLine(
      start.translate(0, -halfCap),
      start.translate(0, halfCap),
      paint,
    );
    canvas.drawLine(
      end.translate(0, -halfCap),
      end.translate(0, halfCap),
      paint,
    );
  } else {
    canvas.drawLine(
      start.translate(-halfCap, 0),
      start.translate(halfCap, 0),
      paint,
    );
    canvas.drawLine(
      end.translate(-halfCap, 0),
      end.translate(halfCap, 0),
      paint,
    );
  }
}

void _drawDashedMeasurement(
  Canvas canvas,
  Offset start,
  Offset end,
  Paint paint,
  double capLength,
  double dashLength,
  double gapLength,
) {
  final delta = end - start;
  if (delta.distance <= 0.000001) {
    return;
  }
  _drawDashedLine(canvas, start, end, paint, dashLength, gapLength);
  final halfCap = capLength / 2;
  if (delta.dx.abs() >= delta.dy.abs()) {
    canvas.drawLine(
      start.translate(0, -halfCap),
      start.translate(0, halfCap),
      paint,
    );
    canvas.drawLine(
      end.translate(0, -halfCap),
      end.translate(0, halfCap),
      paint,
    );
  } else {
    canvas.drawLine(
      start.translate(-halfCap, 0),
      start.translate(halfCap, 0),
      paint,
    );
    canvas.drawLine(
      end.translate(-halfCap, 0),
      end.translate(halfCap, 0),
      paint,
    );
  }
}

class _RuntimeStatus extends StatelessWidget {
  const _RuntimeStatus({required this.message, required this.failed});

  final String message;
  final bool failed;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xfff5f6f7),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            message,
            textAlign: TextAlign.center,
            style: TextStyle(
              color: failed ? const Color(0xffb3261e) : const Color(0xff5f6368),
              fontSize: 14,
            ),
          ),
        ),
      ),
    );
  }
}

ThemeData _theme(CanvasProfile profile, Brightness brightness) {
  final colorScheme = _applyColorSchemeOverrides(
    ColorScheme.fromSeed(
      seedColor: Color(profile.theme.seedArgb),
      brightness: brightness,
    ),
    profile.theme.colorScheme,
  );
  final base = ThemeData.from(colorScheme: colorScheme);
  final roleTheme = base.copyWith(
    textTheme: _applyTextThemeOverrides(
      base.textTheme,
      colorScheme,
      profile.theme.textTheme,
    ),
    platform: canvasAdaptiveTargetPlatform(profile.targetPlatform),
  );
  return _applyComponentColorOverrides(
    roleTheme,
    colorScheme,
    profile.theme.components,
  );
}

ThemeData _applyComponentColorOverrides(
  ThemeData base,
  ColorScheme colorScheme,
  Map<String, CanvasThemeColorValue> values,
) {
  Color? color(String role) => _themeOverrideColor(values[role], colorScheme);
  final hasAppBar = values.keys.any((key) => key.startsWith('appBar.'));
  final hasIcon = values.containsKey('icon.color');
  final hasElevated = values.keys.any(
    (key) => key.startsWith('elevatedButton.'),
  );
  return base.copyWith(
    scaffoldBackgroundColor: color('scaffold.backgroundColor'),
    appBarTheme: hasAppBar
        ? base.appBarTheme.copyWith(
            backgroundColor: color('appBar.backgroundColor'),
            foregroundColor: color('appBar.foregroundColor'),
            shadowColor: color('appBar.shadowColor'),
            surfaceTintColor: color('appBar.surfaceTintColor'),
          )
        : null,
    iconTheme: hasIcon
        ? base.iconTheme.copyWith(color: color('icon.color'))
        : null,
    elevatedButtonTheme: hasElevated
        ? ElevatedButtonThemeData(
            style: (base.elevatedButtonTheme.style ?? const ButtonStyle())
                .copyWith(
                  backgroundColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'backgroundColor',
                  ),
                  foregroundColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'foregroundColor',
                  ),
                  overlayColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'overlayColor',
                  ),
                  shadowColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'shadowColor',
                  ),
                  surfaceTintColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'surfaceTintColor',
                  ),
                  iconColor: _componentButtonColor(
                    colorScheme,
                    values,
                    'iconColor',
                  ),
                ),
          )
        : null,
  );
}

WidgetStateProperty<Color?>? _componentButtonColor(
  ColorScheme colorScheme,
  Map<String, CanvasThemeColorValue> values,
  String property,
) {
  CanvasThemeColorValue? value(String state) =>
      values['elevatedButton.$property.$state'];
  if (const [
    'default',
    'disabled',
    'pressed',
    'hovered',
    'focused',
  ].every((state) => value(state) == null)) {
    return null;
  }
  return WidgetStateProperty.resolveWith<Color?>((states) {
    if (states.contains(WidgetState.disabled)) {
      return _themeOverrideColor(value('disabled'), colorScheme);
    }
    for (final entry in const <(WidgetState, String)>[
      (WidgetState.pressed, 'pressed'),
      (WidgetState.hovered, 'hovered'),
      (WidgetState.focused, 'focused'),
    ]) {
      final configured = value(entry.$2);
      if (states.contains(entry.$1) && configured != null) {
        return _themeOverrideColor(configured, colorScheme);
      }
    }
    return _themeOverrideColor(value('default'), colorScheme);
  });
}

ColorScheme _applyColorSchemeOverrides(
  ColorScheme base,
  Map<String, int> values,
) => base.copyWith(
  primary: _roleColor(values, 'primary'),
  onPrimary: _roleColor(values, 'onPrimary'),
  primaryContainer: _roleColor(values, 'primaryContainer'),
  onPrimaryContainer: _roleColor(values, 'onPrimaryContainer'),
  primaryFixed: _roleColor(values, 'primaryFixed'),
  primaryFixedDim: _roleColor(values, 'primaryFixedDim'),
  onPrimaryFixed: _roleColor(values, 'onPrimaryFixed'),
  onPrimaryFixedVariant: _roleColor(values, 'onPrimaryFixedVariant'),
  secondary: _roleColor(values, 'secondary'),
  onSecondary: _roleColor(values, 'onSecondary'),
  secondaryContainer: _roleColor(values, 'secondaryContainer'),
  onSecondaryContainer: _roleColor(values, 'onSecondaryContainer'),
  secondaryFixed: _roleColor(values, 'secondaryFixed'),
  secondaryFixedDim: _roleColor(values, 'secondaryFixedDim'),
  onSecondaryFixed: _roleColor(values, 'onSecondaryFixed'),
  onSecondaryFixedVariant: _roleColor(values, 'onSecondaryFixedVariant'),
  tertiary: _roleColor(values, 'tertiary'),
  onTertiary: _roleColor(values, 'onTertiary'),
  tertiaryContainer: _roleColor(values, 'tertiaryContainer'),
  onTertiaryContainer: _roleColor(values, 'onTertiaryContainer'),
  tertiaryFixed: _roleColor(values, 'tertiaryFixed'),
  tertiaryFixedDim: _roleColor(values, 'tertiaryFixedDim'),
  onTertiaryFixed: _roleColor(values, 'onTertiaryFixed'),
  onTertiaryFixedVariant: _roleColor(values, 'onTertiaryFixedVariant'),
  error: _roleColor(values, 'error'),
  onError: _roleColor(values, 'onError'),
  errorContainer: _roleColor(values, 'errorContainer'),
  onErrorContainer: _roleColor(values, 'onErrorContainer'),
  surface: _roleColor(values, 'surface'),
  onSurface: _roleColor(values, 'onSurface'),
  surfaceDim: _roleColor(values, 'surfaceDim'),
  surfaceBright: _roleColor(values, 'surfaceBright'),
  surfaceContainerLowest: _roleColor(values, 'surfaceContainerLowest'),
  surfaceContainerLow: _roleColor(values, 'surfaceContainerLow'),
  surfaceContainer: _roleColor(values, 'surfaceContainer'),
  surfaceContainerHigh: _roleColor(values, 'surfaceContainerHigh'),
  surfaceContainerHighest: _roleColor(values, 'surfaceContainerHighest'),
  onSurfaceVariant: _roleColor(values, 'onSurfaceVariant'),
  outline: _roleColor(values, 'outline'),
  outlineVariant: _roleColor(values, 'outlineVariant'),
  shadow: _roleColor(values, 'shadow'),
  scrim: _roleColor(values, 'scrim'),
  inverseSurface: _roleColor(values, 'inverseSurface'),
  onInverseSurface: _roleColor(values, 'onInverseSurface'),
  inversePrimary: _roleColor(values, 'inversePrimary'),
  surfaceTint: _roleColor(values, 'surfaceTint'),
);

Color? _roleColor(Map<String, int> values, String role) =>
    values.containsKey(role) ? Color(values[role]!) : null;

TextTheme _applyTextThemeOverrides(
  TextTheme base,
  ColorScheme colorScheme,
  Map<String, CanvasThemeTextStyleOverride> values,
) => base.copyWith(
  displayLarge: _textRole(
    base.displayLarge,
    colorScheme,
    values['displayLarge'],
  ),
  displayMedium: _textRole(
    base.displayMedium,
    colorScheme,
    values['displayMedium'],
  ),
  displaySmall: _textRole(
    base.displaySmall,
    colorScheme,
    values['displaySmall'],
  ),
  headlineLarge: _textRole(
    base.headlineLarge,
    colorScheme,
    values['headlineLarge'],
  ),
  headlineMedium: _textRole(
    base.headlineMedium,
    colorScheme,
    values['headlineMedium'],
  ),
  headlineSmall: _textRole(
    base.headlineSmall,
    colorScheme,
    values['headlineSmall'],
  ),
  titleLarge: _textRole(base.titleLarge, colorScheme, values['titleLarge']),
  titleMedium: _textRole(base.titleMedium, colorScheme, values['titleMedium']),
  titleSmall: _textRole(base.titleSmall, colorScheme, values['titleSmall']),
  bodyLarge: _textRole(base.bodyLarge, colorScheme, values['bodyLarge']),
  bodyMedium: _textRole(base.bodyMedium, colorScheme, values['bodyMedium']),
  bodySmall: _textRole(base.bodySmall, colorScheme, values['bodySmall']),
  labelLarge: _textRole(base.labelLarge, colorScheme, values['labelLarge']),
  labelMedium: _textRole(base.labelMedium, colorScheme, values['labelMedium']),
  labelSmall: _textRole(base.labelSmall, colorScheme, values['labelSmall']),
);

TextStyle? _textRole(
  TextStyle? base,
  ColorScheme colorScheme,
  CanvasThemeTextStyleOverride? value,
) {
  if (value == null) {
    return null;
  }
  return (base ?? const TextStyle()).copyWith(
    color: _themeOverrideColor(value.color, colorScheme),
    backgroundColor: _themeOverrideColor(value.backgroundColor, colorScheme),
    fontSize: value.fontSize,
    fontWeight: _fontWeight(value.fontWeight),
    fontStyle: switch (value.fontStyle) {
      'normal' => FontStyle.normal,
      'italic' => FontStyle.italic,
      _ => null,
    },
    letterSpacing: value.letterSpacing,
    wordSpacing: value.wordSpacing,
    height: value.height,
    fontFamily: value.fontFamily,
    decoration: _decoration(value.decoration),
    decorationColor: _themeOverrideColor(value.decorationColor, colorScheme),
    decorationStyle: switch (value.decorationStyle) {
      'solid' => TextDecorationStyle.solid,
      'double' => TextDecorationStyle.double,
      'dotted' => TextDecorationStyle.dotted,
      'dashed' => TextDecorationStyle.dashed,
      'wavy' => TextDecorationStyle.wavy,
      _ => null,
    },
    decorationThickness: value.decorationThickness,
  );
}

Color? _themeOverrideColor(
  CanvasThemeColorValue? value,
  ColorScheme colorScheme,
) => switch (value) {
  CanvasThemeLiteralColor(:final argb) => Color(argb),
  CanvasThemeRoleColor(:final role) => _colorSchemeRole(colorScheme, role),
  null => null,
};

Color _colorSchemeRole(ColorScheme scheme, String role) => switch (role) {
  'primary' => scheme.primary,
  'onPrimary' => scheme.onPrimary,
  'primaryContainer' => scheme.primaryContainer,
  'onPrimaryContainer' => scheme.onPrimaryContainer,
  'primaryFixed' => scheme.primaryFixed,
  'primaryFixedDim' => scheme.primaryFixedDim,
  'onPrimaryFixed' => scheme.onPrimaryFixed,
  'onPrimaryFixedVariant' => scheme.onPrimaryFixedVariant,
  'secondary' => scheme.secondary,
  'onSecondary' => scheme.onSecondary,
  'secondaryContainer' => scheme.secondaryContainer,
  'onSecondaryContainer' => scheme.onSecondaryContainer,
  'secondaryFixed' => scheme.secondaryFixed,
  'secondaryFixedDim' => scheme.secondaryFixedDim,
  'onSecondaryFixed' => scheme.onSecondaryFixed,
  'onSecondaryFixedVariant' => scheme.onSecondaryFixedVariant,
  'tertiary' => scheme.tertiary,
  'onTertiary' => scheme.onTertiary,
  'tertiaryContainer' => scheme.tertiaryContainer,
  'onTertiaryContainer' => scheme.onTertiaryContainer,
  'tertiaryFixed' => scheme.tertiaryFixed,
  'tertiaryFixedDim' => scheme.tertiaryFixedDim,
  'onTertiaryFixed' => scheme.onTertiaryFixed,
  'onTertiaryFixedVariant' => scheme.onTertiaryFixedVariant,
  'error' => scheme.error,
  'onError' => scheme.onError,
  'errorContainer' => scheme.errorContainer,
  'onErrorContainer' => scheme.onErrorContainer,
  'surface' => scheme.surface,
  'onSurface' => scheme.onSurface,
  'surfaceDim' => scheme.surfaceDim,
  'surfaceBright' => scheme.surfaceBright,
  'surfaceContainerLowest' => scheme.surfaceContainerLowest,
  'surfaceContainerLow' => scheme.surfaceContainerLow,
  'surfaceContainer' => scheme.surfaceContainer,
  'surfaceContainerHigh' => scheme.surfaceContainerHigh,
  'surfaceContainerHighest' => scheme.surfaceContainerHighest,
  'onSurfaceVariant' => scheme.onSurfaceVariant,
  'outline' => scheme.outline,
  'outlineVariant' => scheme.outlineVariant,
  'shadow' => scheme.shadow,
  'scrim' => scheme.scrim,
  'inverseSurface' => scheme.inverseSurface,
  'onInverseSurface' => scheme.onInverseSurface,
  'inversePrimary' => scheme.inversePrimary,
  'surfaceTint' => scheme.surfaceTint,
  _ => throw StateError('Unreviewed Canvas ColorScheme role: $role'),
};

FontWeight? _fontWeight(String? value) => switch (value) {
  'w100' => FontWeight.w100,
  'w200' => FontWeight.w200,
  'w300' => FontWeight.w300,
  'w400' => FontWeight.w400,
  'w500' => FontWeight.w500,
  'w600' => FontWeight.w600,
  'w700' => FontWeight.w700,
  'w800' => FontWeight.w800,
  'w900' => FontWeight.w900,
  _ => null,
};

TextDecoration? _decoration(Set<String>? value) {
  if (value == null) {
    return null;
  }
  if (value.isEmpty) {
    return TextDecoration.none;
  }
  final lines = <TextDecoration>[
    if (value.contains('underline')) TextDecoration.underline,
    if (value.contains('overline')) TextDecoration.overline,
    if (value.contains('lineThrough')) TextDecoration.lineThrough,
  ];
  return lines.length == 1 ? lines.single : TextDecoration.combine(lines);
}

/// Resolves Flutter adaptive widget semantics inside the native desktop engine.
///
/// Web has no [TargetPlatform] value. The Windows-hosted runner therefore uses
/// Windows adaptive controls while the exact Web preview mode supplies the
/// browser-sized responsive viewport. This is a layout preview, not an emulated
/// browser runtime, so browser-only behavior such as `kIsWeb` remains out of scope.
TargetPlatform canvasAdaptiveTargetPlatform(String platform) =>
    switch (platform) {
      'android' => TargetPlatform.android,
      'ios' => TargetPlatform.iOS,
      'macos' => TargetPlatform.macOS,
      'linux' => TargetPlatform.linux,
      'windows' => TargetPlatform.windows,
      'web' => TargetPlatform.windows,
      _ => throw ArgumentError.value(
        platform,
        'platform',
        'Unsupported target',
      ),
    };

String? _refreshIndicatorReferenceMessage(CanvasNode node) {
  final disabled = [
    for (final name in const ['onRefresh', 'notificationPredicate'])
      if (node.properties[name]?.kind == 'dartObjectReferencePresence') name,
  ];
  final skipped = node.properties.containsKey('onStatusChange');
  if (disabled.isEmpty && !skipped) return null;
  return 'RefreshIndicator.${[...disabled, if (skipped) 'onStatusChange'].join('/')} '
      'preview unavailable: isolated Canvas does not execute project or dependency Dart. '
      '${disabled.isEmpty ? 'The SDK refresh cycle remains available; the project status observer is not invoked.' : 'Refresh activation is disabled, not simulated as a successful callback or a different notification filter.'} '
      'The real SDK wrapper, child scrolling, stored properties and generated Dart are preserved.';
}

// Only the SDK's active Material branch consumes these properties. The actual
// wrapper size is inspected at notification time, after its child has laid out.
String? _refreshIndicatorGeometryMessage(
  CanvasNode node,
  BuildContext context,
  Size size,
  Color? color,
) {
  final variant = node.properties['variant']?.value;
  final platform = Theme.of(context).platform;
  if (variant == 'noSpinner') return null;
  double number(String name, double fallback) =>
      (node.properties[name]?.value as num?)?.toDouble() ?? fallback;
  final displacement = number('displacement', 40);
  final offset = number('edgeOffset', 0);
  final apple =
      variant == 'adaptive' &&
      (platform == TargetPlatform.iOS || platform == TargetPlatform.macOS);
  final overlayHeight = (displacement + (apple ? 20 : 49)) * 1.5;
  if (!size.isFinite ||
      !overlayHeight.isFinite ||
      !(offset + overlayHeight).isFinite) {
    return 'RefreshIndicator.displacement/edgeOffset preview unavailable: the active overlay exceeds finite SDK layout geometry. Child scrolling and stored values are unchanged.';
  }
  if (apple) {
    return null;
  }
  final semanticValue = node.properties['semanticsValue']?.value as String?;
  if (semanticValue != null) {
    final parsed =
        double.tryParse(semanticValue) ??
        (semanticValue.endsWith('%')
            ? double.tryParse(
                semanticValue.substring(0, semanticValue.length - 1),
              )
            : null);
    if (semanticValue.isEmpty || parsed == null || parsed < 0 || parsed > 100) {
      return 'RefreshIndicator.semanticsValue preview unavailable: the SDK drag-phase progressBar requires a number from 0 to 100 or a percentage from 0% to 100%. The explicit string and child are preserved.';
    }
  }
  // RefreshIndicator passes its color animation directly to RefreshProgressIndicator.
  // A transparent effective color suppresses arc/arrow painting, not its Material.
  // ignore: deprecated_member_use
  if ((color ?? Theme.of(context).colorScheme.primary).alpha == 0) return null;
  final paintWidth = math.max(
    0.0,
    math.min(41.0, math.max(0.0, size.width - 8)) - 24,
  );
  if (paintWidth != 17) {
    return 'RefreshIndicator.child preview unavailable for pull-to-refresh: the resolved width ${size.width} makes the SDK arrow paint area non-square. The child remains visible and scrollable; no width is invented.';
  }
  final stroke = number(
    'strokeWidth',
    RefreshProgressIndicator.defaultStrokeWidth,
  );
  final alignment = ProgressIndicatorTheme.of(context).strokeAlign ?? 0;
  final strokeOffset = stroke / 2 * -alignment;
  final arc = 17 - strokeOffset * 2;
  final arrow = stroke * 2;
  if (!strokeOffset.isFinite ||
      !(strokeOffset * 2).isFinite ||
      !arc.isFinite ||
      !(strokeOffset + arc).isFinite ||
      !arrow.isFinite ||
      !(8.5 + arrow).isFinite ||
      !(8.5 - arrow).isFinite) {
    return 'RefreshIndicator.strokeWidth preview unavailable: strokeWidth and the resolved theme strokeAlign overflow finite SDK arc/arrow geometry. The signed value and child are unchanged.';
  }
  return null;
}

class _CanvasRefreshIndicatorPreview extends StatefulWidget {
  const _CanvasRefreshIndicatorPreview({
    required this.node,
    required this.color,
    required this.backgroundColor,
    required this.child,
  });
  final CanvasNode node;
  final Color? color;
  final Color? backgroundColor;
  final Widget child;

  @override
  State<_CanvasRefreshIndicatorPreview> createState() =>
      _CanvasRefreshIndicatorPreviewState();
}

class _CanvasRefreshIndicatorPreviewState
    extends State<_CanvasRefreshIndicatorPreview> {
  GlobalKey<RefreshIndicatorState> _indicatorKey =
      GlobalKey<RefreshIndicatorState>();
  final Completer<void> _unavailableRefresh = Completer<void>();
  String? _geometryFailure;
  bool _notificationsEnabled = true;

  double _number(String name, double fallback) =>
      (widget.node.properties[name]?.value as num?)?.toDouble() ?? fallback;
  String? _string(String name) =>
      widget.node.properties[name]?.value as String?;
  bool get _projectRefresh =>
      widget.node.properties['onRefresh']?.kind ==
      'dartObjectReferencePresence';
  bool get _projectPredicate =>
      widget.node.properties['notificationPredicate']?.kind ==
      'dartObjectReferencePresence';

  Future<void> _onRefresh() => _projectRefresh
      // Never claim that the project callback completed. Product Canvas has no
      // programmatic show action; this also guards direct SDK show invocation.
      ? _unavailableRefresh.future
      : Future<void>.value();

  bool _predicate(ScrollNotification notification) {
    if (_projectRefresh || _projectPredicate) {
      return false;
    }
    final preset = _string('notificationPredicate');
    final accepted =
        preset == 'all' ||
        (preset == 'depthZero'
            ? notification.depth == 0
            : defaultScrollNotificationPredicate(notification));
    if (!accepted) return false;
    final render = _indicatorKey.currentContext?.findRenderObject();
    if (render is RenderBox && render.hasSize) {
      final failure = _refreshIndicatorGeometryMessage(
        widget.node,
        context,
        render.size,
        widget.color,
      );
      if (_geometryFailure != failure) {
        final observedNode = widget.node;
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted && identical(widget.node, observedNode)) {
            setState(() => _geometryFailure = failure);
          }
        });
      }
      if (failure != null) return false;
    }
    return true;
  }

  @override
  void didUpdateWidget(_CanvasRefreshIndicatorPreview oldWidget) {
    super.didUpdateWidget(oldWidget);
    _geometryFailure = null;
  }

  @override
  Widget build(BuildContext context) =>
      _RefreshLayoutObserver(builder: _buildIndicator);

  Widget _buildIndicator(BuildContext context, BoxConstraints constraints) {
    final render = _indicatorKey.currentContext?.findRenderObject();
    final failure = render is RenderBox && render.hasSize
        ? _refreshIndicatorGeometryMessage(
            widget.node,
            context,
            Size(
              constraints.hasTightWidth
                  ? constraints.maxWidth
                  : render.size.width,
              constraints.hasTightHeight
                  ? constraints.maxHeight
                  : render.size.height,
            ),
            widget.color,
          )
        : null;
    final enabled = !_projectRefresh && !_projectPredicate && failure == null;
    if (_notificationsEnabled && !enabled) {
      // When an edit makes a currently active spinner unsafe, dispose that SDK
      // cycle only. Ordinary safe property/history edits keep its exact State.
      _indicatorKey = GlobalKey<RefreshIndicatorState>();
    }
    _notificationsEnabled = enabled;
    final variant = _string('variant');
    final trigger =
        widget.node.properties['triggerMode']?.value as CanvasEnumValue?;
    final mode = trigger?.value == 'anywhere'
        ? RefreshIndicatorTriggerMode.anywhere
        : RefreshIndicatorTriggerMode.onEdge;
    final Widget indicator;
    if (variant == 'noSpinner') {
      indicator = RefreshIndicator.noSpinner(
        key: _indicatorKey,
        onRefresh: _onRefresh,
        // The project observer is intentionally absent, never executed.
        notificationPredicate: _predicate,
        semanticsLabel: _string('semanticsLabel'),
        semanticsValue: _string('semanticsValue'),
        triggerMode: mode,
        elevation: _number('elevation', 2),
        child: widget.child,
      );
    } else if (variant == 'adaptive') {
      indicator = RefreshIndicator.adaptive(
        key: _indicatorKey,
        displacement: _number('displacement', 40),
        edgeOffset: _number('edgeOffset', 0),
        onRefresh: _onRefresh,
        color: widget.color,
        backgroundColor: widget.backgroundColor,
        notificationPredicate: _predicate,
        semanticsLabel: _string('semanticsLabel'),
        semanticsValue: _string('semanticsValue'),
        strokeWidth: _number(
          'strokeWidth',
          RefreshProgressIndicator.defaultStrokeWidth,
        ),
        triggerMode: mode,
        elevation: _number('elevation', 2),
        child: widget.child,
      );
    } else {
      indicator = RefreshIndicator(
        key: _indicatorKey,
        displacement: _number('displacement', 40),
        edgeOffset: _number('edgeOffset', 0),
        onRefresh: _onRefresh,
        color: widget.color,
        backgroundColor: widget.backgroundColor,
        notificationPredicate: _predicate,
        semanticsLabel: _string('semanticsLabel'),
        semanticsValue: _string('semanticsValue'),
        strokeWidth: _number(
          'strokeWidth',
          RefreshProgressIndicator.defaultStrokeWidth,
        ),
        triggerMode: mode,
        elevation: _number('elevation', 2),
        child: widget.child,
      );
    }
    final message = [
      _refreshIndicatorReferenceMessage(widget.node),
      failure ?? _geometryFailure,
    ].whereType<String>().join(' ');
    // Keep the same metadata wrappers even when no diagnostic is needed: they
    // neither cover the child nor replace the real SDK RefreshIndicator State.
    return Tooltip(
      message: message,
      child: Semantics(
        tooltip: message.isEmpty ? null : message,
        child: indicator,
      ),
    );
  }
}

// Unlike LayoutBuilder, the SDK child exists before the first layout. Intrinsic
// and dry-layout queries therefore delegate to that real child without builds,
// state changes, guessed dimensions or a duplicate measurement subtree.
class _RefreshLayoutObserver extends RenderObjectWidget {
  const _RefreshLayoutObserver({required this.builder});
  final Widget Function(BuildContext, BoxConstraints) builder;

  @override
  RenderObjectElement createElement() => _RefreshLayoutObserverElement(this);
  @override
  _RefreshLayoutRenderBox createRenderObject(BuildContext context) =>
      _RefreshLayoutRenderBox();
}

class _RefreshLayoutObserverElement extends RenderObjectElement {
  _RefreshLayoutObserverElement(_RefreshLayoutObserver super.widget);
  Element? _child;
  BoxConstraints? _previousConstraints;
  Size? _previousSize;
  bool _needsBuild = false;
  bool _deferredCallbackScheduled = false;
  late final BuildScope _scope = BuildScope(scheduleRebuild: _scheduleRebuild);

  @override
  BuildScope get buildScope => _scope;

  void _scheduleRebuild() {
    if (_deferredCallbackScheduled) return;
    final phase = SchedulerBinding.instance.schedulerPhase;
    if (phase != SchedulerPhase.idle &&
        phase != SchedulerPhase.postFrameCallbacks) {
      renderObject.scheduleLayoutCallback();
      return;
    }
    _deferredCallbackScheduled = true;
    SchedulerBinding.instance.scheduleFrameCallback((_) {
      _deferredCallbackScheduled = false;
      if (mounted) renderObject.scheduleLayoutCallback();
    });
  }

  @override
  _RefreshLayoutRenderBox get renderObject =>
      super.renderObject as _RefreshLayoutRenderBox;

  void _buildChild(BoxConstraints constraints) {
    _child = updateChild(
      _child,
      (widget as _RefreshLayoutObserver).builder(this, constraints),
      null,
    );
    _needsBuild = false;
  }

  @override
  void mount(Element? parent, Object? newSlot) {
    super.mount(parent, newSlot);
    renderObject.onConstraints = _layout;
    // Eager creation is essential: IntrinsicWidth can query this render object
    // before performLayout has ever been called.
    _buildChild(const BoxConstraints());
  }

  @override
  void update(covariant _RefreshLayoutObserver newWidget) {
    super.update(newWidget);
    // Apply ordinary source/history edits during the normal build phase so a
    // following intrinsic query observes the new child, not stale dimensions.
    _buildChild(_previousConstraints ?? const BoxConstraints());
    renderObject.scheduleLayoutCallback();
  }

  @override
  void markNeedsBuild() {
    _needsBuild = true;
    renderObject.scheduleLayoutCallback();
  }

  @override
  void performRebuild() {
    super.performRebuild();
    _needsBuild = true;
    renderObject.scheduleLayoutCallback();
  }

  bool _layout(BoxConstraints constraints) {
    final measuredSize = renderObject.measuredSize;
    final rebuild =
        _needsBuild ||
        constraints != _previousConstraints ||
        measuredSize != _previousSize;
    _previousConstraints = constraints;
    _previousSize = measuredSize;
    // The SDK's layout-callback mixin permits mutations only in this subtree;
    // buildScope performs the child update before layout/paint, never setState
    // on an ancestor or a post-frame stale-size repair.
    // Always flush this private scope, including SDK ticker/scroll dirty nodes
    // whose geometry did not change. Never flush an ancestor/sibling scope.
    owner!.buildScope(this, rebuild ? () => _buildChild(constraints) : null);
    return rebuild;
  }

  @override
  void visitChildren(ElementVisitor visitor) {
    if (_child != null) visitor(_child!);
  }

  @override
  void forgetChild(Element child) {
    assert(child == _child);
    _child = null;
    super.forgetChild(child);
  }

  @override
  void insertRenderObjectChild(RenderObject child, Object? slot) {
    renderObject.child = child as RenderBox;
  }

  @override
  void moveRenderObjectChild(
    RenderObject child,
    Object? oldSlot,
    Object? newSlot,
  ) {
    assert(false, 'Refresh layout observer has a single child');
  }

  @override
  void removeRenderObjectChild(RenderObject child, Object? slot) {
    renderObject.child = null;
  }

  @override
  void unmount() {
    renderObject.onConstraints = null;
    super.unmount();
  }
}

class _RefreshLayoutRenderBox extends RenderProxyBox
    with RenderObjectWithLayoutCallbackMixin {
  bool Function(BoxConstraints)? onConstraints;
  Size? measuredSize;
  bool _rebuilt = false;
  @override
  void layoutCallback() => _rebuilt = onConstraints?.call(constraints) ?? false;
  @override
  void performLayout() {
    runLayoutCallback();
    super.performLayout();
    // Loose constraints can stay identical while an intrinsic/fixed-size child
    // changes the actual Stack width. Inspect that measured size before paint,
    // then finish layout once more only if disposing an unsafe cycle rebuilt it.
    measuredSize = size;
    runLayoutCallback();
    if (_rebuilt) super.performLayout();
  }
}

class _PersistentHeaderPreviewDelegate extends SliverPersistentHeaderDelegate {
  const _PersistentHeaderPreviewDelegate();
  @override
  double get minExtent => 56;
  @override
  double get maxExtent => 112;
  @override
  Widget build(BuildContext context, double shrinkOffset, bool overlapsContent) =>
      const SizedBox.expand();
  @override
  bool shouldRebuild(covariant _PersistentHeaderPreviewDelegate oldDelegate) => false;
}

// A preview-only controller is owned/disposed here, never borrowed from project code.
class _BottomSheetPreview extends StatefulWidget {
  const _BottomSheetPreview({required this.child, required this.enableDrag, this.showDragHandle, this.sheetKey,
    this.dragHandleColor,this.dragHandleSize,this.backgroundColor,this.shadowColor,this.elevation,
    this.shape,this.clipBehavior,this.constraints});
  final Widget child;
  final bool enableDrag;
  final bool? showDragHandle;
  final Color? dragHandleColor,backgroundColor,shadowColor;
  final Size? dragHandleSize;
  final double? elevation;
  final ShapeBorder? shape;
  final Clip? clipBehavior;
  final BoxConstraints? constraints;
  @override
  State<_BottomSheetPreview> createState() => _BottomSheetPreviewState();
  final Key? sheetKey;
}
class _BottomSheetPreviewState extends State<_BottomSheetPreview> with SingleTickerProviderStateMixin {
  late final AnimationController _controller = BottomSheet.createAnimationController(this)..value = 1;
  @override
  void dispose() { _controller.dispose(); super.dispose(); }
  @override
  Widget build(BuildContext context) => BottomSheet(
    key:widget.sheetKey, animationController:_controller, enableDrag:widget.enableDrag, showDragHandle:widget.showDragHandle,
    dragHandleColor:widget.dragHandleColor,dragHandleSize:widget.dragHandleSize,
    backgroundColor:widget.backgroundColor,shadowColor:widget.shadowColor,elevation:widget.elevation,
    shape:widget.shape,clipBehavior:widget.clipBehavior,constraints:widget.constraints,
    onClosing:() { _controller.value = 1; },
    onDragEnd:(_, {required isClosing}) { _controller.value = 1; },
    builder:(_) => widget.child);
}

String _displayType(String type) => type.substring(type.lastIndexOf('.') + 1);
