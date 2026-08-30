import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

void main() {
  testWidgets(
    'renders every ElevatedButton state and common ButtonStyle assembler',
    (tester) async {
      final model = CanvasModel.decode(
        fixture.elevatedButtonModelBytesForViewTest(),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: fixture.elevatedButtonWidgetIdForViewTest,
          onSelected: (_) {},
        ),
      );

      final finder = _buttonFinder();
      final button = tester.widget<ElevatedButton>(finder);
      final context = tester.element(finder);
      final colors = Theme.of(context).colorScheme;
      final properties = _buttonNode(model).properties;

      expect(button.enabled, isTrue);
      expect(button.onPressed, isNotNull);
      expect(button.onLongPress, isNotNull);
      expect(button.onHover, isNotNull);
      expect(button.onFocusChange, isNotNull);
      expect(button.autofocus, isTrue);
      expect(button.clipBehavior, Clip.antiAlias);
      expect(find.text('Run safely'), findsOneWidget);
      expect(
        find.byKey(
          const ValueKey(
            'canvas-selection-outline-'
            '${fixture.elevatedButtonWidgetIdForViewTest}',
          ),
        ),
        findsOneWidget,
      );

      button.onPressed!();
      button.onLongPress!();
      button.onHover!(true);
      button.onFocusChange!(true);

      final style = button.style!;
      expect(
        style.visualDensity,
        const VisualDensity(horizontal: -2, vertical: 1.5),
      );
      expect(style.tapTargetSize, MaterialTapTargetSize.shrinkWrap);
      expect(style.animationDuration, const Duration(milliseconds: 275));
      expect(style.enableFeedback, isFalse);
      expect(style.alignment, const AlignmentDirectional(0.25, -0.5));
      expect(style.splashFactory, same(InkSplash.splashFactory));

      const states = <(String, Set<WidgetState>)>[
        ('style', <WidgetState>{}),
        ('styleDisabled', <WidgetState>{WidgetState.disabled}),
        ('stylePressed', <WidgetState>{WidgetState.pressed}),
        ('styleHovered', <WidgetState>{WidgetState.hovered}),
        ('styleFocused', <WidgetState>{WidgetState.focused}),
      ];
      const expectedCursors = [
        SystemMouseCursors.none,
        SystemMouseCursors.resizeColumn,
        SystemMouseCursors.resizeRow,
        SystemMouseCursors.zoomIn,
        SystemMouseCursors.zoomOut,
      ];
      for (var index = 0; index < states.length; index++) {
        final (prefix, widgetStates) = states[index];
        expect(
          style.backgroundColor!.resolve(widgetStates),
          Color(properties['${prefix}BackgroundColor']!.value as int),
          reason: '$prefix background',
        );
        expect(
          style.foregroundColor!.resolve(widgetStates),
          colors.onPrimary,
          reason: '$prefix foreground theme token',
        );
        expect(
          style.overlayColor!.resolve(widgetStates),
          Color(properties['${prefix}OverlayColor']!.value as int),
        );
        expect(style.shadowColor!.resolve(widgetStates), colors.shadow);
        expect(
          style.surfaceTintColor!.resolve(widgetStates),
          colors.surfaceTint,
        );
        expect(
          style.elevation!.resolve(widgetStates),
          1.5 + index,
          reason: '$prefix elevation',
        );
        expect(
          style.minimumSize!.resolve(widgetStates),
          Size(40.0 + index, 24.0 + index),
        );
        expect(
          style.fixedSize!.resolve(widgetStates),
          Size(96.0 + index, 44.0 + index),
        );
        expect(
          style.maximumSize!.resolve(widgetStates),
          Size(240.0 + index, 80.0 + index),
        );
        expect(
          style.iconColor!.resolve(widgetStates),
          Color(properties['${prefix}IconColor']!.value as int),
        );
        expect(style.iconSize!.resolve(widgetStates), 18.0 + index);
        expect(
          style.mouseCursor!.resolve(widgetStates),
          expectedCursors[index],
        );

        final padding = style.padding!.resolve(widgetStates)!;
        if (index.isEven) {
          expect(
            padding,
            EdgeInsets.fromLTRB(
              8.0 + index,
              9.0 + index,
              10.0 + index,
              11.0 + index,
            ),
          );
        } else {
          expect(
            padding,
            EdgeInsetsDirectional.fromSTEB(
              8.0 + index,
              9.0 + index,
              10.0 + index,
              11.0 + index,
            ),
          );
        }

        final side = style.side!.resolve(widgetStates)!;
        expect(side.color, colors.outline);
        expect(side.width, 1.0 + index / 4);
        expect(side.style, BorderStyle.solid);
        expect(side.strokeAlign, -1.0 + index / 2);
        final shape = style.shape!.resolve(widgetStates)!;
        expect(shape, isA<RoundedSuperellipseBorder>());
        expect(
          (shape as RoundedSuperellipseBorder).borderRadius,
          BorderRadius.only(
            topLeft: Radius.circular(1.0 + index),
            topRight: Radius.circular(2.0 + index),
            bottomRight: Radius.circular(3.0 + index),
            bottomLeft: Radius.circular(4.0 + index),
          ),
        );

        final textStyle = style.textStyle!.resolve(widgetStates)!;
        expect(textStyle.inherit, isTrue);
        expect(textStyle.backgroundColor, isNull);
        expect(
          textStyle.background?.color.toARGB32(),
          properties['${prefix}TextBackgroundColor']!.value,
        );
        expect(textStyle.fontSize, 14.0 + index);
        expect(textStyle.fontWeight, FontWeight.w600);
        expect(textStyle.fontStyle, FontStyle.italic);
        expect(textStyle.letterSpacing, 0.25 + index);
        expect(textStyle.wordSpacing, 0.5 + index);
        expect(textStyle.textBaseline, TextBaseline.alphabetic);
        expect(textStyle.height, 1.2 + index / 10);
        expect(textStyle.leadingDistribution, TextLeadingDistribution.even);
        expect(
          textStyle.locale,
          const Locale.fromSubtags(
            languageCode: 'en',
            scriptCode: 'Latn',
            countryCode: 'US',
          ),
        );
        expect(textStyle.shadows, hasLength(1));
        expect(textStyle.fontFeatures, hasLength(1));
        expect(textStyle.fontVariations, hasLength(1));
        expect(
          textStyle.decoration,
          TextDecoration.combine(const [
            TextDecoration.underline,
            TextDecoration.lineThrough,
          ]),
        );
        expect(textStyle.decorationColor, colors.error);
        expect(textStyle.decorationStyle, TextDecorationStyle.wavy);
        expect(textStyle.decorationThickness, 1.25 + index);
        expect(textStyle.fontFamily, 'packages/design_fonts/Inter');
        expect(textStyle.fontFamilyFallback, const [
          'packages/design_fonts/Noto Sans',
          'packages/design_fonts/Noto Color Emoji',
        ]);
        expect(textStyle.overflow, TextOverflow.ellipsis);
      }

      expect(
        style.backgroundColor!.resolve(const {
          WidgetState.pressed,
          WidgetState.hovered,
        }),
        Color(properties['stylePressedBackgroundColor']!.value as int),
        reason: 'canonical pressed entry precedes hovered',
      );
    },
  );

  testWidgets('preserves disabled null fallback into ElevatedButtonTheme', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'enabled': {'kind': 'boolean', 'value': false},
          'onPressed': {'kind': 'callbackPresence'},
          'onLongPress': {'kind': 'callbackPresence'},
          'styleBackgroundColor': {'kind': 'color', 'argb': '0xFF102030'},
        },
      ),
    );
    final inherited = ButtonStyle(
      backgroundColor: WidgetStateProperty.fromMap(const {
        WidgetState.disabled: Color(0xffa0b0c0),
        WidgetState.any: Color(0xffd0e0f0),
      }),
    );
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData(
          elevatedButtonTheme: ElevatedButtonThemeData(style: inherited),
        ),
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );
    await tester.pumpAndSettle();

    final button = tester.widget<ElevatedButton>(_buttonFinder());
    expect(button.enabled, isFalse);
    expect(button.onPressed, isNull);
    expect(button.onLongPress, isNull);
    expect(
      button.style!.backgroundColor!.resolve(const {WidgetState.disabled}),
      isNull,
      reason: 'local any value deliberately maps disabled to exact null',
    );
    expect(
      button.style!.backgroundColor!.resolve(const {}),
      const Color(0xff102030),
    );
    final material = tester.widget<Material>(
      find
          .descendant(of: _buttonFinder(), matching: find.byType(Material))
          .first,
    );
    expect(material.color, const Color(0xffa0b0c0));
  });

  testWidgets(
    'inherits omitted pressed compound leaves from the enabled base in the real button',
    (tester) async {
      final model = CanvasModel.decode(
        fixture.elevatedButtonModelBytesForViewTest(
          properties: const {
            'enabled': {'kind': 'boolean', 'value': true},
            'onPressed': {'kind': 'callbackPresence'},
            'styleMinimumWidth': {'kind': 'integer', 'value': 120},
            'styleMinimumHeight': {'kind': 'integer', 'value': 44},
            'stylePressedMinimumWidth': {'kind': 'integer', 'value': 180},
            'styleSideColor': {'kind': 'color', 'argb': '0xFF123456'},
            'styleSideWidth': {'kind': 'double', 'value': 2.0},
            'styleSideStyle': {
              'kind': 'enum',
              'type': 'BorderStyle',
              'value': 'solid',
            },
            'styleSideStrokeAlign': {'kind': 'double', 'value': -0.5},
            'stylePressedSideWidth': {'kind': 'double', 'value': 6.0},
            'styleShapeKind': {'kind': 'string', 'value': 'roundedRectangle'},
            'styleShapeRadiusTopLeft': {'kind': 'double', 'value': 3.0},
            'styleShapeRadiusTopRight': {'kind': 'double', 'value': 4.0},
            'styleShapeRadiusBottomRight': {'kind': 'double', 'value': 5.0},
            'styleShapeRadiusBottomLeft': {'kind': 'double', 'value': 6.0},
            'stylePressedShapeKind': {
              'kind': 'string',
              'value': 'roundedRectangle',
            },
            'stylePressedShapeRadiusTopLeft': {'kind': 'double', 'value': 18.0},
            'styleTextInherit': {'kind': 'boolean', 'value': true},
            'styleDisabledTextInherit': {'kind': 'boolean', 'value': true},
            'styleTextFontSize': {'kind': 'double', 'value': 15.0},
            'styleTextLetterSpacing': {'kind': 'double', 'value': 1.25},
            'styleTextLocaleLanguageCode': {'kind': 'string', 'value': 'en'},
            'styleTextLocaleCountryCode': {'kind': 'string', 'value': 'US'},
            'styleTextDecorationUnderline': {'kind': 'boolean', 'value': true},
            'stylePressedTextFontSize': {'kind': 'double', 'value': 23.0},
            'stylePressedTextLocaleCountryCode': {
              'kind': 'string',
              'value': 'GB',
            },
            'stylePressedTextDecorationUnderline': {
              'kind': 'boolean',
              'value': false,
            },
            'styleVisualDensityHorizontal': {'kind': 'double', 'value': 0.0},
            'styleVisualDensityVertical': {'kind': 'double', 'value': 0.0},
            'styleTapTargetSize': {
              'kind': 'enum',
              'type': 'MaterialTapTargetSize',
              'value': 'shrinkWrap',
            },
          },
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );

      final finder = _buttonFinder();
      final button = tester.widget<ElevatedButton>(finder);
      const pressed = <WidgetState>{WidgetState.pressed};
      expect(button.style!.minimumSize!.resolve(pressed), const Size(180, 44));
      final side = button.style!.side!.resolve(pressed)!;
      expect(side.color, const Color(0xff123456));
      expect(side.width, 6);
      expect(side.style, BorderStyle.solid);
      expect(side.strokeAlign, -0.5);
      final shape =
          button.style!.shape!.resolve(pressed)! as RoundedRectangleBorder;
      expect(
        shape.borderRadius,
        const BorderRadius.only(
          topLeft: Radius.circular(18),
          topRight: Radius.circular(4),
          bottomRight: Radius.circular(5),
          bottomLeft: Radius.circular(6),
        ),
      );
      final text = button.style!.textStyle!.resolve(pressed)!;
      expect(text.inherit, isTrue);
      expect(text.fontSize, 23);
      expect(text.letterSpacing, 1.25);
      expect(text.locale, const Locale('en', 'GB'));
      expect(text.decoration, TextDecoration.none);

      final gesture = await tester.startGesture(tester.getCenter(finder));
      await tester.pumpAndSettle();
      expect(tester.getSize(finder).width, greaterThanOrEqualTo(180));
      expect(tester.getSize(finder).height, 44);
      final material = _buttonMaterial(tester);
      expect(material.textStyle?.fontSize, 23);
      expect(material.textStyle?.letterSpacing, 1.25);
      expect(
        (material.shape as RoundedRectangleBorder).borderRadius,
        shape.borderRadius,
      );
      expect((material.shape as OutlinedBorder).side, side);
      await gesture.up();
    },
  );

  testWidgets(
    'inherits omitted pressed leaves and visual-density axis from ElevatedButtonTheme',
    (tester) async {
      final themePaint = Paint()..color = const Color(0xff445566);
      final themeStyle = ButtonStyle(
        minimumSize: const WidgetStatePropertyAll(Size(130, 52)),
        fixedSize: const WidgetStatePropertyAll(Size(140, 48)),
        shape: const WidgetStatePropertyAll(
          RoundedRectangleBorder(
            side: BorderSide(
              color: Color(0xff228844),
              width: 3,
              strokeAlign: 0,
            ),
            borderRadius: BorderRadius.only(
              topLeft: Radius.circular(8),
              topRight: Radius.circular(9),
              bottomRight: Radius.circular(10),
              bottomLeft: Radius.circular(11),
            ),
          ),
        ),
        textStyle: WidgetStatePropertyAll(
          TextStyle(
            inherit: true,
            background: themePaint,
            fontSize: 17,
            letterSpacing: 2,
            locale: const Locale('en', 'US'),
            decoration: TextDecoration.underline,
            shadows: [Shadow(offset: Offset(1, 2), blurRadius: 3)],
            fontFeatures: [FontFeature('smcp')],
            fontVariations: [FontVariation('wght', 650)],
          ),
        ),
        visualDensity: const VisualDensity(horizontal: -3, vertical: 2),
      );
      final model = CanvasModel.decode(
        fixture.elevatedButtonModelBytesForViewTest(
          properties: const {
            'enabled': {'kind': 'boolean', 'value': true},
            'onPressed': {'kind': 'callbackPresence'},
            'stylePressedMinimumWidth': {'kind': 'integer', 'value': 180},
            'stylePressedFixedWidth': {'kind': 'integer', 'value': 190},
            'stylePressedSideWidth': {'kind': 'double', 'value': 7.0},
            'stylePressedShapeKind': {
              'kind': 'string',
              'value': 'roundedRectangle',
            },
            'stylePressedShapeRadiusTopLeft': {'kind': 'double', 'value': 20.0},
            'stylePressedTextFontSize': {'kind': 'double', 'value': 25.0},
            'stylePressedTextBackgroundColor': {
              'kind': 'color',
              'argb': '0xFFABCDEF',
            },
            'stylePressedTextLocaleCountryCode': {
              'kind': 'string',
              'value': 'GB',
            },
            'stylePressedTextDecorationUnderline': {
              'kind': 'boolean',
              'value': false,
            },
            'stylePressedTextShadows': {
              'kind': 'shadowList',
              'items': <Object?>[],
            },
            'stylePressedTextFontFeatures': {
              'kind': 'fontFeatureList',
              'items': <Object?>[],
            },
            'stylePressedTextFontVariations': {
              'kind': 'fontVariationList',
              'items': <Object?>[],
            },
            'styleVisualDensityHorizontal': {'kind': 'double', 'value': -1.0},
          },
        ),
      );
      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData(
            elevatedButtonTheme: ElevatedButtonThemeData(style: themeStyle),
          ),
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        ),
      );
      await tester.pumpAndSettle();

      final finder = _buttonFinder();
      final button = tester.widget<ElevatedButton>(finder);
      const pressed = <WidgetState>{WidgetState.pressed};
      expect(button.style!.minimumSize!.resolve(pressed), const Size(180, 52));
      expect(button.style!.fixedSize!.resolve(pressed), const Size(190, 48));
      final side = button.style!.side!.resolve(pressed)!;
      expect(side.color, const Color(0xff228844));
      expect(side.width, 7);
      expect(side.strokeAlign, 0);
      final shape =
          button.style!.shape!.resolve(pressed)! as RoundedRectangleBorder;
      expect(
        shape.borderRadius,
        const BorderRadius.only(
          topLeft: Radius.circular(20),
          topRight: Radius.circular(9),
          bottomRight: Radius.circular(10),
          bottomLeft: Radius.circular(11),
        ),
      );
      final text = button.style!.textStyle!.resolve(pressed)!;
      expect(text.backgroundColor, isNull);
      expect(text.background?.color.toARGB32(), 0xffabcdef);
      expect(text.fontSize, 25);
      expect(text.letterSpacing, 2);
      expect(text.locale, const Locale('en', 'GB'));
      expect(text.decoration, TextDecoration.none);
      expect(text.shadows, isEmpty);
      expect(text.fontFeatures, isEmpty);
      expect(text.fontVariations, isEmpty);
      expect(
        button.style!.visualDensity,
        const VisualDensity(horizontal: -1, vertical: 2),
      );

      final gesture = await tester.startGesture(tester.getCenter(finder));
      await tester.pumpAndSettle();
      final material = _buttonMaterial(tester);
      expect(material.textStyle?.backgroundColor, isNull);
      expect(material.textStyle?.background?.color.toARGB32(), 0xffabcdef);
      expect(material.textStyle?.letterSpacing, 2);
      final materialSide = (material.shape as OutlinedBorder).side;
      expect(materialSide.color, const Color(0xff228844));
      expect(materialSide.width, 7);
      await gesture.up();
    },
  );

  testWidgets('widens a local maximum to an inherited theme minimum', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'enabled': {'kind': 'boolean', 'value': true},
          'onPressed': {'kind': 'callbackPresence'},
          'styleMaximumWidth': {'kind': 'integer', 'value': 80},
          'styleMaximumHeight': {'kind': 'integer', 'value': 40},
        },
      ),
    );
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData(
          elevatedButtonTheme: const ElevatedButtonThemeData(
            style: ButtonStyle(
              minimumSize: WidgetStatePropertyAll(Size(100, 48)),
            ),
          ),
        ),
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );
    await tester.pumpAndSettle();

    final finder = _buttonFinder();
    final style = tester.widget<ElevatedButton>(finder).style!;
    expect(style.minimumSize!.resolve(const {}), const Size(100, 48));
    expect(style.maximumSize!.resolve(const {}), const Size(100, 48));
    expect(tester.getSize(finder), const Size(100, 48));
    expect(tester.takeException(), isNull);
  });

  testWidgets('widens an inherited theme maximum to a local minimum', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'enabled': {'kind': 'boolean', 'value': true},
          'onPressed': {'kind': 'callbackPresence'},
          'styleMinimumWidth': {'kind': 'integer', 'value': 120},
          'styleMinimumHeight': {'kind': 'integer', 'value': 52},
        },
      ),
    );
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData(
          elevatedButtonTheme: const ElevatedButtonThemeData(
            style: ButtonStyle(
              maximumSize: WidgetStatePropertyAll(Size(100, 44)),
            ),
          ),
        ),
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );
    await tester.pumpAndSettle();

    final finder = _buttonFinder();
    final style = tester.widget<ElevatedButton>(finder).style!;
    expect(style.minimumSize!.resolve(const {}), const Size(120, 52));
    expect(style.maximumSize!.resolve(const {}), const Size(120, 52));
    expect(tester.getSize(finder), const Size(120, 52));
    expect(tester.takeException(), isNull);
  });

  testWidgets('normalizes paired bounds across simultaneous active states', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'enabled': {'kind': 'boolean', 'value': true},
          'onPressed': {'kind': 'callbackPresence'},
          'autofocus': {'kind': 'boolean', 'value': true},
          'styleFocusedMinimumWidth': {'kind': 'integer', 'value': 120},
          'styleHoveredMaximumWidth': {'kind': 'integer', 'value': 80},
          'stylePressedMinimumHeight': {'kind': 'integer', 'value': 60},
        },
      ),
    );
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData(
          elevatedButtonTheme: const ElevatedButtonThemeData(
            style: ButtonStyle(
              minimumSize: WidgetStatePropertyAll(Size(90, 40)),
              maximumSize: WidgetStatePropertyAll(Size(110, 50)),
            ),
          ),
        ),
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );
    await tester.pumpAndSettle();

    final finder = _buttonFinder();
    final style = tester.widget<ElevatedButton>(finder).style!;
    const active = <WidgetState>{
      WidgetState.focused,
      WidgetState.hovered,
      WidgetState.pressed,
    };
    expect(style.minimumSize!.resolve(active), const Size(120, 60));
    expect(style.maximumSize!.resolve(active), const Size(120, 60));
    expect(style.minimumSize!.resolve(const {}), isNull);
    expect(style.maximumSize!.resolve(const {}), isNull);

    final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
    await mouse.addPointer(location: Offset.zero);
    await mouse.moveTo(tester.getCenter(finder));
    await tester.pumpAndSettle();
    await mouse.down(tester.getCenter(finder));
    await tester.pumpAndSettle();
    expect(tester.takeException(), isNull);
    await mouse.up();
  });

  testWidgets('inherits sparse pressed leaves from framework defaults', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'enabled': {'kind': 'boolean', 'value': true},
          'onPressed': {'kind': 'callbackPresence'},
          'stylePressedMinimumWidth': {'kind': 'integer', 'value': 175},
          'stylePressedTextFontSize': {'kind': 'double', 'value': 24.0},
          'styleVisualDensityHorizontal': {'kind': 'double', 'value': -1.0},
        },
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final finder = _buttonFinder();
    final button = tester.widget<ElevatedButton>(finder);
    final context = tester.element(finder);
    const pressed = <WidgetState>{WidgetState.pressed};
    final defaults = ElevatedButton(
      onPressed: () {},
      child: null,
    ).defaultStyleOf(context);
    final defaultMinimum = defaults.minimumSize!.resolve(pressed)!;
    final defaultText = defaults.textStyle!.resolve(pressed)!;
    expect(
      button.style!.minimumSize!.resolve(pressed),
      Size(175, defaultMinimum.height),
    );
    final text = button.style!.textStyle!.resolve(pressed)!;
    expect(text.fontSize, 24);
    expect(text.fontWeight, defaultText.fontWeight);
    expect(
      button.style!.visualDensity?.vertical,
      defaults.visualDensity?.vertical,
    );
  });

  testWidgets(
    'lets Flutter clamp a fixed-width infinity sentinel to finite maximum height',
    (tester) async {
      final model = CanvasModel.decode(
        fixture.elevatedButtonModelBytesForViewTest(
          properties: const {
            'enabled': {'kind': 'boolean', 'value': true},
            'onPressed': {'kind': 'callbackPresence'},
            'styleFixedWidth': {'kind': 'integer', 'value': 180},
            'styleMaximumHeight': {'kind': 'integer', 'value': 64},
          },
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );

      final finder = _buttonFinder();
      final style = tester.widget<ElevatedButton>(finder).style!;
      expect(
        style.fixedSize!.resolve(const {}),
        const Size(180, double.infinity),
      );
      expect(style.maximumSize!.resolve(const {})!.height, 64);
      expect(tester.getSize(finder), const Size(180, 64));
    },
  );

  testWidgets(
    'lets a local TextStyle Paint replace an inherited background color',
    (tester) async {
      final model = CanvasModel.decode(
        fixture.elevatedButtonModelBytesForViewTest(
          properties: {
            'enabled': const {'kind': 'boolean', 'value': true},
            'onPressed': const {'kind': 'callbackPresence'},
            'stylePressedTextBackground': _paint('0xFF654321'),
          },
        ),
      );
      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData(
            elevatedButtonTheme: const ElevatedButtonThemeData(
              style: ButtonStyle(
                textStyle: WidgetStatePropertyAll(
                  TextStyle(backgroundColor: Color(0xff112233)),
                ),
              ),
            ),
          ),
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        ),
      );
      await tester.pumpAndSettle();

      final text = tester
          .widget<ElevatedButton>(_buttonFinder())
          .style!
          .textStyle!
          .resolve(const {WidgetState.pressed})!;
      expect(text.backgroundColor, isNull);
      expect(text.background?.color.toARGB32(), 0xff654321);
    },
  );

  testWidgets('repackages raw base font leaves for an active package', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'styleTextFontFamily': {'kind': 'string', 'value': 'BaseFamily'},
          'styleTextFontFamilyFallback': {
            'kind': 'string',
            'value': 'BaseFallback\nSecondFallback',
          },
          'styleHoveredTextFontFamily': {
            'kind': 'string',
            'value': 'HoveredFamily',
          },
          'stylePressedTextPackage': {
            'kind': 'string',
            'value': 'design_fonts',
          },
        },
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final text = tester
        .widget<ElevatedButton>(_buttonFinder())
        .style!
        .textStyle!
        .resolve(const {WidgetState.pressed})!;
    expect(text.fontFamily, 'packages/design_fonts/BaseFamily');
    expect(text.fontFamily, isNot(contains('/null')));
    expect(text.fontFamilyFallback, const [
      'packages/design_fonts/BaseFallback',
      'packages/design_fonts/SecondFallback',
    ]);
    final simultaneous = tester
        .widget<ElevatedButton>(_buttonFinder())
        .style!
        .textStyle!
        .resolve(const {WidgetState.hovered, WidgetState.pressed})!;
    expect(simultaneous.fontFamily, 'packages/design_fonts/HoveredFamily');
    expect(simultaneous.fontFamilyFallback, const [
      'packages/design_fonts/BaseFallback',
      'packages/design_fonts/SecondFallback',
    ]);
  });

  testWidgets('packages a fallback-only base without creating a null family', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'styleTextFontFamilyFallback': {
            'kind': 'string',
            'value': 'BaseFallback\nSecondFallback',
          },
          'stylePressedTextPackage': {
            'kind': 'string',
            'value': 'design_fonts',
          },
        },
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final text = tester
        .widget<ElevatedButton>(_buttonFinder())
        .style!
        .textStyle!
        .resolve(const {WidgetState.pressed})!;
    expect(text.fontFamily, isNot('packages/design_fonts/null'));
    expect(text.fontFamilyFallback, const [
      'packages/design_fonts/BaseFallback',
      'packages/design_fonts/SecondFallback',
    ]);
  });

  testWidgets('packages the current active TextTheme family', (tester) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'styleTextInherit': {'kind': 'boolean', 'value': true},
          'styleDisabledTextInherit': {'kind': 'boolean', 'value': true},
          'styleTextFontFamily': {'kind': 'string', 'value': 'ValidationBase'},
          'styleHoveredTextTheme': {
            'kind': 'themeToken',
            'token': 'material.textTheme.labelLarge',
          },
          'styleHoveredTextInherit': {'kind': 'boolean', 'value': true},
          'stylePressedTextPackage': {
            'kind': 'string',
            'value': 'design_fonts',
          },
        },
      ),
    );
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData(
          textTheme: const TextTheme(
            labelLarge: TextStyle(fontFamily: 'ThemeFamily'),
          ),
        ),
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final text = tester
        .widget<ElevatedButton>(_buttonFinder())
        .style!
        .textStyle!
        .resolve(const {WidgetState.hovered, WidgetState.pressed})!;
    expect(text.fontFamily, 'packages/design_fonts/ThemeFamily');
  });

  testWidgets('replaces one old package prefix on a fallback-only style', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'styleTextFontFamilyFallback': {
            'kind': 'string',
            'value': 'FallbackFamily',
          },
          'styleTextPackage': {'kind': 'string', 'value': 'old_fonts'},
          'stylePressedTextPackage': {'kind': 'string', 'value': 'new_fonts'},
        },
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final text = tester
        .widget<ElevatedButton>(_buttonFinder())
        .style!
        .textStyle!
        .resolve(const {WidgetState.pressed})!;
    expect(text.fontFamily, isNot(contains('/null')));
    expect(text.fontFamilyFallback, const [
      'packages/new_fonts/FallbackFamily',
    ]);
  });

  testWidgets('packages the current active TextTheme fallback', (tester) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'styleTextInherit': {'kind': 'boolean', 'value': true},
          'styleDisabledTextInherit': {'kind': 'boolean', 'value': true},
          'styleTextFontFamilyFallback': {
            'kind': 'string',
            'value': 'ValidationFallback',
          },
          'styleHoveredTextTheme': {
            'kind': 'themeToken',
            'token': 'material.textTheme.labelLarge',
          },
          'styleHoveredTextInherit': {'kind': 'boolean', 'value': true},
          'stylePressedTextPackage': {
            'kind': 'string',
            'value': 'design_fonts',
          },
        },
      ),
    );
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData(
          textTheme: const TextTheme(
            labelLarge: TextStyle(
              package: 'old_theme_fonts',
              fontFamilyFallback: ['ThemeFallback'],
            ),
          ),
        ),
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final text = tester
        .widget<ElevatedButton>(_buttonFinder())
        .style!
        .textStyle!
        .resolve(const {WidgetState.hovered, WidgetState.pressed})!;
    expect(text.fontFamily?.contains('/null') ?? false, isFalse);
    expect(text.fontFamilyFallback, const [
      'packages/design_fonts/ThemeFallback',
    ]);
  });

  testWidgets(
    'retains a base package when an active state changes font leaves',
    (tester) async {
      final model = CanvasModel.decode(
        fixture.elevatedButtonModelBytesForViewTest(
          properties: const {
            'styleTextFontFamily': {'kind': 'string', 'value': 'BaseFamily'},
            'styleTextPackage': {'kind': 'string', 'value': 'base_fonts'},
            'stylePressedTextFontFamily': {
              'kind': 'string',
              'value': 'PressedFamily',
            },
            'stylePressedTextFontFamilyFallback': {
              'kind': 'string',
              'value': 'PressedFallback',
            },
          },
        ),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );

      final text = tester
          .widget<ElevatedButton>(_buttonFinder())
          .style!
          .textStyle!
          .resolve(const {WidgetState.pressed})!;
      expect(text.fontFamily, 'packages/base_fonts/PressedFamily');
      expect(text.fontFamilyFallback, const [
        'packages/base_fonts/PressedFallback',
      ]);
    },
  );

  testWidgets(
    'inherit false keeps structured fallback inside its same-state TextTheme',
    (tester) async {
      final model = CanvasModel.decode(
        fixture.elevatedButtonModelBytesForViewTest(
          properties: {
            'styleTextInherit': const {'kind': 'boolean', 'value': false},
            'styleDisabledTextInherit': const {
              'kind': 'boolean',
              'value': false,
            },
            'styleTextLocaleLanguageCode': const {
              'kind': 'string',
              'value': 'en',
            },
            'styleTextLocaleCountryCode': const {
              'kind': 'string',
              'value': 'US',
            },
            'styleTextDecorationUnderline': const {
              'kind': 'boolean',
              'value': true,
            },
            'styleTextDecorationLineThrough': const {
              'kind': 'boolean',
              'value': true,
            },
            'styleTextBackground': _paint('0xFF005500'),
            'stylePressedTextTheme': const {
              'kind': 'themeToken',
              'token': 'material.textTheme.labelLarge',
            },
            'stylePressedTextInherit': const {
              'kind': 'boolean',
              'value': false,
            },
            'stylePressedTextLocaleScriptCode': const {
              'kind': 'string',
              'value': 'Latn',
            },
            'stylePressedTextDecorationUnderline': const {
              'kind': 'boolean',
              'value': false,
            },
          },
        ),
      );
      await tester.pumpWidget(
        MaterialApp(
          theme: ThemeData(
            textTheme: const TextTheme(
              labelLarge: TextStyle(
                backgroundColor: Color(0xffaa3300),
                locale: Locale('fr', 'FR'),
                decoration: TextDecoration.overline,
              ),
            ),
          ),
          home: CanvasDocumentView(
            model: model,
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        ),
      );

      final text = tester
          .widget<ElevatedButton>(_buttonFinder())
          .style!
          .textStyle!
          .resolve(const {WidgetState.pressed})!;
      expect(text.inherit, isFalse);
      expect(
        text.locale,
        const Locale.fromSubtags(
          languageCode: 'fr',
          scriptCode: 'Latn',
          countryCode: 'FR',
        ),
      );
      expect(text.decoration, TextDecoration.overline);
      expect(text.backgroundColor, isNull);
      expect(text.background?.color.toARGB32(), 0xffaa3300);
    },
  );

  testWidgets('normalizes a state TextTheme color before lower Paint merge', (
    tester,
  ) async {
    final lowerPaint = Paint()..color = const Color(0xff005500);
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'styleTextInherit': {'kind': 'boolean', 'value': true},
          'styleDisabledTextInherit': {'kind': 'boolean', 'value': true},
          'stylePressedTextTheme': {
            'kind': 'themeToken',
            'token': 'material.textTheme.labelLarge',
          },
          'stylePressedTextInherit': {'kind': 'boolean', 'value': true},
        },
      ),
    );
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData(
          textTheme: const TextTheme(
            labelLarge: TextStyle(backgroundColor: Color(0xffaa3300)),
          ),
          elevatedButtonTheme: ElevatedButtonThemeData(
            style: ButtonStyle(
              textStyle: WidgetStatePropertyAll(
                TextStyle(background: lowerPaint),
              ),
            ),
          ),
        ),
        home: CanvasDocumentView(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      ),
    );

    final text = tester
        .widget<ElevatedButton>(_buttonFinder())
        .style!
        .textStyle!
        .resolve(const {WidgetState.pressed})!;
    expect(text.backgroundColor, isNull);
    expect(text.background?.color.toARGB32(), 0xffaa3300);
  });

  testWidgets(
    'applies same-state TextTheme before local background color or Paint',
    (tester) async {
      for (final testCase in <(String, Map<String, Object?>, int)>[
        (
          'color',
          const {
            'stylePressedTextBackgroundColor': {
              'kind': 'color',
              'argb': '0xFFABCDEF',
            },
          },
          0xffabcdef,
        ),
        (
          'paint',
          {'stylePressedTextBackground': _paint('0xFF654321')},
          0xff654321,
        ),
      ]) {
        final model = CanvasModel.decode(
          fixture.elevatedButtonModelBytesForViewTest(
            properties: {
              'enabled': const {'kind': 'boolean', 'value': true},
              'onPressed': const {'kind': 'callbackPresence'},
              'styleTextInherit': const {'kind': 'boolean', 'value': true},
              'styleDisabledTextInherit': const {
                'kind': 'boolean',
                'value': true,
              },
              'stylePressedTextTheme': const {
                'kind': 'themeToken',
                'token': 'material.textTheme.labelLarge',
              },
              'stylePressedTextInherit': const {
                'kind': 'boolean',
                'value': true,
              },
              ...testCase.$2,
            },
          ),
        );
        await tester.pumpWidget(
          MaterialApp(
            theme: ThemeData(
              textTheme: const TextTheme(
                labelLarge: TextStyle(
                  backgroundColor: Color(0xff112233),
                  fontSize: 19,
                ),
              ),
            ),
            home: CanvasDocumentView(
              model: model,
              selectedWidgetId: null,
              onSelected: (_) {},
            ),
          ),
        );

        final text = tester
            .widget<ElevatedButton>(_buttonFinder())
            .style!
            .textStyle!
            .resolve(const {WidgetState.pressed})!;
        expect(text.fontSize, 19, reason: testCase.$1);
        expect(text.backgroundColor, isNull, reason: testCase.$1);
        expect(
          text.background?.color.toARGB32(),
          testCase.$3,
          reason: testCase.$1,
        );
      }
    },
  );

  testWidgets('layers independent compound leaves across simultaneous states', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'enabled': {'kind': 'boolean', 'value': true},
          'onPressed': {'kind': 'callbackPresence'},
          'styleMinimumHeight': {'kind': 'integer', 'value': 40},
          'styleHoveredMinimumHeight': {'kind': 'integer', 'value': 54},
          'stylePressedMinimumWidth': {'kind': 'integer', 'value': 180},
          'styleSideColor': {'kind': 'color', 'argb': '0xFF102030'},
          'styleFocusedSideStyle': {
            'kind': 'enum',
            'type': 'BorderStyle',
            'value': 'none',
          },
          'styleHoveredSideColor': {'kind': 'color', 'argb': '0xFF20A040'},
          'stylePressedSideWidth': {'kind': 'double', 'value': 7.0},
          'styleShapeKind': {'kind': 'string', 'value': 'roundedRectangle'},
          'styleShapeRadiusBottomLeft': {'kind': 'double', 'value': 4.0},
          'styleFocusedShapeRadiusBottomRight': {
            'kind': 'double',
            'value': 7.0,
          },
          'styleHoveredShapeRadiusTopLeft': {'kind': 'double', 'value': 8.0},
          'stylePressedShapeRadiusTopLeft': {'kind': 'double', 'value': 12.0},
          'styleTextWordSpacing': {'kind': 'double', 'value': 1.0},
          'styleFocusedTextFontWeight': {
            'kind': 'enum',
            'type': 'FontWeight',
            'value': 'w500',
          },
          'styleHoveredTextFontSize': {'kind': 'double', 'value': 19.0},
          'stylePressedTextFontSize': {'kind': 'double', 'value': 23.0},
        },
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final style = tester.widget<ElevatedButton>(_buttonFinder()).style!;
    const states = <WidgetState>{
      WidgetState.focused,
      WidgetState.hovered,
      WidgetState.pressed,
    };
    expect(style.minimumSize!.resolve(states), const Size(180, 54));
    final side = style.side!.resolve(states)!;
    expect(side.color, const Color(0xff20a040));
    expect(side.width, 7);
    expect(side.style, BorderStyle.none);
    final shape = style.shape!.resolve(states)! as RoundedRectangleBorder;
    expect(
      shape.borderRadius,
      const BorderRadius.only(
        topLeft: Radius.circular(12),
        bottomRight: Radius.circular(7),
        bottomLeft: Radius.circular(4),
      ),
    );
    final text = style.textStyle!.resolve(states)!;
    expect(text.fontSize, 23);
    expect(text.fontWeight, FontWeight.w500);
    expect(text.wordSpacing, 1);
  });

  testWidgets('animates between transition-safe inherit-false text states', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'enabled': {'kind': 'boolean', 'value': true},
          'onPressed': {'kind': 'callbackPresence'},
          'styleTextInherit': {'kind': 'boolean', 'value': false},
          'styleDisabledTextInherit': {'kind': 'boolean', 'value': false},
          'stylePressedTextInherit': {'kind': 'boolean', 'value': false},
          'styleTextFontSize': {'kind': 'double', 'value': 14.0},
          'stylePressedTextFontSize': {'kind': 'double', 'value': 24.0},
        },
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final gesture = await tester.startGesture(
      tester.getCenter(_buttonFinder()),
    );
    await tester.pump(const Duration(milliseconds: 50));
    expect(tester.takeException(), isNull);
    await tester.pumpAndSettle();
    expect(_buttonMaterial(tester).textStyle?.fontSize, 24);
    expect(tester.takeException(), isNull);
    await gesture.up();
  });

  testWidgets('disabled dominates focused atomic and compound state values', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: const {
          'enabled': {'kind': 'boolean', 'value': false},
          'styleFocusedBackgroundColor': {
            'kind': 'color',
            'argb': '0xFFFF0000',
          },
          'styleFocusedMinimumWidth': {'kind': 'integer', 'value': 190},
        },
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final style = tester.widget<ElevatedButton>(_buttonFinder()).style!;
    const disabledFocused = <WidgetState>{
      WidgetState.disabled,
      WidgetState.hovered,
      WidgetState.focused,
    };
    expect(style.backgroundColor!.resolve(disabledFocused), isNull);
    expect(style.minimumSize!.resolve(disabledFocused), isNull);
    expect(style.maximumSize!.resolve(disabledFocused), isNull);
  });

  testWidgets('uses deterministic inert callback enablement semantics', (
    tester,
  ) async {
    final cases = <(String, Map<String, Object?>, bool, bool, bool)>[
      ('default enabled no-op', const {}, true, true, false),
      (
        'long press only',
        const {
          'onLongPress': {'kind': 'callbackPresence'},
        },
        true,
        false,
        true,
      ),
      (
        'configured press',
        const {
          'onPressed': {'kind': 'callbackPresence'},
        },
        true,
        true,
        false,
      ),
      (
        'explicitly disabled',
        const {
          'enabled': {'kind': 'boolean', 'value': false},
          'onPressed': {'kind': 'callbackPresence'},
          'onLongPress': {'kind': 'callbackPresence'},
        },
        false,
        false,
        false,
      ),
    ];
    for (final (label, properties, enabled, pressed, longPressed) in cases) {
      final model = CanvasModel.decode(
        fixture.elevatedButtonModelBytesForViewTest(properties: properties),
      );
      await tester.pumpWidget(
        CanvasModelApp(
          model: model,
          selectedWidgetId: null,
          onSelected: (_) {},
        ),
      );
      final button = tester.widget<ElevatedButton>(_buttonFinder());
      expect(button.enabled, enabled, reason: label);
      expect(button.onPressed != null, pressed, reason: label);
      expect(button.onLongPress != null, longPressed, reason: label);
    }
  });

  testWidgets('renders alternate TextStyle paint and circle shape leaves', (
    tester,
  ) async {
    final model = CanvasModel.decode(
      fixture.elevatedButtonModelBytesForViewTest(
        properties: {
          'styleShapeKind': const {'kind': 'string', 'value': 'circle'},
          'styleShapeCircleEccentricity': const {
            'kind': 'double',
            'value': 0.75,
          },
          'styleTextBackground': _paint('0xFF123456'),
        },
      ),
    );
    await tester.pumpWidget(
      CanvasModelApp(model: model, selectedWidgetId: null, onSelected: (_) {}),
    );

    final style = tester.widget<ElevatedButton>(_buttonFinder()).style!;
    final shape = style.shape!.resolve(const {})! as CircleBorder;
    expect(shape.eccentricity, 0.75);
    final text = style.textStyle!.resolve(const {})!;
    expect(text.background, isNotNull);
    expect(text.background!.color.toARGB32(), 0xff123456);
  });

  testWidgets(
    'exposes a full-node drop zone for the required empty child slot',
    (tester) async {
      CanvasDropResolver? resolver;
      final model = CanvasModel.decode(
        fixture.elevatedButtonModelBytesForViewTest(
          properties: const {},
          withChild: false,
        ),
      );
      await tester.pumpWidget(
        MaterialApp(
          home: Directionality(
            textDirection: TextDirection.ltr,
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

      final button = tester.widget<ElevatedButton>(_buttonFinder());
      expect(button.child, isNull);
      final surface = tester.getRect(find.byType(CanvasDocumentView));
      final buttonRect = tester.getRect(
        find.byKey(
          const ValueKey(
            'canvas-widget-${fixture.elevatedButtonWidgetIdForViewTest}',
          ),
        ),
      );
      final point = buttonRect.center;
      final target = resolver!(
        ((point.dx - surface.left) / surface.width * 1000000).round(),
        ((point.dy - surface.top) / surface.height * 1000000).round(),
        CanvasPaletteDragSource(
          token: 'text',
          widgetType: 'flutter.widgets.Text',
          traits: {},
        ),
      );
      expect(target?.parentWidgetId, fixture.elevatedButtonWidgetIdForViewTest);
      expect(target?.slotName, 'child');
      expect(target?.insertionIndex, 0);
      expect(target?.zone, isNotNull);
    },
  );
}

Finder _buttonFinder() => find
    .descendant(
      of: find.byKey(
        const ValueKey(
          'canvas-widget-${fixture.elevatedButtonWidgetIdForViewTest}',
        ),
      ),
      matching: find.byType(ElevatedButton),
    )
    .first;

CanvasNode _buttonNode(CanvasModel model) => model.root.slot('body')!.child!;

Material _buttonMaterial(WidgetTester tester) => tester.widget<Material>(
  find.descendant(of: _buttonFinder(), matching: find.byType(Material)).first,
);

Map<String, Object?> _paint(String argb) => {
  'kind': 'paint',
  'color': {'kind': 'literal', 'argb': argb},
  'blendMode': 'srcOver',
  'style': 'fill',
  'strokeWidth': 0.0,
  'strokeCap': 'round',
  'strokeJoin': 'bevel',
  'strokeMiterLimit': 4.0,
  'antiAlias': true,
  'filterQuality': 'medium',
  'invertColors': false,
};
