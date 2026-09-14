import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'canvas_sliver_fill_remaining_test.dart' as f;
import 'canvas_animated_builder_test.dart' as a;
import 'canvas_animated_container_test.dart' as c;

const type = 'flutter.widgets.DecoratedBoxTransition';
Map<String, Object?> data({
  Map<String, Object?>? decoration,
  String? position,
  bool rtl = false,
  bool tight = false,
}) {
  final raw = c.data(rtl: rtl, tight: tight), node = a.builder(raw);
  node['type'] = type;
  node['properties'] = <String, Object?>{
    'decoration': decoration ?? c.decoration(0xff112233),
    if (position != null)
      'position': {
        'kind': 'enum',
        'type': 'DecorationPosition',
        'value': position,
      },
  };
  return raw;
}

Finder native() => find.byType(DecoratedBoxTransition);
Finder child() => find.byKey(const ValueKey('canvas-widget-${f.bodyId}'));
BoxDecoration value(WidgetTester tester) =>
    tester.widget<DecoratedBoxTransition>(native()).decoration.value
        as BoxDecoration;
void main() {
  test(
    'required child and decoration, exact domains and no invented parameters',
    () {
      final raw = data(), props = a.builder(raw)['properties'] as Map;
      final original = props.remove('decoration');
      expect(() => f.decode(raw), throwsFormatException);
      for (final bad in [
        {'kind': 'null'},
        {'kind': 'string', 'value': 'arbitraryDart()'},
        {'kind': 'color', 'argb': '0xFF112233'},
      ]) {
        props['decoration'] = bad;
        expect(() => f.decode(raw), throwsFormatException);
      }
      props['decoration'] = original;
      for (final field in [
        'durationUs',
        'curve',
        'onEnd',
        'padding',
        'alignment',
        'clipBehavior',
      ]) {
        props[field] = {'kind': 'boolean', 'value': true};
        expect(() => f.decode(raw), throwsFormatException);
        props.remove(field);
      }
      for (final bad in ['middle', null]) {
        props['position'] = bad == null
            ? {'kind': 'null'}
            : {'kind': 'enum', 'type': 'DecorationPosition', 'value': bad};
        expect(() => f.decode(raw), throwsFormatException);
      }
      props.remove('position');
      a.builder(raw)['slots'] = {'child': f.single(null)};
      expect(() => f.decode(raw), throwsFormatException);
    },
  );
  test('root-compatible required wrapper and all box/sliver destinations', () {
    final raw = data();
    raw['root'] = a.builder(raw);
    expect(() => f.decode(raw), returnsNormally);
    expect(canvasDropSlotsForWidgetType(type), isEmpty);
    final slot = canvasExistingChildWrapTargetSlot(
      parentWidgetType: type,
      slotName: 'child',
    )!;
    expect(slot.slotName, 'child');
    expect(
      canvasWrapperAcceptsExistingChild(
        wrapperWidgetType: type,
        childWidgetType: 'flutter.widgets.Text',
      ),
      true,
    );
    for (final invalid in [
      'flutter.widgets.Expanded',
      'flutter.widgets.PositionedTransition',
      'flutter.widgets.SliverToBoxAdapter',
    ]) {
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: type,
          childWidgetType: invalid,
        ),
        false,
      );
    }
    // Parent data and sliver restrictions are also exhaustively tested by Java parity.
    for (final invalid in [
      'flutter.widgets.Expanded',
      'flutter.widgets.PositionedTransition',
      'flutter.widgets.SliverToBoxAdapter',
    ]) {
      final child = f.findNode(raw['root'] as Map<String, Object?>, f.bodyId);
      child['type'] = invalid;
      child['properties'] = <String, Object?>{};
      expect(() => f.decode(raw), throwsFormatException);
    }
  });
  for (final position in [null, 'background', 'foreground']) {
    for (final rtl in [false, true]) {
      for (final tight in [false, true]) {
        testWidgets(
          'local decoration position=$position rtl=$rtl tight=$tight',
          (tester) async {
            final decoration = _viewBoxDecoration(
              color: _viewLiteralColor('0xFF112233'),
              border: rtl ? _viewDirectionalBorder() : _viewPhysicalBorder(),
              borderRadius: rtl
                  ? _viewDirectionalRadius()
                  : _viewPhysicalRadius(),
              boxShadow: [_viewBoxShadow()],
              gradient: _viewLinearGradient(),
              backgroundBlendMode: 'srcOver',
            );
            await f.pump(
              tester,
              data(
                decoration: decoration,
                position: position,
                rtl: rtl,
                tight: tight,
              ),
            );
            final n = tester.widget<DecoratedBoxTransition>(native()),
                d = value(tester);
            expect(n.decoration, isA<AlwaysStoppedAnimation<Decoration>>());
            expect(
              n.position,
              position == 'foreground'
                  ? DecorationPosition.foreground
                  : DecorationPosition.background,
            );
            expect(d.color, const Color(0xff112233));
            expect(d.border, rtl ? isA<BorderDirectional>() : isA<Border>());
            expect(
              d.borderRadius,
              rtl ? isA<BorderRadiusDirectional>() : isA<BorderRadius>(),
            );
            expect(d.boxShadow!.single.blurStyle, BlurStyle.outer);
            expect(d.gradient, isA<LinearGradient>());
            expect(d.backgroundBlendMode, BlendMode.srcOver);
            final box = tester.renderObject<RenderBox>(native()),
                body = tester.renderObject<RenderBox>(child());
            expect(box.size, body.size);
            expect(
              box.localToGlobal(Offset.zero),
              body.localToGlobal(Offset.zero),
            );
            expect(box.size, tight ? const Size(240, 160) : const Size(48, 48));
            expect(tester.takeException(), isNull);
          },
        );
      }
    }
  }
  for (final gradient in [
    _viewLinearGradient,
    _viewRadialGradient,
    _viewSweepGradient,
  ]) {
    testWidgets('every native gradient ${gradient().values.first}', (
      tester,
    ) async {
      await f.pump(
        tester,
        data(
          decoration: _viewBoxDecoration(gradient: gradient(), shape: 'circle'),
        ),
      );
      final d = value(tester);
      expect(d.shape, BoxShape.circle);
      expect(d.gradient!.colors.length, 2);
      expect(
        d.gradient!.colors.last,
        Theme.of(tester.element(native())).colorScheme.primary,
      );
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets(
    'source fallback, local replacement, position changes and child state are stable',
    (tester) async {
      await f.pump(tester, data());
      final state = tester.state(native()), element = tester.element(child());
      for (final decoration in [
        <String, Object?>{'kind': 'dartObjectReferencePresence'},
        c.decoration(0xff445566),
      ]) {
        await f.pump(
          tester,
          data(decoration: decoration, position: 'foreground'),
        );
        final source = decoration['kind'] == 'dartObjectReferencePresence';
        expect(value(tester).color, source ? null : const Color(0xff445566));
        expect(
          find.byWidgetPredicate(
            (w) =>
                w is Tooltip &&
                w.message?.contains('Animation<Decoration>') == true,
          ),
          source ? findsWidgets : findsNothing,
        );
        expect(identical(state, tester.state(native())), true);
        expect(identical(element, tester.element(child())), true);
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets('zero-size required child stays present and selectable', (
    tester,
  ) async {
    final raw = data(),
        body = f.findNode(raw['root'] as Map<String, Object?>, f.bodyId);
    body['properties'] = {'width': f.number(0), 'height': f.number(0)};
    body['slots'] = {'child': f.single(null)};
    await f.pump(tester, raw);
    expect(native(), findsOneWidget);
    expect(child(), findsOneWidget);
    expect(tester.getSize(native()), Size.zero);
    expect(tester.takeException(), isNull);
  });
  testWidgets(
    'unavailable declared image uses safe placeholder and retains complete image settings',
    (tester) async {
      final image = _viewDecorationImage(
        image: _viewImageProvider(
          resolution: {
            'kind': 'unavailable',
            'code': 'missing',
            'reason': 'Asset not resolved',
          },
        ),
        onError: true,
        fit: 'contain',
        alignment: _viewNestedAlignment(basis: 'directional', horizontal: 1),
        repeat: 'repeatX',
        matchTextDirection: true,
        scale: 2,
        opacity: .5,
        filterQuality: 'high',
        invertColors: true,
        isAntiAlias: true,
      );
      await f.pump(tester, data(decoration: _viewBoxDecoration(image: image)));
      final d = value(tester);
      expect(d.image, isNotNull);
      expect(d.image!.fit, BoxFit.contain);
      expect(d.image!.repeat, ImageRepeat.repeatX);
      expect(d.image!.matchTextDirection, true);
      expect(d.image!.scale, 2);
      expect(d.image!.opacity, .5);
      expect(d.image!.filterQuality, FilterQuality.high);
      expect(d.image!.invertColors, true);
      expect(d.image!.isAntiAlias, true);
      expect(d.image!.alignment, AlignmentDirectional.centerEnd);
      expect(tester.takeException(), isNull);
    },
  );
}

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
