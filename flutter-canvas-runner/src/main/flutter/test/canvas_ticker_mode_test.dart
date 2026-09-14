import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.widgets.TickerMode';
const _id = 'a3867d28-9495-4dfc-8ec9-087075ffb9b3';
const _childId = 'b37a51a0-598e-4189-8f08-0a7dd6d5a772';
const _textId = 'bf6064c6-d24e-4f7d-802c-0cf1e54b0574';
const _otherId = 'ce7074c6-d24e-4f7d-802c-0cf1e54b0574';
const _columnId = 'dbed0e25-f4ee-4b41-b6df-c0d1fe0bb8ee';

void main() {
  test(
    'TickerMode exact required enabled default and optional forceFrames contract',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final end = contract.indexOf('W|flutter.widgets.Transform\n', start);
      expect(
        contract.substring(start, end),
        'W|$_type\n'
        'P|enabled|boolean|1|boolean:true|-|boolean:any\n'
        'P|forceFrames|boolean|0|-|-|boolean:any\n'
        'S|child|single|1|1|1|any\n'
        'C|$_type|paletteCreate|wrapExistingChild|child\n',
      );
      expect(canvasModelProtocolVersion, 19);
      expect(isCanvasReviewedWidgetType(_type), isTrue);
      expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), isTrue);
      expect(isCanvasPaletteWrapperWidgetType(_type), isTrue);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      for (final enabled in [null, false, true]) {
        for (final forceFrames in [null, false, true]) {
          final model = _model(enabled: enabled, forceFrames: forceFrames);
          if (enabled == null) {
            expect(
              () => _decode(model),
              throwsFormatException,
              reason:
                  'Creation default is host metadata, not a decoder substitute for required enabled',
            );
          } else {
            final node = _find(_decode(model).root, _id)!;
            expect(node.properties['enabled']!.value, enabled);
            expect(node.properties['forceFrames']?.value, forceFrames);
          }
        }
      }
    },
  );

  test(
    'TickerMode rejects null malformed values unreviewed properties and malformed required child',
    () {
      for (final name in ['enabled', 'forceFrames']) {
        for (final value in [
          {'kind': 'null'},
          {'kind': 'integer', 'value': 1},
          {'kind': 'string', 'value': 'false'},
          {'kind': 'boolean', 'value': null},
          {'kind': 'boolean', 'value': 0},
          {'kind': 'boolean', 'value': false, 'extra': true},
        ]) {
          final model = _model();
          (_tickerJson(model)['properties']! as Map)[name] = value;
          expect(() => _decode(model), throwsFormatException);
        }
      }
      for (final name in [
        'key',
        'child',
        'visible',
        'ticker',
        'merge',
        'maintainAnimation',
      ]) {
        final model = _model();
        (_tickerJson(model)['properties']! as Map)[name] = _bool(true);
        expect(() => _decode(model), throwsFormatException);
      }
      for (final slots in [
        <String, Object?>{},
        {
          'child': {'kind': 'single', 'child': null},
        },
        {
          'child': {
            'kind': 'list',
            'children': [_text()],
          },
        },
        {
          'children': {'kind': 'single', 'child': _text()},
        },
        {
          'child': {'kind': 'single', 'child': _text(), 'extra': true},
        },
        {
          'child': {'kind': 'single', 'child': _text()},
          'replacement': {'kind': 'single', 'child': null},
        },
      ]) {
        final model = _model();
        _tickerJson(model)['slots'] = slots;
        expect(() => _decode(model), throwsFormatException);
      }
      for (final type in ['Expanded', 'Flexible', 'Spacer']) {
        expect(
          () =>
              _decode(_model(child: _node(_childId, 'flutter.widgets.$type'))),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'TickerMode wraps occupied ordinary slots but cannot manufacture an empty child',
    () {
      for (final parent in [
        (type: 'flutter.widgets.Column', slot: 'children'),
        (type: 'flutter.widgets.Center', slot: 'child'),
        (type: _type, slot: 'child'),
        (type: 'flutter.widgets.Visibility', slot: 'replacement'),
      ]) {
        for (final count in [0, 1]) {
          expect(
            canvasDropTargetAcceptsSource(
              parentWidgetType: parent.type,
              slotName: parent.slot,
              currentChildCount: count,
              insertionIndex: 0,
              source: _source(_type),
            ),
            count == 1,
          );
        }
      }
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: _type,
          childWidgetType: 'flutter.widgets.Text',
        ),
        isTrue,
      );
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: _type,
          childWidgetType: 'flutter.widgets.Expanded',
        ),
        isFalse,
      );
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: _type,
          slotName: 'child',
          currentChildCount: 0,
          insertionIndex: 0,
          source: _source('flutter.widgets.Text'),
        ),
        isFalse,
      );
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'TickerMode actual nested Canvas combines enabled AND forceFrames OR on $platform',
      (tester) async {
        for (final parentEnabled in [false, true]) {
          for (final parentForce in [false, true]) {
            for (final enabled in [false, true]) {
              for (final forceFrames in [false, true]) {
                await _pump(
                  tester,
                  _model(
                    platform: platform,
                    enabled: parentEnabled,
                    forceFrames: parentForce,
                    child: _node(
                      _childId,
                      _type,
                      properties: {
                        'enabled': _bool(enabled),
                        'forceFrames': _bool(forceFrames),
                      },
                      child: _text(),
                    ),
                  ),
                );
                expect(
                  TickerMode.valuesOf(tester.element(_widget(_textId))),
                  TickerModeData(
                    enabled: parentEnabled && enabled,
                    forceFrames: parentForce || forceFrames,
                  ),
                );
                expect(tester.takeException(), isNull);
              }
            }
          }
        }
      },
    );

    testWidgets(
      'TickerMode actual SDK properties and effective values cover all six accepted states on $platform',
      (tester) async {
        for (final enabled in [false, true]) {
          for (final forceFrames in [null, false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                enabled: enabled,
                forceFrames: forceFrames,
              ),
            );
            final ticker = tester.widget<TickerMode>(_ticker());
            expect(ticker.enabled, enabled);
            expect(ticker.forceFrames, forceFrames ?? false);
            final effective = TickerMode.valuesOf(
              tester.element(_widget(_textId)),
            );
            expect(effective.enabled, enabled);
            expect(effective.forceFrames, forceFrames ?? false);
            expect(find.text('Ticker child'), findsOneWidget);
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'TickerMode disabling does not change real layout paint hit testing or application semantics on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final enabled in [true, false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                enabled: enabled,
                forceFrames: true,
                child: _node(
                  _childId,
                  'flutter.widgets.ColoredBox',
                  properties: {
                    'color': {'kind': 'color', 'argb': '0xFF123456'},
                  },
                  child: _node(
                    _otherId,
                    'flutter.widgets.SizedBox',
                    properties: _size(90, 40),
                    child: _text(),
                  ),
                ),
              ),
            );
            final render = tester.renderObject<RenderBox>(_ticker());
            expect(render.size, const Size(90, 40));
            expect(render.getMinIntrinsicWidth(100), 90);
            expect(render.getMaxIntrinsicHeight(100), 40);
            expect(
              render.hitTest(
                BoxHitTestResult(),
                position: const Offset(10, 10),
              ),
              isTrue,
            );
            expect(render, paints..rect(color: const Color(0xFF123456)));
            expect(
              _semanticLabels(
                tester,
              ).any((label) => label.contains('Ticker child')),
              isTrue,
            );
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'TickerMode retains real button focus and its state when enabled changes on $platform',
      (tester) async {
        FocusNode? original;
        for (final enabled in [true, false, true]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              enabled: enabled,
              child: _node(
                _childId,
                'flutter.material.ElevatedButton',
                child: _text(),
              ),
            ),
          );
          final focus = Focus.of(tester.element(find.text('Ticker child')));
          original ??= focus;
          expect(identical(focus, original), isTrue);
          focus.requestFocus();
          await tester.pump();
          expect(focus.hasFocus, isTrue);
          expect(focus.canRequestFocus, isTrue);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'TickerMode disabled child remains selectable and F2 supports commit cancel reopen on $platform',
      (tester) async {
        final selections = <String>[];
        final commits = <(String, String)>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(platform: platform, enabled: false, forceFrames: true),
            ),
            selectedWidgetId: _textId,
            onSelected: selections.add,
            inlineTextEditEnabled: true,
            onInlineTextCommit: (id, text, _) {
              commits.add((id, text));
              return true;
            },
          ),
        );
        await tester.pump();
        await tester.tap(_widget(_textId));
        await tester.pump(const Duration(milliseconds: 350));
        expect(selections, contains(_textId));
        await _f2(tester);
        final field = tester.widget<TextField>(find.byType(TextField));
        expect(field.focusNode!.hasFocus, isTrue);
        expect(tester.widget<TickerMode>(_ticker()).enabled, isFalse);
        await tester.enterText(
          find.byType(TextField),
          'Updated while tickers muted',
        );
        await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
        await tester.sendKeyEvent(LogicalKeyboardKey.enter);
        await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
        await tester.pump();
        expect(commits, [(_textId, 'Updated while tickers muted')]);
        await tester.sendKeyEvent(LogicalKeyboardKey.f2);
        await tester.pump();
        expect(find.byType(TextField), findsOneWidget);
        await tester.sendKeyEvent(LogicalKeyboardKey.escape);
        await tester.pump();
        expect(find.byType(TextField), findsNothing);
        expect(commits, hasLength(1));
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'TickerMode keeps required wrapping nested child destinations and move guard while muted on $platform',
      (tester) async {
        CanvasDropResolver? drop;
        CanvasMovePreviewResolver? move;
        final model = _model(
          platform: platform,
          enabled: false,
          child: _node(
            _childId,
            'flutter.widgets.SizedBox',
            properties: _size(90, 70),
            child: _list(_columnId, []),
          ),
        );
        final ticker = _tickerJson(model);
        _replaceCentered(
          model,
          _list(_otherId, [
            ticker,
            _node(
              '11704f22-ad11-4d98-a779-72c024737b27',
              'flutter.widgets.Center',
            ),
          ]),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(model),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => drop = value,
            onMovePreviewResolverChanged: (value) => move = value,
          ),
        );
        await tester.pump();
        final point = tester.getCenter(_widget(_columnId));
        final target = _resolve(tester, drop!, point);
        expect(target?.parentWidgetId, _columnId);
        expect(target?.slotName, 'children');
        final wrapped = _resolve(tester, drop!, point, _source(_type));
        expect(wrapped?.parentWidgetId, _childId);
        expect(wrapped?.slotName, 'child');
        expect(
          move!(_childId, '11704f22-ad11-4d98-a779-72c024737b27', 'child', 0),
          isNull,
        );
        expect(move!(_childId, _id, 'child', 0), isNotNull);
        expect(
          move!(_id, '11704f22-ad11-4d98-a779-72c024737b27', 'child', 0),
          isNotNull,
        );
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'TickerMode zero-size child uses genuine grouped handles without fabricated geometry on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(
            platform: platform,
            enabled: false,
            child: _node(
              _childId,
              'flutter.widgets.SizedBox',
              properties: _size(0, 0),
            ),
          ),
        );
        expect(tester.getSize(_widget(_id)), Size.zero);
        expect(
          find.byKey(
            const ValueKey('canvas-zero-size-widget-target-group-$_id'),
          ),
          findsOneWidget,
        );
        expect(
          find.descendant(of: _ticker(), matching: find.byType(SizedBox)),
          findsOneWidget,
        );
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'TickerMode composes with Visibility without re-exposing hidden geometry or overriding ticker ancestry on $platform',
      (tester) async {
        for (final visible in [true, false]) {
          for (final maintainAnimation in [false, true]) {
            final model = _model(platform: platform, enabled: true);
            final ticker = _tickerJson(model);
            _replaceCentered(
              model,
              _node(
                _otherId,
                'flutter.widgets.Visibility',
                properties: {
                  'visible': _bool(visible),
                  'maintainState': _bool(true),
                  'maintainAnimation': _bool(maintainAnimation),
                },
                child: ticker,
              ),
            );
            await tester.pumpWidget(
              CanvasModelApp(
                model: _decode(model),
                selectedWidgetId: _textId,
                onSelected: (_) {},
                inlineTextEditEnabled: true,
              ),
            );
            await tester.pump();
            final text = find.byKey(
              const ValueKey('canvas-widget-$_textId'),
              skipOffstage: false,
            );
            expect(
              TickerMode.valuesOf(tester.element(text)).enabled,
              visible || maintainAnimation,
            );
            await _f2(tester);
            expect(
              find.byType(TextField, skipOffstage: false),
              visible ? findsOneWidget : findsNothing,
            );
            if (visible) {
              await tester.sendKeyEvent(LogicalKeyboardKey.escape);
              await tester.pump();
            }
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'TickerMode preserves project TextField preview focus guard while muted on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(
            platform: platform,
            enabled: false,
            child: {
              'id': _childId,
              'type': 'flutter.material.TextField',
              'properties': {
                'autofocus': _bool(true),
                'canRequestFocus': _bool(true),
              },
              'slots': <String, Object?>{},
            },
          ),
        );
        final field = tester.widget<EditableText>(find.byType(EditableText));
        field.focusNode.requestFocus();
        await tester.pump();
        expect(field.focusNode.canRequestFocus, isFalse);
        expect(field.focusNode.hasFocus, isFalse);
      },
    );
  }

  for (final multiple in [false, true]) {
    testWidgets(
      'SDK TickerMode ${multiple ? 'multi' : 'single'} provider mutes callbacks not elapsed time and preserves forceFrames',
      (tester) async {
        final key = GlobalKey();
        final probe = multiple ? _MultiProbe(key: key) : _SingleProbe(key: key);
        Future<void> show(bool enabled, bool forceFrames) async {
          await tester.pumpWidget(
            Directionality(
              textDirection: TextDirection.ltr,
              child: TickerMode(
                enabled: enabled,
                forceFrames: forceFrames,
                child: probe,
              ),
            ),
          );
        }

        await show(true, false);
        await tester.pump(const Duration(milliseconds: 100));
        final state = key.currentState! as _ProbeStateBase;
        expect(state.tickers, hasLength(multiple ? 2 : 1));
        expect(state.elapsed.every((events) => events.isNotEmpty), isTrue);
        await show(false, true);
        final counts = state.elapsed.map((events) => events.length).toList();
        final before = state.elapsed.map((events) => events.last).toList();
        await tester.pump(const Duration(seconds: 2));
        for (var i = 0; i < state.tickers.length; i++) {
          expect(state.tickers[i].muted, isTrue);
          expect(state.tickers[i].forceFrames, isTrue);
          expect(state.tickers[i].isActive, isTrue);
          expect(state.elapsed[i], hasLength(counts[i]));
        }
        await show(true, false);
        await tester.pump(const Duration(milliseconds: 100));
        expect(identical(key.currentState, state), isTrue);
        for (var i = 0; i < state.tickers.length; i++) {
          expect(state.tickers[i].muted, isFalse);
          expect(state.tickers[i].forceFrames, isFalse);
          expect(
            state.elapsed[i].last - before[i],
            greaterThanOrEqualTo(const Duration(seconds: 2)),
          );
        }
        await tester.pumpWidget(const SizedBox());
        expect(state.disposed, isTrue);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'SDK TickerMode ${multiple ? 'multi' : 'single'} providers resubscribe to inherited values after GlobalKey reparenting',
      (tester) async {
        final key = GlobalKey();
        final probe = multiple ? _MultiProbe(key: key) : _SingleProbe(key: key);
        for (final left in [true, false, true]) {
          await tester.pumpWidget(
            Directionality(
              textDirection: TextDirection.ltr,
              child: Row(
                children: [
                  TickerMode(
                    enabled: false,
                    forceFrames: true,
                    child: left ? probe : const SizedBox(),
                  ),
                  TickerMode(
                    enabled: true,
                    child: left ? const SizedBox() : probe,
                  ),
                ],
              ),
            ),
          );
          final state = key.currentState! as _ProbeStateBase;
          for (final ticker in state.tickers) {
            expect(ticker.muted, left);
            expect(ticker.forceFrames, left);
          }
        }
        await tester.pumpWidget(const SizedBox());
        expect(tester.takeException(), isNull);
      },
    );
  }

  testWidgets(
    'SDK TickerMode nested enabled AND forceFrames OR covers all sixteen states and merge null equivalence',
    (tester) async {
      for (final parentEnabled in [false, true]) {
        for (final parentForce in [false, true]) {
          for (final enabled in [false, true]) {
            for (final force in [false, true]) {
              TickerModeData? values;
              await tester.pumpWidget(
                TickerMode(
                  enabled: parentEnabled,
                  forceFrames: parentForce,
                  child: TickerMode(
                    enabled: enabled,
                    forceFrames: force,
                    child: Builder(
                      builder: (context) {
                        values = TickerMode.valuesOf(context);
                        return const SizedBox();
                      },
                    ),
                  ),
                ),
              );
              expect(
                values,
                TickerModeData(
                  enabled: parentEnabled && enabled,
                  forceFrames: parentForce || force,
                ),
              );
            }
          }
          for (final merge in [false, true]) {
            TickerModeData? values;
            final child = Builder(
              builder: (context) {
                values = TickerMode.valuesOf(context);
                return const SizedBox();
              },
            );
            await tester.pumpWidget(
              TickerMode(
                enabled: parentEnabled,
                forceFrames: parentForce,
                child: merge
                    ? TickerMode.merge(child: child)
                    : TickerMode(enabled: true, child: child),
              ),
            );
            expect(
              values,
              TickerModeData(enabled: parentEnabled, forceFrames: parentForce),
            );
          }
        }
      }
    },
  );

  testWidgets(
    'SDK TickerMode values notifier stays stable and reports both fields without rebuilding nondependent children',
    (tester) async {
      ValueListenable<TickerModeData>? original;
      var builds = 0;
      final notifications = <TickerModeData>[];
      final child = Builder(
        builder: (context) {
          builds++;
          final notifier = TickerMode.getValuesNotifier(context);
          original ??= notifier;
          expect(identical(notifier, original), isTrue);
          return const SizedBox();
        },
      );
      await tester.pumpWidget(TickerMode(enabled: true, child: child));
      void changed() => notifications.add(original!.value);
      original!.addListener(changed);
      for (final values in [
        const TickerModeData(enabled: false, forceFrames: false),
        const TickerModeData(enabled: false, forceFrames: true),
        const TickerModeData(enabled: true, forceFrames: true),
      ]) {
        await tester.pumpWidget(
          TickerMode(
            enabled: values.enabled,
            forceFrames: values.forceFrames,
            child: child,
          ),
        );
        expect(original!.value, values);
        expect(notifications.last, values);
      }
      expect(builds, 1);
      expect(notifications, hasLength(3));
      original!.removeListener(changed);
      await tester.pumpWidget(const SizedBox());
    },
  );

  testWidgets(
    'SDK TickerMode AnimationController value catches up after muting instead of pausing elapsed time',
    (tester) async {
      final key = GlobalKey<_AnimationProbeState>();
      final child = _AnimationProbe(key: key);
      Future<void> show(bool enabled) =>
          tester.pumpWidget(TickerMode(enabled: enabled, child: child));
      await show(true);
      final state = key.currentState!;
      state.controller.forward();
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));
      expect(state.controller.value, closeTo(0.1, 0.001));
      await show(false);
      final mutedValue = state.controller.value;
      await tester.pump(const Duration(seconds: 2));
      expect(state.controller.value, mutedValue);
      expect(state.controller.isAnimating, isTrue);
      await show(true);
      await tester.pump(const Duration(milliseconds: 1));
      expect(identical(key.currentState, state), isTrue);
      expect(state.controller.value, 1);
      expect(state.controller.status, AnimationStatus.completed);
      await tester.pumpWidget(const SizedBox());
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'SDK TickerMode ambient fallback and plain unmanaged ticker are not silently rewritten',
    (tester) async {
      final values = <TickerModeData>[];
      await tester.pumpWidget(
        Builder(
          builder: (context) {
            values.add(TickerMode.valuesOf(context));
            expect(
              TickerMode.getValuesNotifier(context).value,
              TickerModeData.fallback,
            );
            return const SizedBox();
          },
        ),
      );
      expect(values, [TickerModeData.fallback]);
      var ticks = 0;
      final unmanaged = Ticker((_) => ticks++)..start();
      try {
        await tester.pumpWidget(
          const TickerMode(enabled: false, child: SizedBox()),
        );
        await tester.pump(const Duration(milliseconds: 50));
        expect(ticks, greaterThan(0));
        expect(
          unmanaged.muted,
          isFalse,
          reason: 'TickerMode only controls widget-aware providers',
        );
      } finally {
        unmanaged.dispose();
      }
    },
  );
}

Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder _ticker() =>
    find.descendant(of: _widget(_id), matching: find.byType(TickerMode)).first;
Future<void> _pump(WidgetTester tester, Map<String, Object?> model) async {
  await tester.pumpWidget(
    CanvasModelApp(
      model: _decode(model),
      selectedWidgetId: null,
      onSelected: (_) {},
    ),
  );
  await tester.pump();
}

Future<void> _f2(WidgetTester tester) async {
  await tester.tapAt(
    tester.getTopLeft(
          find.byKey(const ValueKey('canvas-interaction-surface')),
        ) +
        const Offset(2, 2),
  );
  await tester.sendKeyEvent(LogicalKeyboardKey.f2);
  await tester.pump();
}

CanvasPaletteDragSource _source(String type) => CanvasPaletteDragSource(
  token: type,
  widgetType: type,
  traits: canvasWidgetTraitsForType(type),
);
CanvasDropTarget? _resolve(
  WidgetTester tester,
  CanvasDropResolver resolver,
  Offset point, [
  CanvasPaletteDragSource? source,
]) {
  final surface = tester.getRect(find.byType(CanvasDocumentView));
  return resolver(
    ((point.dx - surface.left) / surface.width * 1000000).round(),
    ((point.dy - surface.top) / surface.height * 1000000).round(),
    source,
  );
}

CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Map<String, Object?> _model({
  String platform = 'windows',
  bool? enabled = true,
  bool? forceFrames,
  Map<String, Object?>? child,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
  model['root'] = {
    'id': 'f775415c-4c7d-4467-b070-7555c3af053b',
    'type': 'flutter.material.Scaffold',
    'properties': <String, Object?>{},
    'slots': {
      'body': {
        'kind': 'single',
        'child': _node(
          'eb7804ec-1f0c-4c30-8037-5e6f89cc7f1d',
          'flutter.widgets.Center',
          child: _node(
            _id,
            _type,
            properties: {
              if (enabled != null) 'enabled': _bool(enabled),
              if (forceFrames != null) 'forceFrames': _bool(forceFrames),
            },
            child: child ?? _text(),
          ),
        ),
      },
    },
  };
  return model;
}

Map _centerSlot(Map<String, Object?> model) {
  final root = model['root']! as Map;
  final body = (root['slots']! as Map)['body']! as Map;
  final center = body['child']! as Map;
  return (center['slots']! as Map)['child']! as Map;
}

Map<String, Object?> _tickerJson(Map<String, Object?> model) =>
    _centerSlot(model)['child']! as Map<String, Object?>;
void _replaceCentered(Map<String, Object?> model, Map<String, Object?> node) =>
    _centerSlot(model)['child'] = node;
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _node(
  String id,
  String type, {
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
}) => {
  'id': id,
  'type': type,
  'properties': properties,
  'slots': {
    'child': {'kind': 'single', 'child': child},
  },
};
Map<String, Object?> _text() => {
  'id': _textId,
  'type': 'flutter.widgets.Text',
  'properties': {
    'data': {'kind': 'string', 'value': 'Ticker child'},
  },
  'slots': <String, Object?>{},
};
Map<String, Object?> _size(int width, int height) => {
  'width': {'kind': 'integer', 'value': width},
  'height': {'kind': 'integer', 'value': height},
};
Map<String, Object?> _list(String id, List<Object?> children) => {
  'id': id,
  'type': 'flutter.widgets.Column',
  'properties': <String, Object?>{},
  'slots': {
    'children': {'kind': 'list', 'children': children},
  },
};
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

Set<String> _semanticLabels(WidgetTester tester) {
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

class _SingleProbe extends StatefulWidget {
  const _SingleProbe({super.key});
  @override
  State<_SingleProbe> createState() => _SingleProbeState();
}

class _MultiProbe extends StatefulWidget {
  const _MultiProbe({super.key});
  @override
  State<_MultiProbe> createState() => _MultiProbeState();
}

abstract class _ProbeStateBase<T extends StatefulWidget> extends State<T>
    implements TickerProvider {
  int get tickerCount;
  late final List<Ticker> tickers;
  late final List<List<Duration>> elapsed;
  var disposed = false;
  @override
  void initState() {
    super.initState();
    elapsed = List.generate(tickerCount, (_) => <Duration>[]);
    tickers = List.generate(
      tickerCount,
      (index) => createTicker(elapsed[index].add)..start(),
    );
  }

  @override
  void dispose() {
    disposed = true;
    super.dispose();
  }

  void disposeTickers() {
    for (final ticker in tickers) {
      ticker.dispose();
    }
  }

  @override
  Widget build(BuildContext context) => const SizedBox(width: 10, height: 10);
}

class _SingleProbeState extends _ProbeStateBase<_SingleProbe>
    with SingleTickerProviderStateMixin<_SingleProbe> {
  @override
  int get tickerCount => 1;
  @override
  void dispose() {
    disposeTickers();
    super.dispose();
  }
}

class _MultiProbeState extends _ProbeStateBase<_MultiProbe>
    with TickerProviderStateMixin<_MultiProbe> {
  @override
  int get tickerCount => 2;
  @override
  void dispose() {
    disposeTickers();
    super.dispose();
  }
}

class _AnimationProbe extends StatefulWidget {
  const _AnimationProbe({super.key});
  @override
  State<_AnimationProbe> createState() => _AnimationProbeState();
}

class _AnimationProbeState extends State<_AnimationProbe>
    with SingleTickerProviderStateMixin {
  late final controller = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 1),
  );
  @override
  Widget build(BuildContext context) => const SizedBox();
  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }
}
