import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

void main() {
  test('models the reviewed multi-slot CORE_V1 insertion matrix', () {
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
      isNull,
    );
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Padding'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Center'), const [
      canvasEmptyChildDropSlot,
    ]);
    expect(canvasDropSlotsForWidgetType('flutter.widgets.Text'), isEmpty);
    expect(
      canvasEmptyChildDropSlot.accepts(currentChildCount: 0, insertionIndex: 1),
      isFalse,
    );
    expect(canvasChildrenAppendDropSlot.modelSlotKind, 'list');
    expect(canvasScaffoldBodyDropSlot.modelSlotKind, 'single');
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
      expect(deleteCalls, 1);

      await tester.sendKeyDownEvent(LogicalKeyboardKey.shiftLeft);
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      await tester.sendKeyUpEvent(LogicalKeyboardKey.shiftLeft);
      expect(deleteCalls, 1);

      FocusManager.instance.primaryFocus?.unfocus();
      await tester.pump();
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(deleteCalls, 1, reason: 'an unfocused Canvas must stay inert');

      await tester.tap(find.byKey(ValueKey('canvas-widget-$selected')));
      await tester.pump();
      expect(selections, hasLength(1));
      expect(model.widgetIds, contains(selections.single));
      await tester.sendKeyEvent(LogicalKeyboardKey.delete);
      expect(deleteCalls, 2, reason: 'widget tap restores Canvas focus');
    },
  );

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
}

Map<String, Object?> _modelJsonForView() =>
    jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
        as Map<String, Object?>;

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
