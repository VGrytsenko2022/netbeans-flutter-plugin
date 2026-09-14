import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.widgets.ExcludeFocus';
const _id = 'a3867d28-9495-4dfc-8ec9-087075ffb9b3';
const _childId = 'b37a51a0-598e-4189-8f08-0a7dd6d5a772';
const _textId = 'bf6064c6-d24e-4f7d-802c-0cf1e54b0574';
const _otherId = 'ce7074c6-d24e-4f7d-802c-0cf1e54b0574';
const _columnId = 'dbed0e25-f4ee-4b41-b6df-c0d1fe0bb8ee';

void main() {
  test(
    'ExcludeFocus exact optional boolean and required child wrapper contract',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final end = contract.indexOf(
        'W|flutter.widgets.ExcludeFocusTraversal\n',
        start,
      );
      expect(
        contract.substring(start, end),
        'W|$_type\nP|excluding|boolean|0|-|-|boolean:any\nS|child|single|1|1|1|any\nC|$_type|paletteCreate|wrapExistingChild|child\n',
      );
      expect(canvasModelProtocolVersion, 19);
      expect(isCanvasReviewedWidgetType(_type), isTrue);
      expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), isTrue);
      expect(isCanvasPaletteWrapperWidgetType(_type), isTrue);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      for (final value in [null, false, true]) {
        final node = _find(_decode(_model(excluding: value)).root, _id)!;
        expect(node.properties['excluding']?.value, value);
        expect(node.slot('child')!.child!.id, _textId);
      }
    },
  );

  test(
    'ExcludeFocus rejects missing empty wrong-shaped child and unreviewed scalar states',
    () {
      for (final slots in [
        <String, Object?>{},
        {
          'child': {'kind': 'single', 'child': null},
        },
        {
          'child': {'kind': 'list', 'children': <Object?>[]},
        },
        {
          'children': {'kind': 'single', 'child': _text()},
        },
        {
          'child': {'kind': 'single', 'child': _text(), 'extra': true},
        },
      ]) {
        final model = _model();
        _findJson(model['root']! as Map<String, Object?>, _id)!['slots'] =
            slots;
        expect(() => _decode(model), throwsFormatException);
      }
      for (final value in [
        {'kind': 'null'},
        {'kind': 'integer', 'value': 1},
        {'kind': 'string', 'value': 'false'},
        {'kind': 'boolean', 'value': null},
        {'kind': 'boolean', 'value': 1},
        {'kind': 'boolean', 'value': false, 'extra': true},
        {'kind': 'dartExpression', 'value': 'projectCode()'},
      ]) {
        final model = _model();
        _findJson(model['root']! as Map<String, Object?>, _id)!['properties'] =
            {'excluding': value};
        expect(() => _decode(model), throwsFormatException);
      }
      for (final name in [
        'key',
        'child',
        'autofocus',
        'canRequestFocus',
        'descendantsAreFocusable',
        'includeSemantics',
      ]) {
        final model = _model();
        _findJson(
          model['root']! as Map<String, Object?>,
          _id,
        )!['properties'] = {
          name: {'kind': 'boolean', 'value': true},
        };
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
    'ExcludeFocus atomically wraps ordinary occupied slots without adding a required-child insertion destination',
    () {
      final source = CanvasPaletteDragSource(
        token: _type,
        widgetType: _type,
        traits: const {},
      );
      for (final parent in [
        (type: 'flutter.widgets.Column', slot: 'children'),
        (type: 'flutter.widgets.Center', slot: 'child'),
        (type: _type, slot: 'child'),
      ]) {
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: parent.type,
            slotName: parent.slot,
            currentChildCount: 1,
            insertionIndex: 0,
            source: source,
          ),
          isTrue,
        );
        expect(
          canvasDropTargetAcceptsSource(
            parentWidgetType: parent.type,
            slotName: parent.slot,
            currentChildCount: 0,
            insertionIndex: 0,
            source: source,
          ),
          isFalse,
        );
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
          source: CanvasPaletteDragSource(
            token: 'text',
            widgetType: 'flutter.widgets.Text',
            traits: const {},
          ),
        ),
        isFalse,
      );
    },
  );

  testWidgets(
    'SDK ExcludeFocus blocks explicit TextField focus without disabling its local request setting',
    (tester) async {
      final focus = FocusNode();
      addTearDown(focus.dispose);
      for (final excluding in [false, true, false]) {
        await tester.pumpWidget(
          MaterialApp(
            home: Material(
              child: ExcludeFocus(
                excluding: excluding,
                child: TextField(focusNode: focus),
              ),
            ),
          ),
        );
        await tester.pump();
        expect(
          focus.hasFocus,
          isFalse,
          reason: 'reenabling does not restore former focus',
        );
        focus.requestFocus();
        await tester.pump();
        expect(focus.hasFocus, !excluding);
        expect(focus.canRequestFocus, !excluding);
      }
      await tester.pumpWidget(const SizedBox.shrink());
    },
  );

  for (final platform in ['windows', 'web']) {
    testWidgets(
      'ExcludeFocus uses actual SDK Focus properties and live retained button focus on $platform',
      (tester) async {
        FocusNode? retained;
        for (final excluding in [null, false, true, false]) {
          await _pump(
            tester,
            _model(platform: platform, excluding: excluding, child: _button()),
          );
          final widget = tester.widget<ExcludeFocus>(_excludeFinder());
          expect(widget.excluding, excluding ?? true);
          final focusWidget = tester.widget<Focus>(
            find
                .descendant(of: _excludeFinder(), matching: find.byType(Focus))
                .first,
          );
          expect(focusWidget.canRequestFocus, isFalse);
          expect(focusWidget.skipTraversal, isTrue);
          expect(focusWidget.includeSemantics, isFalse);
          expect(focusWidget.descendantsAreFocusable, !(excluding ?? true));
          final node = _buttonFocus(tester, 'Focus target');
          retained ??= node;
          expect(node, same(retained));
          expect(
            node.hasFocus,
            isFalse,
            reason:
                'active descendants lose focus, false never automatically restores it',
          );
          node.requestFocus();
          await tester.pump();
          expect(node.hasFocus, !(excluding ?? true));
          expect(node.canRequestFocus, !(excluding ?? true));
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'ExcludeFocus real traversal skips only the excluded subtree on $platform',
      (tester) async {
        for (final excluding in [null, false, true]) {
          final model = _model(
            platform: platform,
            excluding: excluding,
            child: _button(),
          );
          final wrapper = _findJson(
            model['root']! as Map<String, Object?>,
            _id,
          )!;
          _replaceCentered(
            model,
            _list(_columnId, [
              _button(
                id: '28b948db-7965-4729-9889-a017de92e97b',
                textId: '567c1346-4098-4e23-a5f8-edcf4cbdcb52',
                label: 'Before',
              ),
              wrapper,
              _button(
                id: _otherId,
                textId: 'ecbb3bcb-a378-4280-803f-608cd35e65a3',
                label: 'After',
              ),
            ]),
          );
          await _pump(tester, model);
          final before = _buttonFocus(tester, 'Before');
          final target = _buttonFocus(tester, 'Focus target');
          final after = _buttonFocus(tester, 'After');
          before.requestFocus();
          await tester.pump();
          expect(before.hasFocus, isTrue);
          expect(before.nextFocus(), isTrue);
          await tester.pump();
          expect(
            FocusManager.instance.primaryFocus,
            same((excluding ?? true) ? after : target),
          );
          after.requestFocus();
          await tester.pump();
          expect(after.previousFocus(), isTrue);
          await tester.pump();
          expect(
            FocusManager.instance.primaryFocus,
            same((excluding ?? true) ? before : target),
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'ExcludeFocus nested exclusions compose and reparenting restores application focus on $platform',
      (tester) async {
        for (final outer in [false, true]) {
          for (final inner in [false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                excluding: outer,
                child: _node(
                  _otherId,
                  _type,
                  properties: {
                    'excluding': {'kind': 'boolean', 'value': inner},
                  },
                  child: _button(),
                ),
              ),
            );
            final focus = _buttonFocus(tester, 'Focus target');
            focus.requestFocus();
            await tester.pump();
            expect(focus.hasFocus, !outer && !inner);
            expect(tester.takeException(), isNull);
          }
        }
        for (final outside in [true, false, true]) {
          FocusManager.instance.primaryFocus?.unfocus();
          await tester.pump();
          final model = _model(
            platform: platform,
            child: outside
                ? _text(label: 'Retained required child')
                : _button(),
          );
          if (outside) {
            final wrapper = _findJson(
              model['root']! as Map<String, Object?>,
              _id,
            )!;
            _replaceCentered(
              model,
              _list(_columnId, [wrapper, _button(textId: _otherId)]),
            );
          }
          await _pump(tester, model);
          final focus = _buttonFocus(tester, 'Focus target');
          focus.requestFocus();
          await tester.pump();
          expect(focus.hasFocus, outside);
        }
      },
    );

    testWidgets(
      'ExcludeFocus retains application semantics and pointer selection on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        final selected = <String>[];
        for (final excluding in [null, false, true]) {
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(
                _model(
                  platform: platform,
                  excluding: excluding,
                  child: _button(),
                ),
              ),
              selectedWidgetId: null,
              onSelected: selected.add,
            ),
          );
          await tester.pump();
          final nodes = _semantics(tester);
          final button = nodes.singleWhere(
            (n) => n.getSemanticsData().label.contains('Focus target'),
          );
          expect(
            button.getSemanticsData().hasAction(SemanticsAction.tap),
            isTrue,
          );
          tester.binding.renderViews.single.owner!.semanticsOwner!
              .performAction(button.id, SemanticsAction.tap);
          await tester.pump();
          expect(
            selected.last,
            _textId,
            reason:
                'ordinary Designer semantic selection remains available; ExcludeFocus does not exclude semantics',
          );
          await tester.tap(find.text('Focus target'));
          await tester.pump();
          expect(selected.last, _textId);
          expect(tester.takeException(), isNull);
        }
        handle.dispose();
      },
    );

    testWidgets(
      'ExcludeFocus preserves layout paint hit testing and child updates on $platform',
      (tester) async {
        RenderBox? retained;
        for (final entry in [
          (excluding: true, width: 80, height: 50, argb: '0xFF123456'),
          (excluding: false, width: 120, height: 90, argb: '0x80112233'),
        ]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              excluding: entry.excluding,
              child: _node(
                _childId,
                'flutter.widgets.ColoredBox',
                properties: {
                  'color': {'kind': 'color', 'argb': entry.argb},
                },
                child: _sized(entry.width, entry.height),
              ),
            ),
          );
          final render = tester.renderObject<RenderBox>(_widget());
          retained ??= render;
          expect(render, same(retained));
          expect(
            render.size,
            Size(entry.width.toDouble(), entry.height.toDouble()),
          );
          expect(render.getMinIntrinsicWidth(100), entry.width);
          expect(render.getMaxIntrinsicWidth(100), entry.width);
          expect(render.getMinIntrinsicHeight(100), entry.height);
          expect(render.getMaxIntrinsicHeight(100), entry.height);
          expect(
            render.hitTest(BoxHitTestResult(), position: const Offset(20, 20)),
            isTrue,
          );
          expect(
            render.hitTest(
              BoxHitTestResult(),
              position: Offset(entry.width + 1, 20),
            ),
            isFalse,
          );
          expect(
            tester.renderObject(
              find
                  .descendant(of: _widget(), matching: find.byType(ColoredBox))
                  .first,
            ),
            paints..rect(
              color: Color(int.parse(entry.argb.substring(2), radix: 16)),
            ),
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'ExcludeFocus zero-size required child uses external selection without a fake child on $platform',
      (tester) async {
        final selected = <String>[];
        CanvasMovePreviewResolver? move;
        final model = _model(platform: platform, child: _sized(0, 0));
        final wrapper = _findJson(model['root']! as Map<String, Object?>, _id)!;
        _replaceCentered(
          model,
          _list(_columnId, [
            wrapper,
            _node(_otherId, 'flutter.widgets.Center'),
          ]),
        );
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(model),
            selectedWidgetId: null,
            onSelected: selected.add,
            onMovePreviewResolverChanged: (value) => move = value,
          ),
        );
        await tester.pump();
        expect(tester.getSize(_widget()), Size.zero);
        final target = find.byKey(
          const ValueKey('canvas-zero-size-widget-target-group-$_id'),
        );
        expect(tester.getSize(target), const Size.square(36));
        expect(
          find.descendant(
            of: _excludeFinder(),
            matching: find.byType(SizedBox),
          ),
          findsOneWidget,
        );
        await tester.tap(target);
        await tester.pump();
        expect(selected.last, _id);
        expect(move!(_id, _otherId, 'child', 0)?.parentWidgetId, _otherId);
        expect(
          move!(_textId, _id, 'child', 0),
          isNotNull,
          reason: 'no-op move into the same required slot is stable',
        );
        expect(
          move!(_textId, _otherId, 'child', 0),
          isNull,
          reason:
              'move preview must not promise a move that empties the required source slot',
        );
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'required-child move previews reject source underflow for all reviewed wrappers on $platform',
      (tester) async {
        const destination = '0bc023cc-361b-4abc-a0ce-5a2765d2c7d9';
        const bound = '16791a40-5145-41a1-a6c3-7f6f5c8100ae';
        for (final type in [
          'ExcludeFocus',
          'ExcludeFocusTraversal',
          'SafeArea',
          'Directionality',
          'Expanded',
          'Flexible',
          'Center',
          'Column',
        ]) {
          CanvasMovePreviewResolver? move;
          final model = _model(platform: platform);
          final wrapper = type == 'Column'
              ? _list(_id, [_sized(80, 60)])
              : _node(
                  _id,
                  'flutter.widgets.$type',
                  properties: {
                    if (type == 'Directionality')
                      'textDirection': {
                        'kind': 'enum',
                        'type': 'TextDirection',
                        'value': 'rtl',
                      },
                  },
                  child: _sized(80, 60),
                );
          _replaceCentered(
            model,
            _list(_columnId, [
              wrapper,
              _node(
                bound,
                'flutter.widgets.SizedBox',
                properties: {
                  'height': {'kind': 'integer', 'value': 60},
                },
                child: _list(destination, []),
              ),
            ]),
          );
          await tester.pumpWidget(
            CanvasModelApp(
              model: _decode(model),
              selectedWidgetId: null,
              onSelected: (_) {},
              onMovePreviewResolverChanged: (value) => move = value,
            ),
          );
          await tester.pump();
          final required = !['Center', 'Column'].contains(type);
          expect(
            move!(_textId, destination, 'children', 0),
            required ? isNull : isNotNull,
            reason: type,
          );
          expect(
            move!(_textId, _id, type == 'Column' ? 'children' : 'child', 0),
            isNotNull,
            reason: '$type same-slot no-op stays valid',
          );
          expect(
            move!(_id, destination, 'children', 0),
            isNotNull,
            reason: '$type wrapper can move intact without losing its child',
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'ExcludeFocus keeps the separate project TextField preview focus guard intact on $platform',
      (tester) async {
        for (final excluding in [null, false, true]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              excluding: excluding,
              child: _node(
                _childId,
                'flutter.widgets.SizedBox',
                properties: {
                  'width': {'kind': 'integer', 'value': 180},
                  'height': {'kind': 'integer', 'value': 48},
                },
                child: {
                  'id': _otherId,
                  'type': 'flutter.material.TextField',
                  'properties': {
                    'autofocus': {'kind': 'boolean', 'value': true},
                    'canRequestFocus': {'kind': 'boolean', 'value': true},
                  },
                  'slots': <String, Object?>{},
                },
              ),
            ),
          );
          final editable = tester.widget<EditableText>(
            find.byType(EditableText),
          );
          editable.focusNode.requestFocus();
          await tester.pump();
          expect(editable.focusNode.hasFocus, isFalse);
          expect(
            editable.focusNode.canRequestFocus,
            isFalse,
            reason:
                'the existing safe project preview guard is independent of ExcludeFocus.excluding',
          );
          expect(
            tester.widget<ExcludeFocus>(_excludeFinder()).excluding,
            excluding ?? true,
          );
          expect(
            tester
                .widgetList<ExcludeFocus>(find.byType(ExcludeFocus))
                .last
                .excluding,
            isTrue,
          );
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'ExcludeFocus preserves Indexed and Merge application semantics scopes on $platform',
      (tester) async {
        final handle = tester.ensureSemantics();
        for (final type in [
          'flutter.widgets.IndexedSemantics',
          'flutter.widgets.MergeSemantics',
        ]) {
          for (final excluding in [false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                excluding: excluding,
                child: _node(
                  _childId,
                  type,
                  properties: {
                    if (type.endsWith('IndexedSemantics'))
                      'index': {'kind': 'integer', 'value': 3},
                  },
                  child: _text(),
                ),
              ),
            );
            final nodes = _semantics(tester)
                .where(
                  (n) => n.getSemanticsData().label.contains('Focus target'),
                )
                .toList();
            expect(nodes, hasLength(1));
            expect(
              nodes.single.getSemanticsData().label,
              isNot(contains(_textId)),
            );
            if (type.endsWith('IndexedSemantics')) {
              expect(nodes.single.indexInParent, 3);
            }
            expect(tester.takeException(), isNull);
          }
        }
        handle.dispose();
      },
    );

    testWidgets(
      'ExcludeFocus supports nested required-child wrapping and descendant geometric insertion on $platform',
      (tester) async {
        CanvasDropResolver? resolver;
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(_model(platform: platform, child: _sized(80, 60))),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
          ),
        );
        await tester.pump();
        final point = tester.getCenter(
          find.byKey(const ValueKey('canvas-widget-$_textId')),
        );
        final wrapped = _resolve(
          tester,
          resolver!,
          point,
          CanvasPaletteDragSource(
            token: _type,
            widgetType: _type,
            traits: const {},
          ),
        );
        expect(wrapped?.parentWidgetId, _id);
        expect(wrapped?.slotName, 'child');
        expect(wrapped?.insertionIndex, 0);
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                platform: platform,
                child: _node(
                  _childId,
                  'flutter.widgets.SizedBox',
                  properties: {
                    'width': {'kind': 'integer', 'value': 120},
                    'height': {'kind': 'integer', 'value': 100},
                  },
                  child: _list(_columnId, []),
                ),
              ),
            ),
            selectedWidgetId: null,
            onSelected: (_) {},
            onDropResolverChanged: (value) => resolver = value,
          ),
        );
        await tester.pump();
        final childPoint = tester.getCenter(
          find.byKey(const ValueKey('canvas-widget-$_columnId')),
        );
        final drop = _resolve(tester, resolver!, childPoint);
        expect(drop?.parentWidgetId, _columnId);
        expect(drop?.slotName, 'children');
        expect(drop?.insertionIndex, 0);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'ExcludeFocus F2 Designer editor remains focusable without reenabling the application subtree on $platform',
      (tester) async {
        final commits = <(String, String)>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                platform: platform,
                child: _list(_columnId, [
                  _text(),
                  _button(
                    id: _otherId,
                    textId: _childId,
                    label: 'Excluded real button',
                  ),
                ]),
              ),
            ),
            selectedWidgetId: _textId,
            onSelected: (_) {},
            inlineTextEditEnabled: true,
            onInlineTextCommit: (id, text, _) {
              commits.add((id, text));
              return true;
            },
          ),
        );
        await tester.pump();
        await tester.tapAt(
          tester.getTopLeft(
                find.byKey(const ValueKey('canvas-interaction-surface')),
              ) +
              const Offset(2, 2),
        );
        await tester.sendKeyEvent(LogicalKeyboardKey.f2);
        await tester.pump();
        final editor = find.byKey(
          const ValueKey('canvas-inline-text-editor-$_textId'),
        );
        expect(editor, findsOneWidget);
        final field = tester.widget<TextField>(
          find.descendant(of: editor, matching: find.byType(TextField)),
        );
        expect(field.focusNode!.hasFocus, isTrue);
        expect(tester.widget<ExcludeFocus>(_excludeFinder()).excluding, isTrue);
        final appFocus = _buttonFocus(tester, 'Excluded real button');
        expect(appFocus.canRequestFocus, isFalse);
        appFocus.requestFocus();
        await tester.pump();
        expect(appFocus.hasFocus, isFalse);
        expect(field.focusNode!.hasFocus, isTrue);
        await tester.enterText(
          find.byType(TextField),
          'Designer edit inside ExcludeFocus',
        );
        await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
        await tester.sendKeyEvent(LogicalKeyboardKey.enter);
        await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
        await tester.pump();
        expect(commits, [(_textId, 'Designer edit inside ExcludeFocus')]);
        expect(editor, findsNothing);
        expect(tester.takeException(), isNull);
      },
    );

    testWidgets(
      'ExcludeFocus F2 Escape cancels without changing exclusion or emitting a commit on $platform',
      (tester) async {
        var commits = 0;
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(_model(platform: platform)),
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
        await tester.tapAt(
          tester.getTopLeft(
                find.byKey(const ValueKey('canvas-interaction-surface')),
              ) +
              const Offset(2, 2),
        );
        await tester.sendKeyEvent(LogicalKeyboardKey.f2);
        await tester.pump();
        await tester.enterText(find.byType(TextField), 'Discard draft');
        await tester.sendKeyEvent(LogicalKeyboardKey.escape);
        await tester.pump();
        expect(find.byType(TextField), findsNothing);
        expect(find.text('Focus target'), findsOneWidget);
        expect(commits, 0);
        expect(tester.widget<ExcludeFocus>(_excludeFinder()).excluding, isTrue);
        await tester.sendKeyEvent(LogicalKeyboardKey.f2);
        await tester.pump();
        expect(
          tester.widget<TextField>(find.byType(TextField)).focusNode!.hasFocus,
          isTrue,
          reason:
              'closing the service editor restores Designer keyboard ownership',
        );
        expect(tester.takeException(), isNull);
      },
    );
  }
}

FocusNode _buttonFocus(WidgetTester tester, String label) =>
    Focus.of(tester.element(find.text(label)));
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

Finder _widget() => find.byKey(const ValueKey('canvas-widget-$_id'));
Finder _excludeFinder() =>
    find.descendant(of: _widget(), matching: find.byType(ExcludeFocus)).first;
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

CanvasModel _decode(Map<String, Object?> model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Map<String, Object?> _model({
  String platform = 'windows',
  bool? excluding,
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
              if (excluding != null)
                'excluding': {'kind': 'boolean', 'value': excluding},
            },
            child: child ?? _text(),
          ),
        ),
      },
    },
  };
  return model;
}

void _replaceCentered(
  Map<String, Object?> model,
  Map<String, Object?> content,
) {
  final root = model['root']! as Map<String, Object?>;
  final center =
      ((root['slots']! as Map<String, Object?>)['body']!
              as Map<String, Object?>)['child']!
          as Map<String, Object?>;
  ((center['slots']! as Map<String, Object?>)['child']!
          as Map<String, Object?>)['child'] =
      content;
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
Map<String, Object?> _text({
  String id = _textId,
  String label = 'Focus target',
}) => {
  'id': id,
  'type': 'flutter.widgets.Text',
  'properties': {
    'data': {'kind': 'string', 'value': label},
    'semanticsLabel': {'kind': 'string', 'value': label},
  },
  'slots': <String, Object?>{},
};
Map<String, Object?> _button({
  String id = _childId,
  String textId = _textId,
  String label = 'Focus target',
}) => _node(
  id,
  'flutter.material.ElevatedButton',
  child: _text(id: textId, label: label),
);
Map<String, Object?> _sized(int width, int height) => _node(
  _textId,
  'flutter.widgets.SizedBox',
  properties: {
    'width': {'kind': 'integer', 'value': width},
    'height': {'kind': 'integer', 'value': height},
  },
);
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

Map<String, Object?>? _findJson(Map<String, Object?> node, String id) {
  if (node['id'] == id) return node;
  for (final value in (node['slots']! as Map<String, Object?>).values) {
    final slot = value! as Map<String, Object?>;
    if (slot['child'] case final Map<String, Object?> child) {
      final found = _findJson(child, id);
      if (found != null) return found;
    }
    if (slot['children'] case final List<Object?> children) {
      for (final child in children) {
        final found = _findJson(child! as Map<String, Object?>, id);
        if (found != null) return found;
      }
    }
  }
  return null;
}

List<SemanticsNode> _semantics(WidgetTester tester) {
  final result = <SemanticsNode>[];
  void visit(SemanticsNode node) {
    if (!node.isMergedIntoParent) result.add(node);
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
