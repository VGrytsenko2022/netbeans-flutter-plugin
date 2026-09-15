import 'dart:convert';
import 'dart:ui' show PointerDeviceKind;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';

import 'canvas_model_test.dart' as fixture;

const _type = 'flutter.widgets.DefaultSelectionStyle';
const _id = 'a3867d28-9495-4dfc-8ec9-087075ffb9b3';
const _childId = 'b37a51a0-598e-4189-8f08-0a7dd6d5a772';
const _textId = 'bf6064c6-d24e-4f7d-802c-0cf1e54b0574';
const _otherId = 'ce7074c6-d24e-4f7d-802c-0cf1e54b0574';
const _columnId = 'dbed0e25-f4ee-4b41-b6df-c0d1fe0bb8ee';
const _outerCursor = Color(0xFF112233);
const _outerSelection = Color(0x88445566);
const _localCursor = Color(0xFFAA2244);
const _localSelection = Color(0x88779911);
const _cursors = <String, MouseCursor>{
  'none': SystemMouseCursors.none,
  'basic': SystemMouseCursors.basic,
  'click': SystemMouseCursors.click,
  'forbidden': SystemMouseCursors.forbidden,
  'wait': SystemMouseCursors.wait,
  'progress': SystemMouseCursors.progress,
  'contextMenu': SystemMouseCursors.contextMenu,
  'help': SystemMouseCursors.help,
  'text': SystemMouseCursors.text,
  'verticalText': SystemMouseCursors.verticalText,
  'cell': SystemMouseCursors.cell,
  'precise': SystemMouseCursors.precise,
  'move': SystemMouseCursors.move,
  'grab': SystemMouseCursors.grab,
  'grabbing': SystemMouseCursors.grabbing,
  'noDrop': SystemMouseCursors.noDrop,
  'alias': SystemMouseCursors.alias,
  'copy': SystemMouseCursors.copy,
  'disappearing': SystemMouseCursors.disappearing,
  'allScroll': SystemMouseCursors.allScroll,
  'resizeLeftRight': SystemMouseCursors.resizeLeftRight,
  'resizeUpDown': SystemMouseCursors.resizeUpDown,
  'resizeUpLeftDownRight': SystemMouseCursors.resizeUpLeftDownRight,
  'resizeUpRightDownLeft': SystemMouseCursors.resizeUpRightDownLeft,
  'resizeUp': SystemMouseCursors.resizeUp,
  'resizeDown': SystemMouseCursors.resizeDown,
  'resizeLeft': SystemMouseCursors.resizeLeft,
  'resizeRight': SystemMouseCursors.resizeRight,
  'resizeUpLeft': SystemMouseCursors.resizeUpLeft,
  'resizeUpRight': SystemMouseCursors.resizeUpRight,
  'resizeDownLeft': SystemMouseCursors.resizeDownLeft,
  'resizeDownRight': SystemMouseCursors.resizeDownRight,
  'resizeColumn': SystemMouseCursors.resizeColumn,
  'resizeRow': SystemMouseCursors.resizeRow,
  'zoomIn': SystemMouseCursors.zoomIn,
  'zoomOut': SystemMouseCursors.zoomOut,
  'defer': MouseCursor.defer,
  'uncontrolled': MouseCursor.uncontrolled,
  'clickable': WidgetStateMouseCursor.clickable,
  'adaptiveClickable': WidgetStateMouseCursor.adaptiveClickable,
  'textable': WidgetStateMouseCursor.textable,
};

void main() {
  test(
    'DefaultSelectionStyle closes typed colors41mouse presets merge and required-child schema',
    () {
      final contract = canvasRuntimeWidgetSchemaContractForTesting();
      final start = contract.indexOf('W|$_type\n');
      final end = contract.indexOf(
        'W|flutter.widgets.DefaultTextHeightBehavior\n',
        start,
      );
      final block = contract.substring(start, end);
      expect(RegExp(r'^P\|', multiLine: true).allMatches(block), hasLength(4));
      expect(block, contains('P|cursorColor|color,themeToken|0|-|-|'));
      expect(block, contains('P|selectionColor|color,themeToken|0|-|-|'));
      expect(
        block,
        contains('P|merge|boolean|1|boolean:false|-|boolean:any\n'),
      );
      expect(
        block,
        endsWith(
          'S|child|single|1|1|1|any\nC|$_type|paletteCreate|wrapExistingChild|child\n',
        ),
      );
      expect(canvasModelProtocolVersion, 20);
      expect(isCanvasReviewedRequiredChildWrapperWidgetType(_type), isTrue);
      expect(canvasDropSlotsForWidgetType(_type), isEmpty);
      final defaultProperties = _find(_decode(_model()).root, _id)!.properties;
      expect(defaultProperties.keys, ['merge']);
      expect(defaultProperties['merge']!.value, isFalse);
      for (final merge in [false, true]) {
        expect(
          _find(
            _decode(_model(properties: {'merge': _bool(merge)})).root,
            _id,
          )!.properties['merge']!.value,
          merge,
        );
      }
      expect(_cursors, hasLength(41));
      for (final cursor in _cursors.keys) {
        expect(
          _find(
            _decode(_model(properties: {'mouseCursor': _string(cursor)})).root,
            _id,
          )!.properties['mouseCursor']!.value,
          cursor,
        );
      }
      for (final name in ['cursorColor', 'selectionColor']) {
        for (final token in canvasColorSchemeThemeTokens) {
          expect(
            () => _decode(
              _model(
                properties: {
                  name: {'kind': 'themeToken', 'token': token},
                },
              ),
            ),
            returnsNormally,
          );
        }
      }
    },
  );

  test(
    'DefaultSelectionStyle rejects missing merge null coercion rawcode unreviewed cursor and unsupported fallback branches',
    () {
      final missingMerge = _model();
      (_wrapperJson(missingMerge)['properties']! as Map).remove('merge');
      expect(() => _decode(missingMerge), throwsFormatException);
      for (final name in ['cursorColor', 'selectionColor']) {
        for (final value in [
          {'kind': 'null'},
          {'kind': 'integer', 'value': 123},
          {'kind': 'string', 'value': 'Colors.red'},
          {'kind': 'color', 'argb': '0xff112233'},
          {'kind': 'color', 'argb': '0xFF112233', 'extra': true},
          {'kind': 'themeToken', 'token': 'material.textTheme.bodyLarge'},
        ]) {
          expect(
            () => _decode(_model(properties: {name: value})),
            throwsFormatException,
          );
        }
      }
      for (final value in [
        {'kind': 'null'},
        {'kind': 'boolean', 'value': true},
        _string('invalid'),
        _string('SystemMouseCursors.click'),
        _string('resolveWith(projectCallback)'),
        {'kind': 'string', 'value': 'click', 'extra': true},
      ]) {
        expect(
          () => _decode(_model(properties: {'mouseCursor': value})),
          throwsFormatException,
        );
      }
      for (final value in [
        null,
        {'kind': 'null'},
        {'kind': 'integer', 'value': 1},
        _string('true'),
        {'kind': 'boolean', 'value': null},
        {'kind': 'boolean', 'value': 'false'},
        {'kind': 'boolean', 'value': false, 'extra': true},
      ]) {
        expect(
          () => _decode(_model(properties: {'merge': value})),
          throwsFormatException,
        );
      }
      for (final name in [
        'key',
        'child',
        'fallback',
        'constructor',
        'selectionHandleColor',
      ]) {
        expect(
          () => _decode(_model(properties: {name: _bool(true)})),
          throwsFormatException,
        );
      }
    },
  );

  test(
    'DefaultSelectionStyle rejects empty malformed child and illegal Flex parent data',
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
          'children': {'kind': 'single', 'child': _text()},
        },
        {
          'child': {'kind': 'single', 'child': _text(), 'extra': true},
        },
      ]) {
        final model = _model();
        _wrapperJson(model)['slots'] = slots;
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
    'DefaultSelectionStyle wrapper creation consumes an existing required child only',
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
      'DefaultSelectionStyle direct clears each absent field while merge inherits across all16 combinations on $platform',
      (tester) async {
        for (final merge in [false, true]) {
          for (final cursor in [false, true]) {
            for (final selection in [false, true]) {
              for (final mouse in [false, true]) {
                final model = _model(
                  platform: platform,
                  properties: {
                    'cursorColor': _color('0xFF112233'),
                    'selectionColor': _color('0x88445566'),
                    'mouseCursor': _string('help'),
                  },
                  child: _node(
                    _childId,
                    _type,
                    properties: {
                      'merge': _bool(merge),
                      if (cursor) 'cursorColor': _color('0xFFAA2244'),
                      if (selection) 'selectionColor': _color('0x88779911'),
                      if (mouse) 'mouseCursor': _string('text'),
                    },
                    child: _text(),
                  ),
                );
                await _pump(tester, model);
                final effective = DefaultSelectionStyle.of(
                  tester.element(_widget(_textId)),
                );
                expect(
                  effective.cursorColor,
                  cursor
                      ? _localCursor
                      : merge
                      ? _outerCursor
                      : null,
                );
                expect(
                  effective.selectionColor,
                  selection
                      ? _localSelection
                      : merge
                      ? _outerSelection
                      : null,
                );
                expect(
                  effective.mouseCursor,
                  mouse
                      ? SystemMouseCursors.text
                      : merge
                      ? SystemMouseCursors.help
                      : null,
                );
                expect(
                  _richText(tester).selectionColor,
                  effective.selectionColor ??
                      DefaultSelectionStyle.defaultColor,
                );
                expect(tester.takeException(), isNull);
              }
            }
          }
        }
      },
    );

    testWidgets(
      'DefaultSelectionStyle semantic colors resolve from the current real Theme and Text local selection wins on $platform',
      (tester) async {
        for (final merge in [false, true]) {
          for (final local in [false, true]) {
            await _pump(
              tester,
              _model(
                platform: platform,
                properties: {
                  'merge': _bool(merge),
                  'cursorColor': {
                    'kind': 'themeToken',
                    'token': 'material.colorScheme.primary',
                  },
                  'selectionColor': {
                    'kind': 'themeToken',
                    'token': 'material.colorScheme.secondary',
                  },
                },
                child: _text(
                  properties: local
                      ? {'selectionColor': _color('0x88779911')}
                      : {},
                ),
              ),
            );
            final context = tester.element(_widget(_textId));
            final effective = DefaultSelectionStyle.of(context);
            expect(
              effective.cursorColor,
              Theme.of(context).colorScheme.primary,
            );
            expect(
              effective.selectionColor,
              Theme.of(context).colorScheme.secondary,
            );
            expect(
              _richText(tester).selectionColor,
              local ? _localSelection : effective.selectionColor,
            );
            expect(tester.takeException(), isNull);
          }
        }
      },
    );

    testWidgets(
      'DefaultSelectionStyle forwards all41 actual SDK cursors without changing TextField validation on $platform',
      (tester) async {
        for (final entry in _cursors.entries) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {'mouseCursor': _string(entry.key)},
            ),
          );
          final cursor = DefaultSelectionStyle.of(
            tester.element(_widget(_textId)),
          ).mouseCursor;
          expect(cursor, entry.value);
          if (cursor is WidgetStateMouseCursor) {
            expect(
              cursor.resolve({WidgetState.disabled}),
              SystemMouseCursors.basic,
            );
            final enabled = entry.key == 'textable'
                ? SystemMouseCursors.text
                : entry.key == 'adaptiveClickable' && !kIsWeb
                ? SystemMouseCursors.basic
                : SystemMouseCursors.click;
            expect(
              cursor.resolve({}),
              enabled,
              reason:
                  'adaptiveClickable follows real kIsWeb, not the viewport profile',
            );
          }
        }
        for (final preset in [
          'defer',
          'uncontrolled',
          'clickable',
          'adaptiveClickable',
          'textable',
        ]) {
          expect(
            () => _decode(
              _model(
                child: _field(properties: {'mouseCursor': _string(preset)}),
              ),
            ),
            throwsFormatException,
          );
        }
      },
    );

    testWidgets(
      'DefaultSelectionStyle project TextField consumes cursor style preserves local override and guarded focus on $platform',
      (tester) async {
        for (final local in [false, true]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {
                'cursorColor': _color('0xFF112233'),
                'selectionColor': _color('0x88445566'),
                'mouseCursor': _string('help'),
              },
              child: _field(
                properties: {if (local) 'cursorColor': _color('0xFFAA2244')},
              ),
            ),
          );
          final editable = tester.widget<EditableText>(
            find.byType(EditableText),
          );
          expect(editable.cursorColor, local ? _localCursor : _outerCursor);
          expect(
            editable.selectionColor,
            isNull,
            reason:
                'SDK TextField paints selection only while focused; the project preview is intentionally unfocusable',
          );
          expect(
            DefaultSelectionStyle.of(
              tester.element(find.byType(TextField)),
            ).selectionColor,
            _outerSelection,
          );
          editable.focusNode.requestFocus();
          await tester.pump();
          expect(editable.focusNode.canRequestFocus, isFalse);
          expect(editable.focusNode.hasFocus, isFalse);
          expect(editable.controller.text, isEmpty);
          expect(tester.takeException(), isNull);
        }
      },
    );

    testWidgets(
      'DefaultSelectionStyle color updates retain button focus layout semantics and pointer selection on $platform',
      (tester) async {
        final semantics = tester.ensureSemantics();
        try {
          FocusNode? original;
          Size? originalSize;
          final selections = <String>[];
          for (final color in [null, '0xFF112233', '0xFFAA2244', null]) {
            await tester.pumpWidget(
              CanvasModelApp(
                model: _decode(
                  _model(
                    platform: platform,
                    properties: {
                      if (color != null) 'selectionColor': _color(color),
                    },
                    child: _node(
                      _childId,
                      'flutter.material.ElevatedButton',
                      child: _text(),
                    ),
                  ),
                ),
                selectedWidgetId: null,
                onSelected: selections.add,
              ),
            );
            await tester.pump();
            final focus = Focus.of(
              tester.element(find.text('Selection child')),
            );
            original ??= focus;
            expect(identical(focus, original), isTrue);
            focus.requestFocus();
            await tester.pump();
            expect(focus.hasFocus, isTrue);
            originalSize ??= tester.getSize(_widget(_id));
            expect(tester.getSize(_widget(_id)), originalSize);
            expect(
              _semanticLabels(
                tester,
              ).any((label) => label.contains('Selection child')),
              isTrue,
            );
            await tester.tap(_widget(_textId));
            await tester.pump(const Duration(milliseconds: 350));
            expect(selections, isNotEmpty);
            expect(tester.takeException(), isNull);
          }
        } finally {
          semantics.dispose();
        }
      },
    );

    testWidgets(
      'DefaultSelectionStyle F2 editor preserves its explicit Designer cursor and supports commit cancel reopen on $platform',
      (tester) async {
        final commits = <(String, String)>[];
        await tester.pumpWidget(
          CanvasModelApp(
            model: _decode(
              _model(
                platform: platform,
                properties: {
                  'merge': _bool(true),
                  'cursorColor': _color('0xFF112233'),
                  'selectionColor': _color('0x88445566'),
                },
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
        await _f2(tester);
        final field = tester.widget<TextField>(find.byType(TextField));
        expect(field.focusNode!.hasFocus, isTrue);
        await tester.pump();
        final editable = tester.widget<EditableText>(find.byType(EditableText));
        expect(
          editable.cursorColor,
          Theme.of(tester.element(find.byType(TextField))).colorScheme.primary,
          reason:
              'The service editor supplies its own explicit cursor color; inherited style must not overwrite it',
        );
        expect(editable.selectionColor, _outerSelection);
        await tester.enterText(find.byType(TextField), 'Selection editor');
        await tester.sendKeyDownEvent(LogicalKeyboardKey.controlLeft);
        await tester.sendKeyEvent(LogicalKeyboardKey.enter);
        await tester.sendKeyUpEvent(LogicalKeyboardKey.controlLeft);
        await tester.pump();
        expect(commits, [(_textId, 'Selection editor')]);
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
      'DefaultSelectionStyle generic wrapper preserves nested drop targets and required child move guards on $platform',
      (tester) async {
        CanvasDropResolver? drop;
        CanvasMovePreviewResolver? move;
        final model = _model(
          platform: platform,
          child: _node(
            _childId,
            'flutter.widgets.SizedBox',
            properties: _size(90, 70),
            child: _list(_columnId, []),
          ),
        );
        final wrapper = _wrapperJson(model);
        _replaceCentered(
          model,
          _list(_otherId, [
            wrapper,
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
        expect(_resolve(tester, drop!, point)?.parentWidgetId, _columnId);
        final wrap = _resolve(tester, drop!, point, _source(_type));
        expect(wrap?.parentWidgetId, _childId);
        expect(wrap?.slotName, 'child');
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
      'DefaultSelectionStyle zero-size child has only genuine grouped selection geometry on $platform',
      (tester) async {
        for (final merge in [false, true]) {
          await _pump(
            tester,
            _model(
              platform: platform,
              properties: {'merge': _bool(merge)},
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
            find.descendant(of: _widget(_id), matching: find.byType(SizedBox)),
            findsOneWidget,
          );
        }
      },
    );
  }

  testWidgets(
    'SDK DefaultSelectionStyle merge responds to parent changes but direct null remains a per-field boundary',
    (tester) async {
      for (final merge in [false, true]) {
        for (final outer in [null, _outerCursor, _localCursor]) {
          DefaultSelectionStyle? effective;
          final child = Builder(
            builder: (context) {
              effective = DefaultSelectionStyle.of(context);
              return const SizedBox();
            },
          );
          await tester.pumpWidget(
            DefaultSelectionStyle(
              cursorColor: outer,
              selectionColor: _outerSelection,
              mouseCursor: SystemMouseCursors.help,
              child: merge
                  ? DefaultSelectionStyle.merge(
                      selectionColor: _localSelection,
                      child: child,
                    )
                  : DefaultSelectionStyle(
                      selectionColor: _localSelection,
                      child: child,
                    ),
            ),
          );
          expect(effective!.cursorColor, merge ? outer : null);
          expect(effective!.selectionColor, _localSelection);
          expect(
            effective!.mouseCursor,
            merge ? SystemMouseCursors.help : null,
          );
        }
      }
    },
  );

  testWidgets(
    'SDK DefaultSelectionStyle notifies only changed fields and inherited updates preserve state identity',
    (tester) async {
      final key = GlobalKey<_SelectionProbeState>();
      final child = _SelectionProbe(key: key);
      Future<void> show({
        Color? cursor,
        Color? selection,
        MouseCursor? mouse,
      }) => tester.pumpWidget(
        DefaultSelectionStyle(
          cursorColor: cursor,
          selectionColor: selection,
          mouseCursor: mouse,
          child: child,
        ),
      );
      await show();
      final state = key.currentState!;
      expect(state.builds, 1);
      await show();
      expect(state.builds, 1);
      await show(cursor: _outerCursor);
      expect(state.builds, 2);
      await show(cursor: _outerCursor, selection: _outerSelection);
      expect(state.builds, 3);
      await show(
        cursor: _outerCursor,
        selection: _outerSelection,
        mouse: SystemMouseCursors.help,
      );
      expect(state.builds, 4);
      await show();
      expect(state.builds, 5);
      expect(identical(key.currentState, state), isTrue);
    },
  );

  testWidgets(
    'SDK DefaultSelectionStyle capture and wrap preserve exact resolved fields without becoming merge',
    (tester) async {
      BuildContext? outside;
      BuildContext? inside;
      await tester.pumpWidget(
        Builder(
          builder: (context) {
            outside = context;
            return DefaultSelectionStyle(
              cursorColor: _outerCursor,
              mouseCursor: SystemMouseCursors.help,
              child: Builder(
                builder: (context) {
                  inside = context;
                  return const SizedBox();
                },
              ),
            );
          },
        ),
      );
      final themes = InheritedTheme.capture(from: inside!, to: outside);
      DefaultSelectionStyle? captured;
      await tester.pumpWidget(
        DefaultSelectionStyle(
          selectionColor: _localSelection,
          child: themes.wrap(
            Builder(
              builder: (context) {
                captured = DefaultSelectionStyle.of(context);
                return const SizedBox();
              },
            ),
          ),
        ),
      );
      expect(captured!.cursorColor, _outerCursor);
      expect(captured!.selectionColor, isNull);
      expect(captured!.mouseCursor, SystemMouseCursors.help);
      final capturedWidget = captured!;
      await tester.pumpWidget(
        capturedWidget.wrap(
          tester.element(find.byType(SizedBox)),
          Builder(
            builder: (context) {
              captured = DefaultSelectionStyle.of(context);
              return const SizedBox();
            },
          ),
        ),
      );
      expect(captured!.cursorColor, _outerCursor);
      expect(captured!.selectionColor, isNull);
    },
  );

  testWidgets(
    'SDK ThemeData selection colors override outer inherited colors and its mouse field follows actual theme boundary',
    (tester) async {
      for (final themed in [false, true]) {
        DefaultSelectionStyle? effective;
        await tester.pumpWidget(
          DefaultSelectionStyle(
            cursorColor: _outerCursor,
            selectionColor: _outerSelection,
            mouseCursor: SystemMouseCursors.help,
            child: Theme(
              data: ThemeData(
                textSelectionTheme: TextSelectionThemeData(
                  cursorColor: themed ? _localCursor : null,
                  selectionColor: themed ? _localSelection : null,
                ),
              ),
              child: Builder(
                builder: (context) {
                  effective = DefaultSelectionStyle.of(context);
                  return const SizedBox();
                },
              ),
            ),
          ),
        );
        expect(effective!.cursorColor, themed ? _localCursor : _outerCursor);
        expect(
          effective!.selectionColor,
          themed ? _localSelection : _outerSelection,
        );
        expect(
          effective!.mouseCursor,
          isNull,
          reason:
              'SDK Theme creates a direct DefaultSelectionStyle without mouseCursor',
        );
      }
    },
  );

  testWidgets(
    'SDK focused TextField uses inherited selection color and direct local cursor before theme fallback',
    (tester) async {
      final focus = FocusNode();
      final controller = TextEditingController(text: 'Actual editable');
      try {
        for (final direct in [false, true]) {
          for (final local in [false, true]) {
            await tester.pumpWidget(
              MaterialApp(
                home: Material(
                  child: DefaultSelectionStyle(
                    cursorColor: direct ? _outerCursor : null,
                    selectionColor: direct ? _outerSelection : null,
                    child: TextField(
                      focusNode: focus,
                      controller: controller,
                      cursorColor: local ? _localCursor : null,
                    ),
                  ),
                ),
              ),
            );
            focus.requestFocus();
            await tester.pump();
            await tester.pump();
            final editable = tester.widget<EditableText>(
              find.byType(EditableText),
            );
            final theme = Theme.of(tester.element(find.byType(TextField)));
            expect(
              editable.cursorColor,
              local
                  ? _localCursor
                  : direct
                  ? _outerCursor
                  : theme.colorScheme.primary,
            );
            expect(
              editable.selectionColor,
              direct
                  ? _outerSelection
                  : theme.colorScheme.primary.withValues(alpha: 0.4),
            );
            expect(editable.focusNode.hasFocus, isTrue);
            expect(tester.takeException(), isNull);
          }
        }
        await tester.pumpWidget(const SizedBox());
      } finally {
        focus.dispose();
        controller.dispose();
      }
    },
  );

  testWidgets(
    'SDK inner TextSelectionTheme directly replaces inherited fields without merging absent colors',
    (tester) async {
      DefaultSelectionStyle? effective;
      await tester.pumpWidget(
        DefaultSelectionStyle(
          cursorColor: _outerCursor,
          selectionColor: _outerSelection,
          mouseCursor: SystemMouseCursors.help,
          child: TextSelectionTheme(
            data: const TextSelectionThemeData(cursorColor: _localCursor),
            child: Builder(
              builder: (context) {
                effective = DefaultSelectionStyle.of(context);
                return const SizedBox();
              },
            ),
          ),
        ),
      );
      expect(effective!.cursorColor, _localCursor);
      expect(effective!.selectionColor, isNull);
      expect(effective!.mouseCursor, isNull);
    },
  );

  testWidgets(
    'SDK selectable Text uses inherited selection and mouse fields with local color precedence and working cursor sessions',
    (tester) async {
      final calls = <MethodCall>[];
      tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
        SystemChannels.mouseCursor,
        (call) async {
          calls.add(call);
          return null;
        },
      );
      try {
        for (final cursor in [
          SystemMouseCursors.help,
          WidgetStateMouseCursor.clickable,
          WidgetStateMouseCursor.adaptiveClickable,
          WidgetStateMouseCursor.textable,
        ]) {
          for (final local in [false, true]) {
            String? selected;
            await tester.pumpWidget(const SizedBox());
            await tester.pumpWidget(
              MaterialApp(
                home: Scaffold(
                  body: SelectionArea(
                    onSelectionChanged: (value) => selected = value?.plainText,
                    child: DefaultSelectionStyle(
                      selectionColor: _outerSelection,
                      mouseCursor: cursor,
                      child: Text(
                        'Selectable words',
                        key: const ValueKey('selectable'),
                        selectionColor: local ? _localSelection : null,
                      ),
                    ),
                  ),
                ),
              ),
            );
            await tester.pump();
            final text = find.byKey(const ValueKey('selectable'));
            final rich = tester.widget<RichText>(
              find.descendant(of: text, matching: find.byType(RichText)).first,
            );
            expect(
              rich.selectionColor,
              local ? _localSelection : _outerSelection,
            );
            expect(rich.selectionRegistrar, isNotNull);
            final region = tester.widget<MouseRegion>(
              find
                  .descendant(of: text, matching: find.byType(MouseRegion))
                  .first,
            );
            expect(region.cursor, cursor);
            final gesture = await tester.createGesture(
              kind: PointerDeviceKind.mouse,
            );
            final rect = tester.getRect(text);
            final paragraph = tester.renderObject<RenderParagraph>(
              find.descendant(of: text, matching: find.byType(RichText)).first,
            );
            Offset point(int offset) {
              final boxes = paragraph.getBoxesForSelection(
                TextSelection(baseOffset: offset, extentOffset: offset + 1),
              );
              return paragraph.localToGlobal(boxes.single.toRect().center);
            }

            await gesture.addPointer(
              location: rect.topLeft + const Offset(2, 5),
            );
            await tester.pump();
            expect(calls, isNotEmpty);
            await gesture.down(point(1));
            await tester.pump();
            await gesture.moveTo(point(9));
            await tester.pumpAndSettle();
            await gesture.up();
            await tester.pump();
            expect(selected, isNotNull, reason: '$cursor local=$local');
            await gesture.removePointer();
            expect(tester.takeException(), isNull);
          }
        }
      } finally {
        tester.binding.defaultBinaryMessenger.setMockMethodCallHandler(
          SystemChannels.mouseCursor,
          null,
        );
      }
    },
  );

  testWidgets(
    'SDK DefaultSelectionStyle fallback is readable but never insertable and null Text selection uses default gray',
    (tester) async {
      await tester.pumpWidget(
        Directionality(
          textDirection: TextDirection.ltr,
          child: Builder(
            builder: (context) {
              final fallback = DefaultSelectionStyle.of(context);
              expect(fallback.cursorColor, isNull);
              expect(fallback.selectionColor, isNull);
              expect(fallback.mouseCursor, isNull);
              return const Text('Fallback text');
            },
          ),
        ),
      );
      expect(
        tester.widget<RichText>(find.byType(RichText)).selectionColor,
        DefaultSelectionStyle.defaultColor,
      );
      await tester.pumpWidget(const DefaultSelectionStyle.fallback());
      final error = tester.takeException();
      expect(error, isA<FlutterError>());
      expect(
        error.toString(),
        contains('cannot be incorporated into the widget tree'),
      );
      await tester.pumpWidget(const SizedBox());
    },
  );
}

Finder _widget(String id) => find.byKey(ValueKey('canvas-widget-$id'));
RichText _richText(WidgetTester tester) => tester.widget<RichText>(
  find.descendant(of: _widget(_textId), matching: find.byType(RichText)).first,
);
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
  Map<String, Object?> properties = const {},
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
            properties: properties,
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

Map<String, Object?> _wrapperJson(Map<String, Object?> model) =>
    _centerSlot(model)['child']! as Map<String, Object?>;
void _replaceCentered(Map<String, Object?> model, Map<String, Object?> node) =>
    _centerSlot(model)['child'] = node;
Map<String, Object?> _bool(bool value) => {'kind': 'boolean', 'value': value};
Map<String, Object?> _string(String value) => {
  'kind': 'string',
  'value': value,
};
Map<String, Object?> _color(String argb) => {'kind': 'color', 'argb': argb};
Map<String, Object?> _node(
  String id,
  String type, {
  Map<String, Object?> properties = const {},
  Map<String, Object?>? child,
}) => {
  'id': id,
  'type': type,
  'properties': {if (type == _type) 'merge': _bool(false), ...properties},
  'slots': {
    'child': {'kind': 'single', 'child': child},
  },
};
Map<String, Object?> _text({Map<String, Object?> properties = const {}}) => {
  'id': _textId,
  'type': 'flutter.widgets.Text',
  'properties': {
    'data': {'kind': 'string', 'value': 'Selection child'},
    ...properties,
  },
  'slots': <String, Object?>{},
};
Map<String, Object?> _field({Map<String, Object?> properties = const {}}) => {
  'id': _childId,
  'type': 'flutter.material.TextField',
  'properties': properties,
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

class _SelectionProbe extends StatefulWidget {
  const _SelectionProbe({super.key});
  @override
  State<_SelectionProbe> createState() => _SelectionProbeState();
}

class _SelectionProbeState extends State<_SelectionProbe> {
  var builds = 0;
  @override
  Widget build(BuildContext context) {
    builds++;
    DefaultSelectionStyle.of(context);
    return const SizedBox();
  }
}
