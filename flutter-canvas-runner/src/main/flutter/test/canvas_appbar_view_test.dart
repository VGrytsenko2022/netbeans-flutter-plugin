import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

void main() {
  testWidgets(
    'renders the complete flattened AppBar contract through the real widget',
    (tester) async {
      final model = CanvasModel.decode(fixture.appBarModelBytesForViewTest());
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: fixture.appBarWidgetIdForViewTest,
          onSelected: (_) {},
        ),
      );

      final scaffold = tester.widget<Scaffold>(
        find
            .descendant(
              of: find.byKey(ValueKey('canvas-widget-${model.root.id}')),
              matching: find.byType(Scaffold),
            )
            .first,
      );
      expect(scaffold.appBar, isA<PreferredSizeWidget>());
      expect(
        scaffold.appBar!.preferredSize.height,
        120,
        reason: '64 toolbar + the nested default-height AppBar.bottom',
      );

      final appBarFinder = find
          .descendant(
            of: find.byKey(
              const ValueKey(
                'canvas-widget-${fixture.appBarWidgetIdForViewTest}',
              ),
            ),
            matching: find.byType(AppBar),
          )
          .first;
      final appBar = tester.widget<AppBar>(appBarFinder);
      final context = tester.element(appBarFinder);
      final colors = Theme.of(context).colorScheme;

      expect(appBar.leading, isNotNull);
      expect(appBar.title, isNotNull);
      expect(appBar.actions, hasLength(2));
      expect(appBar.flexibleSpace, isNotNull);
      expect(appBar.bottom, isA<PreferredSizeWidget>());
      expect(appBar.automaticallyImplyLeading, isFalse);
      expect(appBar.automaticallyImplyActions, isFalse);
      expect(appBar.elevation, 4);
      expect(appBar.scrolledUnderElevation, 7.5);
      expect(appBar.shadowColor, const Color(0xff010203));
      expect(appBar.surfaceTintColor, colors.surfaceTint);
      expect(appBar.backgroundColor, colors.surfaceContainer);
      expect(appBar.foregroundColor, colors.onSurface);
      expect(appBar.primary, isFalse);
      expect(appBar.centerTitle, isTrue);
      expect(appBar.excludeHeaderSemantics, isTrue);
      expect(appBar.titleSpacing, 13.5);
      expect(appBar.toolbarOpacity, 0.8);
      expect(appBar.bottomOpacity, 0.7);
      expect(appBar.toolbarHeight, 64);
      expect(appBar.leadingWidth, 52);
      expect(appBar.forceMaterialTransparency, isTrue);
      expect(appBar.useDefaultSemanticsOrder, isFalse);
      expect(appBar.clipBehavior, Clip.antiAlias);
      expect(
        appBar.actionsPadding,
        const EdgeInsetsDirectional.fromSTEB(3, 4, 5, 6),
      );
      expect(appBar.animateColor, isTrue);

      final iconTheme = appBar.iconTheme!;
      expect(iconTheme.size, 27);
      expect(iconTheme.fill, 0.25);
      expect(iconTheme.weight, 500);
      expect(iconTheme.grade, -10);
      expect(iconTheme.opticalSize, 28);
      expect(iconTheme.color, colors.primary);
      expect(iconTheme.opacity, 0.65);
      expect(iconTheme.shadows, hasLength(1));
      expect(iconTheme.applyTextScaling, isTrue);
      expect(appBar.actionsIconTheme, iconTheme);

      final toolbarStyle = appBar.toolbarTextStyle!;
      expect(toolbarStyle.inherit, isFalse);
      expect(toolbarStyle.color, const Color(0xff102030));
      expect(toolbarStyle.backgroundColor, const Color(0xffe0d0c0));
      expect(toolbarStyle.fontSize, 18.5);
      expect(toolbarStyle.fontWeight, FontWeight.w600);
      expect(toolbarStyle.fontStyle, FontStyle.italic);
      expect(toolbarStyle.letterSpacing, 1.25);
      expect(toolbarStyle.wordSpacing, 2.5);
      expect(toolbarStyle.textBaseline, TextBaseline.ideographic);
      expect(toolbarStyle.height, 1.4);
      expect(
        toolbarStyle.leadingDistribution,
        TextLeadingDistribution.proportional,
      );
      expect(
        toolbarStyle.locale,
        const Locale.fromSubtags(
          languageCode: 'en',
          scriptCode: 'Latn',
          countryCode: 'GB',
        ),
      );
      expect(
        toolbarStyle.decoration,
        TextDecoration.combine(const [
          TextDecoration.underline,
          TextDecoration.overline,
          TextDecoration.lineThrough,
        ]),
      );
      expect(toolbarStyle.decorationColor, const Color(0xff112233));
      expect(toolbarStyle.decorationStyle, TextDecorationStyle.wavy);
      expect(toolbarStyle.decorationThickness, 2.25);
      expect(toolbarStyle.debugLabel, 'designer text');
      expect(toolbarStyle.fontFamily, 'packages/design_fonts/Inter');
      expect(toolbarStyle.fontFamilyFallback, const [
        'packages/design_fonts/Noto Sans',
        'packages/design_fonts/Noto Color Emoji',
      ]);
      expect(toolbarStyle.overflow, TextOverflow.fade);
      expect(appBar.titleTextStyle, toolbarStyle);

      final shape = appBar.shape! as RoundedRectangleBorder;
      expect(shape.side.color, colors.outline);
      expect(shape.side.width, 2);
      expect(shape.side.style, BorderStyle.solid);
      expect(shape.side.strokeAlign, 12.5);
      expect(
        shape.borderRadius,
        const BorderRadius.only(
          topLeft: Radius.circular(1),
          topRight: Radius.circular(2),
          bottomRight: Radius.circular(3),
          bottomLeft: Radius.circular(4),
        ),
      );

      final overlay = appBar.systemOverlayStyle!;
      expect(overlay.systemNavigationBarColor, const Color(0xff111213));
      expect(overlay.systemNavigationBarDividerColor, colors.outlineVariant);
      expect(overlay.systemNavigationBarIconBrightness, Brightness.dark);
      expect(overlay.systemNavigationBarContrastEnforced, isFalse);
      expect(overlay.statusBarColor, const Color(0xff212223));
      expect(overlay.statusBarBrightness, Brightness.light);
      expect(overlay.statusBarIconBrightness, Brightness.dark);
      expect(overlay.systemStatusBarContrastEnforced, isTrue);

      final notification = ScrollUpdateNotification(
        metrics: FixedScrollMetrics(
          minScrollExtent: 0,
          maxScrollExtent: 100,
          pixels: 0,
          viewportDimension: 100,
          axisDirection: AxisDirection.down,
          devicePixelRatio: 1,
        ),
        context: context,
        scrollDelta: 1,
      );
      expect(appBar.notificationPredicate(notification), isTrue);
      expect(
        find.byKey(
          const ValueKey(
            'canvas-selection-outline-${fixture.appBarWidgetIdForViewTest}',
          ),
        ),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'keeps omitted AppBar fields nullable while Flutter theme supplies them',
    (tester) async {
      final model = CanvasModel.decode(_appBarBytes(properties: const {}));
      const inherited = AppBarTheme(
        backgroundColor: Color(0xff123456),
        foregroundColor: Color(0xfff0e0d0),
        elevation: 9,
        iconTheme: IconThemeData(size: 33),
      );
      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData(appBarTheme: inherited),
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        ),
      );

      final appBar = tester.widget<AppBar>(find.byType(AppBar).first);
      expect(appBar.backgroundColor, isNull);
      expect(appBar.foregroundColor, isNull);
      expect(appBar.elevation, isNull);
      expect(appBar.iconTheme, isNull);
      expect(appBar.actionsIconTheme, isNull);
      expect(appBar.toolbarTextStyle, isNull);
      expect(appBar.titleTextStyle, isNull);
      expect(appBar.systemOverlayStyle, isNull);
      expect(appBar.shape, isNull);
      expect(appBar.automaticallyImplyLeading, isTrue);
      expect(appBar.automaticallyImplyActions, isTrue);
      expect(appBar.primary, isTrue);
      expect(appBar.excludeHeaderSemantics, isFalse);
      expect(appBar.toolbarOpacity, 1);
      expect(appBar.bottomOpacity, 1);
      expect(appBar.forceMaterialTransparency, isFalse);
      expect(appBar.useDefaultSemanticsOrder, isTrue);
      expect(appBar.animateColor, isFalse);
    },
  );

  for (final direction in TextDirection.values) {
    testWidgets('resolves source-aware AppBar slots in ${direction.name}', (
      tester,
    ) async {
      CanvasDropResolver? resolver;
      final model = CanvasModel.decode(_emptyAppBarBytes());
      await tester.pumpWidget(
        MaterialApp(
          home: Directionality(
            textDirection: direction,
            child: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
              onDropResolverChanged: (value) => resolver = value,
            ),
          ),
        ),
      );
      await tester.pump();

      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final appBarRect = tester.getRect(
        find.byKey(
          const ValueKey('canvas-widget-${fixture.appBarWidgetIdForViewTest}'),
        ),
      );
      final textSource = CanvasPaletteDragSource(
        token: 'text',
        widgetType: 'flutter.widgets.Text',
        traits: const {},
      );
      final appBarSource = CanvasPaletteDragSource(
        token: 'appbar',
        widgetType: 'flutter.material.AppBar',
        traits: const {canvasPreferredSizeWidgetTrait},
      );

      CanvasDropTarget? resolve(Offset point, CanvasPaletteDragSource source) =>
          resolver!(
            ((point.dx - surface.left) / surface.width * 1000000).round(),
            ((point.dy - surface.top) / surface.height * 1000000).round(),
            source,
          );

      final logicalLeading = Offset(
        direction == TextDirection.ltr
            ? appBarRect.left + 8
            : appBarRect.right - 8,
        appBarRect.top + 12,
      );
      final logicalTrailing = Offset(
        direction == TextDirection.ltr
            ? appBarRect.right - 8
            : appBarRect.left + 8,
        appBarRect.top + 12,
      );
      expect(resolve(logicalLeading, textSource)?.slotName, 'leading');
      expect(
        resolve(
          appBarRect.topCenter + const Offset(0, 12),
          textSource,
        )?.slotName,
        'title',
      );
      expect(resolve(logicalTrailing, textSource)?.slotName, 'actions');

      final bottomPoint = Offset(appBarRect.center.dx, appBarRect.bottom - 2);
      expect(
        resolve(bottomPoint, textSource)?.slotName,
        'flexibleSpace',
        reason: 'Text cannot enter the PreferredSizeWidget-only bottom slot',
      );
      expect(resolve(bottomPoint, appBarSource)?.slotName, 'bottom');
      expect(resolve(bottomPoint, appBarSource)?.insertionIndex, 0);
    });
  }
}

Uint8List _appBarBytes({required Map<String, Object?> properties}) {
  final json =
      jsonDecode(utf8.decode(fixture.appBarModelBytesForViewTest()))
          as Map<String, Object?>;
  final appBar = _appBarNode(json);
  appBar['properties'] = properties;
  return Uint8List.fromList(utf8.encode(jsonEncode(json)));
}

Uint8List _emptyAppBarBytes() {
  final json =
      jsonDecode(utf8.decode(fixture.appBarModelBytesForViewTest()))
          as Map<String, Object?>;
  final appBar = _appBarNode(json);
  appBar['properties'] = <String, Object?>{};
  final slots = appBar['slots']! as Map<String, Object?>;
  for (final name in const ['leading', 'title', 'flexibleSpace', 'bottom']) {
    (slots[name]! as Map<String, Object?>)['child'] = null;
  }
  (slots['actions']! as Map<String, Object?>)['children'] = <Object?>[];
  return Uint8List.fromList(utf8.encode(jsonEncode(json)));
}

Map<String, Object?> _appBarNode(Map<String, Object?> model) {
  final root = model['root']! as Map<String, Object?>;
  final slots = root['slots']! as Map<String, Object?>;
  final appBarSlot = slots['appBar']! as Map<String, Object?>;
  return appBarSlot['child']! as Map<String, Object?>;
}
