import 'dart:convert';
import 'dart:ui' as ui;

import 'package:flutter/cupertino.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_runtime.dart';
import 'package:netbeans_flutter_canvas_runner/src/sha256.dart';
import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.material.Switch';
const _id = 'e166c0e6-a533-41f3-acf8-f868f2659c13';
const _boundaryId = '658c0969-15b7-47e1-87b3-3e721cf203f1';
const _reference = {'kind': 'dartObjectReferencePresence'};
const _states = <WidgetState, String>{
  WidgetState.disabled: 'Disabled',
  WidgetState.error: 'Error',
  WidgetState.dragged: 'Dragged',
  WidgetState.pressed: 'Pressed',
  WidgetState.selected: 'Selected',
  WidgetState.scrolledUnder: 'ScrolledUnder',
  WidgetState.hovered: 'Hovered',
  WidgetState.focused: 'Focused',
};
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _number(num value) => {
  'kind': value is int ? 'integer' : 'double',
  'value': value,
};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _color(int value) => {
  'kind': 'color',
  'argb': '0x${value.toRadixString(16).padLeft(8, '0').toUpperCase()}',
};
Map<String, Object?> _enum(String type, String value) => {
  'kind': 'enum',
  'type': type,
  'value': value,
};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _node(
  String id,
  String type,
  Map<String, Object?> properties, [
  Map<String, Object?> slots = const {},
]) => {'id': id, 'type': type, 'properties': properties, 'slots': slots};
Map<String, Object?> _model({
  String variant = 'standard',
  String platform = 'windows',
  Map<String, Object?> properties = const {},
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  model['root'] = _node(
    '8798daa7-a409-45d6-9ee5-f8bc0686ed26',
    'flutter.material.Scaffold',
    {},
    {
      'body': _single(
        _node(
          'ef4c8288-b304-4e4c-89e2-d1ff8d31dcaa',
          'flutter.widgets.Center',
          {},
          {
            'child': _single(
              _node(_boundaryId, 'flutter.widgets.RepaintBoundary', {}, {
                'child': _single(
                  _node(_id, _type, {
                    'value': _bool(false),
                    'enabled': _bool(true),
                    'variant': _string(variant),
                    ...properties,
                  }),
                ),
              }),
            ),
          },
        ),
      ),
    },
  );
  return model;
}

Map _control(Map model) =>
    model['root']['slots']['body']['child']['slots']['child']['child']['slots']['child']['child']
        as Map;
CanvasModel _decode(Map model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Switch _sdk(WidgetTester tester) => tester.widget<Switch>(find.byType(Switch));
String _diagnostics(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((v) => v.message ?? '')
    .join('\n');
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  List<String>? selected,
  String? selectedId,
  CanvasImageResourceBundle? resources,
  CanvasImageErrorReporter? onImageError,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme ?? ThemeData(),
      themeAnimationDuration: Duration.zero,
      home: CanvasDocumentView(
        model: _decode(model),
        imageResources: resources,
        onImageError: onImageError,
        selectedWidgetId: selectedId,
        onSelected: selected?.add ?? (_) {},
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 300));
  await tester.pump();
}

Future<Uint8List> _pixels(
  WidgetTester tester,
  RenderRepaintBoundary boundary,
) async => (await tester.runAsync(() async {
  final image = await boundary.toImage();
  try {
    final data = await image.toByteData(format: ui.ImageByteFormat.rawRgba);
    return Uint8List.fromList(data!.buffer.asUint8List());
  } finally {
    image.dispose();
  }
}))!;

Switch _raw({
  String variant = 'standard',
  bool value = false,
  bool enabled = true,
  Color? activeColor,
  Color? activeThumbColor,
  Color? activeTrackColor,
  Color? inactiveThumbColor,
  Color? inactiveTrackColor,
  Color? focusColor,
  Color? hoverColor,
  WidgetStateProperty<Color?>? thumbColor,
  WidgetStateProperty<Color?>? trackColor,
  WidgetStateProperty<Color?>? trackOutlineColor,
  WidgetStateProperty<Color?>? overlayColor,
  WidgetStateProperty<double?>? trackOutlineWidth,
  WidgetStateProperty<Icon?>? thumbIcon,
  EdgeInsetsGeometry? padding,
  double? splashRadius,
  MaterialTapTargetSize? tapTarget,
  bool? applyCupertinoTheme,
  bool autofocus = false,
  FocusNode? focusNode,
  ValueChanged<bool>? onChanged,
  ValueChanged<bool>? onFocusChange,
  ImageProvider? activeThumbImage,
  ImageProvider? inactiveThumbImage,
}) => variant == 'adaptive'
    ? Switch.adaptive(
        value: value,
        onChanged: enabled ? (onChanged ?? (_) {}) : null,
        // ignore: deprecated_member_use
        activeColor: activeColor,
        activeThumbColor: activeThumbColor,
        activeTrackColor: activeTrackColor,
        inactiveThumbColor: inactiveThumbColor,
        inactiveTrackColor: inactiveTrackColor,
        thumbColor: thumbColor,
        trackColor: trackColor,
        trackOutlineColor: trackOutlineColor,
        trackOutlineWidth: trackOutlineWidth,
        thumbIcon: thumbIcon,
        overlayColor: overlayColor,
        padding: padding,
        splashRadius: splashRadius,
        materialTapTargetSize: tapTarget,
        autofocus: autofocus,
        focusNode: focusNode,
        onFocusChange: onFocusChange,
        focusColor: focusColor,
        hoverColor: hoverColor,
        activeThumbImage: activeThumbImage,
        inactiveThumbImage: inactiveThumbImage,
        applyCupertinoTheme: applyCupertinoTheme,
      )
    : Switch(
        value: value,
        onChanged: enabled ? (onChanged ?? (_) {}) : null,
        // ignore: deprecated_member_use
        activeColor: activeColor,
        activeThumbColor: activeThumbColor,
        activeTrackColor: activeTrackColor,
        inactiveThumbColor: inactiveThumbColor,
        inactiveTrackColor: inactiveTrackColor,
        thumbColor: thumbColor,
        trackColor: trackColor,
        trackOutlineColor: trackOutlineColor,
        trackOutlineWidth: trackOutlineWidth,
        thumbIcon: thumbIcon,
        overlayColor: overlayColor,
        padding: padding,
        splashRadius: splashRadius,
        materialTapTargetSize: tapTarget,
        autofocus: autofocus,
        focusNode: focusNode,
        onFocusChange: onFocusChange,
        focusColor: focusColor,
        hoverColor: hoverColor,
        activeThumbImage: activeThumbImage,
        inactiveThumbImage: inactiveThumbImage,
      );

Future<void> _expectRawPixels(
  WidgetTester tester,
  Switch raw,
  ThemeData theme,
) async {
  tester
          .renderObject<RenderCustomPaint>(
            find.byKey(const ValueKey('canvas-widget-outline-$_id')),
          )
          .foregroundPainter =
      null;
  await tester.pump();
  final pixels = await _pixels(
    tester,
    tester.renderObject<RenderRepaintBoundary>(
      find
          .descendant(
            of: _widget(_boundaryId),
            matching: find.byType(RepaintBoundary),
          )
          .first,
    ),
  );
  final size = tester.getSize(find.byType(Switch));
  final direction = Directionality.of(tester.element(find.byType(Switch)));
  final key = GlobalKey();
  await tester.pumpWidget(
    MaterialApp(
      theme: theme,
      home: Scaffold(
        body: Center(
          child: RepaintBoundary(
            key: key,
            child: Directionality(textDirection: direction, child: raw),
          ),
        ),
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 300));
  expect(tester.getSize(find.byType(Switch)), size);
  expect(
    pixels,
    orderedEquals(
      await _pixels(
        tester,
        key.currentContext!.findRenderObject()! as RenderRepaintBoundary,
      ),
    ),
  );
  expect(tester.takeException(), isNull);
}

const _null = {'kind': 'null'};
const _infinity = {'kind': 'enum', 'type': 'double', 'value': 'infinity'};
const _icon = {
  'kind': 'iconData',
  'codePoint': 58873,
  'fontFamily': 'MaterialIcons',
  'fontPackage': null,
  'matchTextDirection': false,
  'fontFamilyFallback': <String>[],
};
Map<String, Object?> _insets(
  double a,
  double b,
  double c,
  double d, {
  bool directional = false,
}) => {
  'kind': directional ? 'edgeInsetsDirectional' : 'edgeInsets',
  if (directional) 'start': a else 'left': a,
  'top': b,
  if (directional) 'end': c else 'right': c,
  'bottom': d,
};
final _bytes = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAA'
  'AARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/'
  'j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=',
);

Map<String, Object?> _provider({
  Uint8List? bytes,
  bool exact = false,
  bool resize = false,
  bool missing = false,
}) => {
  'kind': 'imageProvider',
  'value': {
    'kind': exact ? 'exactAsset' : 'asset',
    'assetName': 'assets/switch.png',
    'packageName': exact ? 'controls' : null,
    'exactScale': exact ? 2 : null,
    'resize': resize
        ? {'width': 4, 'height': null, 'policy': 'fit', 'allowUpscaling': true}
        : null,
    'resolution': missing
        ? {
            'kind': 'unavailable',
            'code': 'missing',
            'reason': 'Switch image not declared',
          }
        : {
            'kind': 'resolved',
            'resourceId': sha256Hex(bytes ?? _bytes),
            'resolvedScale': 2,
          },
  },
};
CanvasImageResourceBundle _resources([Uint8List? other]) =>
    CanvasImageResourceBundle.fromResources([
      for (final bytes in [_bytes, ?other])
        CanvasImageResource(
          resourceId: sha256Hex(bytes),
          mediaType: 'image/png',
          pixelWidth: 8,
          pixelHeight: 8,
          encodedBytes: bytes,
        ),
    ]);

class _SwitchAdaptation extends Adaptation<SwitchThemeData> {
  const _SwitchAdaptation(this.theme);
  final SwitchThemeData theme;
  @override
  SwitchThemeData adapt(ThemeData theme, SwitchThemeData defaultValue) =>
      this.theme;
}

void main() {
  testWidgets(
    'Switch thumb glyph fallback uses real SDK IconTheme variable axes and shadows without adopting color or size',
    (tester) async {
      for (final material3 in [false, true]) {
        for (final apple in [false, true]) {
          final theme = ThemeData(
            useMaterial3: material3,
            platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
            iconTheme: const IconThemeData(
              size: 80,
              color: Colors.red,
              fill: .8,
              weight: 650,
              grade: -40,
              opticalSize: 32,
              shadows: [
                Shadow(
                  color: Colors.blue,
                  blurRadius: 2,
                  offset: Offset(1, -1),
                ),
              ],
            ),
          );
          await _pump(
            tester,
            _model(
              variant: 'adaptive',
              properties: {'thumbIconDefaultData': _icon},
            ),
            theme: theme,
          );
          expect(_sdk(tester).thumbIcon!.resolve({})!.size, isNull);
          expect(_sdk(tester).thumbIcon!.resolve({})!.color, isNull);
          await _expectRawPixels(
            tester,
            _raw(
              variant: 'adaptive',
              thumbIcon: const WidgetStatePropertyAll(Icon(Icons.star)),
            ),
            theme,
          );
        }
      }
    },
  );
  testWidgets(
    'Switch overflowed padding only rejects an actually unbounded parent and keeps model editable',
    (tester) async {
      final errors = <FlutterErrorDetails>[];
      final previous = FlutterError.onError;
      FlutterError.onError = errors.add;
      try {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: Row(
                children: [
                  Switch(
                    value: false,
                    onChanged: (_) {},
                    padding: const EdgeInsets.symmetric(horizontal: 1.7e308),
                  ),
                ],
              ),
            ),
          ),
        );
        await tester.pumpWidget(const SizedBox());
      } finally {
        FlutterError.onError = previous;
      }
      expect(
        errors.any((error) => error.exceptionAsString().contains('infinite')),
        true,
        reason: errors.map((error) => error.exceptionAsString()).join('\n'),
      );
      final model = _model(
        properties: {'padding': _insets(1.7e308, 0, 1.7e308, 0)},
      );
      final control = _control(model);
      (model['root'] as Map)['slots']['body'] = _single(
        _node(
          'ce2cce42-9cf0-4bf0-90a3-919e73db4ae2',
          'flutter.widgets.Row',
          {},
          {
            'children': {
              'kind': 'list',
              'children': [control],
            },
          },
        ),
      );
      final before = jsonEncode(model);
      final selected = <String>[];
      await _pump(tester, model, selected: selected);
      expect(find.byType(Switch), findsNothing);
      expect(
        _diagnostics(tester),
        contains('padding sum overflows to infinity on an unbounded width'),
      );
      expect(_widget(_id), findsOneWidget);
      final handle = find.byKey(
        const ValueKey('canvas-zero-size-widget-target-$_id'),
      );
      expect(handle, findsOneWidget);
      await tester.tap(handle);
      await tester.pump();
      expect(selected, contains(_id));
      expect(jsonEncode(model), before);
      expect(tester.takeException(), isNull);
      // The same model property is legal in a bounded Center: no arbitrary cap.
      await _pump(
        tester,
        _model(properties: {'padding': _insets(1.7e308, 0, 1.7e308, 0)}),
      );
      expect(find.byType(Switch), findsOneWidget);
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'Switch layout observer preserves intrinsic APIs and TickerMode across model changes',
    (tester) async {
      Map<String, Object?> intrinsic(bool enabled, bool value) {
        final model = _model(properties: {'value': _bool(value)});
        final boundary =
            (model['root']
                    as Map)['slots']['body']['child']['slots']['child']['child']
                as Map;
        final control = boundary['slots']['child']['child'];
        boundary['slots']['child'] = _single(
          _node(
            '247409d5-95b2-4c0c-bdc4-0d86f1bc7c18',
            'flutter.widgets.IntrinsicWidth',
            {},
            {
              'child': _single(
                _node(
                  'dbef33b5-c33d-409a-b0b8-257e1bd7e6ca',
                  'flutter.widgets.IntrinsicHeight',
                  {},
                  {
                    'child': _single(
                      _node(
                        'bdf4b463-fc6f-4e2b-be42-3d128871ca8d',
                        'flutter.widgets.TickerMode',
                        {'enabled': _bool(enabled)},
                        {'child': _single(control)},
                      ),
                    ),
                  },
                ),
              ),
            },
          ),
        );
        return model;
      }

      FocusNode? focus;
      for (final enabled in [true, false, true]) {
        await _pump(tester, intrinsic(enabled, !enabled));
        focus ??= _sdk(tester).focusNode;
        expect(_sdk(tester).focusNode, same(focus));
        expect(
          TickerMode.valuesOf(tester.element(find.byType(Switch))).enabled,
          enabled,
        );
        final box = tester.renderObject<RenderBox>(find.byType(Switch));
        expect(box.getMinIntrinsicWidth(50).isFinite, true);
        expect(box.getMaxIntrinsicHeight(100).isFinite, true);
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets(
    'Raw Switch extreme finite thumb icon size and bounded padding retain valid layout',
    (tester) async {
      for (final size in [0.0, 1.7e308]) {
        await tester.pumpWidget(
          MaterialApp(
            home: Scaffold(
              body: Center(
                child: Switch(
                  value: false,
                  onChanged: (_) {},
                  thumbIcon: WidgetStatePropertyAll(
                    Icon(Icons.star, size: size),
                  ),
                  padding: const EdgeInsets.symmetric(horizontal: 1.7e308),
                ),
              ),
            ),
          ),
        );
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets(
    'Switch infinite width state pairs preserve exact safe endpoints and explicitly approximate unsafe pairs',
    (tester) async {
      for (final material3 in [false, true]) {
        for (final apple in [false, true]) {
          final theme = ThemeData(
            useMaterial3: material3,
            platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
          );
          for (final enabled in [false, true]) {
            for (final mixed in [false, true]) {
              final model = _model(
                variant: 'adaptive',
                properties: {
                  'enabled': _bool(enabled),
                  'trackOutlineWidthDefault': _infinity,
                  if (mixed) 'trackOutlineWidthSelected': _number(2),
                  'trackOutlineColorDefault': _color(0xff123456),
                },
              );
              final original = jsonEncode(model);
              await _pump(tester, model, theme: theme);
              expect(
                _sdk(tester).trackOutlineWidth!.resolve({}),
                mixed ? 2 : double.infinity,
              );
              expect(
                _sdk(tester).trackOutlineWidth!.resolve({WidgetState.selected}),
                mixed ? 2 : double.infinity,
              );
              expect(
                _diagnostics(
                  tester,
                ).contains('SDK painter-default width 2 approximation'),
                mixed,
              );
              expect(jsonEncode(model), original);
              await _expectRawPixels(
                tester,
                _raw(
                  variant: 'adaptive',
                  enabled: enabled,
                  trackOutlineWidth: WidgetStatePropertyAll(
                    mixed ? 2 : double.infinity,
                  ),
                  trackOutlineColor: const WidgetStatePropertyAll(
                    Color(0xff123456),
                  ),
                ),
                theme,
              );
            }
          }
          // A disabled override outranks a selected infinity and makes both
          // currently reachable disabled endpoints finite.
          await _pump(
            tester,
            _model(
              variant: 'adaptive',
              properties: {
                'enabled': _bool(false),
                'trackOutlineWidthDefault': _number(2),
                'trackOutlineWidthSelected': _infinity,
                'trackOutlineWidthDisabled': _number(4),
              },
            ),
            theme: theme,
          );
          expect(
            _diagnostics(tester),
            isNot(contains('width 2 approximation')),
          );
          expect(
            _sdk(tester).trackOutlineWidth!.resolve({
              WidgetState.disabled,
              WidgetState.selected,
            }),
            4,
          );
        }
      }
    },
  );
  testWidgets(
    'Switch explicit null width fallback observes adapted theme and diagnoses only unequal resolved endpoints',
    (tester) async {
      for (final apple in [false, true]) {
        for (final adapted in [false, true]) {
          final stateTheme = SwitchThemeData(
            trackOutlineWidth: WidgetStateProperty.resolveWith(
              (states) =>
                  states.contains(WidgetState.selected) ? double.infinity : 3,
            ),
          );
          final theme = ThemeData(
            platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
            switchTheme: stateTheme,
            adaptations: [if (adapted) _SwitchAdaptation(stateTheme)],
          );
          await _pump(
            tester,
            _model(
              variant: 'adaptive',
              properties: {
                'trackOutlineWidthDefault': _null,
                'trackOutlineWidthSelected': _null,
              },
            ),
            theme: theme,
          );
          final guarded = !apple || adapted;
          expect(
            _diagnostics(tester).contains('width 2 approximation'),
            guarded,
          );
          if (guarded) {
            expect(
              _sdk(tester).trackOutlineWidth!.resolve({WidgetState.selected}),
              2,
            );
          }
          expect(tester.takeException(), isNull);
        }
      }
      await _pump(
        tester,
        _model(properties: {'trackOutlineWidthDefault': _null}),
        theme: ThemeData(
          switchTheme: const SwitchThemeData(
            trackOutlineWidth: WidgetStatePropertyAll(double.infinity),
          ),
        ),
      );
      expect(_diagnostics(tester), isNot(contains('width 2 approximation')));
      expect(
        _sdk(tester).trackOutlineWidth!.resolve({}),
        isNull,
        reason:
            'null is passed through so actual SDK inherits safe equal theme infinity',
      );
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'Switch enabled Canvas drag selects existing widget without modifying controlled value',
    (tester) async {
      for (final profile in ['windows', 'web']) {
        final selected = <String>[];
        final model = _model(platform: profile);
        final before = jsonEncode(model);
        await _pump(tester, model, selected: selected);
        await tester.drag(find.byType(Switch), const Offset(70, 0));
        await tester.pump(const Duration(milliseconds: 350));
        expect(selected, contains(_id));
        expect(_sdk(tester).value, false);
        expect(jsonEncode(model), before);
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets(
    'Raw retained Switch leaks Cupertino paint while Canvas remounts only that SDK boundary and retains focus',
    (tester) async {
      final boundary = GlobalKey();
      Future<void> raw(TargetPlatform platform, {bool fresh = false}) async {
        await tester.pumpWidget(
          MaterialApp(
            themeAnimationDuration: Duration.zero,
            theme: ThemeData(platform: platform),
            home: Scaffold(
              body: Center(
                child: RepaintBoundary(
                  key: boundary,
                  child: Switch.adaptive(
                    key: fresh ? const ValueKey('fresh') : null,
                    value: false,
                    onChanged: (_) {},
                  ),
                ),
              ),
            ),
          ),
        );
        await tester.pump(const Duration(milliseconds: 350));
      }

      await raw(TargetPlatform.iOS);
      await raw(TargetPlatform.windows);
      final leaked = await _pixels(
        tester,
        boundary.currentContext!.findRenderObject()! as RenderRepaintBoundary,
      );
      await raw(TargetPlatform.windows, fresh: true);
      final correct = await _pixels(
        tester,
        boundary.currentContext!.findRenderObject()! as RenderRepaintBoundary,
      );
      expect(
        listEquals(leaked, correct),
        false,
        reason: 'Pinned upstream sticky Cupertino painter regression',
      );
      await _pump(
        tester,
        _model(variant: 'adaptive', properties: {'autofocus': _bool(true)}),
        theme: ThemeData(platform: TargetPlatform.iOS),
        selectedId: _id,
      );
      final ownedFocus = _sdk(tester).focusNode!;
      ownedFocus.requestFocus();
      await tester.pump();
      expect(ownedFocus.hasFocus, true);
      final before = tester
          .widgetList<CustomPaint>(
            find.descendant(
              of: find.byType(Switch),
              matching: find.byType(CustomPaint),
            ),
          )
          .singleWhere((widget) => widget.painter != null)
          .painter;
      await _pump(
        tester,
        _model(variant: 'adaptive', properties: {'autofocus': _bool(true)}),
        theme: ThemeData(platform: TargetPlatform.windows),
        selectedId: _id,
      );
      final after = tester
          .widgetList<CustomPaint>(
            find.descendant(
              of: find.byType(Switch),
              matching: find.byType(CustomPaint),
            ),
          )
          .singleWhere((widget) => widget.painter != null)
          .painter;
      expect(identical(before, after), false);
      expect(_sdk(tester).focusNode, same(ownedFocus));
      expect(ownedFocus.hasFocus, true);
      expect(
        _diagnostics(tester),
        contains('recreates only the SDK animation shell'),
      );
      // Focus appearance is compared to the fresh SDK with the same focus state.
      ownedFocus.unfocus();
      await tester.pump(const Duration(milliseconds: 350));
      await _expectRawPixels(
        tester,
        _raw(variant: 'adaptive'),
        ThemeData(platform: TargetPlatform.windows),
      );
    },
  );
  testWidgets(
    'Switch ordinary value theme style reference and history updates retain SDK painter and focus identity',
    (tester) async {
      CustomPainter painter() => tester
          .widgetList<CustomPaint>(
            find.descendant(
              of: find.byType(Switch),
              matching: find.byType(CustomPaint),
            ),
          )
          .singleWhere((widget) => widget.painter != null)
          .painter!;
      await _pump(
        tester,
        _model(properties: {'autofocus': _bool(true)}),
        selectedId: _id,
      );
      final original = painter();
      final focus = _sdk(tester).focusNode!;
      for (final value in [true, false, true, false]) {
        await _pump(
          tester,
          _model(
            properties: {
              'value': _bool(value),
              'onChanged': _reference,
              'focusNode': _reference,
              'thumbColorSelected': _color(0xff123456),
              'autofocus': _bool(true),
            },
          ),
          theme: ThemeData(
            brightness: value ? Brightness.dark : Brightness.light,
          ),
          selectedId: _id,
        );
        expect(painter(), same(original));
        expect(_sdk(tester).focusNode, same(focus));
        expect(focus.hasFocus, true);
        expect(_sdk(tester).value, value);
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets(
    'Switch multiple instances keep distinct owned focus keys and renderer state',
    (tester) async {
      final model = _model();
      final first = _control(model);
      final second = jsonDecode(jsonEncode(first)) as Map;
      second['id'] = '4d4fcb3d-d51c-42c0-8d07-dc6c0eea1140';
      second['properties']['value'] = _bool(true);
      (model['root'] as Map)['slots']['body'] = _single(
        _node(
          'c6ca1d28-756e-445b-8712-903479c911bb',
          'flutter.widgets.Column',
          {},
          {
            'children': {
              'kind': 'list',
              'children': [first, second],
            },
          },
        ),
      );
      await _pump(tester, model);
      final controls = tester.widgetList<Switch>(find.byType(Switch)).toList();
      expect(controls, hasLength(2));
      expect(identical(controls[0].focusNode, controls[1].focusNode), false);
      expect(controls.map((v) => v.value), [false, true]);
      expect(tester.takeException(), isNull);
    },
  );
  for (final variant in ['standard', 'adaptive']) {
    for (final apple in [false, true]) {
      testWidgets(
        'Switch controlled tap drag focus and toggled semantics $variant Apple=$apple',
        (tester) async {
          final theme = ThemeData(
            platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
          );
          final focus = FocusNode();
          addTearDown(focus.dispose);
          for (final rtl in [false, true]) {
            final changed = <bool>[];
            await tester.pumpWidget(
              MaterialApp(
                theme: theme,
                home: Scaffold(
                  body: Center(
                    child: Directionality(
                      textDirection: rtl
                          ? TextDirection.rtl
                          : TextDirection.ltr,
                      child: _raw(
                        variant: variant,
                        focusNode: focus,
                        onChanged: changed.add,
                      ),
                    ),
                  ),
                ),
              ),
            );
            await tester.tap(find.byType(Switch));
            await tester.pump(const Duration(milliseconds: 300));
            expect(changed, [true]);
            expect(_sdk(tester).value, false);
            changed.clear();
            await tester.drag(find.byType(Switch), Offset(rtl ? -70 : 70, 0));
            await tester.pump(const Duration(milliseconds: 300));
            expect(changed, contains(true));
            expect(_sdk(tester).value, false);
            focus.requestFocus();
            await tester.pump();
            expect(focus.hasFocus, true);
          }
          final handle = tester.ensureSemantics();
          try {
            for (final value in [false, true]) {
              for (final enabled in [false, true]) {
                final selected = <String>[];
                await _pump(
                  tester,
                  _model(
                    variant: variant,
                    properties: {
                      'value': _bool(value),
                      'enabled': _bool(enabled),
                      'onChanged': _reference,
                    },
                  ),
                  theme: theme,
                  selected: selected,
                );
                final semantics = tester
                    .getSemantics(find.byType(Switch))
                    .getSemanticsData();
                expect(
                  semantics.flagsCollection.isToggled == ui.Tristate.isTrue,
                  value,
                );
                expect(
                  semantics.flagsCollection.isEnabled == ui.Tristate.isTrue,
                  enabled,
                );
                await tester.tap(find.byType(Switch));
                await tester.pump();
                expect(selected, contains(_id));
                expect(_sdk(tester).value, value);
                _sdk(tester).onChanged?.call(!value);
                expect(_sdk(tester).value, value);
                expect(tester.takeException(), isNull);
              }
            }
          } finally {
            handle.dispose();
          }
        },
      );
    }
  }
  testWidgets(
    'Switch directional padding and tap target retain exact SDK sizing under text scale',
    (tester) async {
      for (final material3 in [false, true]) {
        for (final variant in ['standard', 'adaptive']) {
          for (final directional in [false, true]) {
            for (final shrink in [false, true]) {
              final model = _model(
                variant: variant,
                properties: {
                  'padding': _insets(1, 2, 3, 4, directional: directional),
                  'materialTapTargetSize': _enum(
                    'MaterialTapTargetSize',
                    shrink ? 'shrinkWrap' : 'padded',
                  ),
                  'thumbIconDefaultMode': _string('icon'),
                  'thumbIconDefaultData': _icon,
                  'thumbIconDefaultApplyTextScaling': _bool(true),
                },
              );
              (model['profile'] as Map)['textScaleFactor'] = 3.0;
              final theme = ThemeData(
                useMaterial3: material3,
                platform: TargetPlatform.iOS,
              );
              await _pump(tester, model, theme: theme);
              await _expectRawPixels(
                tester,
                _raw(
                  variant: variant,
                  padding: directional
                      ? const EdgeInsetsDirectional.fromSTEB(1, 2, 3, 4)
                      : const EdgeInsets.fromLTRB(1, 2, 3, 4),
                  tapTarget: shrink
                      ? MaterialTapTargetSize.shrinkWrap
                      : MaterialTapTargetSize.padded,
                  thumbIcon: const WidgetStatePropertyAll(
                    Icon(Icons.star, applyTextScaling: true),
                  ),
                ),
                theme,
              );
            }
          }
        }
      }
    },
  );
  testWidgets(
    'Raw SDK Switch positive infinite outline width equal and unequal endpoint characterization',
    (tester) async {
      for (final apple in [false, true]) {
        for (final equal in [true, false]) {
          await tester.pumpWidget(
            MaterialApp(
              theme: ThemeData(
                platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
              ),
              home: Scaffold(
                body: Center(
                  child: Switch.adaptive(
                    value: false,
                    onChanged: (_) {},
                    trackOutlineWidth: WidgetStateProperty.resolveWith(
                      (states) => equal || states.contains(WidgetState.selected)
                          ? double.infinity
                          : 2,
                    ),
                    trackOutlineColor: const WidgetStatePropertyAll(Colors.red),
                  ),
                ),
              ),
            ),
          );
          final error = tester.takeException();
          if (equal) {
            expect(error, isNull);
          } else {
            expect(error, isA<AssertionError>());
            expect(
              error.toString(),
              contains(
                'Cannot interpolate between finite and non-finite values',
              ),
            );
          }
          await tester.pumpWidget(const SizedBox());
        }
      }
    },
  );
  testWidgets(
    'Switch nullable state colors and widths use all 256 presence-aware priority combinations',
    (tester) async {
      for (final family in [
        'thumbColor',
        'trackColor',
        'trackOutlineColor',
        'overlayColor',
      ]) {
        final properties = <String, Object?>{
          '${family}Default': _color(0xffabcdef),
        };
        for (var i = 0; i < _states.length; i++) {
          properties['$family${_states.values.elementAt(i)}'] = i.isEven
              ? _null
              : _color(0xff000000 + i);
        }
        await _pump(tester, _model(properties: properties));
        final sdk = _sdk(tester);
        final map = switch (family) {
          'thumbColor' => sdk.thumbColor!,
          'trackColor' => sdk.trackColor!,
          'trackOutlineColor' => sdk.trackOutlineColor!,
          _ => sdk.overlayColor!,
        };
        for (var mask = 0; mask < 256; mask++) {
          final states = <WidgetState>{
            for (var i = 0; i < 8; i++)
              if (mask & (1 << i) != 0) _states.keys.elementAt(i),
          };
          final first = mask == 0
              ? -1
              : List.generate(
                  8,
                  (i) => i,
                ).firstWhere((i) => mask & (1 << i) != 0);
          expect(
            map.resolve(states),
            first == -1
                ? const Color(0xffabcdef)
                : first.isEven
                ? null
                : Color(0xff000000 + first),
            reason: '$family mask=$mask',
          );
        }
      }
      await _pump(
        tester,
        _model(
          properties: {
            'trackOutlineWidthDefault': _number(-3),
            for (var i = 0; i < 8; i++)
              'trackOutlineWidth${_states.values.elementAt(i)}': i.isEven
                  ? _null
                  : _number(i),
          },
        ),
      );
      for (var mask = 0; mask < 256; mask++) {
        final states = <WidgetState>{
          for (var i = 0; i < 8; i++)
            if (mask & (1 << i) != 0) _states.keys.elementAt(i),
        };
        final first = mask == 0
            ? -1
            : List.generate(
                8,
                (i) => i,
              ).firstWhere((i) => mask & (1 << i) != 0);
        expect(
          _sdk(tester).trackOutlineWidth!.resolve(states),
          first == -1
              ? -3.0
              : first.isEven
              ? null
              : first.toDouble(),
        );
      }
    },
  );
  testWidgets(
    'Switch full Icon metadata survives every state bucket and explicit inherit stops fallback',
    (tester) async {
      final fields = fixture.iconPropertiesForViewTest();
      final properties = <String, Object?>{};
      for (final state in ['Default', ..._states.values]) {
        properties['thumbIcon${state}Mode'] = _string('icon');
        for (final field in fields.entries) {
          final suffix = field.key == 'icon'
              ? 'Data'
              : field.key[0].toUpperCase() + field.key.substring(1);
          properties['thumbIcon$state$suffix'] = field.value;
        }
      }
      await _pump(tester, _model(properties: properties));
      final theme = Theme.of(tester.element(find.byType(Switch)));
      for (final state in [
        <WidgetState>{},
        for (final state in _states.keys) {state},
      ]) {
        final icon = _sdk(tester).thumbIcon!.resolve(state)!;
        expect(icon.icon!.codePoint, 0xe5fc);
        expect(icon.icon!.matchTextDirection, true);
        expect(icon.size, 32);
        expect(icon.fill, .75);
        expect(icon.weight, 600);
        expect(icon.grade, -25);
        expect(icon.opticalSize, 24);
        expect(icon.color, theme.colorScheme.primary);
        expect(icon.shadows!.single.blurRadius, 3);
        expect(icon.semanticLabel, 'Reviewed icon');
        expect(icon.textDirection, TextDirection.rtl);
        expect(icon.applyTextScaling, false);
        expect(icon.blendMode, BlendMode.multiply);
        expect(icon.fontWeight, FontWeight.w700);
      }
      await _pump(
        tester,
        _model(
          properties: {
            'thumbIconDefaultData': _icon,
            'thumbIconDisabledMode': _string('inherit'),
            'thumbIconSelectedMode': _string('icon'),
          },
        ),
      );
      expect(
        _sdk(
          tester,
        ).thumbIcon!.resolve({WidgetState.disabled, WidgetState.selected}),
        isNull,
      );
      expect(
        _sdk(tester).thumbIcon!.resolve({WidgetState.selected}),
        isA<Icon>(),
      );
      expect(
        _sdk(tester).thumbIcon!.resolve({WidgetState.selected})!.icon,
        isNull,
      );
      expect(_sdk(tester).thumbIcon!.resolve({})!.icon!.codePoint, 58873);
    },
  );
  testWidgets(
    'Switch local project references are explicit isolated previews and disabled callbacks stay inactive',
    (tester) async {
      for (final enabled in [false, true]) {
        await _pump(
          tester,
          _model(
            variant: 'adaptive',
            properties: {
              'enabled': _bool(enabled),
              'onChanged': _reference,
              'onFocusChange': _reference,
              'focusNode': _reference,
              'mouseCursor': _reference,
              for (final name in [
                'thumbColor',
                'trackColor',
                'trackOutlineColor',
                'overlayColor',
                'trackOutlineWidth',
                'thumbIcon',
              ])
                name: _reference,
            },
          ),
          theme: ThemeData(platform: TargetPlatform.iOS),
        );
        final sdk = _sdk(tester);
        expect(sdk.onChanged != null, enabled);
        expect(
          _diagnostics(tester).contains('Project onChanged is not invoked'),
          enabled,
        );
        expect(sdk.onFocusChange, isNotNull);
        sdk.onFocusChange!(true);
        sdk.onChanged?.call(true);
        expect(sdk.value, false);
        expect(_diagnostics(tester), contains('explicitly approximate'));
        expect(_diagnostics(tester), contains('Project focus ownership'));
        expect(_diagnostics(tester), contains('Project cursor'));
        expect(sdk.thumbColor, isNull);
        expect(sdk.trackColor, isNull);
        expect(sdk.trackOutlineWidth, isNull);
        expect(sdk.thumbIcon, isNull);
        expect(tester.takeException(), isNull);
      }
    },
  );
  testWidgets(
    'Switch M2 M3 adaptive custom adaptation Cupertino theme and legacy alias equal real SDK',
    (tester) async {
      for (final material3 in [false, true]) {
        for (final apple in [false, true]) {
          for (final apply in [null, false, true]) {
            for (final adapted in [false, true]) {
              final stateTheme = SwitchThemeData(
                thumbColor: const WidgetStatePropertyAll(Colors.pink),
                trackColor: const WidgetStatePropertyAll(Colors.orange),
                trackOutlineColor: const WidgetStatePropertyAll(Colors.cyan),
                trackOutlineWidth: const WidgetStatePropertyAll(3),
                thumbIcon: const WidgetStatePropertyAll(
                  Icon(Icons.star, color: Colors.green, size: 12),
                ),
                padding: const EdgeInsetsDirectional.fromSTEB(2, 3, 4, 5),
              );
              final theme = ThemeData(
                useMaterial3: material3,
                platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
                cupertinoOverrideTheme: const CupertinoThemeData(
                  primaryColor: Colors.purple,
                  applyThemeToAll: true,
                ),
                switchTheme: stateTheme,
                adaptations: [if (adapted) _SwitchAdaptation(stateTheme)],
              );
              final properties = <String, Object?>{
                'value': _bool(true),
                'applyCupertinoTheme': apply == null ? _null : _bool(apply),
                'activeColor': _color(0xff123456),
                'thumbColorSelected': _null,
                'trackColorSelected': _null,
                'overlayColorFocused': _null,
              };
              await _pump(
                tester,
                _model(variant: 'adaptive', properties: properties),
                theme: theme,
              );
              await _expectRawPixels(
                tester,
                _raw(
                  variant: 'adaptive',
                  value: true,
                  activeColor: const Color(0xff123456),
                  applyCupertinoTheme: apply,
                  thumbColor: WidgetStateProperty<Color?>.fromMap({
                    WidgetState.selected: null,
                  }),
                  trackColor: WidgetStateProperty<Color?>.fromMap({
                    WidgetState.selected: null,
                  }),
                  overlayColor: WidgetStateProperty<Color?>.fromMap({
                    WidgetState.focused: null,
                  }),
                ),
                theme,
              );
            }
          }
        }
      }
    },
  );
  for (final material3 in [false, true]) {
    for (final apple in [false, true]) {
      testWidgets(
        'Switch full icon paint uses actual SDK fields M3=$material3 Apple=$apple',
        (tester) async {
          final theme = ThemeData(
            useMaterial3: material3,
            platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
          );
          for (final value in [false, true]) {
            for (final empty in [false, true]) {
              final props = <String, Object?>{
                'value': _bool(value),
                'thumbIconDefaultMode': _string('icon'),
                if (!empty) 'thumbIconDefaultData': _icon,
                'thumbIconDefaultSize': _number(14),
                'thumbIconDefaultFill': _number(.5),
                'thumbIconDefaultWeight': _number(500.0),
                'thumbIconDefaultGrade': _number(-20.0),
                'thumbIconDefaultOpticalSize': _number(20.0),
                'thumbIconDefaultColor': _color(0xff339966),
                'thumbIconDefaultSemanticLabel': _string('Ignored thumb label'),
                'thumbIconDefaultApplyTextScaling': _bool(true),
                'thumbIconDefaultTextDirection': _enum('TextDirection', 'rtl'),
                'thumbIconDefaultFontWeight': _enum('FontWeight', 'w900'),
                'thumbIconDefaultBlendMode': _enum('BlendMode', 'clear'),
              };
              await _pump(
                tester,
                _model(variant: 'adaptive', properties: props),
                theme: theme,
              );
              await _expectRawPixels(
                tester,
                _raw(
                  variant: 'adaptive',
                  value: value,
                  thumbIcon: WidgetStatePropertyAll(
                    Icon(
                      empty ? null : Icons.star,
                      size: 14,
                      fill: .5,
                      weight: 500,
                      grade: -20,
                      opticalSize: 20,
                      color: const Color(0xff339966),
                      semanticLabel: 'Ignored thumb label',
                      applyTextScaling: true,
                      textDirection: TextDirection.rtl,
                      fontWeight: FontWeight.w900,
                      blendMode: BlendMode.clear,
                    ),
                  ),
                ),
                theme,
              );
            }
          }
        },
      );
    }
  }
  test(
    'Switch closes 201 scalar fields and no slots with exact Java-authored proof',
    () {
      String block(String contract) {
        final start = contract.indexOf('W|$_type\n');
        return contract.substring(start, contract.indexOf('\nW|', start + 1));
      }

      final actual = block(
        canvasRuntimeWidgetSchemaContractForTesting(),
      ).split('\n');
      final expected = block(canvasReviewedWidgetSchemaContract).split('\n');
      expect(actual.where((v) => v.startsWith('P|')), hasLength(201));
      expect(actual.length, expected.length);
      for (var i = 0; i < actual.length; i++) {
        expect(actual[i], expected[i], reason: 'Java Switch proof $i');
      }
      expect(
        actual.where((v) => v.startsWith('S|') || v.startsWith('C|')),
        isEmpty,
      );
      expect(canvasModelProtocolVersion, 20);
      expect(_control(_model())['properties'], hasLength(3));
    },
  );
  test(
    'Switch decoder rejects constructor relations, arbitrary expressions, mismatched state maps and kinds',
    () {
      for (final properties in <Map<String, Object?>>[
        {'value': _null},
        {'enabled': _null},
        {'variant': _string('cupertino')},
        {'applyCupertinoTheme': _null},
        {'onActiveThumbImageError': _reference},
        {
          'onInactiveThumbImageError': _reference,
          'activeThumbImage': _provider(),
        },
        {
          'onChanged': {'kind': 'callbackPresence'},
        },
        {'thumbColor': _reference, 'thumbColorDefault': _null},
        {'trackColor': _reference, 'trackColorDisabled': _color(0xff123456)},
        {'overlayColor': _reference, 'overlayColorHovered': _null},
        {'trackOutlineColor': _reference, 'trackOutlineColorSelected': _null},
        {
          'trackOutlineWidth': _reference,
          'trackOutlineWidthDefault': _number(0),
        },
        {'thumbIcon': _reference, 'thumbIconDefaultMode': _string('inherit')},
        {
          'thumbIconDefaultMode': _string('inherit'),
          'thumbIconDefaultData': _icon,
        },
        {'thumbIconDefaultMode': _string('null')},
        {
          'thumbIconDefaultData': {..._icon, 'fontFamily': 'ProjectFont'},
        },
        {'thumbIconDefaultSize': _number(-1)},
        {'thumbIconDefaultFill': _number(1.1)},
        {'thumbIconDefaultWeight': _number(0.0)},
        {
          'trackOutlineWidthDefault': {
            ..._infinity,
            'value': 'negativeInfinity',
          },
        },
        {'splashRadius': _null},
        {'padding': _insets(-1, 0, 0, 0)},
        {'thumbIconDefaultColor': _null},
        {'thumbIconDefaultFontWeight': _enum('FontWeight', 'strong')},
        {
          'value': {'kind': 'dartExpression', 'value': 'runProject()'},
        },
      ]) {
        expect(
          () => _decode(_model(properties: properties)),
          throwsFormatException,
          reason: '$properties',
        );
      }
      for (final state in ['Default', ..._states.values]) {
        for (final value in [
          _number(-9007199254740991),
          _number(-1.7e308),
          _number(0),
          _number(1.7e308),
          _null,
        ]) {
          expect(
            () =>
                _decode(_model(properties: {'trackOutlineWidth$state': value})),
            returnsNormally,
          );
        }
      }
      for (final variant in ['standard', 'adaptive']) {
        expect(() => _decode(_model(variant: variant)), returnsNormally);
      }
    },
  );
  for (final profile in ['windows', 'web']) {
    testWidgets(
      'Switch signed splash and outline geometry obey raw SDK on $profile',
      (tester) async {
        for (final material3 in [false, true]) {
          for (final apple in [false, true]) {
            final theme = ThemeData(
              useMaterial3: material3,
              platform: apple ? TargetPlatform.iOS : TargetPlatform.windows,
            );
            for (final width in [-1.7e308, -4.0, 0.0, 1.7e308]) {
              await _pump(
                tester,
                _model(
                  platform: profile,
                  variant: 'adaptive',
                  properties: {
                    'trackOutlineWidthDefault': _number(width),
                    'trackOutlineColorDefault': _color(0xffcc3355),
                  },
                ),
                theme: theme,
              );
              expect(tester.takeException(), isNull);
              await _expectRawPixels(
                tester,
                _raw(
                  variant: 'adaptive',
                  trackOutlineWidth: WidgetStatePropertyAll(width),
                  trackOutlineColor: const WidgetStatePropertyAll(
                    Color(0xffcc3355),
                  ),
                ),
                theme,
              );
            }
            for (final radius in [-9.0, 0.0, double.infinity]) {
              final model = _model(
                platform: profile,
                variant: 'adaptive',
                properties: {
                  'splashRadius': radius.isInfinite
                      ? _infinity
                      : _number(radius),
                },
              );
              await _pump(tester, model, theme: theme);
              expect(_sdk(tester).splashRadius, radius);
              final gesture = await tester.createGesture(
                kind: PointerDeviceKind.mouse,
              );
              await gesture.addPointer(location: const Offset(1, 1));
              await gesture.moveTo(tester.getCenter(find.byType(Switch)));
              await tester.pump(const Duration(milliseconds: 250));
              await gesture.down(tester.getCenter(find.byType(Switch)));
              await tester.pump(const Duration(milliseconds: 100));
              await gesture.up();
              await gesture.removePointer();
              await tester.pump(const Duration(milliseconds: 300));
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );
    testWidgets(
      'Switch constructors defaults match SDK M2 M3 Apple Material RTL disabled and controlled values on $profile',
      (tester) async {
        for (final material3 in [false, true]) {
          for (final variant in ['standard', 'adaptive']) {
            for (final apple in [false, true]) {
              for (final value in [false, true]) {
                for (final enabled in [false, true]) {
                  for (final rtl in [false, true]) {
                    final theme = ThemeData(
                      useMaterial3: material3,
                      platform: apple
                          ? TargetPlatform.iOS
                          : TargetPlatform.windows,
                    );
                    final model = _model(
                      platform: profile,
                      variant: variant,
                      properties: {
                        'value': _bool(value),
                        'enabled': _bool(enabled),
                      },
                    );
                    if (rtl) {
                      final root = model['root'] as Map;
                      final body = root['slots']['body']['child'];
                      root['slots']['body'] = _single(
                        _node(
                          'afbfcd3a-9da1-4c30-a445-498970b41d04',
                          'flutter.widgets.Directionality',
                          {'textDirection': _enum('TextDirection', 'rtl')},
                          {'child': _single(body)},
                        ),
                      );
                    }
                    await _pump(tester, model, theme: theme);
                    expect(_sdk(tester).value, value);
                    expect(_sdk(tester).onChanged != null, enabled);
                    // Raw control uses the same document Directionality below.
                    await _expectRawPixels(
                      tester,
                      _raw(variant: variant, value: value, enabled: enabled),
                      theme,
                    );
                  }
                }
              }
            }
          }
        }
      },
    );
    testWidgets(
      'Switch resource providers preserve exact resize omitted layers and error isolation on $profile',
      (tester) async {
        for (final variant in ['standard', 'adaptive']) {
          for (final active in [false, true]) {
            final field = active ? 'activeThumbImage' : 'inactiveThumbImage';
            final callback = active
                ? 'onActiveThumbImageError'
                : 'onInactiveThumbImageError';
            for (final exact in [false, true]) {
              for (final resize in [false, true]) {
                await _pump(
                  tester,
                  _model(
                    platform: profile,
                    variant: variant,
                    properties: {
                      'value': _bool(active),
                      field: _provider(exact: exact, resize: resize),
                      callback: _reference,
                    },
                  ),
                  resources: _resources(),
                );
                final sdk = _sdk(tester);
                ImageProvider provider = (active
                    ? sdk.activeThumbImage
                    : sdk.inactiveThumbImage)!;
                if (resize) {
                  expect(provider, isA<ResizeImage>());
                  final resized = provider as ResizeImage;
                  expect(resized.width, 4);
                  expect(resized.policy, ResizeImagePolicy.fit);
                  expect(resized.allowUpscaling, true);
                  provider = resized.imageProvider;
                }
                expect(provider, isA<MemoryImage>());
                expect((provider as MemoryImage).bytes, _bytes);
                expect(provider.scale, 2);
                expect(
                  _diagnostics(tester),
                  contains('Project image error callbacks are not invoked'),
                );
                expect(tester.takeException(), isNull);
              }
            }
            for (final missing in [true, false]) {
              await _pump(
                tester,
                _model(
                  platform: profile,
                  variant: variant,
                  properties: {
                    field: _provider(missing: missing),
                    callback: _reference,
                  },
                ),
              );
              expect(
                active
                    ? _sdk(tester).activeThumbImage
                    : _sdk(tester).inactiveThumbImage,
                isNull,
              );
              expect(
                active
                    ? _sdk(tester).onActiveThumbImageError
                    : _sdk(tester).onInactiveThumbImageError,
                isNull,
              );
              final semantics = tester
                  .widgetList<Semantics>(find.byType(Semantics))
                  .map((w) => w.properties.label ?? '')
                  .join(' ');
              expect(semantics, contains(field));
              expect(semantics, contains('Image preview unavailable'));
            }
          }
        }
      },
    );
    testWidgets(
      'Switch malformed images report without executing project callbacks on $profile',
      (tester) async {
        final corrupt = Uint8List.fromList([
          137,
          80,
          78,
          71,
          13,
          10,
          26,
          10,
          1,
          2,
          3,
        ]);
        for (final active in [false, true]) {
          for (final callback in [false, true]) {
            final errors = <String>[];
            await _pump(
              tester,
              _model(
                platform: profile,
                properties: {
                  'value': _bool(active),
                  active ? 'activeThumbImage' : 'inactiveThumbImage': _provider(
                    bytes: corrupt,
                  ),
                  if (callback)
                    active
                            ? 'onActiveThumbImageError'
                            : 'onInactiveThumbImageError':
                        _reference,
                },
              ),
              resources: _resources(corrupt),
              onImageError: (id, error, stack) => errors.add(id),
            );
            await tester.runAsync(
              () => Future<void>.delayed(const Duration(milliseconds: 80)),
            );
            await tester.pump();
            expect(errors, contains(sha256Hex(corrupt)));
            expect(tester.takeException(), isNull);
          }
        }
      },
    );
  }
}
