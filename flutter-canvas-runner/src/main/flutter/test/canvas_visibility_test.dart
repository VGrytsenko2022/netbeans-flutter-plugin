import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.widgets.Visibility';
const _id = 'a3867d28-9495-4dfc-8ec9-087075ffb9b3';
const _childId = 'b37a51a0-598e-4189-8f08-0a7dd6d5a772';
const _textId = 'bf6064c6-d24e-4f7d-802c-0cf1e54b0574';
const _replacementId = 'ce7074c6-d24e-4f7d-802c-0cf1e54b0574';
const _columnId = 'dbed0e25-f4ee-4b41-b6df-c0d1fe0bb8ee';
const _flags = [
  'visible',
  'maintainState',
  'maintainAnimation',
  'maintainSize',
  'maintainSemantics',
  'maintainInteractivity',
  'maintainFocusability',
];
const _allMaintain = <String, bool>{
  'maintainState': true,
  'maintainAnimation': true,
  'maintainSize': true,
  'maintainSemantics': true,
  'maintainInteractivity': true,
  'maintainFocusability': true,
};

void main() {
  test('Visibility exact constructor fingerprint and both slot contracts', () {
    final contract = canvasRuntimeWidgetSchemaContractForTesting();
    final start = contract.indexOf('W|$_type\n');
    final end = contract.indexOf('W|flutter.widgets.Wrap\n', start);
    final names = _flags.toList()..sort();
    expect(
      contract.substring(start, end),
      'W|$_type\n'
      '${names.map((name) => 'P|$name|boolean|0|-|-|boolean:any\n').join()}'
      'S|child|single|1|1|1|any\n'
      'S|replacement|single|0|0|1|any\n'
      'C|$_type|paletteCreate|wrapExistingChild|child\n',
    );
    expect(canvasModelProtocolVersion, 20);
    expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), isTrue);
    expect(isCanvasPaletteWrapperWidgetType(_type), isTrue);
    expect(canvasDropSlotsForWidgetType(_type).map((slot) => slot.slotName), [
      'replacement',
    ]);
    expect(_find(_decode(_model()).root, _id)!.properties, isEmpty);
  });

  test(
    'Visibility validates all 2187 omitted false true combinations using exactly the five SDK implications',
    () {
      var valid = 0;
      var invalid = 0;
      for (var permutation = 0; permutation < 2187; permutation++) {
        var remaining = permutation;
        final flags = <String, bool>{};
        for (final flag in _flags) {
          final value = remaining % 3;
          remaining ~/= 3;
          if (value != 0) flags[flag] = value == 2;
        }
        final accepted =
            (!(_on(flags, 'maintainAnimation')) ||
                _on(flags, 'maintainState')) &&
            (!_on(flags, 'maintainSize') || _on(flags, 'maintainAnimation')) &&
            (!_on(flags, 'maintainSemantics') || _on(flags, 'maintainSize')) &&
            (!_on(flags, 'maintainInteractivity') ||
                _on(flags, 'maintainSize')) &&
            (!_on(flags, 'maintainFocusability') ||
                _on(flags, 'maintainState'));
        if (accepted) {
          final node = _find(_decode(_model(flags: flags)).root, _id)!;
          expect(node.properties.length, flags.length);
          valid++;
        } else {
          expect(
            () => _decode(_model(flags: flags)),
            throwsFormatException,
            reason: '$flags must fail even when visible is true or omitted',
          );
          invalid++;
        }
      }
      expect(valid + invalid, 2187);
      expect(valid, greaterThan(0));
      expect(invalid, greaterThan(valid));
    },
  );

  test(
    'Visibility rejects malformed scalars and constructor fields encoded as scalars',
    () {
      for (final flag in _flags) {
        for (final value in [
          {'kind': 'null'},
          {'kind': 'integer', 'value': 1},
          {'kind': 'string', 'value': 'false'},
          {'kind': 'boolean', 'value': null},
          {'kind': 'boolean', 'value': 1},
          {'kind': 'boolean', 'value': false, 'extra': true},
        ]) {
          final model = _model();
          _visibilityJson(model)['properties'] = {flag: value};
          expect(() => _decode(model), throwsFormatException);
        }
      }
      for (final name in [
        'key',
        'child',
        'replacement',
        'maintain',
        'opacity',
      ]) {
        final model = _model();
        _visibilityJson(model)['properties'] = {
          name: {'kind': 'boolean', 'value': true},
        };
        expect(() => _decode(model), throwsFormatException);
      }
    },
  );

  test(
    'Visibility required child is strict and replacement supports omitted empty or one reviewed child',
    () {
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
          'child': {'kind': 'single', 'child': _text()},
          'replacement': {'kind': 'list', 'children': []},
        },
        {
          'child': {'kind': 'single', 'child': _text()},
          'children': {'kind': 'single', 'child': null},
        },
      ]) {
        final model = _model();
        _visibilityJson(model)['slots'] = slots;
        expect(() => _decode(model), throwsFormatException);
      }
      for (final replacement in [null, _text(id: _replacementId)]) {
        final model = _model(replacement: replacement);
        expect(
          _find(
                _decode(model).root,
                _id,
              )!.slot('replacement')?.children.length ??
              0,
          replacement == null ? 0 : 1,
        );
        final omitted = _model();
        (_visibilityJson(omitted)['slots']! as Map).remove('replacement');
        expect(() => _decode(omitted), returnsNormally);
      }
      for (final type in ['Expanded', 'Flexible', 'Spacer']) {
        expect(
          () =>
              _decode(_model(child: _node(_childId, 'flutter.widgets.$type'))),
          throwsFormatException,
        );
        expect(
          () => _decode(
            _model(replacement: _node(_replacementId, 'flutter.widgets.$type')),
          ),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'Visibility required-child wrapping and independent replacement acceptance stay host revalidatable',
    () {
      final wrapper = _source(_type);
      for (final slot in ['child', 'replacement']) {
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: _type,
            slotName: slot,
            currentChildCount: 1,
            insertionIndex: 0,
            source: wrapper,
          ),
          isTrue,
        );
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: _type,
            slotName: slot,
            currentChildCount: 0,
            insertionIndex: 0,
            source: wrapper,
          ),
          isFalse,
        );
      }
      expect(
        canvasDropTargetAcceptsSource(
          parentWidgetType: _type,
          slotName: 'replacement',
          currentChildCount: 0,
          insertionIndex: 0,
          source: _source('flutter.widgets.Text'),
        ),
        isTrue,
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
      expect(
        canvasWrapperAcceptsExistingChild(
          wrapperWidgetType: _type,
          childWidgetType: 'flutter.widgets.Expanded',
        ),
        isFalse,
      );
    },
  );

  for (final platform in ['windows', 'android']) {
    testWidgets(
      'Visibility actual SDK constructor defaults replacement clear and all-maintain equivalence on $platform',
      (tester) async {
        await _pump(tester, _model(platform: platform));
        final defaults = tester.widget<Visibility>(_visibility());
        expect(defaults.visible, isTrue);
        expect(_maintainValues(defaults), everyElement(isFalse));
        expect(defaults.replacement, isA<SizedBox>());
        expect((defaults.replacement as SizedBox).width, 0);
        await _pump(
          tester,
          _model(
            platform: platform,
            flags: {'visible': false},
            replacement: _text(id: _replacementId, label: 'Replacement'),
          ),
        );
        expect(find.text('Child'), findsNothing);
        expect(find.text('Replacement'), findsOneWidget);
        await _pump(
          tester,
          _model(platform: platform, flags: {'visible': false}),
        );
        expect(find.text('Replacement'), findsNothing);
        expect(tester.getSize(_widget(_id)), Size.zero);
        await _pump(
          tester,
          _model(
            platform: platform,
            flags: {'visible': false, ..._allMaintain},
          ),
        );
        final actual = tester.widget<Visibility>(_visibility());
        const named = Visibility.maintain(visible: false, child: Text('Child'));
        expect(actual.visible, named.visible);
        expect(_maintainValues(actual), _maintainValues(named));
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'Visibility child replacement switching uses genuine mounted branches and SDK Visibility.of on $platform',
      (tester) async {
        for (final visible in [true, false, true]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              flags: {'visible': visible},
              replacement: _text(id: _replacementId, label: 'Replacement'),
            ),
          );
          final active = _widget(visible ? _textId : _replacementId);
          expect(active, findsOneWidget);
          expect(_widget(visible ? _replacementId : _textId), findsNothing);
          expect(Visibility.of(tester.element(active)), visible);
        }
      },
    );

    testWidgets(
      'Visibility hidden retained subtree has no synthetic handles drop geometry or F2 resurrection on $platform',
      (tester) async {
        for (final flags in [
          <String, bool>{'visible': false},
          {'visible': false, 'maintainState': true},
          {'visible': false, ..._allMaintain},
        ]) {
          CanvasDropResolver? drop;
          CanvasMovePreviewResolver? move;
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  flags: flags,
                  child: _node(
                    _childId,
                    'flutter.widgets.SizedBox',
                    properties: _size(120, 80),
                    child: _list(_columnId, [_text()]),
                  ),
                ),
              ),
              selectedWidgetId: _textId,
              onSelected: (_) {},
              inlineTextEditEnabled: true,
              onDropResolverChanged: (value) => drop = value,
              onMovePreviewResolverChanged: (value) => move = value,
            ),
          );
          await tester.pump();
          await _f2(tester);
          expect(find.byType(TextField, skipOffstage: false), findsNothing);
          expect(
            find.byKey(const ValueKey('canvas-zero-sized-target-$_textId')),
            findsNothing,
          );
          expect(move!(_replacementId, _columnId, 'children', 0), isNull);
          final result = _resolve(
            tester,
            drop!,
            tester.getCenter(_widget(_id)),
          );
          expect(
            result?.parentWidgetId,
            isNot(anyOf(_childId, _columnId, _textId)),
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'Visibility active child and replacement preserve selection and F2 commit cancel reopen on $platform',
      (tester) async {
        for (final visible in [true, false]) {
          final activeId = visible ? _textId : _replacementId;
          final commits = <(String, String)>[];
          final selections = <String>[];
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  flags: {'visible': visible},
                  replacement: _text(id: _replacementId, label: 'Replacement'),
                ),
              ),
              selectedWidgetId: activeId,
              onSelected: selections.add,
              inlineTextEditEnabled: true,
              onInlineTextCommit: (id, text, _) {
                commits.add((id, text));
                return true;
              },
            ),
          );
          await tester.pump();
          await tester.tap(_widget(activeId));
          await tester.pump(const Duration(milliseconds: 350));
          expect(selections, contains(activeId));
          await _f2(tester);
          expect(
            tester
                .widget<TextField>(find.byType(TextField))
                .focusNode!
                .hasFocus,
            isTrue,
          );
          await tester.enterText(find.byType(TextField), 'Updated branch');
          await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
          await tester.sendKeyEvent(LogicalKeyboardKey.enter);
          await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
          await tester.pump();
          expect(commits, [(activeId, 'Updated branch')]);
          await tester.sendKeyEvent(LogicalKeyboardKey.f2);
          await tester.pump();
          await tester.sendKeyEvent(LogicalKeyboardKey.escape);
          await tester.pump();
          expect(find.byType(TextField), findsNothing);
          expect(commits, hasLength(1));
        }
      },
    );

    testWidgets(
      'Visibility hiding the active branch cancels F2 without committing its draft on $platform',
      (tester) async {
        var commits = 0;
        Future<void> show(bool visible) async {
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  flags: {'visible': visible, ..._allMaintain},
                ),
              ),
              selectedWidgetId: _textId,
              onSelected: (_) {},
              inlineTextEditEnabled: true,
              onInlineTextCommit: (_, _, _) {
                commits++;
                return true;
              },
            ),
          );
          await tester.pump();
        }

        await show(true);
        await _f2(tester);
        await tester.enterText(find.byType(TextField), 'Draft must not leak');
        await show(false);
        expect(find.byType(TextField, skipOffstage: false), findsNothing);
        expect(commits, 0);
        await show(true);
        expect(find.text('Child'), findsOneWidget);
        await _f2(tester);
        expect(
          tester.widget<TextField>(find.byType(TextField)).focusNode!.hasFocus,
          isTrue,
        );
        await tester.sendKeyEvent(LogicalKeyboardKey.escape);
        await tester.pump();
        expect(commits, 0);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'Visibility wrapping and drop previews expose only the active slot on $platform',
      (tester) async {
        for (final visible in [true, false]) {
          CanvasDropResolver? drop;
          CanvasMovePreviewResolver? move;
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  flags: {'visible': visible},
                  child: _node(
                    _childId,
                    'flutter.widgets.SizedBox',
                    properties: _size(80, 60),
                  ),
                  replacement: _node(
                    _replacementId,
                    'flutter.widgets.SizedBox',
                    properties: _size(70, 50),
                  ),
                ),
              ),
              selectedWidgetId: null,
              onSelected: (_) {},
              onDropResolverChanged: (value) => drop = value,
              onMovePreviewResolverChanged: (value) => move = value,
            ),
          );
          await tester.pump();
          final activeId = visible ? _childId : _replacementId;
          final activeSlot = visible ? 'child' : 'replacement';
          final result = _resolve(
            tester,
            drop!,
            tester.getCenter(_widget(activeId)),
            _source(_type),
          );
          expect(result?.parentWidgetId, _id);
          expect(result?.slotName, activeSlot);
          expect(
            move!(_childId, _id, 'replacement', 0),
            isNull,
            reason: 'required child cannot be moved out even when hidden',
          );
          if (!visible) {
            expect(
              move!(_replacementId, _id, 'replacement', 0)?.slotName,
              'replacement',
            );
          }
        }
      },
    );

    testWidgets(
      'Visibility empty replacement only exposes a destination while actually active on $platform',
      (tester) async {
        CanvasDropResolver? drop;
        for (final flags in [
          <String, bool>{},
          {'visible': false},
          {'visible': false, 'maintainState': true},
        ]) {
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(_model(platform: platform, flags: flags)),
              selectedWidgetId: null,
              onSelected: (_) {},
              onDropResolverChanged: (value) => drop = value,
            ),
          );
          await tester.pump();
          final result = _resolve(
            tester,
            drop!,
            tester.getCenter(_widget(_id)),
          );
          if (flags['visible'] == false && flags['maintainState'] != true) {
            expect(result?.parentWidgetId, _id);
            expect(result?.slotName, 'replacement');
          } else {
            expect(
              result?.parentWidgetId == _id &&
                  result?.slotName == 'replacement',
              isFalse,
            );
          }
        }
      },
    );

    testWidgets(
      'Visibility preview focus remains independent from project TextField privacy on $platform',
      (tester) async {
        await _pump(
          tester,
          _model(
            platform: platform,
            flags: _allMaintain,
            child: {
              'id': _childId,
              'type': 'flutter.material.TextField',
              'properties': <String, Object?>{},
              'slots': <String, Object?>{},
            },
          ),
        );
        final field = tester.widget<EditableText>(find.byType(EditableText));
        field.focusNode.requestFocus();
        await tester.pump();
        expect(field.focusNode.canRequestFocus, isFalse);
        expect(field.focusNode.hasFocus, isFalse);
        expect(field.controller.text, isEmpty);
      },
    );

    testWidgets(
      'Visibility maintained-size semantics transitions invalidate the actual SDK render object on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final maintainSemantics in [false, true]) {
            for (final visible in [true, false, true, false, true]) {
              await _pump(
                tester,
                _model(
                  platform: platform,
                  flags: {
                    'visible': visible,
                    ..._allMaintain,
                    'maintainSemantics': maintainSemantics,
                  },
                  child: _text(label: 'Maintained label'),
                ),
              );
              expect(
                _semanticLabels(
                  tester,
                ).any((label) => label.contains('Maintained label')),
                visible || maintainSemantics,
              );
              expect(tester.takeException(), isNull);
            }
          }
          // Changing maintain flags can legitimately replace the subtree, but
          // must not leave stale semantics or introduce a fake child.
          await _pump(
            tester,
            _model(
              platform: platform,
              flags: {'visible': false},
              replacement: _text(
                id: _replacementId,
                label: 'Replacement label',
              ),
            ),
          );
          expect(
            _semanticLabels(
              tester,
            ).any((label) => label.contains('Maintained label')),
            isFalse,
          );
          expect(
            _semanticLabels(
              tester,
            ).any((label) => label.contains('Replacement label')),
            isTrue,
          );
          await _pump(
            tester,
            _model(platform: platform, flags: {'visible': false}),
          );
          expect(
            _semanticLabels(
              tester,
            ).any((label) => label.contains('Replacement label')),
            isFalse,
          );
          expect(tester.takeException(), isNull);
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'Visibility actual Canvas paint intrinsic size and button focus respect each maintained branch on $platform',
      (tester) async {
        for (final maintainSize in [false, true]) {
          for (final visible in [true, false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                flags: {
                  'visible': visible,
                  'maintainState': true,
                  'maintainAnimation': true,
                  'maintainSize': maintainSize,
                },
                child: _node(
                  _childId,
                  'flutter.widgets.ColoredBox',
                  properties: {
                    'color': {'kind': 'color', 'argb': '0xFFFF0000'},
                  },
                  child: _node(
                    _textId,
                    'flutter.widgets.SizedBox',
                    properties: _size(90, 40),
                  ),
                ),
              ),
            );
            final render = tester.renderObject<RenderBox>(_visibility());
            expect(
              render.size,
              visible || maintainSize ? const Size(90, 40) : Size.zero,
            );
            expect(
              render.getMinIntrinsicWidth(100),
              visible || maintainSize ? 90 : 0,
            );
            expect(
              render.getMaxIntrinsicHeight(100),
              visible || maintainSize ? 40 : 0,
            );
            if (visible) {
              expect(render, paints..rect(color: const Color(0xFFFF0000)));
            } else {
              expect(render, paintsNothing);
            }
          }
        }
        for (final maintainFocusability in [false, true]) {
          FocusNode? prior;
          for (final visible in [true, false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                flags: {
                  'visible': visible,
                  'maintainState': true,
                  'maintainFocusability': maintainFocusability,
                },
                child: _node(
                  _childId,
                  'flutter.material.ElevatedButton',
                  child: _text(label: 'Button'),
                ),
              ),
            );
            final element = tester.element(
              find.text('Button', skipOffstage: false),
            );
            final focus = Focus.of(element);
            if (prior != null) expect(identical(focus, prior), isTrue);
            prior = focus;
            focus.requestFocus();
            await tester.pump();
            expect(focus.hasFocus, visible || maintainFocusability);
            expect(tester.takeException(), isNull);
          }
          await tester.pumpWidget(const SizedBox());
        }
      },
    );

    testWidgets(
      'Visibility hidden zero-size descendants never leak synthetic handles on $platform',
      (tester) async {
        for (final visible in [true, false]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              flags: {'visible': visible, 'maintainState': true},
              child: _node(
                _childId,
                'flutter.widgets.SizedBox',
                properties: _size(0, 0),
              ),
              replacement: _node(
                _replacementId,
                'flutter.widgets.SizedBox',
                properties: _size(0, 0),
              ),
            ),
          );
          final targets = find.byWidgetPredicate(
            (widget) =>
                widget.key is ValueKey<String> &&
                (widget.key! as ValueKey<String>).value.startsWith(
                  'canvas-zero-size-widget-target-',
                ),
          );
          final keys = tester
              .widgetList(targets)
              .map((widget) => (widget.key! as ValueKey<String>).value)
              .join(' ');
          expect(keys.contains(_id), isTrue);
          expect(
            keys.contains('group-$_id'),
            visible,
            reason:
                'only the visible zero-size child may join the wrapper target group',
          );
          expect(keys.contains(_replacementId), isFalse);
        }
      },
    );

    testWidgets(
      'Visibility nested IndexedSemantics and MergeSemantics retain correct application label transitions on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          for (final visible in [true, false, true]) {
            final inner = _node(
              _childId,
              _type,
              properties: {
                for (final entry in {
                  'visible': visible,
                  ..._allMaintain,
                  'maintainSemantics': false,
                }.entries)
                  entry.key: {'kind': 'boolean', 'value': entry.value},
              },
              child: _node(
                _columnId,
                'flutter.widgets.MergeSemantics',
                child: _node(
                  _replacementId,
                  'flutter.widgets.IndexedSemantics',
                  properties: {
                    'index': {'kind': 'integer', 'value': 3},
                  },
                  child: _text(label: 'Nested semantic label'),
                ),
              ),
            );
            await _pump(
              tester,
              _model(platform: platform, flags: _allMaintain, child: inner),
            );
            expect(
              _semanticLabels(
                tester,
              ).any((label) => label.contains('Nested semantic label')),
              visible,
            );
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );
  }

  for (final mode in [
    <String, bool>{},
    {'maintainState': true},
    {'maintainState': true, 'maintainAnimation': true},
    {'maintainState': true, 'maintainAnimation': true, 'maintainSize': true},
    {
      'maintainState': true,
      'maintainAnimation': true,
      'maintainSize': true,
      'maintainSemantics': true,
    },
    {
      'maintainState': true,
      'maintainAnimation': true,
      'maintainSize': true,
      'maintainInteractivity': true,
    },
    {'maintainState': true, 'maintainFocusability': true},
    _allMaintain,
  ]) {
    testWidgets(
      'SDK Visibility state tickers paint semantics hit size and focus mode $mode',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          final stateKey = GlobalKey<_ProbeState>();
          final focus = FocusNode();
          addTearDown(focus.dispose);
          var disposed = 0;
          var tapped = 0;
          final probe = _Probe(
            key: stateKey,
            focus: focus,
            onDisposed: () => disposed++,
            onTap: () => tapped++,
          );
          Widget build(bool visible) => MaterialApp(
            home: Center(
              child: _sdkVisibility(
                flags: {'visible': visible, ...mode},
                child: probe,
              ),
            ),
          );
          await tester.pumpWidget(build(true));
          await tester.pump(const Duration(milliseconds: 50));
          final original = stateKey.currentState!;
          final rect = tester.getRect(find.byKey(const ValueKey('probe-box')));
          focus.requestFocus();
          await tester.pump();
          expect(focus.hasFocus, isTrue);
          await tester.pumpWidget(build(false));
          final beforeTicks = original.ticks;
          await tester.pump(const Duration(milliseconds: 50));
          final retained = _on(mode, 'maintainState');
          expect(stateKey.currentState != null, retained);
          expect(disposed, retained ? 0 : 1);
          if (retained) {
            expect(identical(stateKey.currentState, original), isTrue);
            expect(
              original.ticks > beforeTicks,
              _on(mode, 'maintainAnimation'),
            );
            expect(
              TickerMode.valuesOf(original.context).enabled,
              _on(mode, 'maintainAnimation'),
            );
            expect(Visibility.of(original.context), isFalse);
            expect(focus.hasFocus, _on(mode, 'maintainFocusability'));
            focus.requestFocus();
            await tester.pump();
            expect(focus.hasFocus, _on(mode, 'maintainFocusability'));
          }
          final visibility = find.byType(Visibility).first;
          expect(
            tester.getSize(visibility),
            _on(mode, 'maintainSize') ? const Size(90, 40) : Size.zero,
          );
          expect(
            _semanticLabels(tester).contains('Probe label'),
            _on(mode, 'maintainSemantics') ||
                _on(mode, 'maintainInteractivity'),
            reason:
                'Pinned uninstrumented Flutter 3.44.8 characterization: '
                'when IgnorePointer remains unchanged, Visibility.visible only '
                'invalidates paint and a cached child keeps stale semantics. '
                'The separate Canvas transition tests require corrected semantics.',
          );
          await tester.tapAt(rect.center);
          await tester.pump();
          expect(tapped, _on(mode, 'maintainInteractivity') ? 1 : 0);
          expect(tester.takeException(), isNull, reason: 'hidden interactions');
          await tester.pumpWidget(build(true));
          await tester.pump();
          expect(stateKey.currentState, isNotNull);
          expect(identical(stateKey.currentState, original), retained);
          expect(
            focus.hasFocus,
            retained && _on(mode, 'maintainFocusability'),
            reason:
                'restoring visibility does not automatically refocus an excluded child',
          );
          await tester.pumpWidget(const SizedBox());
          expect(tester.takeException(), isNull);
        } finally {
          semantics.dispose();
        }
      },
    );
  }

  testWidgets(
    'SDK nested Visibility.of composes nearest and outer visibility without changing maintain contracts',
    (tester) async {
      for (final outer in [true, false]) {
        for (final inner in [true, false]) {
          bool? effective;
          await tester.pumpWidget(
            MaterialApp(
              home: Visibility.maintain(
                visible: outer,
                child: Visibility.maintain(
                  visible: inner,
                  child: Builder(
                    builder: (context) {
                      effective = Visibility.of(context);
                      return const SizedBox(width: 10, height: 10);
                    },
                  ),
                ),
              ),
            ),
          );
          expect(effective, outer && inner);
        }
      }
    },
  );
}

bool _on(Map<String, bool> flags, String name) => flags[name] == true;
List<bool> _maintainValues(Visibility widget) => [
  widget.maintainState,
  widget.maintainAnimation,
  widget.maintainSize,
  widget.maintainSemantics,
  widget.maintainInteractivity,
  widget.maintainFocusability,
];
Visibility _sdkVisibility({
  required Map<String, bool> flags,
  required Widget child,
}) => Visibility(
  visible: flags['visible'] ?? true,
  maintainState: _on(flags, 'maintainState'),
  maintainAnimation: _on(flags, 'maintainAnimation'),
  maintainSize: _on(flags, 'maintainSize'),
  maintainSemantics: _on(flags, 'maintainSemantics'),
  maintainInteractivity: _on(flags, 'maintainInteractivity'),
  maintainFocusability: _on(flags, 'maintainFocusability'),
  child: child,
);
Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
Finder _visibility() =>
    find.descendant(of: _widget(_id), matching: find.byType(Visibility)).first;
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
  Map<String, bool> flags = const {},
  Map<String, Object?>? child,
  Map<String, Object?>? replacement,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
  final visibility = _node(
    _id,
    _type,
    properties: {
      for (final entry in flags.entries)
        entry.key: {'kind': 'boolean', 'value': entry.value},
    },
    child: child ?? _text(),
  );
  (visibility['slots']! as Map<String, Object?>)['replacement'] = {
    'kind': 'single',
    'child': replacement,
  };
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
          child: visibility,
        ),
      },
    },
  };
  return model;
}

Map<String, Object?> _visibilityJson(Map<String, Object?> model) {
  final root = model['root']! as Map;
  final body = (root['slots']! as Map)['body']! as Map;
  final center = body['child']! as Map;
  final child = (center['slots']! as Map)['child']! as Map;
  return child['child']! as Map<String, Object?>;
}

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
Map<String, Object?> _text({String id = _textId, String label = 'Child'}) => {
  'id': id,
  'type': 'flutter.widgets.Text',
  'properties': {
    'data': {'kind': 'string', 'value': label},
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

class _Probe extends StatefulWidget {
  const _Probe({
    super.key,
    required this.focus,
    required this.onDisposed,
    required this.onTap,
  });
  final FocusNode focus;
  final VoidCallback onDisposed;
  final VoidCallback onTap;
  @override
  State<_Probe> createState() => _ProbeState();
}

class _ProbeState extends State<_Probe> with SingleTickerProviderStateMixin {
  late final AnimationController controller;
  var ticks = 0;
  @override
  void initState() {
    super.initState();
    controller =
        AnimationController(vsync: this, duration: const Duration(seconds: 1))
          ..addListener(() => ticks++)
          ..repeat();
  }

  @override
  void dispose() {
    controller.dispose();
    widget.onDisposed();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Focus(
    focusNode: widget.focus,
    includeSemantics: false,
    child: Semantics(
      label: 'Probe label',
      child: GestureDetector(
        onTap: widget.onTap,
        excludeFromSemantics: true,
        child: const ColoredBox(
          color: Colors.red,
          child: SizedBox(key: ValueKey('probe-box'), width: 90, height: 40),
        ),
      ),
    ),
  );
}
