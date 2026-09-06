import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  for (final value in [0.0, double.infinity, 1e308]) {
    testWidgets('raw SDK FAB five elevations accept $value', (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: FloatingActionButton(
              onPressed: () {},
              heroTag: null,
              elevation: value,
              focusElevation: value,
              hoverElevation: value,
              highlightElevation: value,
              disabledElevation: value,
              child: const Icon(Icons.add),
            ),
          ),
        ),
      );
      await tester.pump();
      expect(
        tester
            .widget<RawMaterialButton>(find.byType(RawMaterialButton))
            .elevation,
        value,
      );
      expect(tester.takeException(), isNull);
    });
  }
  testWidgets(
    'raw SDK FAB infinite-to-finite hover interpolation fails physical elevation assertion',
    (tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Center(
            child: FloatingActionButton(
              onPressed: () {},
              elevation: 1,
              hoverElevation: double.infinity,
              heroTag: null,
            ),
          ),
        ),
      );
      final mouse = await tester.createGesture(kind: PointerDeviceKind.mouse);
      await mouse.addPointer(location: const Offset(1, 1));
      await mouse.moveTo(tester.getCenter(find.byType(FloatingActionButton)));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 50));
      expect(
        tester
            .widget<RawMaterialButton>(find.byType(RawMaterialButton))
            .hoverElevation,
        double.infinity,
      );
      expect(tester.takeException(), isNull);
      final errors = <String>[];
      final previous = FlutterError.onError;
      FlutterError.onError = (details) =>
          errors.add(details.exceptionAsString());
      try {
        await mouse.moveTo(const Offset(1, 1));
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 50));
        expect(errors.any((e) => e.contains('elevation >= 0.0')), true);
        await mouse.removePointer();
        await tester.pumpWidget(const SizedBox.shrink());
      } finally {
        FlutterError.onError = previous;
      }
    },
  );
  for (final value in [-2.0, double.infinity]) {
    testWidgets('raw SDK inactive extended spacing $value is allowed', (
      tester,
    ) async {
      for (final icon in [false, true]) {
        await tester.pumpWidget(
          MaterialApp(
            home: Center(
              child: FloatingActionButton.extended(
                onPressed: () {},
                heroTag: null,
                extendedIconLabelSpacing: value,
                isExtended: !icon,
                icon: icon ? const Icon(Icons.add) : null,
                label: const Text('Action'),
              ),
            ),
          ),
        );
        await tester.pump();
        expect(tester.takeException(), isNull);
      }
    });
    testWidgets('raw SDK active extended spacing $value has invalid layout', (
      tester,
    ) async {
      final errors = <String>[];
      final previous = FlutterError.onError;
      FlutterError.onError = (details) =>
          errors.add(details.exceptionAsString());
      try {
        await tester.pumpWidget(
          MaterialApp(
            home: Center(
              child: FloatingActionButton.extended(
                onPressed: () {},
                heroTag: null,
                extendedIconLabelSpacing: value,
                icon: const Icon(Icons.add),
                label: const Text('Action'),
              ),
            ),
          ),
        );
        await tester.pump();
        expect(
          errors.any(
            (e) => e.contains(value.isInfinite ? 'infinite' : 'negative'),
          ),
          true,
        );
        await tester.pumpWidget(const SizedBox.shrink());
      } finally {
        FlutterError.onError = previous;
      }
    });
  }
  testWidgets(
    'raw SDK duplicate default Hero tags fail on navigation, not static button layout',
    (tester) async {
      final nav = GlobalKey<NavigatorState>();
      await tester.pumpWidget(
        MaterialApp(
          navigatorKey: nav,
          home: Row(
            children: [
              FloatingActionButton(onPressed: () {}),
              FloatingActionButton(onPressed: () {}),
            ],
          ),
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      final errors = <String>[];
      final previous = FlutterError.onError;
      FlutterError.onError = (details) =>
          errors.add(details.exceptionAsString());
      try {
        nav.currentState!.push(
          MaterialPageRoute<void>(builder: (_) => const SizedBox.shrink()),
        );
        await tester.pump();
        await tester.pump(const Duration(milliseconds: 400));
        expect(errors.any((e) => e.contains('multiple heroes')), true);
        await tester.pumpWidget(const SizedBox.shrink());
      } finally {
        FlutterError.onError = previous;
      }
    },
  );
}
