import 'dart:convert';
import 'dart:ui' as ui;

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

const _type = 'flutter.material.CircleAvatar';
const _id = '82d9aa26-3fd4-4d42-81d2-677bb3f1d20c';
const _childId = 'bb462f89-8f67-4a84-b03e-471bcf7f543f';
const _fieldId = '07303f47-9cbe-408b-a4fa-4fd9c5e1c765';
const _infinity = {'kind': 'enum', 'type': 'double', 'value': 'infinity'};
const _callback = {'kind': 'callbackPresence'};
final _bytes = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAYAAADED76LAAAAAXNSR0IArs4c6QAA'
  'AARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAAeSURBVChTY/'
  'j6/OV/ZHzWzg8FM9BBAboAugY6KAAAyITDgZYboFoAAAAASUVORK5CYII=',
);
final _bytes2 = base64Decode(
  'iVBORw0KGgoAAAANSUhEUgAAAAQAAAAECAYAAACp8Z5+AAAAAXNSR0IArs4c6QAA'
  'AARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAASSURBVBhXY5Dr'
  'ePofGTOQLgAATc8oocRj8pEAAAAASUVORK5CYII=',
);

void main() {
  test(
    'CircleAvatar closes nine optional SDK fields and ordinary child slot without synthetic defaults',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final block = contract.substring(
        start,
        contract.indexOf(
          'W|flutter.material.CircularProgressIndicator\n',
          start,
        ),
      );
      expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(9));
      expect(block, contains('S|child|single|0|0|1|any\n'));
      expect(block, isNot(contains('C|')));
      expect(canvasModelProtocolVersion, 20);
      expect(_avatar(_decode(_model())).properties, isEmpty);
      expect(canvasWidgetTraitsForType(_type), isEmpty);
      expect(canvasDropSlotsForWidgetType(_type), const [
        canvasEmptyChildDropSlot,
      ]);
      expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), false);
      for (final name in ['radius', 'minRadius', 'maxRadius']) {
        for (final value in [
          _int(0),
          _int(9007199254740991),
          _double(25.5),
          _double(1.7e308),
          _infinity,
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            returnsNormally,
          );
        }
        for (final value in [
          _int(-1),
          _double(-.1),
          _string('infinity'),
          {'kind': 'null'},
          {..._infinity, 'value': 'nan'},
          {..._infinity, 'value': 'negativeInfinity'},
          {..._infinity, 'type': 'Radius'},
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            throwsFormatException,
          );
        }
      }
      for (final properties in <Map<String, Object?>>[
        {'radius': _int(10), 'minRadius': _int(10)},
        {'radius': _infinity, 'maxRadius': _infinity},
        {'minRadius': _int(20), 'maxRadius': _int(19)},
        {'minRadius': _infinity, 'maxRadius': _double(1)},
        {'onBackgroundImageError': _callback},
        {'onForegroundImageError': _callback},
        {'onForegroundImageError': _callback, 'backgroundImage': _provider()},
        {
          'backgroundImage': {'kind': 'null'},
        },
        {
          'foregroundColor': {'kind': 'null'},
        },
        {'key': _string('key')},
        {'clipBehavior': _string('antiAlias')},
        {'child': _string('initials')},
        {'radius': _callback},
      ]) {
        expect(
          () => _decode(_model(properties: properties)),
          throwsFormatException,
        );
      }
      // Validation follows the SDK's actual doubled constraints, not the
      // mathematical radii, including finite multiplication overflow.
      expect(
        () => _decode(
          _model(
            properties: {
              'minRadius': _double(1.7e308),
              'maxRadius': _double(1e308),
            },
          ),
        ),
        returnsNormally,
      );
      for (final token in canvasColorSchemeThemeTokens) {
        expect(
          () => _decode(
            _model(
              properties: {
                'backgroundColor': _theme(token),
                'foregroundColor': _theme(token),
              },
            ),
          ),
          returnsNormally,
        );
      }
      final badChild = _model(
        child: {
          'id': _childId,
          'type': 'flutter.widgets.Spacer',
          'properties': <String, Object?>{},
          'slots': <String, Object?>{},
        },
      );
      expect(() => _decode(badChild), throwsFormatException);
      final encoded = jsonEncode(
        _model(properties: {'radius': _double(123.5)}),
      ).replaceAll('123.5', '1e309');
      expect(
        () => CanvasModel.decode(Uint8List.fromList(utf8.encode(encoded))),
        throwsFormatException,
      );
    },
  );

  test(
    'CircleAvatar resolves both layers through closed asset package exact resize and callback presence domains',
    () {
      for (final name in ['backgroundImage', 'foregroundImage']) {
        for (final exact in [false, true]) {
          for (final package in [null, 'avatars']) {
            for (final resize in [
              null,
              _resize(),
              _resize(width: null, height: 3, fit: true, allow: true),
            ]) {
              final image = _provider(
                exact: exact,
                package: package,
                resize: resize,
              );
              final model = _decode(
                _model(
                  properties: {
                    name: image,
                    name == 'backgroundImage'
                            ? 'onBackgroundImageError'
                            : 'onForegroundImageError':
                        _callback,
                  },
                ),
              );
              expect(model.imageResourceIds, {sha256Hex(_bytes)});
              final provider =
                  _avatar(model).properties[name]!.value
                      as CanvasImageProviderValue;
              expect(provider.providerKind, exact ? 'exactAsset' : 'asset');
              expect(provider.packageName, package);
            }
          }
        }
        for (final patch in <Map<String, Object?>>[
          {'kind': 'network'},
          {'assetName': '../avatar.png'},
          {'resize': _resize(width: 0)},
          {'resize': _resize(width: 16385)},
          {'resize': _resize(width: null)},
          {'unknown': true},
        ]) {
          final image = _provider();
          (image['value'] as Map).addAll(patch);
          expect(
            () => _decode(_model(properties: {name: image})),
            throwsFormatException,
          );
        }
      }
      expect(
        _decode(
          _model(
            properties: {
              'backgroundImage': _provider(),
              'foregroundImage': _provider(bytes: _bytes2),
            },
          ),
        ).imageResourceIds,
        {sha256Hex(_bytes), sha256Hex(_bytes2)},
      );
      expect(
        _decode(
          _model(
            properties: {
              'backgroundImage': _provider(),
              'foregroundImage': _provider(),
            },
          ),
        ).imageResourceIds,
        {sha256Hex(_bytes)},
      );
      expect(
        _decode(
          _model(
            properties: {
              'backgroundImage': _missing(),
              'foregroundImage': _missing(),
            },
          ),
        ).imageResourceIds,
        isEmpty,
      );
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'CircleAvatar pixels match raw SDK foreground background and unclipped square child paint on $platform',
      (tester) async {
        for (final background in [false, true]) {
          for (final foreground in [false, true]) {
            final child = <String, Object?>{
              'id': _childId,
              'type': 'flutter.widgets.ColoredBox',
              'properties': {'color': _color('0xFFFF0000')},
              'slots': {
                'child': {
                  'kind': 'single',
                  'child': {
                    'id': _fieldId,
                    'type': 'flutter.widgets.SizedBox',
                    'properties': {'width': _int(40), 'height': _int(40)},
                    'slots': <String, Object?>{},
                  },
                },
              },
            };
            final model = _model(
              platform: platform,
              properties: {
                'radius': _int(20),
                'backgroundColor': _color('0xFF0000FF'),
                if (background) 'backgroundImage': _provider(),
                if (foreground) 'foregroundImage': _provider(bytes: _bytes2),
              },
              child: child,
            );
            final slot = _centerSlot(model);
            slot['child'] = {
              'id': '9dcf296a-34b8-43bd-b118-29e3590acdfc',
              'type': 'flutter.widgets.RepaintBoundary',
              'properties': <String, Object?>{},
              'slots': {
                'child': {'kind': 'single', 'child': slot['child']},
              },
            };
            await _pump(
              tester,
              model,
              resources: _resources(_bytes2),
              selectedId: null,
            );
            final avatar = tester.widget<CircleAvatar>(_finder());
            await tester.runAsync(() async {
              if (avatar.backgroundImage != null) {
                await precacheImage(
                  avatar.backgroundImage!,
                  tester.element(_finder()),
                );
              }
              if (avatar.foregroundImage != null) {
                await precacheImage(
                  avatar.foregroundImage!,
                  tester.element(_finder()),
                );
              }
            });
            await tester.pump();
            final boundary = tester.renderObject<RenderRepaintBoundary>(
              find
                  .descendant(
                    of: _widget('9dcf296a-34b8-43bd-b118-29e3590acdfc'),
                    matching: find.byType(RepaintBoundary),
                  )
                  .first,
            );
            final actual = await _pixels(tester, boundary);
            final key = GlobalKey();
            await tester.pumpWidget(
              MaterialApp(
                home: Scaffold(
                  body: Center(
                    child: RepaintBoundary(
                      key: key,
                      child: CircleAvatar(
                        radius: 20,
                        backgroundColor: const Color(0xFF0000FF),
                        backgroundImage: background
                            ? MemoryImage(_bytes, scale: 2)
                            : null,
                        foregroundImage: foreground
                            ? MemoryImage(_bytes2, scale: 2)
                            : null,
                        child: const ColoredBox(
                          color: Color(0xFFFF0000),
                          child: SizedBox(width: 40, height: 40),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            );
            await tester.pump(const Duration(milliseconds: 300));
            final rawAvatar = tester.widget<CircleAvatar>(
              find.byType(CircleAvatar),
            );
            await tester.runAsync(() async {
              if (rawAvatar.backgroundImage != null) {
                await precacheImage(
                  rawAvatar.backgroundImage!,
                  tester.element(find.byType(CircleAvatar)),
                );
              }
              if (rawAvatar.foregroundImage != null) {
                await precacheImage(
                  rawAvatar.foregroundImage!,
                  tester.element(find.byType(CircleAvatar)),
                );
              }
            });
            await tester.pump();
            final expected = await _pixels(
              tester,
              key.currentContext!.findRenderObject()! as RenderRepaintBoundary,
            );
            // Canvas adds reviewed selection guides along widget bounds.
            // Compare interior pixels including the circular edge and
            // outside-circle child corners but excluding those guides.
            expect(_interior(actual), orderedEquals(_interior(expected)));
            expect(
              actual.sublist((4 * 40 + 4) * 4, (4 * 40 + 4) * 4 + 4),
              [255, 0, 0, 255],
              reason:
                  'SDK circular decoration does not clip child at the square corner',
            );
            if (foreground) {
              expect(
                actual.sublist((20 * 40 + 20) * 4, (20 * 40 + 20) * 4 + 4),
                isNot([255, 0, 0, 255]),
                reason: 'Foreground image paints over the child',
              );
            }
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'CircleAvatar radius and Infinity changes retain active inline child editor text state and focus on $platform',
      (tester) async {
        final model = _model(platform: platform, child: _text());
        await _pump(tester, model, selectedId: _childId, inline: true);
        await tester.tap(_widget(_childId));
        await tester.pump();
        await tester.sendKeyEvent(LogicalKeyboardKey.f2);
        await tester.pump();
        final editor = find.byType(EditableText);
        expect(editor, findsOneWidget);
        await tester.enterText(editor, 'Keep avatar focus');
        final element = tester.element(editor);
        final state = tester.state(editor);
        final focus = tester.widget<EditableText>(editor).focusNode;
        for (final radius in [_int(40), _infinity, _int(25)]) {
          (_centerSlot(model)['child'] as Map)['properties'] = {
            'radius': radius,
          };
          await _pump(tester, model, selectedId: _childId, inline: true);
          expect(tester.element(editor), same(element));
          expect(tester.state(editor), same(state));
          expect(tester.widget<EditableText>(editor).focusNode, same(focus));
          expect(focus.hasFocus, true);
          expect(
            tester.widget<EditableText>(editor).controller.text,
            'Keep avatar focus',
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'CircleAvatar SDK M2 M3 theme colors text Icon inheritance and no text scaling on $platform',
      (tester) async {
        for (final material3 in [false, true]) {
          for (final dark in [false, true]) {
            final theme = ThemeData(
              useMaterial3: material3,
              brightness: dark ? Brightness.dark : Brightness.light,
            );
            for (final explicit in [false, true]) {
              final properties = <String, Object?>{
                if (explicit)
                  'backgroundColor': _theme('material.colorScheme.primary'),
                if (explicit) 'foregroundColor': _color('0xFF123456'),
              };
              for (final icon in [false, true]) {
                await _pump(
                  tester,
                  _model(
                    platform: platform,
                    properties: properties,
                    child: icon ? _icon() : _text(),
                  ),
                  theme: theme,
                  textScale: 4,
                );
                final avatar = tester.widget<CircleAvatar>(_finder());
                expect(avatar.radius, isNull);
                expect(avatar.minRadius, isNull);
                expect(avatar.maxRadius, isNull);
                expect(tester.getSize(_finder()), const Size.square(40));
                final effective = Theme.of(tester.element(_finder()));
                final childElement = tester.element(_widget(_childId));
                expect(
                  MediaQuery.textScalerOf(tester.element(_finder())).scale(10),
                  40,
                );
                expect(
                  MediaQuery.textScalerOf(childElement),
                  TextScaler.noScaling,
                );
                final textStyle = DefaultTextStyle.of(childElement).style;
                final expectedForeground = explicit
                    ? const Color(0xFF123456)
                    : material3
                    ? effective.colorScheme.onPrimaryContainer
                    : effective.primaryTextTheme.titleMedium!.color;
                expect(textStyle.color, expectedForeground);
                expect(
                  textStyle.fontSize,
                  (material3 ? effective.textTheme : effective.primaryTextTheme)
                      .titleMedium!
                      .fontSize,
                );
                expect(IconTheme.of(childElement).color, expectedForeground);
                final decoration =
                    tester.widget<AnimatedContainer>(_container()).decoration!
                        as BoxDecoration;
                expect(decoration.shape, BoxShape.circle);
                expect(
                  decoration.color,
                  explicit
                      ? effective.colorScheme.primary
                      : material3
                      ? effective.colorScheme.primaryContainer
                      : ThemeData.estimateBrightnessForColor(
                              expectedForeground!,
                            ) ==
                            Brightness.dark
                      ? effective.primaryColorLight
                      : effective.primaryColorDark,
                );
                expect(
                  find.descendant(
                    of: _finder(),
                    matching: find.byType(ClipOval),
                  ),
                  findsNothing,
                );
                expect(tester.takeException(), isNull);
              }
            }
          }
        }
        // M2 automatic foreground contrast follows the explicit background.
        for (final background in ['0xFF000000', '0xFFFFFFFF']) {
          await _pump(
            tester,
            _model(
              platform: platform,
              child: _text(),
              properties: {'backgroundColor': _color(background)},
            ),
            theme: ThemeData(useMaterial3: false),
          );
          final theme = Theme.of(tester.element(_finder()));
          expect(
            DefaultTextStyle.of(tester.element(_widget(_childId))).style.color,
            background == '0xFF000000'
                ? theme.primaryColorLight
                : theme.primaryColorDark,
          );
        }
      },
    );

    testWidgets(
      'CircleAvatar has genuine child insertion empty and zero size selection without synthetic visible child on $platform',
      (tester) async {
        for (final radius in [null, 0, 31]) {
          CanvasDropResolver? resolver;
          final selected = <String>[];
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {if (radius != null) 'radius': _int(radius)},
            ),
            onDrop: (value) => resolver = value,
            selected: selected,
          );
          expect(tester.widget<CircleAvatar>(_finder()).child, isNull);
          expect(tester.getSize(_finder()), Size.square((radius ?? 20) * 2.0));
          final targetFinder = radius == 0
              ? find.byKey(
                  const ValueKey('canvas-zero-size-widget-target-$_id'),
                )
              : _widget(_id);
          expect(targetFinder, findsOneWidget);
          await tester.tap(targetFinder);
          await tester.pump();
          expect(selected, contains(_id));
          final surface = tester.getRect(find.byType(CanvasDocumentView));
          final point = tester.getCenter(targetFinder);
          final target = resolver!(
            ((point.dx - surface.left) / surface.width * 1000000).round(),
            ((point.dy - surface.top) / surface.height * 1000000).round(),
            CanvasPaletteDragSource(
              token: 'text',
              widgetType: 'flutter.widgets.Text',
              traits: const {},
            ),
          );
          expect(target?.parentWidgetId, _id);
          expect(target?.slotName, 'child');
          expect(target?.insertionIndex, 0);
          expect(tester.takeException(), isNull);
        }
        await _pump(tester, _model(platform: platform, child: _text()));
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: _type,
            slotName: 'child',
            currentChildCount: 1,
            insertionIndex: 0,
            source: CanvasPaletteDragSource(
              token: 'text',
              widgetType: 'flutter.widgets.Text',
              traits: const {},
            ),
          ),
          false,
        );
      },
    );

    testWidgets(
      'CircleAvatar forwards every image layer binding and renders fallback for absent rejected and missing resources on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final layer in ['backgroundImage', 'foregroundImage']) {
            for (final exact in [false, true]) {
              for (final resize in [
                null,
                _resize(),
                _resize(width: null, height: 3, fit: true, allow: true),
              ]) {
                await _pump(
                  tester,
                  _model(
                    platform: platform,
                    child: _text(),
                    properties: {
                      layer: _provider(
                        exact: exact,
                        package: 'avatars',
                        resize: resize,
                      ),
                      layer == 'backgroundImage'
                              ? 'onBackgroundImageError'
                              : 'onForegroundImageError':
                          _callback,
                    },
                  ),
                  resources: _resources(),
                );
                final avatar = tester.widget<CircleAvatar>(_finder());
                ImageProvider provider = (layer == 'backgroundImage'
                    ? avatar.backgroundImage
                    : avatar.foregroundImage)!;
                if (resize != null) {
                  final resized = provider as ResizeImage;
                  expect(resized.width, resize['width']);
                  expect(resized.height, resize['height']);
                  expect(
                    resized.policy,
                    resize['policy'] == 'fit'
                        ? ResizeImagePolicy.fit
                        : ResizeImagePolicy.exact,
                  );
                  expect(resized.allowUpscaling, resize['allowUpscaling']);
                  provider = resized.imageProvider;
                }
                expect((provider as MemoryImage).bytes, orderedEquals(_bytes));
                expect(provider.scale, 2);
                final container = tester.widget<AnimatedContainer>(
                  _container(),
                );
                final decoration =
                    (layer == 'backgroundImage'
                            ? container.decoration
                            : container.foregroundDecoration)!
                        as BoxDecoration;
                expect(decoration.image!.fit, BoxFit.cover);
                expect(decoration.shape, BoxShape.circle);
                expect(tester.takeException(), isNull);
              }
            }
            for (final rejected in [false, true]) {
              final missing = rejected ? _provider(bytes: _bytes2) : _missing();
              final rejection = CanvasImageResourceRejection.encodedContent(
                sha256Hex(_bytes2),
              );
              final resources = rejected
                  ? CanvasImageResourceBundle.fromResources(
                      [_resources()[sha256Hex(_bytes)]!],
                      rejections: [rejection],
                    )
                  : _resources();
              await _pump(
                tester,
                _model(
                  platform: platform,
                  child: _text(),
                  properties: {
                    'backgroundImage': _provider(),
                    'foregroundImage': _provider(),
                    layer: missing,
                  },
                ),
                resources: resources,
              );
              final avatar = tester.widget<CircleAvatar>(_finder());
              expect(
                layer == 'backgroundImage'
                    ? avatar.backgroundImage
                    : avatar.foregroundImage,
                isNull,
              );
              expect(
                layer == 'backgroundImage'
                    ? avatar.foregroundImage
                    : avatar.backgroundImage,
                isNotNull,
              );
              expect(
                layer == 'backgroundImage'
                    ? avatar.onBackgroundImageError
                    : avatar.onForegroundImageError,
                isNull,
              );
              expect(
                _labels(tester),
                contains(
                  'Image preview unavailable for $layer: app:assets/avatar.png',
                ),
              );
              expect(
                _labels(tester),
                contains(rejected ? rejection.reason : 'Avatar not declared'),
              );
              expect(find.text('AH'), findsOneWidget);
              expect(tester.takeException(), isNull);
            }
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'CircleAvatar reports corrupt image errors safely with or without callback refs preserving other layer on $platform',
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
        ]);
        for (final callback in [false, true]) {
          for (final layer in ['backgroundImage', 'foregroundImage']) {
            await tester.pumpWidget(const SizedBox());
            PaintingBinding.instance.imageCache.clear();
            final errors = <String>[];
            await _pump(
              tester,
              _model(
                platform: platform,
                child: _text(),
                properties: {
                  'backgroundImage': _provider(),
                  'foregroundImage': _provider(),
                  layer: _provider(bytes: corrupt),
                  if (callback)
                    layer == 'backgroundImage'
                            ? 'onBackgroundImageError'
                            : 'onForegroundImageError':
                        _callback,
                },
              ),
              resources: _resources(corrupt),
              onError: (id, error, stack) => errors.add(id),
            );
            final avatar = tester.widget<CircleAvatar>(_finder());
            final provider = (layer == 'backgroundImage'
                ? avatar.backgroundImage
                : avatar.foregroundImage)!;
            await tester.runAsync(() async {
              await precacheImage(
                provider,
                tester.element(_finder()),
                onError: (_, _) {},
              );
            });
            await tester.pump();
            expect(errors, contains(sha256Hex(corrupt)));
            expect(find.text('AH'), findsOneWidget);
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'CircleAvatar finite radius edits animate and Infinity transitions preserve child TextField state on $platform',
      (tester) async {
        Element? element;
        State? state;
        FocusNode? focus;
        for (final properties in <Map<String, Object?>>[
          {},
          {'radius': _int(45)},
          {'minRadius': _int(20), 'maxRadius': _int(60)},
          {'minRadius': _int(20)},
          {'maxRadius': _infinity},
          {'radius': _infinity},
          {'radius': _double(1.7e308)},
          {'minRadius': _infinity, 'maxRadius': _infinity},
          {'maxRadius': _int(90)},
          {},
        ]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: properties,
              child: _fieldBox(),
            ),
          );
          final field = find.byType(TextField);
          final editable = tester.widget<EditableText>(
            find.byType(EditableText),
          );
          if (element != null) {
            expect(tester.element(field), same(element));
            expect(tester.state(field), same(state));
            expect(editable.focusNode, same(focus));
            expect(editable.controller.text, 'retained avatar state');
          } else {
            editable.controller.text = 'retained avatar state';
          }
          element = tester.element(field);
          state = tester.state(field);
          focus = editable.focusNode;
          expect(tester.getSize(_finder()).isFinite, true);
          expect(tester.takeException(), isNull);
        }
        await _pump(
          tester,
          _model(platform: platform, properties: {'radius': _int(20)}),
        );
        final shell = tester.state(_container());
        await _pump(
          tester,
          _model(platform: platform, properties: {'radius': _int(40)}),
          settle: false,
        );
        expect(tester.state(_container()), same(shell));
        expect(tester.getSize(_finder()).width, 40);
        await tester.pump(const Duration(milliseconds: 100));
        expect(tester.getSize(_finder()).width, inExclusiveRange(40, 80));
        await tester.pump(const Duration(milliseconds: 200));
        expect(tester.getSize(_finder()), const Size.square(80));
        expect(tester.takeException(), isNull);
      },
    );
  }
}

Map<String, Object?> _model({
  String platform = 'windows',
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = platform;
  model['root'] = {
    'id': 'cd8537b9-fdda-4d41-9c09-ff1577332716',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': {
          'id': '80e42b1c-ae0d-428a-8c19-674ad86c68ee',
          'type': 'flutter.widgets.Center',
          'properties': <String, Object?>{},
          'slots': {
            'child': {
              'kind': 'single',
              'child': {
                'id': _id,
                'type': _type,
                'properties': properties,
                'slots': {
                  if (child != null)
                    'child': {'kind': 'single', 'child': child},
                },
              },
            },
          },
        },
      },
    },
  };
  return model;
}

Map<String, Object?> _text() => {
  'id': _childId,
  'type': 'flutter.widgets.Text',
  'properties': {'data': _string('AH')},
  'slots': <String, Object?>{},
};
Map<String, Object?> _icon() => {
  'id': _childId,
  'type': 'flutter.widgets.Icon',
  'properties': {
    'icon': {
      'kind': 'iconData',
      'codePoint': 0xe5f9,
      'fontFamily': 'MaterialIcons',
      'fontPackage': null,
      'matchTextDirection': false,
      'fontFamilyFallback': <String>[],
    },
  },
  'slots': <String, Object?>{},
};
Map<String, Object?> _fieldBox() => {
  'id': _childId,
  'type': 'flutter.widgets.SizedBox',
  'properties': {'width': _int(100), 'height': _int(45)},
  'slots': {
    'child': {
      'kind': 'single',
      'child': {
        'id': _fieldId,
        'type': 'flutter.material.TextField',
        'properties': <String, Object?>{},
        'slots': <String, Object?>{},
      },
    },
  },
};
Map<String, Object?> _int(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _double(double value) => {
  'kind': 'double',
  'value': value,
};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _color(String value) => {'kind': 'color', 'argb': value};
Map<String, Object?> _theme(String token) => {
  'kind': 'themeToken',
  'token': token,
};
Map<String, Object?> _provider({
  Uint8List? bytes,
  bool exact = false,
  String? package,
  Map<String, Object?>? resize,
  Map<String, Object?>? resolution,
}) => {
  'kind': 'imageProvider',
  'value': {
    'kind': exact ? 'exactAsset' : 'asset',
    'assetName': 'assets/avatar.png',
    'packageName': package,
    'exactScale': exact ? 2 : null,
    'resize': resize,
    'resolution':
        resolution ??
        {
          'kind': 'resolved',
          'resourceId': sha256Hex(bytes ?? _bytes),
          'resolvedScale': 2,
        },
  },
};
Map<String, Object?> _resize({
  int? width = 2,
  int? height,
  bool fit = false,
  bool allow = false,
}) => {
  'width': width,
  'height': height,
  'policy': fit ? 'fit' : 'exact',
  'allowUpscaling': allow,
};
Map<String, Object?> _missing() => _provider(
  resolution: {
    'kind': 'unavailable',
    'code': 'missing',
    'reason': 'Avatar not declared',
  },
);
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode _avatar(CanvasModel model) =>
    model.root.slot('body')!.children.single.slot('child')!.children.single;
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
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder _finder() =>
    find.descendant(of: _widget(_id), matching: find.byType(CircleAvatar));
Finder _container() =>
    find.descendant(of: _finder(), matching: find.byType(AnimatedContainer));
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  ThemeData? theme,
  double textScale = 1,
  bool settle = true,
  CanvasImageResourceBundle? resources,
  CanvasImageErrorReporter? onError,
  void Function(CanvasDropResolver?)? onDrop,
  List<String>? selected,
  String? selectedId = _id,
  bool inline = false,
}) async {
  (model['profile'] as Map)['textScaleFactor'] = textScale;
  await tester.pumpWidget(
    MaterialApp(
      theme: theme ?? ThemeData(),
      themeAnimationDuration: Duration.zero,
      home: MediaQuery(
        data: MediaQueryData(textScaler: TextScaler.linear(textScale)),
        child: CanvasDocumentView(
          model: _decode(model),
          imageResources: resources,
          onImageError: onError,
          selectedWidgetId: selectedId,
          inlineTextEditEnabled: inline,
          onInlineTextCommit: (_, _, _) => true,
          onSelected: selected?.add ?? (_) {},
          onDropResolverChanged: onDrop,
        ),
      ),
    ),
  );
  if (settle) {
    await tester.pump(const Duration(milliseconds: 300));
    await tester.pump();
  }
}

Map _centerSlot(Map<String, Object?> model) =>
    (((((model['root'] as Map)['slots'] as Map)['body'] as Map)['child']
                as Map)['slots']
            as Map)['child']
        as Map;
Future<Uint8List> _pixels(
  WidgetTester tester,
  RenderRepaintBoundary boundary,
) async => (await tester.runAsync(() async {
  final image = await boundary.toImage();
  try {
    return (await image.toByteData(
      format: ui.ImageByteFormat.rawRgba,
    ))!.buffer.asUint8List();
  } finally {
    image.dispose();
  }
}))!;

List<int> _interior(Uint8List pixels) => [
  for (var y = 4; y < 36; y++)
    for (var x = 4; x < 36; x++)
      ...pixels.sublist((y * 40 + x) * 4, (y * 40 + x) * 4 + 4),
];

String _labels(WidgetTester tester) {
  final labels = <String>[];
  void visit(SemanticsNode node) {
    if (!node.isMergedIntoParent) labels.add(node.label);
    node.visitChildren((child) {
      visit(child);
      return true;
    });
  }

  visit(
    tester.binding.renderViews.single.owner!.semanticsOwner!.rootSemanticsNode!,
  );
  return labels.join('\n');
}
