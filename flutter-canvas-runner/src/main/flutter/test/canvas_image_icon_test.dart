import 'dart:convert';

import 'package:flutter/foundation.dart';
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

const _type = 'flutter.widgets.ImageIcon';
const _id = '82d9aa26-3fd4-4d42-81d2-677bb3f1d20c';
const _themeId = '1286cb5b-0d04-474c-9f0b-47e7e43d8011';
const _innerId = 'bb462f89-8f67-4a84-b03e-471bcf7f543f';
const _columnId = '07303f47-9cbe-408b-a4fa-4fd9c5e1c765';
const _null = <String, Object?>{'kind': 'null'};
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
final _resourceId = sha256Hex(_bytes);

void main() {
  test(
    'ImageIcon exact nullable required image and three optional SDK fields close the leaf contract',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final block = contract.substring(
        start,
        contract.indexOf('W|flutter.widgets.IndexedSemantics\n', start),
      );
      expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(4));
      expect(
        block,
        contains(
          'P|image|imageProvider,null|1|null|-|imageProvider:imageProvider:v1:asset,exactAsset:package:exactScale:resize(1..16384,exact,fit,allowUpscaling);null:any\n',
        ),
      );
      expect(block, isNot(contains('S|')));
      expect(block, isNot(contains('C|')));
      expect(canvasModelProtocolVersion, 20);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), false);
      final empty = _decode(_model());
      expect(_find(empty.root, _id)!.properties['image']!.kind, 'null');
      expect(_find(empty.root, _id)!.properties['image']!.value, isNull);
      expect(empty.imageResourceIds, isEmpty);
      for (final image in [_null, _provider()]) {
        for (final size in [null, _int(0), _double(17.5)]) {
          for (final color in [
            null,
            _color('0x80123456'),
            _theme('material.colorScheme.primary'),
          ]) {
            for (final label in [null, '', 'Image icon']) {
              final properties = <String, Object?>{
                'image': image,
                'size': ?size,
                'color': ?color,
                if (label != null) 'semanticLabel': _string(label),
              };
              final model = _decode(_model(properties: properties));
              expect(
                _find(model.root, _id)!.properties.keys,
                unorderedEquals(properties.keys),
              );
              expect(
                model.imageResourceIds,
                identical(image, _null) ? isEmpty : {_resourceId},
              );
            }
          }
        }
      }
    },
  );

  test(
    'ImageIcon distinguishes required typed null from omission raw null and malformed provider envelopes',
    () {
      final missing = _model();
      (_leaf(missing)['properties'] as Map).remove('image');
      expect(() => _decode(missing), throwsFormatException);
      for (final image in [
        null,
        {'kind': 'null', 'value': null},
        {'kind': 'null', 'extra': true},
        _string('assets/icon.png'),
        {'kind': 'imageProvider'},
        {'kind': 'imageProvider', 'value': null},
        _bool(false),
      ]) {
        expect(
          () => _decode(_model(properties: {'image': image})),
          throwsFormatException,
        );
      }
      for (final name in ['size', 'color', 'semanticLabel']) {
        for (final value in [null, _null, _bool(true)]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            throwsFormatException,
          );
        }
      }
      for (final name in [
        'key',
        'child',
        'children',
        'useOriginalColors',
        'opacity',
        'applyTextScaling',
        'shadows',
        'fill',
        'weight',
        'grade',
        'opticalSize',
        'imageBuilder',
        'errorBuilder',
        'fit',
        'fontWeight',
        'blendMode',
      ]) {
        expect(
          () => _decode(_model(properties: {name: _bool(true)})),
          throwsFormatException,
        );
      }
      for (final slots in [
        {
          'child': {'kind': 'single', 'child': null},
        },
        {
          'children': {'kind': 'list', 'children': []},
        },
      ]) {
        final model = _model();
        _leaf(model)['slots'] = slots;
        expect(() => _decode(model), throwsFormatException);
      }
    },
  );

  test(
    'ImageIcon uses existing safe number color label provider and Resize domains',
    () {
      for (final size in [
        _int(0),
        _int(9007199254740991),
        _double(0),
        _double(24.5),
      ]) {
        expect(
          () => _decode(_model(properties: {'size': size})),
          returnsNormally,
        );
      }
      for (final size in [_int(-1), _double(-.1), _string('24')]) {
        expect(
          () => _decode(_model(properties: {'size': size})),
          throwsFormatException,
        );
      }
      for (final token in canvasColorSchemeThemeTokens) {
        expect(
          () => _decode(_model(properties: {'color': _theme(token)})),
          returnsNormally,
        );
      }
      expect(
        () => _decode(
          _model(properties: {'color': _theme('material.textTheme.bodyLarge')}),
        ),
        throwsFormatException,
      );
      expect(
        () => _decode(
          _model(properties: {'semanticLabel': _string('x' * 65537)}),
        ),
        throwsFormatException,
      );
      for (final patch in <Map<String, Object?>>[
        {'kind': 'network'},
        {'assetName': '../private.png'},
        {'assetName': 'C:/private.png'},
        {'packageName': 'Invalid-Package'},
        {'exactScale': 2.0},
        {'kind': 'exactAsset', 'exactScale': 0},
        {'resize': _resize(width: 0)},
        {'resize': _resize(width: 16385)},
        {'resize': _resize(width: null)},
        {
          'resize': {..._resize(), 'policy': 'unknown'},
        },
        {
          'resize': {..._resize(), 'allowUpscaling': 'true'},
        },
        {'unknown': true},
        {
          'resolution': {
            'kind': 'unavailable',
            'code': 'missing',
            'reason': '',
          },
        },
      ]) {
        final image = _provider();
        (image['value'] as Map).addAll(patch);
        expect(
          () => _decode(_model(properties: {'image': image})),
          throwsFormatException,
        );
      }
      final encoded = jsonEncode(
        _model(properties: {'size': _double(78912.5)}),
      ).replaceAll('78912.5', '1e309');
      expect(
        () => CanvasModel.decode(Uint8List.fromList(utf8.encode(encoded))),
        throwsFormatException,
      );
    },
  );

  test(
    'ImageIcon resource closure records only resolved providers across all asset and resize forms',
    () {
      for (final exact in [false, true]) {
        for (final package in [null, 'icons_pack']) {
          for (final resize in [
            null,
            _resize(),
            _resize(
              width: null,
              height: 2,
              policy: 'fit',
              allowUpscaling: true,
            ),
          ]) {
            final decoded = _decode(
              _model(
                properties: {
                  'image': _provider(
                    exact: exact,
                    package: package,
                    resize: resize,
                  ),
                },
              ),
            );
            final provider =
                _find(decoded.root, _id)!.properties['image']!.value
                    as CanvasImageProviderValue;
            expect(provider.providerKind, exact ? 'exactAsset' : 'asset');
            expect(provider.packageName, package);
            expect(provider.exactScale, exact ? 2 : null);
            expect(decoded.imageResourceIds, {_resourceId});
          }
        }
      }
      final unavailable = _provider(
        resolution: {
          'kind': 'unavailable',
          'code': 'missing',
          'reason': 'The declared image is absent',
        },
      );
      expect(
        _decode(_model(properties: {'image': unavailable})).imageResourceIds,
        isEmpty,
      );
    },
  );

  test(
    'ImageIcon is an ordinary leaf source accepted by empty legal slots with no asset prerequisite',
    () {
      for (final parent in [
        (type: 'flutter.widgets.Column', slot: 'children'),
        (type: 'flutter.widgets.Center', slot: 'child'),
        (type: 'flutter.widgets.Stack', slot: 'children'),
        (type: 'flutter.widgets.Visibility', slot: 'replacement'),
      ]) {
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: parent.type,
            slotName: parent.slot,
            currentChildCount: 0,
            insertionIndex: 0,
            source: _source(),
          ),
          true,
        );
      }
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: 'flutter.widgets.IconTheme',
          childWidgetType: _type,
        ),
        true,
      );
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: 'flutter.widgets.Expanded',
          childWidgetType: _type,
        ),
        true,
      );
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'ImageIcon explicit null uses actual SDK empty square with no resource placeholder or diagnostic on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final size in [null, 0, 39]) {
            final errors = <Object>[];
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: {
                  if (size != null) 'size': _int(size),
                  'semanticLabel': _string('Empty icon'),
                },
              ),
              onError: (id, error, stack) => errors.add(error),
            );
            final icon = tester.widget<ImageIcon>(_imageIcon());
            expect(icon.image, isNull);
            expect(icon.size, size?.toDouble());
            expect(icon.color, isNull);
            expect(
              find.descendant(of: _widget(_id), matching: find.byType(Image)),
              findsNothing,
            );
            expect(
              tester.getSize(_imageIcon()),
              Size.square((size ?? 24).toDouble()),
            );
            expect(
              _labels(tester).any((label) => label.contains('Empty icon')),
              size != 0,
              reason:
                  'The actual SDK culls zero-area semantics; the Designer handle remains available separately',
            );
            expect(
              _labels(tester).any((label) => label.contains('unavailable')),
              false,
            );
            expect(errors, isEmpty);
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'ImageIcon actual SDK image forwards asset exact package resize binding and scaleDown semantics on $platform',
      (tester) async {
        for (final exact in [false, true]) {
          for (final package in [null, 'icons_pack']) {
            for (final resize in [
              null,
              _resize(),
              _resize(
                width: null,
                height: 3,
                policy: 'fit',
                allowUpscaling: true,
              ),
            ]) {
              await _pump(
                tester,
                _model(
                  platform: platform,
                  properties: {
                    'image': _provider(
                      exact: exact,
                      package: package,
                      resize: resize,
                    ),
                    'size': _double(41.5),
                    'semanticLabel': _string('Image icon'),
                  },
                ),
                resources: _resources(),
              );
              final icon = tester.widget<ImageIcon>(_imageIcon());
              final image = tester.widget<Image>(_image());
              expect(image.image, same(icon.image));
              ImageProvider provider = image.image;
              if (resize != null) {
                expect(provider, isA<ResizeImage>());
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
              expect(image.width, 41.5);
              expect(image.height, 41.5);
              expect(image.fit, BoxFit.scaleDown);
              expect(image.excludeFromSemantics, true);
              expect(image.semanticLabel, isNull);
              expect(image.errorBuilder, isNull);
              expect(
                image.color,
                IconTheme.of(tester.element(_imageIcon())).color,
              );
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );

    testWidgets(
      'ImageIcon inherits IconTheme direct or merge size tint opacity but not text scaling shadows or axes on $platform',
      (tester) async {
        for (final merge in [false, true]) {
          for (final local in [false, true]) {
            for (final explicitNull in [false, true]) {
              final properties = <String, Object?>{
                'image': explicitNull ? _null : _provider(),
                if (local) 'size': _int(17),
                if (local) 'color': _color('0x8013579B'),
              };
              final model = _model(platform: platform, properties: properties);
              final leaf = _leaf(model);
              _centerSlot(model)['child'] = _themeNode(_themeId, {
                'size': _int(37),
                'color': _color('0xCC2468AC'),
                'opacity': _double(.5),
                'applyTextScaling': _bool(true),
                'fill': _double(.8),
                'weight': _double(620),
                'grade': _double(7),
                'opticalSize': _double(48),
                'shadows': {
                  'kind': 'shadowList',
                  'items': [
                    {
                      'id': '77777777-7777-4777-8777-777777777777',
                      'color': {'kind': 'literal', 'argb': '0xFFFFFFFF'},
                      'offsetX': 3.0,
                      'offsetY': 2.0,
                      'blurRadius': 4.0,
                    },
                  ],
                },
              }, _themeNode(_innerId, {'merge': _bool(merge)}, leaf));
              (model['profile'] as Map)['textScaleFactor'] = 3;
              await _pump(tester, model, resources: _resources());
              final effective = IconTheme.of(tester.element(_imageIcon()));
              final size = local
                  ? 17.0
                  : merge
                  ? 37.0
                  : 24.0;
              expect(tester.getSize(_imageIcon()), Size.square(size));
              if (!explicitNull) {
                final image = tester.widget<Image>(_image());
                final color = local
                    ? const Color(0x8013579B)
                    : effective.color!;
                expect(image.color, _attenuate(color, effective.opacity!));
                expect(image.opacity, isNull);
                expect(
                  find.descendant(
                    of: _widget(_id),
                    matching: find.byType(RichText),
                  ),
                  findsNothing,
                );
                expect(
                  find.descendant(
                    of: _widget(_id),
                    matching: find.byType(DecoratedBox),
                  ),
                  findsNothing,
                );
              }
              expect(tester.takeException(), isNull);
            }
          }
        }
      },
    );

    testWidgets(
      'ImageIcon semantic local colors and negative or excessive inherited opacity follow actual SDK on $platform',
      (tester) async {
        for (final opacity in [-2.0, 0.0, .25, 1.0, 4.0]) {
          for (final semantic in [false, true]) {
            final model = _model(
              platform: platform,
              properties: {
                'image': _provider(),
                'color': semantic
                    ? _theme('material.colorScheme.primary')
                    : _color('0x80123456'),
              },
            );
            _centerSlot(model)['child'] = _themeNode(_themeId, {
              'opacity': _double(opacity),
            }, _leaf(model));
            await _pump(tester, model, resources: _resources());
            final color = semantic
                ? Theme.of(tester.element(_imageIcon())).colorScheme.primary
                : const Color(0x80123456);
            expect(
              tester.widget<Image>(_image()).color,
              _attenuate(color, opacity.clamp(0.0, 1.0)),
            );
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'ImageIcon null provider asset replacement and null again retain model identity and fresh bindings on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final bytes in [null, _bytes, _bytes2, null, _bytes]) {
            final image = bytes == null
                ? _null
                : _provider(
                    asset: identical(bytes, _bytes)
                        ? 'assets/icon.png'
                        : 'assets/second.png',
                    resolution: {
                      'kind': 'resolved',
                      'resourceId': sha256Hex(bytes),
                      'resolvedScale': 1,
                    },
                  );
            final model = _model(
              platform: platform,
              properties: {
                'image': image,
                'size': _int(33),
                'semanticLabel': _string('Persistent icon'),
              },
            );
            await _pump(
              tester,
              model,
              resources: bytes == null
                  ? CanvasImageResourceBundle.empty
                  : _resources(bytes),
            );
            expect(_widget(_id), findsOneWidget);
            expect(tester.getSize(_imageIcon()), const Size.square(33));
            if (bytes == null) {
              expect(tester.widget<ImageIcon>(_imageIcon()).image, isNull);
              expect(_image(), findsNothing);
            } else {
              expect(
                (tester.widget<ImageIcon>(_imageIcon()).image as MemoryImage)
                    .bytes,
                orderedEquals(bytes),
              );
            }
            expect(
              _labels(tester).any((label) => label.contains('Persistent icon')),
              true,
            );
            expect(
              _labels(tester).any((label) => label.contains('unavailable')),
              false,
            );
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'ImageIcon unavailable missing and rejected resources use real nonnull SDK placeholder and concrete diagnostics on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final failure in [
            'unavailable',
            'missing',
            'encoded',
            'resize',
          ]) {
            final provider = _provider(
              package: 'icons_pack',
              asset: 'assets/missing.png',
              resize: _resize(),
              resolution: failure == 'unavailable'
                  ? {
                      'kind': 'unavailable',
                      'code': 'missing',
                      'reason': 'Declared icon file does not exist',
                    }
                  : null,
            );
            final rejection = failure == 'encoded'
                ? CanvasImageResourceRejection.encodedContent(_resourceId)
                : failure == 'resize'
                ? CanvasImageResourceRejection.invalidResizeTarget(_resourceId)
                : null;
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: {
                  'image': provider,
                  'size': _int(40),
                  'semanticLabel': _string('Unavailable icon'),
                },
              ),
              resources: CanvasImageResourceBundle.fromResources(
                [],
                rejections: [?rejection],
              ),
            );
            final icon = tester.widget<ImageIcon>(_imageIcon());
            expect(
              icon.image,
              isA<MemoryImage>(),
              reason:
                  'Unavailable provider is not explicit null and must not be resized',
            );
            expect(
              (icon.image as MemoryImage).bytes,
              orderedEquals(_bytes),
              reason: 'Uses the same reviewed safe placeholder as Image',
            );
            expect((icon.image as MemoryImage).scale, 1);
            expect(tester.widget<Image>(_image()).fit, BoxFit.scaleDown);
            expect(tester.getSize(_imageIcon()), const Size.square(40));
            final label = _labels(tester).join(' ');
            expect(
              label,
              contains(
                'Image preview unavailable for package:icons_pack:assets/missing.png',
              ),
            );
            expect(label, contains('Status ${rejection?.code ?? 'missing'}'));
            expect(
              label,
              contains(
                failure == 'unavailable'
                    ? 'Declared icon file does not exist'
                    : rejection?.reason ??
                          'the content-addressed resource is not bound to this revision',
              ),
            );
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'ImageIcon nonzero null and loaded icons stay selectable and never enter the Text F2 editor on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final empty in [false, true]) {
            final selected = <String>[];
            await tester.pumpWidget(
              CanvasModelApp(
                model: _decode(
                  _model(
                    platform: platform,
                    properties: {
                      'image': empty ? _null : _provider(),
                      'semanticLabel': _string('Select image icon'),
                    },
                  ),
                ),
                imageResources: _resources(),
                selectedWidgetId: _id,
                onSelected: selected.add,
                inlineTextEditEnabled: true,
                onInlineTextCommit: (_, _, _) =>
                    throw StateError('ImageIcon must not commit text'),
              ),
            );
            await tester.pump();
            await tester.tap(_widget(_id));
            await tester.pump();
            expect(selected, contains(_id));
            await tester.sendKeyEvent(LogicalKeyboardKey.f2);
            await tester.pump();
            expect(find.byType(TextField), findsNothing);
            expect(
              _labels(
                tester,
              ).any((label) => label.contains('Select image icon')),
              true,
            );
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'ImageIcon zero-size still has genuine leaf selection target and ordinary drop insertion on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(platform: platform, properties: {'size': _int(0)}),
        );
        expect(tester.getSize(_imageIcon()), Size.zero);
        expect(
          find.byKey(const ValueKey('canvas-zero-size-widget-target-$_id')),
          findsOneWidget,
        );
        CanvasDropResolver? drop;
        final model = _model(platform: platform);
        _centerSlot(model)['child'] = {
          'id': _columnId,
          'type': 'flutter.widgets.SizedBox',
          'properties': {'width': _int(80), 'height': _int(70)},
          'slots': {
            'child': {
              'kind': 'single',
              'child': {
                'id': _innerId,
                'type': 'flutter.widgets.Column',
                'properties': <String, Object?>{},
                'slots': {
                  'children': {'kind': 'list', 'children': []},
                },
              },
            },
          },
        };
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(model),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => drop = value,
          ),
        );
        await tester.pump();
        final surface = tester.getRect(find.byType(CanvasDocumentView));
        final point = tester.getCenter(_widget(_innerId));
        final target = drop!(
          ((point.dx - surface.left) / surface.width * 1000000).round(),
          ((point.dy - surface.top) / surface.height * 1000000).round(),
          _source(),
        );
        expect(target?.parentWidgetId, _innerId);
        expect(target?.slotName, 'children');
        expect(target?.insertionIndex, 0);
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'ImageIcon binds actual decoded pixels then removes the image stream on explicit null for both Canvas profiles',
    (tester) async {
      for (final platform in ['windows', 'web']) {
        await _pump(
          tester,
          _model(
            platform: platform,
            properties: {
              'image': _provider(
                resolution: {
                  'kind': 'resolved',
                  'resourceId': sha256Hex(_bytes2),
                  'resolvedScale': 2,
                },
              ),
              'size': _int(36),
              'color': _color('0x80123456'),
            },
          ),
          resources: _resources(_bytes2),
        );
        final icon = tester.widget<ImageIcon>(_imageIcon());
        final context = tester.element(_imageIcon());
        await tester.runAsync(() => precacheImage(icon.image!, context));
        await tester.pump();
        final raw = tester.widget<RawImage>(
          find.descendant(of: _widget(_id), matching: find.byType(RawImage)),
        );
        expect(raw.image, isNotNull);
        expect(raw.image!.width, 4);
        expect(raw.image!.height, 4);
        expect(raw.scale, 2);
        expect(raw.fit, BoxFit.scaleDown);
        expect(raw.color, const Color(0x80123456));
        expect(raw.colorBlendMode ?? BlendMode.srcIn, BlendMode.srcIn);
        expect(tester.getSize(_imageIcon()), const Size.square(36));
        await _pump(
          tester,
          _model(platform: platform, properties: {'size': _int(36)}),
        );
        expect(
          find.descendant(of: _widget(_id), matching: find.byType(RawImage)),
          findsNothing,
        );
        expect(tester.widget<ImageIcon>(_imageIcon()).image, isNull);
        expect(tester.takeException(), isNull);
      }
    },
  );

  testWidgets(
    'SDK ImageIcon no ambient theme and direct empty theme use black tint fallback and a single semantic label',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        for (final theme in [false, true]) {
          final icon = ImageIcon(
            MemoryImage(_bytes),
            semanticLabel: 'Raw SDK image icon',
          );
          await tester.pumpWidget(
            Directionality(
              textDirection: TextDirection.ltr,
              child: Center(
                child: theme
                    ? IconTheme(data: const IconThemeData(), child: icon)
                    : icon,
              ),
            ),
          );
          await tester.pump();
          final image = tester.widget<Image>(find.byType(Image));
          expect(image.color, Colors.black);
          expect(image.fit, BoxFit.scaleDown);
          expect(image.excludeFromSemantics, true);
          expect(tester.getSize(find.byType(ImageIcon)), const Size.square(24));
          expect(
            _labels(tester).where((label) => label == 'Raw SDK image icon'),
            hasLength(1),
          );
          expect(tester.takeException(), isNull);
        }
      } finally {
        semantics.dispose();
      }
    },
  );
}

Map<String, Object?> _provider({
  bool exact = false,
  String? package,
  String asset = 'assets/icon.png',
  Map<String, Object?>? resize,
  Map<String, Object?>? resolution,
}) => {
  'kind': 'imageProvider',
  'value': {
    'kind': exact ? 'exactAsset' : 'asset',
    'assetName': asset,
    'packageName': package,
    'exactScale': exact ? 2 : null,
    'resize': resize,
    'resolution':
        resolution ??
        {'kind': 'resolved', 'resourceId': _resourceId, 'resolvedScale': 2},
  },
};
Map<String, Object?> _resize({
  int? width = 2,
  int? height,
  String policy = 'exact',
  bool allowUpscaling = false,
}) => {
  'width': width,
  'height': height,
  'policy': policy,
  'allowUpscaling': allowUpscaling,
};
Map<String, Object?> _model({
  String platform = 'windows',
  Map<String, Object?> properties = const {},
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
                'properties': {'image': _null, ...properties},
                'slots': <String, Object?>{},
              },
            },
          },
        },
      },
    },
  };
  return model;
}

Map _centerSlot(Map<String, Object?> model) =>
    (((((model['root'] as Map)['slots'] as Map)['body'] as Map)['child']
                as Map)['slots']
            as Map)['child']
        as Map;
Map<String, Object?> _leaf(Map<String, Object?> model) =>
    _centerSlot(model)['child'] as Map<String, Object?>;
Map<String, Object?> _themeNode(
  String id,
  Map<String, Object?> properties,
  Map<String, Object?> child,
) => {
  'id': id,
  'type': 'flutter.widgets.IconTheme',
  'properties': {'merge': _bool(false), ...properties},
  'slots': {
    'child': {'kind': 'single', 'child': child},
  },
};
Map<String, Object?> _int(int value) => {'kind': 'integer', 'value': value};
Map<String, Object?> _double(double value) => {
  'kind': 'double',
  'value': value,
};
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _color(String value) => {'kind': 'color', 'argb': value};
Map<String, Object?> _theme(String token) => {
  'kind': 'themeToken',
  'token': token,
};
CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
CanvasNode? _find(CanvasNode node, String id) {
  if (node.id == id) return node;
  for (final slot in node.slots.values) {
    for (final child in slot.children) {
      final found = _find(child, id);
      if (found != null) return found;
    }
  }
  return null;
}

CanvasImageResourceBundle _resources([Uint8List? bytes]) =>
    CanvasImageResourceBundle.fromResources([
      CanvasImageResource(
        resourceId: sha256Hex(bytes ?? _bytes),
        mediaType: 'image/png',
        pixelWidth: identical(bytes, _bytes2) ? 4 : 8,
        pixelHeight: identical(bytes, _bytes2) ? 4 : 8,
        encodedBytes: bytes ?? _bytes,
      ),
    ]);
Future<void> _pump(
  WidgetTester tester,
  Map<String, Object?> model, {
  CanvasImageResourceBundle? resources,
  CanvasImageErrorReporter? onError,
}) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: _decode(model),
      imageResources: resources,
      selectedWidgetId: null,
      onSelected: (_) {},
      onImageError: onError,
    ),
  );
  await tester.pump();
}

Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder _imageIcon() =>
    find.descendant(of: _widget(_id), matching: find.byType(ImageIcon));
Finder _image() =>
    find.descendant(of: _widget(_id), matching: find.byType(Image));
CanvasPaletteDragSource _source() => CanvasPaletteDragSource(
  token: _type,
  widgetType: _type,
  traits: canvasWidgetTraitsForType(_type),
);
Color _attenuate(Color color, double opacity) => opacity == 1
    ? color
    : Color.fromARGB(
        (color.a * opacity * 255).round(),
        (color.r * 255).round(),
        (color.g * 255).round(),
        (color.b * 255).round(),
      );
Set<String> _labels(WidgetTester tester) {
  final result = <String>{};
  void visit(SemanticsNode node) {
    if (!node.isMergedIntoParent) result.add(node.label);
    node.visitChildren((child) {
      visit(child);
      return true;
    });
  }

  visit(
    tester.binding.renderViews.single.owner!.semanticsOwner!.rootSemanticsNode!,
  );
  return result;
}
