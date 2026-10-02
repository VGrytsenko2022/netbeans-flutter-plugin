// The pinned constructors intentionally retain their deprecated legacy API.
// ignore_for_file: deprecated_member_use
import 'package:flutter/material.dart';
import 'package:flutter/gestures.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  testWidgets(
    'raw SDK enabled true requires callback or an exactly matching typed group',
    (tester) async {
      for (final adaptive in [false, true]) {
        for (final platform in [TargetPlatform.windows, TargetPlatform.iOS]) {
          final theme = ThemeData(platform: platform);
          final radio = adaptive
              ? const Radio<String>.adaptive(value: 'option', enabled: true)
              : const Radio<String>(value: 'option', enabled: true);
          await tester.pumpWidget(
            MaterialApp(
              theme: theme,
              home: Material(child: radio),
            ),
          );
          expect(tester.takeException(), isAssertionError);
          await tester.pumpWidget(
            MaterialApp(
              theme: theme,
              home: Material(
                child: RadioGroup<String>(
                  groupValue: 'option',
                  onChanged: (_) {},
                  child: radio,
                ),
              ),
            ),
          );
          await tester.pump();
          expect(tester.takeException(), isNull);
          await tester.pumpWidget(
            MaterialApp(
              theme: theme,
              home: Material(
                child: RadioGroup<Object?>(
                  groupValue: 'option',
                  onChanged: (_) {},
                  child: radio,
                ),
              ),
            ),
          );
          expect(tester.takeException(), isAssertionError);
        }
      }
    },
  );

  testWidgets('Radio raw numeric probe', (tester) async {
    final oldHighlight = FocusManager.instance.highlightStrategy;
    FocusManager.instance.highlightStrategy =
        FocusHighlightStrategy.alwaysTraditional;
    addTearDown(() => FocusManager.instance.highlightStrategy = oldHighlight);
    final outcomes = <String, Object?>{};
    for (final material3 in [false, true]) {
      for (final adaptive in [false, true]) {
        for (final platform in [TargetPlatform.windows, TargetPlatform.iOS]) {
          for (final field in ['innerRadius', 'splashRadius']) {
            for (final radius in [
              double.negativeInfinity,
              -1e308,
              -1.0,
              0.0,
              1.0,
              1e308,
              double.infinity,
            ]) {
              final errors = <String>[];
              final handler = FlutterError.onError;
              FlutterError.onError = (d) => errors.add(d.exceptionAsString());
              final focus = FocusNode();
              final phases = <String, bool>{};
              final mouse = await tester.createGesture(
                kind: PointerDeviceKind.mouse,
              );
              try {
                Future<void> mount(bool selected) async {
                  final props = WidgetStatePropertyAll<double?>(radius);
                  final Widget radio = adaptive
                      ? Radio<int>.adaptive(
                          value: 1,
                          groupValue: selected ? 1 : 2,
                          onChanged: (_) {},
                          focusNode: focus,
                          innerRadius: field == 'innerRadius' ? props : null,
                          splashRadius: field == 'splashRadius' ? radius : null,
                        )
                      : Radio<int>(
                          value: 1,
                          groupValue: selected ? 1 : 2,
                          onChanged: (_) {},
                          focusNode: focus,
                          innerRadius: field == 'innerRadius' ? props : null,
                          splashRadius: field == 'splashRadius' ? radius : null,
                        );
                  await tester.pumpWidget(
                    MaterialApp(
                      theme: ThemeData(
                        platform: platform,
                        useMaterial3: material3,
                      ),
                      themeAnimationDuration: Duration.zero,
                      home: Material(child: Center(child: radio)),
                    ),
                  );
                  await tester.pump(const Duration(milliseconds: 25));
                  phases['selectionIntermediate'] = errors.isEmpty;
                  await tester.pump(const Duration(milliseconds: 275));
                }

                await mount(false);
                phases['unselected'] = errors.isEmpty;
                await mount(true);
                phases['selectedTransition'] = errors.isEmpty;
                focus.requestFocus();
                await tester.pump(const Duration(milliseconds: 300));
                phases['focused'] = errors.isEmpty;
                await mouse.addPointer(location: Offset.zero);
                await mouse.moveTo(tester.getCenter(find.byType(Radio<int>)));
                await tester.pump(const Duration(milliseconds: 300));
                phases['hovered'] = errors.isEmpty;
                await mouse.down(tester.getCenter(find.byType(Radio<int>)));
                await tester.pump(const Duration(milliseconds: 25));
                phases['pressIntermediate'] = errors.isEmpty;
                await tester.pump(const Duration(milliseconds: 300));
                phases['pressed'] = errors.isEmpty;
                await mouse.up();
                await mouse.removePointer();
                await tester.pump(const Duration(seconds: 1));
                await mount(false);
                phases['unselectTransition'] = errors.isEmpty;
                await tester.pumpWidget(const SizedBox());
                await tester.pump();
              } catch (error) {
                errors.add(error.toString());
              } finally {
                FlutterError.onError = handler;
                focus.dispose();
              }
              final key = '$material3 $adaptive $platform $field $radius';
              outcomes[key] = {
                'phases': phases,
                'errors': errors.toSet().take(3).toList(),
              };
              expect(errors, isEmpty, reason: key);
            }
          }
        }
      }
    }
    expect(outcomes, hasLength(112));
  });

  testWidgets('Radio actual state sets and plain versus stateful side', (
    tester,
  ) async {
    final fills = <Set<WidgetState>>[],
        backs = <Set<WidgetState>>[],
        inners = <Set<WidgetState>>[],
        sides = <Set<WidgetState>>[],
        overlays = <Set<WidgetState>>[];
    final focus = FocusNode();
    try {
      Widget radio(bool selected) => Radio<int>(
        value: 1,
        groupValue: selected ? 1 : 2,
        onChanged: (_) {},
        focusNode: focus,
        fillColor: WidgetStateProperty.resolveWith((states) {
          fills.add(Set.of(states));
          return null;
        }),
        backgroundColor: WidgetStateProperty.resolveWith((states) {
          backs.add(Set.of(states));
          return null;
        }),
        innerRadius: WidgetStateProperty.resolveWith((states) {
          inners.add(Set.of(states));
          return null;
        }),
        overlayColor: WidgetStateProperty.resolveWith((states) {
          overlays.add(Set.of(states));
          return null;
        }),
        side: WidgetStateBorderSide.resolveWith((states) {
          sides.add(Set.of(states));
          return null;
        }),
      );
      for (final selected in [false, true]) {
        fills.clear();
        backs.clear();
        inners.clear();
        sides.clear();
        overlays.clear();
        await tester.pumpWidget(
          MaterialApp(home: Material(child: radio(selected))),
        );
        await tester.pump(const Duration(milliseconds: 300));
        expect(fills.every((s) => !s.contains(WidgetState.pressed)), isTrue);
        expect(backs.every((s) => !s.contains(WidgetState.pressed)), isTrue);
        expect(
          inners.every(
            (s) =>
                s.contains(WidgetState.selected) &&
                s.contains(WidgetState.pressed),
          ),
          isTrue,
        );
        expect(sides.every((s) => s.contains(WidgetState.pressed)), isTrue);
        expect(fills, isNotEmpty);
        expect(backs, isNotEmpty);
        expect(inners, isNotEmpty);
        expect(sides, isNotEmpty);
      }
      await tester.pumpWidget(const SizedBox());
    } finally {
      focus.dispose();
    }
  });
}
