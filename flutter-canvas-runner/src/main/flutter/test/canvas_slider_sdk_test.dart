import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

class _CountingTicks extends SliderTickMarkShape {
  _CountingTicks([this.width = 0]);
  final double width;
  int count = 0;
  @override
  Size getPreferredSize({
    required SliderThemeData sliderTheme,
    bool? isEnabled,
  }) => Size(width, 0);
  @override
  void paint(
    PaintingContext context,
    Offset center, {
    required RenderBox parentBox,
    required SliderThemeData sliderTheme,
    required Animation<double> enableAnimation,
    required TextDirection textDirection,
    required Offset thumbCenter,
    bool? isEnabled,
  }) {
    count++;
  }
}

void main() {
  testWidgets('raw Flutter 3.44.8 Slider numeric mount and gesture controls', (
    tester,
  ) async {
    final cases = <String, (double, double, double, int?)>{
      'normal': (0, 1, .5, null),
      'equalFinite': (1, 1, 1, null),
      'equalPositiveInfinity': (
        double.infinity,
        double.infinity,
        double.infinity,
        null,
      ),
      'equalNegativeInfinity': (
        double.negativeInfinity,
        double.negativeInfinity,
        double.negativeInfinity,
        null,
      ),
      'maxInfinityFiniteValue': (0, double.infinity, .5, null),
      'maxInfinityInfiniteValue': (0, double.infinity, double.infinity, null),
      'minNegativeInfinity': (double.negativeInfinity, 1, 0, null),
      'bothInfinite': (double.negativeInfinity, double.infinity, 0, null),
      'finiteOverflowMiddle': (-1e308, 1e308, 0, null),
      'finiteOverflowMaximum': (-1e308, 1e308, 1e308, null),
      'finiteOverflowMiddleDiscrete': (-1e308, 1e308, 0, 7),
      'hugeDivisions': (0, 1, .5, 9007199254740991),
    };
    for (final platform in [TargetPlatform.windows, TargetPlatform.iOS]) {
      for (final entry in cases.entries) {
        final (min, max, value, divisions) = entry.value;
        final errors = <String>[];
        final values = <double>[];
        final handler = FlutterError.onError;
        FlutterError.onError = (details) =>
            errors.add(details.exceptionAsString());
        var mounted = false;
        try {
          await tester.pumpWidget(
            MaterialApp(
              theme: ThemeData(platform: platform),
              home: Scaffold(
                body: Center(
                  child: SizedBox(
                    width: 300,
                    child: Slider.adaptive(
                      value: value,
                      min: min,
                      max: max,
                      divisions: divisions,
                      onChanged: values.add,
                    ),
                  ),
                ),
              ),
            ),
          );
          await tester.pump(const Duration(milliseconds: 100));
          mounted = errors.isEmpty;
          if (mounted) {
            final rect = tester.getRect(find.byType(Slider));
            final normalized = max > min ? (value - min) / (max - min) : 0;
            await tester.dragFrom(
              Offset(
                rect.left + 22 + normalized * (rect.width - 44),
                rect.center.dy,
              ),
              const Offset(60, 0),
            );
            await tester.pump(const Duration(milliseconds: 100));
          }
          await tester.pumpWidget(const SizedBox());
          await tester.pump(const Duration(seconds: 1));
        } catch (error) {
          errors.add(error.toString());
        } finally {
          FlutterError.onError = handler;
        }
        final alwaysInvalid = {
          'maxInfinityInfiniteValue',
          'minNegativeInfinity',
          'bothInfinite',
          'finiteOverflowMaximum',
        }.contains(entry.key);
        final equal = entry.key.startsWith('equal');
        final apple = platform == TargetPlatform.iOS;
        expect(
          mounted,
          !(alwaysInvalid || (apple && equal)),
          reason: '${platform.name} ${entry.key}',
        );
        if (!mounted) {
          expect(errors.join('\n'), contains('value >= 0.0 && value <= 1.0'));
        } else if (apple && entry.key == 'maxInfinityFiniteValue') {
          expect(
            errors.join('\n'),
            contains('Cannot interpolate between finite and non-finite values'),
          );
        } else {
          expect(errors, isEmpty, reason: '${platform.name} ${entry.key}');
          if (equal) expect(values, isEmpty);
          if (entry.key == 'maxInfinityFiniteValue' ||
              entry.key.startsWith('finiteOverflowMiddle')) {
            expect(values, isNotEmpty);
            expect(values.any((value) => value.isNaN), !apple);
            expect(values.any((value) => value.isInfinite), !apple);
          }
        }
      }
    }
    for (final width in [0.0, 1e-300]) {
      final ticks = _CountingTicks(width);
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Center(
              child: SizedBox(
                width: 300,
                child: SliderTheme(
                  data: SliderThemeData(tickMarkShape: ticks),
                  child: Slider(value: .5, divisions: 10000, onChanged: (_) {}),
                ),
              ),
            ),
          ),
        ),
      );
      expect(ticks.count, 10001);
      await tester.pumpWidget(const SizedBox());
    }
  });
  testWidgets(
    'raw supplied FocusNode to null retains SDK state and fails its missing private node',
    (tester) async {
      final focus = FocusNode();
      await tester.pumpWidget(
        MaterialApp(
          home: Material(
            child: Slider(value: .5, onChanged: (_) {}, focusNode: focus),
          ),
        ),
      );
      final errors = <String>[];
      final handler = FlutterError.onError;
      FlutterError.onError = (details) =>
          errors.add(details.exceptionAsString());
      try {
        await tester.pumpWidget(
          MaterialApp(
            home: Material(child: Slider(value: .5, onChanged: (_) {})),
          ),
        );
        await tester.pumpWidget(const SizedBox());
      } finally {
        FlutterError.onError = handler;
        focus.dispose();
      }
      expect(
        errors.join('\n'),
        contains('Null check operator used on a null value'),
      );
    },
  );
}
