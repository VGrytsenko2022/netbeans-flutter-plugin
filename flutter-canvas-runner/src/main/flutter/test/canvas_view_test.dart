import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

void main() {
  testWidgets('applies every exact adaptive target to the Flutter theme', (
    tester,
  ) async {
    const expected = <String, TargetPlatform>{
      'android': TargetPlatform.android,
      'ios': TargetPlatform.iOS,
      'windows': TargetPlatform.windows,
      'macos': TargetPlatform.macOS,
      'linux': TargetPlatform.linux,
      // A true Web runtime is a separate backend; the native Windows runner
      // exposes its fallback explicitly and NetBeans does not present it.
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
}
