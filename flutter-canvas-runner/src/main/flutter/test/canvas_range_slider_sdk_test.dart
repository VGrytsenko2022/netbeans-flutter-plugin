import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

class _Ticks extends RangeSliderTickMarkShape {
  _Ticks(this.width);
  final double width;
  int count = 0;
  @override
  Size getPreferredSize({
    required SliderThemeData sliderTheme,
    bool isEnabled = false,
  }) => Size(width, 0);
  @override
  void paint(
    PaintingContext context,
    Offset center, {
    required RenderBox parentBox,
    required SliderThemeData sliderTheme,
    required Animation<double> enableAnimation,
    required Offset startThumbCenter,
    required Offset endThumbCenter,
    bool isEnabled = false,
    required TextDirection textDirection,
  }) {
    count++;
  }
}

void main() {
  testWidgets(
    'raw RangeSlider normalization finite interpolation equal and signed infinity controls',
    (tester) async {
      final cases = <String, (double, double, RangeValues)>{
        'ordinary': (0, 1, const RangeValues(.25, .75)),
        'equalFinite': (1, 1, const RangeValues(1, 1)),
        'equalPositiveInfinity': (
          double.infinity,
          double.infinity,
          const RangeValues(double.infinity, double.infinity),
        ),
        'equalNegativeInfinity': (
          double.negativeInfinity,
          double.negativeInfinity,
          const RangeValues(double.negativeInfinity, double.negativeInfinity),
        ),
        'maxInfinityFiniteValues': (
          0,
          double.infinity,
          const RangeValues(.25, .75),
        ),
        'maxInfinityInfiniteEnd': (
          0,
          double.infinity,
          const RangeValues(0, double.infinity),
        ),
        'minNegativeInfinity': (
          double.negativeInfinity,
          1,
          const RangeValues(0, 1),
        ),
        'bothInfinite': (
          double.negativeInfinity,
          double.infinity,
          const RangeValues(0, 0),
        ),
        'finiteOverflowSafeValues': (
          -1e308,
          1e308,
          const RangeValues(-1e308, 0),
        ),
        'finiteOverflowBadEnd': (-1e308, 1e308, const RangeValues(0, 1e308)),
      };
      for (final enabled in [false, true]) {
        for (final entry in cases.entries) {
          final (min, max, values) = entry.value;
          final errors = <String>[], changes = <RangeValues>[];
          final handler = FlutterError.onError;
          FlutterError.onError = (details) =>
              errors.add(details.exceptionAsString());
          var mounted = false;
          try {
            await tester.pumpWidget(
              MaterialApp(
                home: Material(
                  child: Center(
                    child: SizedBox(
                      width: 300,
                      height: 80,
                      child: RangeSlider(
                        values: values,
                        min: min,
                        max: max,
                        onChanged: enabled ? changes.add : null,
                      ),
                    ),
                  ),
                ),
              ),
            );
            await tester.pump(const Duration(milliseconds: 200));
            mounted = errors.isEmpty;
            if (mounted) {
              final rect = tester.getRect(find.byType(RangeSlider));
              final norm = max > min ? (values.start - min) / (max - min) : 0;
              await tester.dragFrom(
                Offset(
                  rect.left + 24 + norm * (rect.width - 48),
                  rect.center.dy,
                ),
                const Offset(65, 0),
              );
              await tester.pump(const Duration(milliseconds: 200));
            }
            await tester.pumpWidget(const SizedBox());
            await tester.pump(const Duration(seconds: 1));
          } catch (error) {
            errors.add(error.toString());
          } finally {
            FlutterError.onError = handler;
          }
          final invalid = {
            'maxInfinityInfiniteEnd',
            'minNegativeInfinity',
            'bothInfinite',
            'finiteOverflowBadEnd',
          }.contains(entry.key);
          expect(mounted, !invalid, reason: '${entry.key} enabled=$enabled');
          if (invalid) {
            expect(errors.join('\n'), contains('values'), reason: entry.key);
          } else if (enabled && entry.key == 'maxInfinityFiniteValues') {
            expect(
              errors.join('\n'),
              contains(
                'Cannot interpolate between finite and non-finite values',
              ),
            );
          } else {
            expect(errors, isEmpty, reason: '${entry.key} enabled=$enabled');
            expect(
              changes.every((v) => v.start.isFinite && v.end.isFinite),
              isTrue,
            );
            if (enabled && entry.key == 'finiteOverflowSafeValues') {
              expect(changes, isNotEmpty);
            }
            if (!enabled || entry.key.startsWith('equal')) {
              expect(changes, isEmpty);
            }
          }
        }
      }
    },
  );

  testWidgets(
    'raw tiny zero ticks enter every paint iteration while normal huge divisions suppress',
    (tester) async {
      for (final width in [0.0, 1e-300]) {
        final ticks = _Ticks(width);
        await tester.pumpWidget(
          MaterialApp(
            home: Material(
              child: SizedBox(
                width: 300,
                height: 80,
                child: SliderTheme(
                  data: SliderThemeData(rangeTickMarkShape: ticks),
                  child: RangeSlider(
                    values: const RangeValues(.25, .75),
                    divisions: 10000,
                    onChanged: (_) {},
                  ),
                ),
              ),
            ),
          ),
        );
        expect(ticks.count, 10001);
        await tester.pumpWidget(const SizedBox());
      }
      await tester.pumpWidget(
        MaterialApp(
          home: Material(
            child: RangeSlider(
              values: const RangeValues(.25, .75),
              divisions: 9007199254740991,
              onChanged: (_) {},
            ),
          ),
        ),
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'raw thumb crossing overlap discrete snapping and narrow layouts remain SDK-owned',
    (tester) async {
      for (final rtl in [false, true]) {
        for (final width in [0.0, 1.0, 24.0, 300.0]) {
          for (final divisions in [null, 4]) {
            final changes = <RangeValues>[];
            await tester.pumpWidget(
              MaterialApp(
                home: Material(
                  child: Center(
                    child: Directionality(
                      textDirection: rtl
                          ? TextDirection.rtl
                          : TextDirection.ltr,
                      child: SizedBox(
                        width: width,
                        height: 80,
                        child: RangeSlider(
                          values: const RangeValues(.25, .75),
                          divisions: divisions,
                          onChanged: changes.add,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            );
            if (width >= 24) {
              final rect = tester.getRect(find.byType(RangeSlider));
              await tester.dragFrom(rect.center, const Offset(150, 0));
              await tester.pump(const Duration(milliseconds: 250));
            }
            expect(
              changes.every(
                (v) => v.start >= 0 && v.end <= 1 && v.start <= v.end,
              ),
              isTrue,
            );
            expect(
              tester.takeException(),
              isNull,
              reason: 'width=$width rtl=$rtl divisions=$divisions',
            );
            await tester.pumpWidget(const SizedBox());
          }
        }
      }
    },
  );
}
