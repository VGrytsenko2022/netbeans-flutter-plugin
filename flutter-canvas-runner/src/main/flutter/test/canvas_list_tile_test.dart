import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_drop.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_view.dart';
import 'canvas_model_test.dart' as fixture;

const _ref = {'kind': 'dartObjectReferencePresence'};
const _null = {'kind': 'null'};
const _type = 'flutter.material.ListTile';
const _names = ['leading', 'title', 'subtitle', 'trailing'];
const _states = <WidgetState, String>{
  WidgetState.disabled: 'Disabled',
  WidgetState.error: 'Error',
  WidgetState.dragged: 'Dragged',
  WidgetState.pressed: 'Pressed',
  WidgetState.selected: 'Selected',
  WidgetState.scrolledUnder: 'ScrolledUnder',
  WidgetState.hovered: 'Hovered',
  WidgetState.focused: 'Focused',
};
Map<String, Object?> _s(String v) => {'kind': 'string', 'value': v};
Map<String, Object?> _b(bool v) => {'kind': 'boolean', 'value': v};
Map<String, Object?> _n(num v) => {
  'kind': v is int ? 'integer' : 'double',
  'value': v,
};
Map<String, Object?> _e(String t, String v) => {
  'kind': 'enum',
  'type': t,
  'value': v,
};
Map<String, Object?> _c(int v) => {
  'kind': 'color',
  'argb': '0x${v.toRadixString(16).padLeft(8, '0').toUpperCase()}',
};
Map<String, Object?> _insets(
  double l,
  double t,
  double r,
  double b, {
  bool directional = false,
}) => {
  'kind': directional ? 'edgeInsetsDirectional' : 'edgeInsets',
  directional ? 'start' : 'left': l,
  'top': t,
  directional ? 'end' : 'right': r,
  'bottom': b,
};
String _id(int i) => 'a792df78-0d7e-4cf1-a375-${i.toString().padLeft(12, '0')}';
Map<String, Object?> _node(
  int i,
  String type,
  Map<String, Object?> props, [
  Map<String, Object?> slots = const {},
]) => {'id': _id(i), 'type': type, 'properties': props, 'slots': slots};
Map<String, Object?> _single(Object? child) => {
  'kind': 'single',
  'child': child,
};
Map<String, Object?> _wrap(
  int i,
  String type,
  Object child, [
  Map<String, Object?> props = const {},
]) => _node(i, type, props, {'child': _single(child)});
Map<String, Object?> _text(int i, String text) =>
    _node(i, 'flutter.widgets.Text', {'data': _s(text)});
Map<String, Object?> _box(int i, double width, [double height = 20]) => _node(
  i,
  'flutter.widgets.SizedBox',
  {'width': _n(width), 'height': _n(height)},
);
Map<String, Object?> _tile([
  Map<String, Object?> props = const {},
  Map<String, Object?> children = const {},
]) => _node(1, _type, props, {
  for (final entry in children.entries) entry.key: _single(entry.value),
});
Map<String, Object?> _model(
  Object tile, {
  String profile = 'windows',
  bool rtl = false,
  bool wrapWidth = true,
}) {
  final model =
      jsonDecode(utf8.decode(fixture.modelBytesForViewTest()))
          as Map<String, Object?>;
  (model['profile'] as Map)['targetPlatform'] = profile;
  Object child = _wrap(82, 'flutter.widgets.RepaintBoundary', tile);
  if (wrapWidth) {
    child = _wrap(83, 'flutter.widgets.SizedBox', child, {'width': _n(280)});
  }
  child = _wrap(84, 'flutter.widgets.Directionality', child, {
    'textDirection': _e('TextDirection', rtl ? 'rtl' : 'ltr'),
  });
  model['root'] = _node(90, 'flutter.material.Scaffold', {}, {
    'body': _single(_wrap(91, 'flutter.widgets.Center', child)),
  });
  return model;
}

CanvasModel _decode(Map model) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(model))));
Finder _at(int id) => find.byKey(ValueKey('canvas-widget-${_id(id)}'));
ListTile _sdk(WidgetTester tester) =>
    tester.widget<ListTile>(find.byType(ListTile).first);
String _messages(WidgetTester tester) => tester
    .widgetList<Tooltip>(find.byType(Tooltip))
    .map((w) => w.message ?? '')
    .where((v) => v.isNotEmpty)
    .join('\n');
Future<void> _pump(
  WidgetTester tester,
  Object tile, {
  ThemeData? theme,
  bool rtl = false,
  String profile = 'windows',
  List<String>? selected,
  bool wrapWidth = true,
  ValueChanged<CanvasDropResolver?>? onDrop,
  ValueChanged<CanvasMovePreviewResolver?>? onMove,
}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: theme,
      themeAnimationDuration: Duration.zero,
      home: CanvasDocumentView(
        model: _decode(
          _model(tile, profile: profile, rtl: rtl, wrapWidth: wrapWidth),
        ),
        selectedWidgetId: null,
        onSelected: selected?.add ?? (_) {},
        onDropResolverChanged: onDrop,
        onMovePreviewResolverChanged: onMove,
      ),
    ),
  );
  await tester.pump(const Duration(milliseconds: 350));
  await tester.pump();
  expect(tester.takeException(), isNull);
}

Future<Uint8List> _pixels(
  WidgetTester tester,
  RenderRepaintBoundary boundary,
) async => (await tester.runAsync(() async {
  final img = await boundary.toImage();
  try {
    final bytes = await img.toByteData(format: ui.ImageByteFormat.rawRgba);
    return Uint8List.fromList(bytes!.buffer.asUint8List());
  } finally {
    img.dispose();
  }
}))!;
Future<List<String>> _rawErrors(WidgetTester tester, Widget child) async {
  final errors = <String>[], handler = FlutterError.onError;
  FlutterError.onError = (details) => errors.add(details.exceptionAsString());
  try {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(body: Center(child: child)),
      ),
    );
    await tester.pump();
  } finally {
    FlutterError.onError = handler;
  }
  await tester.pumpWidget(const SizedBox());
  return errors;
}

void _completeTests() {
  testWidgets(
    'quarantined nonfinite geometry withholds child semantics and recovers',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        for (final value in [_n(4), _e('double', 'nan'), _n(5)]) {
          await _pump(
            tester,
            _tile(
              {'minVerticalPadding': value},
              {'title': _text(2, 'Secret label')},
            ),
          );
          final labels = <String>[];
          void visit(SemanticsNode node) {
            labels.add(node.label);
            node.visitChildren((child) {
              visit(child);
              return true;
            });
          }

          void visitOwner(PipelineOwner owner) {
            final root = owner.semanticsOwner?.rootSemanticsNode;
            if (root != null) visit(root);
            owner.visitChildren(visitOwner);
          }

          visitOwner(tester.binding.rootPipelineOwner);
          expect(
            labels.any((s) => s.contains('Secret label')),
            value['kind'] != 'enum',
          );
        }
      } finally {
        semantics.dispose();
      }
    },
  );
  testWidgets(
    'independent tiles keep separate child keys and reactive theme defaults',
    (tester) async {
      final second = _node(6, _type, {}, {
        'title': _single(_node(7, 'flutter.material.TextField', {})),
      });
      final column = _node(
        8,
        'flutter.widgets.Column',
        {'mainAxisSize': _e('MainAxisSize', 'min')},
        {
          'children': {
            'kind': 'list',
            'children': [
              _tile({}, {'title': _node(3, 'flutter.material.TextField', {})}),
              second,
            ],
          },
        },
      );
      await _pump(tester, column);
      final before = tester
          .stateList<EditableTextState>(find.byType(EditableText))
          .toList();
      expect(before, hasLength(2));
      expect(identical(before[0], before[1]), false);
      await _pump(
        tester,
        column,
        theme: ThemeData(
          useMaterial3: false,
          listTileTheme: const ListTileThemeData(
            textColor: Colors.blue,
            visualDensity: VisualDensity(horizontal: 1, vertical: -1),
            minTileHeight: 70,
          ),
        ),
      );
      final after = tester
          .stateList<EditableTextState>(find.byType(EditableText))
          .toList();
      expect(after[0], same(before[0]));
      expect(after[1], same(before[1]));
      expect(_messages(tester), isEmpty);
    },
  );
  testWidgets(
    'four distinct RTL and LTR drop handles and move zones on both profiles',
    (tester) async {
      for (final profile in ['windows', 'web']) {
        for (final rtl in [false, true]) {
          CanvasDropResolver? drop;
          CanvasMovePreviewResolver? move;
          await _pump(
            tester,
            _tile(),
            profile: profile,
            rtl: rtl,
            onDrop: (r) => drop = r,
            onMove: (r) => move = r,
          );
          final rect = tester.getRect(find.byType(ListTile)),
              surface = tester.getRect(find.byType(CanvasDocumentView));
          final source = CanvasPaletteDragSource(
            token: 'text',
            widgetType: 'flutter.widgets.Text',
            traits: const {},
          );
          final points = <String, Offset>{
            'leading': Offset(
              rtl ? rect.right - 4 : rect.left + 4,
              rect.center.dy,
            ),
            'title': Offset(rect.center.dx, rect.top + 4),
            'subtitle': Offset(rect.center.dx, rect.bottom - 4),
            'trailing': Offset(
              rtl ? rect.left + 4 : rect.right - 4,
              rect.center.dy,
            ),
          };
          for (final entry in points.entries) {
            final p = entry.value;
            final target = drop!(
              ((p.dx - surface.left) / surface.width * 1000000).round(),
              ((p.dy - surface.top) / surface.height * 1000000).round(),
              source,
            );
            expect(target?.parentWidgetId, _id(1));
            expect(target?.slotName, entry.key);
            expect(target?.insertionIndex, 0);
          }
          // Existing-widget move resolver uses the same distinct slot geometry.
          await _pump(
            tester,
            _tile({}, {'title': _text(2, 'Title')}),
            profile: profile,
            rtl: rtl,
            onMove: (r) => move = r,
          );
          final targets = [
            for (final name in ['leading', 'subtitle', 'trailing'])
              move!(_id(2), _id(1), name, 0),
          ];
          expect(targets.every((t) => t != null), true);
          expect(targets.map((t) => t!.slotName), [
            'leading',
            'subtitle',
            'trailing',
          ]);
        }
      }
    },
  );
  testWidgets('ten built-in shapes and side/radius variants stay actual SDK', (
    tester,
  ) async {
    for (final kind in [
      'roundedRectangle',
      'beveledRectangle',
      'continuousRectangle',
      'roundedSuperellipse',
      'circle',
      'oval',
      'stadium',
      'linear',
      'star',
      'polygon',
    ]) {
      await _pump(
        tester,
        _tile(
          {
            'shapeKind': _s(kind),
            'shapeSideColor': _c(0xff245678),
            'shapeSideWidth': _n(2),
            if (kind == 'linear') ...{
              'shapeStartSize': _n(0.5),
              'shapeEndAlignment': _n(-0.4),
            },
            if (kind == 'star' || kind == 'polygon') ...{
              'shapePoints': _n(6),
              'shapeRotation': _n(13.0),
            },
          },
          {'title': _text(2, 'Title')},
        ),
        rtl: true,
      );
      final shape = _sdk(tester).shape!;
      final expected = switch (kind) {
        'roundedRectangle' => RoundedRectangleBorder,
        'beveledRectangle' => BeveledRectangleBorder,
        'continuousRectangle' => ContinuousRectangleBorder,
        'roundedSuperellipse' => RoundedSuperellipseBorder,
        'circle' => CircleBorder,
        'oval' => OvalBorder,
        'stadium' => StadiumBorder,
        'linear' => LinearBorder,
        _ => StarBorder,
      };
      expect(shape.runtimeType, expected);
      expect(_messages(tester), isEmpty);
      expect(
        shape
            .getOuterPath(
              const Rect.fromLTWH(0, 0, 280, 56),
              textDirection: TextDirection.rtl,
            )
            .getBounds()
            .isFinite,
        true,
      );
    }
  });
  testWidgets(
    'all 31 TextStyle leaves are admitted and render for each replacement family',
    (tester) async {
      for (final prefix in [
        'titleTextStyle',
        'subtitleTextStyle',
        'leadingAndTrailingTextStyle',
      ]) {
        final fields = <String, Object?>{
          'ThemeTextStyle': {
            'kind': 'themeToken',
            'token': 'material.textTheme.bodyLarge',
          },
          'Inherit': _b(false),
          'Color': _c(0xff123456),
          'BackgroundColor': _c(0x33112233),
          'FontSize': _n(18.5),
          'FontWeight': _e('FontWeight', 'w600'),
          'FontStyle': _e('FontStyle', 'italic'),
          'LetterSpacing': _n(1.25),
          'WordSpacing': _n(2.5),
          'TextBaseline': _e('TextBaseline', 'ideographic'),
          'Height': _n(1.4),
          'LeadingDistribution': _e('TextLeadingDistribution', 'even'),
          'LocaleLanguageCode': _s('en'),
          'LocaleScriptCode': _s('Latn'),
          'LocaleCountryCode': _s('GB'),
          'DecorationUnderline': _b(true),
          'DecorationOverline': _b(true),
          'DecorationLineThrough': _b(true),
          'Shadows': {
            'kind': 'shadowList',
            'items': [
              {
                'id': _id(41),
                'color': {'kind': 'literal', 'argb': '0xFF000000'},
                'offsetX': 1.0,
                'offsetY': 2.0,
                'blurRadius': 3.0,
              },
            ],
          },
          'FontFeatures': {
            'kind': 'fontFeatureList',
            'items': [
              {'id': _id(42), 'tag': 'liga', 'value': 1},
            ],
          },
          'FontVariations': {
            'kind': 'fontVariationList',
            'items': [
              {'id': _id(43), 'axis': 'wght', 'value': 700.0},
            ],
          },
          'DecorationColor': _c(0xffff8800),
          'DecorationStyle': _e('TextDecorationStyle', 'wavy'),
          'DecorationThickness': _n(2.25),
          'DebugLabel': _s('local style'),
          'FontFamily': _s('Ahem'),
          'FontFamilyFallback': _s('Roboto'),
          'Package': _s('fonts'),
          'Overflow': _e('TextOverflow', 'fade'),
        };
        expect(fields, hasLength(29));
        final props = {for (final e in fields.entries) prefix + e.key: e.value};
        await _pump(
          tester,
          _tile(props, {
            'leading': _text(2, 'Lead'),
            'title': _text(3, 'Title'),
            'subtitle': _text(4, 'Sub'),
            'trailing': _text(5, 'Trail'),
          }),
        );
        final sdk = _sdk(tester);
        final style = switch (prefix) {
          'titleTextStyle' => sdk.titleTextStyle,
          'subtitleTextStyle' => sdk.subtitleTextStyle,
          _ => sdk.leadingAndTrailingTextStyle,
        }!;
        expect(
          style.locale,
          const Locale.fromSubtags(
            languageCode: 'en',
            scriptCode: 'Latn',
            countryCode: 'GB',
          ),
        );
        expect(style.shadows, hasLength(1));
        expect(style.fontFeatures, [const ui.FontFeature('liga', 1)]);
        expect(style.fontVariations, [const ui.FontVariation('wght', 700)]);
        expect(
          style.decoration,
          TextDecoration.combine([
            TextDecoration.underline,
            TextDecoration.overline,
            TextDecoration.lineThrough,
          ]),
        );
        expect(style.fontFamily, 'packages/fonts/Ahem');
        expect(style.overflow, TextOverflow.fade);
        // Foreground/background Paint are exclusive with local color counterparts.
        props.remove('${prefix}Color');
        props.remove('${prefix}BackgroundColor');
        for (final name in ['Foreground', 'Background']) {
          props[prefix + name] = {
            'kind': 'paint',
            'color': {'kind': 'literal', 'argb': '0xFF224466'},
            'blendMode': 'srcOver',
            'style': 'fill',
            'strokeWidth': 0.0,
            'strokeCap': 'butt',
            'strokeJoin': 'miter',
            'strokeMiterLimit': 4.0,
            'antiAlias': true,
            'filterQuality': 'none',
            'invertColors': false,
          };
        }
        await _pump(tester, _tile(props, {'title': _text(3, 'Title')}));
        final s = switch (prefix) {
          'titleTextStyle' => _sdk(tester).titleTextStyle,
          'subtitleTextStyle' => _sdk(tester).subtitleTextStyle,
          _ => _sdk(tester).leadingAndTrailingTextStyle,
        }!;
        expect(s.foreground?.color.toARGB32(), 0xff224466);
        expect(s.background?.color.toARGB32(), 0xff224466);
      }
    },
  );
  testWidgets(
    'all title alignments styles and local theme overrides are forwarded',
    (tester) async {
      for (final alignment in ListTileTitleAlignment.values) {
        for (final style in ListTileStyle.values) {
          await _pump(
            tester,
            _tile(
              {
                'titleAlignment': _e('ListTileTitleAlignment', alignment.name),
                'style': _e('ListTileStyle', style.name),
                'horizontalTitleGap': _n(11),
                'minVerticalPadding': _n(7),
                'minLeadingWidth': _n(30),
                'minTileHeight': _n(86),
                'focusColor': _c(0xff112233),
                'hoverColor': _c(0xff223344),
                'splashColor': _c(0xff334455),
                'selectedColor': _c(0xff445566),
                'tileColor': _c(0xff556677),
                'selectedTileColor': _c(0xff667788),
                'enableFeedback': _b(false),
                'autofocus': _b(true),
                'onTap': _s('noop'),
              },
              {
                'title': _text(2, 'Title'),
                'subtitle': _text(3, 'Sub'),
                'leading': _box(4, 20),
                'trailing': _box(5, 20),
              },
            ),
          );
          final sdk = _sdk(tester);
          expect(sdk.titleAlignment, alignment);
          expect(sdk.style, style);
          expect(sdk.minTileHeight, 86);
          expect(sdk.minLeadingWidth, 30);
          expect(sdk.minVerticalPadding, 7);
          expect(sdk.horizontalTitleGap, 11);
          expect(sdk.enableFeedback, false);
          expect(sdk.autofocus, true);
          expect(sdk.selectedTileColor, const Color(0xff667788));
          expect(_messages(tester), isEmpty);
        }
      }
    },
  );
  testWidgets(
    'baseline-aligned parent cannot receive nonfinite quarantined tile baselines',
    (tester) async {
      for (final value in [_n(4), _e('double', 'nan'), _n(5)]) {
        await _pump(
          tester,
          _node(
            7,
            'flutter.widgets.Row',
            {
              'crossAxisAlignment': _e('CrossAxisAlignment', 'baseline'),
              'textBaseline': _e('TextBaseline', 'alphabetic'),
            },
            {
              'children': {
                'kind': 'list',
                'children': [
                  _wrap(
                    8,
                    'flutter.widgets.SizedBox',
                    _tile(
                      {'minVerticalPadding': value},
                      {'title': _text(2, 'Title')},
                    ),
                    {'width': _n(160)},
                  ),
                  _text(9, 'Peer'),
                ],
              },
            },
          ),
        );
        final rect = tester.getRect(_at(9));
        expect(rect.isFinite, true);
        expect(
          _messages(tester).contains('nonfinite positions'),
          value['kind'] == 'enum',
        );
      }
    },
  );
  testWidgets(
    'all slot moves preserve stable child State across model history',
    (tester) async {
      final field = _node(3, 'flutter.material.TextField', {});
      Object tile(String slot) => _tile({}, {
        slot: _wrap(4, 'flutter.widgets.SizedBox', field, {'width': _n(80)}),
      });
      await _pump(tester, tile('title'));
      final state = tester.state<EditableTextState>(find.byType(EditableText));
      state.widget.controller.text = 'history';
      for (final slot in ['leading', 'subtitle', 'trailing', 'title']) {
        await _pump(tester, tile(slot));
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(state),
        );
        expect(state.widget.controller.text, 'history');
      }
    },
  );
}

void main() {
  _completeTests();
  testWidgets(
    'finite negative intrinsic widths remain actual SDK parent-clamped values',
    (tester) async {
      expect(
        await _rawErrors(
          tester,
          const Row(
            children: [
              IntrinsicWidth(
                child: ListTile(
                  horizontalTitleGap: -100,
                  minLeadingWidth: 0,
                  leading: SizedBox(width: 20),
                  title: Text('T'),
                ),
              ),
            ],
          ),
        ),
        isEmpty,
      );
      await _pump(
        tester,
        _node(7, 'flutter.widgets.Row', {}, {
          'children': {
            'kind': 'list',
            'children': [
              _wrap(
                8,
                'flutter.widgets.IntrinsicWidth',
                _tile(
                  {'horizontalTitleGap': _n(-100), 'minLeadingWidth': _n(0)},
                  {'leading': _box(3, 20), 'title': _text(2, 'T')},
                ),
              ),
            ],
          },
        }),
        wrapWidth: false,
      );
      expect(_messages(tester), isNot(contains('preview unavailable')));
      expect(tester.takeException(), isNull);
    },
  );
  testWidgets(
    'intrinsic Infinity is diagnosed before an ancestor forces infinite constraints',
    (tester) async {
      expect(
        await _rawErrors(
          tester,
          const Row(
            children: [
              IntrinsicWidth(
                child: ListTile(
                  minLeadingWidth: double.infinity,
                  leading: SizedBox(width: 20),
                  title: Text('Title'),
                ),
              ),
            ],
          ),
        ),
        isNotEmpty,
      );
      for (final value in [_n(30), _e('double', 'infinity'), _n(40)]) {
        final tile = _tile(
          {'minLeadingWidth': value},
          {'leading': _box(3, 20), 'title': _text(2, 'Title')},
        );
        await _pump(
          tester,
          _node(7, 'flutter.widgets.Row', {}, {
            'children': {
              'kind': 'list',
              'children': [_wrap(8, 'flutter.widgets.IntrinsicWidth', tile)],
            },
          }),
          wrapWidth: false,
        );
        expect(
          _messages(tester).contains('intrinsic dimension'),
          value['kind'] == 'enum',
        );
      }
    },
  );
  testWidgets('finite intrinsic geometry remains identical to real SDK', (
    tester,
  ) async {
    for (final vertical in [false, true]) {
      final tile = _tile({}, {
        'title': _text(2, 'Title'),
        'leading': _box(3, 20),
      });
      final type = vertical
          ? 'flutter.widgets.IntrinsicHeight'
          : 'flutter.widgets.IntrinsicWidth';
      await _pump(tester, _wrap(8, type, tile));
      final actual = tester.getSize(find.byType(ListTile));
      const raw = ListTile(
        title: Text('Title'),
        leading: SizedBox(width: 20, height: 20),
      );
      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: Center(
              child: SizedBox(
                width: 280,
                child: vertical
                    ? const IntrinsicHeight(child: raw)
                    : const IntrinsicWidth(child: raw),
              ),
            ),
          ),
        ),
      );
      await tester.pump();
      expect(tester.getSize(find.byType(ListTile)), actual);
      expect(tester.takeException(), isNull);
    }
  });
  testWidgets(
    'missing Material is a raw SDK limitation and Canvas supplies its existing surface',
    (tester) async {
      for (final child in [
        const ListTile(title: Text('Title')),
        const Offstage(child: TextField()),
      ]) {
        final errors = <String>[], handler = FlutterError.onError;
        FlutterError.onError = (details) =>
            errors.add(details.exceptionAsString());
        try {
          await tester.pumpWidget(MaterialApp(home: child));
          await tester.pump();
        } finally {
          FlutterError.onError = handler;
        }
        expect(errors.join(), contains('Material'));
        await tester.pumpWidget(const SizedBox());
      }
      final model = _model(
        _tile({}, {'title': _node(3, 'flutter.material.TextField', {})}),
      );
      model['root'] = _tile({}, {
        'title': _node(3, 'flutter.material.TextField', {}),
      });
      await tester.pumpWidget(
        MaterialApp(
          home: CanvasDocumentView(
            model: _decode(model),
            selectedWidgetId: null,
            onSelected: (_) {},
          ),
        ),
      );
      await tester.pump();
      expect(tester.takeException(), isNull);
      expect(find.byType(ListTile), findsOneWidget);
      expect(
        find.ancestor(
          of: find.byType(ListTile),
          matching: find.byType(Material),
        ),
        findsOneWidget,
      );
    },
  );
  test(
    '176 scalar rows and four optional slots match reviewed independent contract',
    () {
      String block(String source) => source.substring(
        source.indexOf('W|$_type\n'),
        source.indexOf('W|flutter.material.Material\n'),
      );
      final actual = block(canvasRuntimeWidgetSchemaContractForTesting());
      expect(actual, block(canvasReviewedWidgetSchemaContract));
      expect(actual.trim().split('\n'), hasLength(181));
      expect(
        actual.split('\n').where((line) => line.startsWith('P|')),
        hasLength(176),
      );
      expect(
        actual.split('\n').where((line) => line.startsWith('S|')),
        hasLength(4),
      );
      expect(actual, isNot(contains('C|')));
      expect(_decode(_model(_tile())), isA<CanvasModel>());
    },
  );
  test('closed nullable numerics and exact callback presence relations', () {
    for (final name in [
      'horizontalTitleGap',
      'minVerticalPadding',
      'minLeadingWidth',
      'minTileHeight',
    ]) {
      for (final value in [
        _null,
        _n(-12),
        _n(1.3e308),
        _e('double', 'infinity'),
        _e('double', 'negativeInfinity'),
        _e('double', 'nan'),
      ]) {
        expect(() => _decode(_model(_tile({name: value}))), returnsNormally);
      }
    }
    for (final name in ['onTap', 'onLongPress', 'onFocusChange']) {
      for (final value in [_null, _ref, _s('noop')]) {
        expect(() => _decode(_model(_tile({name: value}))), returnsNormally);
      }
      expect(
        () => _decode(_model(_tile({name: _s('arbitrary()')}))),
        throwsFormatException,
      );
    }
    expect(
      () => _decode(_model(_tile({'isThreeLine': _b(true)}))),
      throwsFormatException,
    );
    expect(
      () => _decode(
        _model(_tile({'isThreeLine': _b(true)}, {'subtitle': _text(3, 'Sub')})),
      ),
      returnsNormally,
    );
    expect(
      () => _decode(_model(_tile({'visualDensityHorizontal': _n(4.0)}))),
      returnsNormally,
    );
    expect(
      () => _decode(_model(_tile({'visualDensityHorizontal': _n(4)}))),
      throwsFormatException,
    );
    expect(
      () => _decode(
        _model(
          _tile({'visualDensity': _ref, 'visualDensityHorizontal': _n(1.0)}),
        ),
      ),
      throwsFormatException,
    );
  });
  test(
    'state families require nonnull Default and exclude whole references',
    () {
      for (final family in ['iconColor', 'textColor', 'mouseCursor']) {
        final value = family == 'mouseCursor' ? _s('basic') : _c(0xff114477);
        expect(
          () => _decode(_model(_tile({'${family}Selected': value}))),
          throwsFormatException,
        );
        expect(
          () => _decode(
            _model(
              _tile({'${family}Default': value, '${family}Selected': value}),
            ),
          ),
          returnsNormally,
        );
        expect(
          () => _decode(_model(_tile({'${family}Default': _null}))),
          throwsFormatException,
        );
        expect(
          () =>
              _decode(_model(_tile({family: _ref, '${family}Default': value}))),
          throwsFormatException,
        );
      }
      for (final prefix in [
        'titleTextStyle',
        'subtitleTextStyle',
        'leadingAndTrailingTextStyle',
      ]) {
        expect(
          () => _decode(
            _model(_tile({prefix: _ref, '${prefix}FontSize': _n(14)})),
          ),
          throwsFormatException,
        );
      }
    },
  );
  test(
    'four optional ANY drop slots admit ordinary children not Flex-only widgets',
    () {
      final slots = canvasDropSlotsForWidgetType(_type);
      expect(slots.map((s) => s.slotName), _names);
      for (final slot in slots) {
        for (final (type, accepted) in [
          ('flutter.widgets.Text', true),
          (_type, true),
          ('flutter.widgets.Expanded', false),
          ('flutter.widgets.Spacer', false),
        ]) {
          final source = CanvasPaletteDragSource(
            token: type,
            widgetType: type,
            traits: canvasWidgetTraitsForType(type),
          );
          expect(
            canvasDropTargetAcceptsSource(
              parentWidgetType: _type,
              slotName: slot.slotName,
              currentChildCount: 0,
              insertionIndex: 0,
              source: source,
            ),
            accepted,
          );
          expect(
            canvasDropTargetAcceptsSource(
              parentWidgetType: _type,
              slotName: slot.slotName,
              currentChildCount: 1,
              insertionIndex: 0,
              source: source,
            ),
            false,
          );
        }
      }
      expect(isCanvasPaletteWrapperWidgetType(_type), false);
    },
  );
  for (final profile in ['windows', 'web']) {
    testWidgets(
      'empty and each optional slot preserve real SDK payload on $profile',
      (tester) async {
        for (final name in [null, ..._names]) {
          final model = _tile({}, {?name: _text(2, name)});
          final before = jsonEncode(model);
          await _pump(tester, model, profile: profile);
          final sdk = _sdk(tester);
          expect(sdk.leading != null, name == 'leading');
          expect(sdk.title != null, name == 'title');
          expect(sdk.subtitle != null, name == 'subtitle');
          expect(sdk.trailing != null, name == 'trailing');
          expect(sdk.onTap, isNull);
          expect(sdk.onLongPress, isNull);
          expect(sdk.dense, isNull);
          expect(sdk.visualDensity, isNull);
          expect(jsonEncode(model), before);
          expect(_messages(tester), isEmpty);
        }
      },
    );
  }
  testWidgets(
    'actual SDK pixels match 48 M2 M3 RTL line density and selected branches',
    (tester) async {
      for (final m3 in [false, true]) {
        for (final rtl in [false, true]) {
          for (final lines in [1, 2, 3]) {
            for (final dense in [false, true]) {
              for (final selected in [false, true]) {
                final theme = ThemeData(
                  useMaterial3: m3,
                  platform: TargetPlatform.windows,
                );
                final props = {
                  'dense': _b(dense),
                  'selected': _b(selected),
                  if (lines == 3) 'isThreeLine': _b(true),
                };
                await _pump(
                  tester,
                  _tile(props, {
                    'leading': _text(2, 'L'),
                    'title': _text(3, 'Title'),
                    if (lines > 1) 'subtitle': _text(4, 'Subtitle'),
                    'trailing': _text(5, 'R'),
                  }),
                  theme: theme,
                  rtl: rtl,
                );
                for (final painter
                    in tester.renderObjectList<RenderCustomPaint>(
                      find.byWidgetPredicate(
                        (w) =>
                            w is CustomPaint &&
                            w.key.toString().contains('canvas-widget-outline-'),
                      ),
                    )) {
                  painter.foregroundPainter = null;
                }
                await tester.pump();
                final boundary = tester.renderObject<RenderRepaintBoundary>(
                  find
                      .descendant(
                        of: _at(82),
                        matching: find.byType(RepaintBoundary),
                      )
                      .last,
                );
                final expected = await _pixels(tester, boundary),
                    size = boundary.size;
                final key = GlobalKey();
                await tester.pumpWidget(
                  MaterialApp(
                    theme: theme,
                    home: Scaffold(
                      body: Center(
                        child: SizedBox(
                          width: 280,
                          child: Directionality(
                            textDirection: rtl
                                ? TextDirection.rtl
                                : TextDirection.ltr,
                            child: RepaintBoundary(
                              key: key,
                              child: ListTile(
                                leading: const Text('L'),
                                title: const Text('Title'),
                                subtitle: lines > 1
                                    ? const Text('Subtitle')
                                    : null,
                                trailing: const Text('R'),
                                dense: dense,
                                selected: selected,
                                isThreeLine: lines == 3 ? true : null,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                );
                await tester.pump(const Duration(milliseconds: 350));
                final raw =
                    key.currentContext!.findRenderObject()!
                        as RenderRepaintBoundary;
                expect(raw.size, size);
                expect(await _pixels(tester, raw), expected);
                expect(tester.takeException(), isNull);
              }
            }
          }
        }
      }
    },
  );
  testWidgets(
    'stateful colors preserve all 256 presence-priority combinations',
    (tester) async {
      final props = <String, Object?>{};
      for (final family in ['iconColor', 'textColor']) {
        props['${family}Default'] = _c(0xff001122);
        var i = 0;
        for (final state in _states.values) {
          props[family + state] = _c(0xff112200 + i++);
        }
      }
      await _pump(tester, _tile(props, {'title': _text(2, 'Title')}));
      final sdk = _sdk(tester);
      for (var mask = 0; mask < 256; mask++) {
        final states = <WidgetState>{
          for (var i = 0; i < 8; i++)
            if (mask & (1 << i) != 0) _states.keys.elementAt(i),
        };
        final first = _states.keys.toList().indexWhere(states.contains);
        final color = Color(first < 0 ? 0xff001122 : 0xff112200 + first);
        expect((sdk.iconColor! as WidgetStateColor).resolve(states), color);
        expect((sdk.textColor! as WidgetStateColor).resolve(states), color);
      }
      for (final enabled in [false, true]) {
        for (final selected in [false, true]) {
          await _pump(
            tester,
            _tile(
              {...props, 'enabled': _b(enabled), 'selected': _b(selected)},
              {'title': _text(2, 'Title')},
            ),
          );
          expect(
            DefaultTextStyle.of(tester.element(find.text('Title'))).style.color,
            Color(
              !enabled
                  ? 0xff112200
                  : selected
                  ? 0xff112204
                  : 0xff001122,
            ),
          );
        }
      }
    },
  );
  testWidgets(
    'plain colors and stateful Default differ under disabled and selected SDK overrides',
    (tester) async {
      final theme = ThemeData(
        listTileTheme: const ListTileThemeData(selectedColor: Colors.green),
      );
      for (final map in [false, true]) {
        await _pump(
          tester,
          _tile(
            {
              map ? 'textColorDefault' : 'textColor': _c(0xffff0000),
              'enabled': _b(false),
              'selected': _b(true),
            },
            {'title': _text(2, 'Title')},
          ),
          theme: theme,
        );
        expect(
          DefaultTextStyle.of(tester.element(find.text('Title'))).style.color,
          map ? const Color(0xffff0000) : theme.disabledColor,
        );
      }
    },
  );
  testWidgets(
    'cursor sees disabled but not selected and callback presence remains distinct',
    (tester) async {
      final props = {
        'mouseCursorDefault': _s('click'),
        'mouseCursorSelected': _s('wait'),
        'mouseCursorDisabled': _s('forbidden'),
      };
      for (final enabled in [false, true]) {
        for (final callback in [false, true]) {
          await _pump(
            tester,
            _tile(
              {
                ...props,
                'selected': _b(true),
                'enabled': _b(enabled),
                if (callback) 'onTap': _s('noop'),
              },
              {'title': _text(2, 'Title')},
            ),
          );
          final cursors = tester
              .widgetList<MouseRegion>(
                find.descendant(
                  of: find.byType(ListTile),
                  matching: find.byType(MouseRegion),
                ),
              )
              .map((w) => w.cursor);
          expect(
            cursors,
            contains(
              enabled && callback
                  ? SystemMouseCursors.click
                  : SystemMouseCursors.forbidden,
            ),
          );
        }
      }
    },
  );
  testWidgets(
    'callbacks isolate project code and disabled semantic-button presence is retained',
    (tester) async {
      final semantics = tester.ensureSemantics();
      try {
        for (final enabled in [false, true]) {
          final selected = <String>[];
          await _pump(
            tester,
            _tile(
              {
                'enabled': _b(enabled),
                'onTap': _ref,
                'onLongPress': _ref,
                'onFocusChange': _ref,
              },
              {'title': _text(2, 'Title')},
            ),
            selected: selected,
          );
          expect(_sdk(tester).onTap, isNotNull);
          expect(
            tester
                .widgetList<Semantics>(
                  find.descendant(
                    of: find.byType(ListTile),
                    matching: find.byType(Semantics),
                  ),
                )
                .any((s) => s.properties.button == true),
            true,
          );
          expect(
            _messages(tester),
            contains('disabled semantic-button presence'),
          );
          _sdk(tester).onTap!();
          expect(selected, contains(_id(1)));
        }
        await _pump(
          tester,
          _tile(
            {'onTap': _null, 'onLongPress': _null, 'onFocusChange': _null},
            {'title': _text(2, 'Title')},
          ),
        );
        expect(_sdk(tester).onTap, isNull);
        expect(_messages(tester), isEmpty);
        await _pump(
          tester,
          _tile(
            {'onTap': _s('noop'), 'internalAddSemanticForOnTap': _b(false)},
            {'title': _text(2, 'Title')},
          ),
        );
        expect(_sdk(tester).internalAddSemanticForOnTap, false);
      } finally {
        semantics.dispose();
      }
    },
  );
  testWidgets(
    'density peer axis is zero and three-line theme does not require stored subtitle',
    (tester) async {
      final theme = ThemeData(
        listTileTheme: const ListTileThemeData(
          isThreeLine: true,
          visualDensity: VisualDensity(vertical: -3),
        ),
      );
      await _pump(
        tester,
        _tile(
          {'visualDensityHorizontal': _n(1.0)},
          {'title': _text(2, 'Title')},
        ),
        theme: theme,
      );
      expect(_sdk(tester).isThreeLine, isNull);
      expect(_sdk(tester).visualDensity, const VisualDensity(horizontal: 1));
      expect(_messages(tester), isEmpty);
    },
  );
  testWidgets(
    'three TextStyle replacements remain exact before SDK dense and color copyWith',
    (tester) async {
      final props = <String, Object?>{
        'dense': _b(true),
        'textColor': _c(0xff123456),
      };
      for (final prefix in [
        'titleTextStyle',
        'subtitleTextStyle',
        'leadingAndTrailingTextStyle',
      ]) {
        props.addAll({
          '${prefix}FontSize': _n(30.0),
          '${prefix}FontWeight': _e('FontWeight', 'w700'),
          '${prefix}LetterSpacing': _n(2.0),
          '${prefix}Inherit': _b(false),
          '${prefix}Height': _n(1.4),
          '${prefix}DebugLabel': _s(prefix),
        });
      }
      await _pump(
        tester,
        _tile(props, {
          'leading': _text(2, 'Lead'),
          'title': _text(3, 'Title'),
          'subtitle': _text(4, 'Sub'),
        }),
      );
      final sdk = _sdk(tester);
      for (final style in [
        sdk.titleTextStyle,
        sdk.subtitleTextStyle,
        sdk.leadingAndTrailingTextStyle,
      ]) {
        expect(style!.fontSize, 30);
        expect(style.fontWeight, FontWeight.w700);
        expect(style.letterSpacing, 2);
        expect(style.fontFamily, isNull);
        expect(style.inherit, false);
      }
      expect(
        DefaultTextStyle.of(tester.element(find.text('Title'))).style.fontSize,
        13,
      );
      expect(
        DefaultTextStyle.of(tester.element(find.text('Sub'))).style.fontSize,
        12,
      );
      expect(
        DefaultTextStyle.of(tester.element(find.text('Lead'))).style.fontSize,
        30,
      );
      expect(
        DefaultTextStyle.of(tester.element(find.text('Title'))).style.color,
        const Color(0xff123456),
      );
    },
  );
  testWidgets(
    'all whole project references keep real ListTile and explicit approximation',
    (tester) async {
      final properties = <String, Object?>{
        for (final name in [
          'selectedColor',
          'iconColor',
          'textColor',
          'focusColor',
          'hoverColor',
          'splashColor',
          'tileColor',
          'selectedTileColor',
          'shape',
          'titleTextStyle',
          'subtitleTextStyle',
          'leadingAndTrailingTextStyle',
          'contentPadding',
          'visualDensity',
          'mouseCursor',
          'focusNode',
          'statesController',
        ])
          name: _ref,
      };
      await _pump(tester, _tile(properties, {'title': _text(2, 'Title')}));
      expect(find.byType(ListTile), findsOneWidget);
      expect(find.text('Title'), findsOneWidget);
      for (final name in properties.keys) {
        expect(_messages(tester), contains(name));
      }
      expect(_messages(tester), contains('explicit approximation'));
    },
  );
  testWidgets(
    'signed content padding delegates SafeArea minimum and RTL resolution',
    (tester) async {
      for (final rtl in [false, true]) {
        await _pump(
          tester,
          _tile(
            {'contentPadding': _insets(-40, -10, 18, -20, directional: true)},
            {'title': _text(2, 'Title')},
          ),
          rtl: rtl,
        );
        expect(
          _sdk(tester).contentPadding,
          const EdgeInsetsDirectional.fromSTEB(-40, -10, 18, -20),
        );
        expect(_messages(tester), isEmpty);
      }
    },
  );
  testWidgets(
    'raw SDK full-width side and unbounded-width failures are scoped in Canvas',
    (tester) async {
      expect(
        await _rawErrors(
          tester,
          const Row(children: [ListTile(title: Text('Title'))]),
        ),
        isNotEmpty,
      );
      for (final name in ['leading', 'trailing']) {
        final raw = ListTile(
          title: const Text('Title'),
          leading: name == 'leading'
              ? const SizedBox(width: double.infinity)
              : null,
          trailing: name == 'trailing'
              ? const SizedBox(width: double.infinity)
              : null,
        );
        expect(
          (await _rawErrors(tester, SizedBox(width: 280, child: raw))).join(),
          contains('consumes the entire tile width'),
        );
        await _pump(
          tester,
          _tile({}, {
            'title': _text(2, 'Title'),
            name: _node(3, 'flutter.widgets.SizedBox', {
              'width': _n(1000.0),
              'height': _n(20),
            }),
          }),
        );
        expect(_messages(tester), contains('ListTile ${_id(1)}.$name'));
        expect(find.byType(ListTile), findsOneWidget);
      }
      await _pump(
        tester,
        _node(8, 'flutter.widgets.Row', {}, {
          'children': {
            'kind': 'list',
            'children': [
              _tile({}, {'title': _text(2, 'Title')}),
            ],
          },
        }),
        wrapWidth: false,
      );
      expect(_messages(tester), contains('unbounded width'));
    },
  );
  testWidgets('raw SDK zero-width side is legal and stays unguarded', (
    tester,
  ) async {
    expect(
      await _rawErrors(
        tester,
        const SizedBox(
          width: 0,
          child: ListTile(
            leading: SizedBox(width: double.infinity),
            title: Text('Title'),
          ),
        ),
      ),
      isEmpty,
    );
    await _pump(
      tester,
      _wrap(
        7,
        'flutter.widgets.SizedBox',
        _tile({}, {
          'leading': _node(3, 'flutter.widgets.SizedBox', {
            'width': _n(1000.0),
            'height': _n(10),
          }),
          'title': _text(2, 'Title'),
        }),
        {'width': _n(0)},
      ),
      wrapWidth: false,
    );
    expect(_messages(tester), isNot(contains('preview unavailable')));
  });
  testWidgets(
    'active nonfinite offsets are fenced but inactive geometry is preserved',
    (tester) async {
      for (final name in ['horizontalTitleGap', 'minLeadingWidth']) {
        for (final value in ['infinity', 'negativeInfinity', 'nan']) {
          final props = {name: _e('double', value)};
          await _pump(tester, _tile(props, {'title': _text(2, 'Title')}));
          expect(_messages(tester), isEmpty);
          await _pump(
            tester,
            _tile(props, {'title': _text(2, 'Title'), 'leading': _box(3, 20)}),
          );
          expect(
            _messages(tester).contains('nonfinite positions'),
            name == 'horizontalTitleGap' || value != 'negativeInfinity',
          );
        }
      }
      for (final name in ['minVerticalPadding', 'minTileHeight']) {
        await _pump(
          tester,
          _tile({name: _e('double', 'nan')}, {'title': _text(2, 'Title')}),
        );
        expect(_messages(tester), contains('nonfinite positions'));
        await _pump(
          tester,
          _tile(
            {name: _e('double', 'negativeInfinity')},
            {'title': _text(2, 'Title')},
          ),
        );
        expect(_messages(tester), isEmpty);
      }
    },
  );
  testWidgets(
    'unbounded height stays SDK-native unless resolved nonfinite height is unsafe',
    (tester) async {
      expect(
        await _rawErrors(
          tester,
          const SingleChildScrollView(
            child: ListTile(
              minVerticalPadding: double.infinity,
              title: Text('Title'),
            ),
          ),
        ),
        isNotEmpty,
      );
      for (final props in [
        <String, Object?>{},
        {'minVerticalPadding': _e('double', 'infinity')},
      ]) {
        await _pump(
          tester,
          _wrap(
            7,
            'flutter.widgets.SingleChildScrollView',
            _tile(props, {'title': _text(2, 'Title')}),
          ),
        );
        expect(
          _messages(tester).contains('nonfinite height'),
          props.isNotEmpty,
        );
      }
    },
  );
  testWidgets(
    'opaque intermediate keeps SDK ink and existing nonfatal warning',
    (tester) async {
      final errors = <String>[], handler = FlutterError.onError;
      FlutterError.onError = (d) => errors.add(d.exceptionAsString());
      try {
        await _pump(
          tester,
          _wrap(
            7,
            'flutter.widgets.ColoredBox',
            _tile(
              {'onTap': _s('noop'), 'tileColor': _c(0xffabcdef)},
              {'title': _text(2, 'Title')},
            ),
            {'color': _c(0xffff0000)},
          ),
        );
      } finally {
        FlutterError.onError = handler;
      }
      expect(errors.join(), contains('background'));
      expect(_messages(tester), contains('intermediate ColoredBox'));
      expect(find.byType(ListTile), findsOneWidget);
      expect(
        find.ancestor(
          of: find.byType(ListTile),
          matching: find.byType(Material),
        ),
        findsNWidgets(2), // Existing Canvas surface + modeled Scaffold.
      );
    },
  );
  testWidgets(
    'TextField State and owned focus identity survive side-width guard recovery',
    (tester) async {
      final field = _node(3, 'flutter.material.TextField', {});
      Object tile(double width) => _tile({}, {
        'title': _text(2, 'Title'),
        'leading': _wrap(4, 'flutter.widgets.SizedBox', field, {
          'width': _n(width),
        }),
      });
      await _pump(tester, tile(80));
      final editable = find.byType(EditableText),
          state = tester.state<EditableTextState>(editable),
          focus = tester.widget<EditableText>(editable).focusNode;
      state.widget.controller.text = 'Retained';
      for (final width in [1000.0, 80.0, 2000.0, 90.0]) {
        await _pump(tester, tile(width));
        expect(tester.state<EditableTextState>(editable), same(state));
        expect(tester.widget<EditableText>(editable).focusNode, same(focus));
        expect(state.widget.controller.text, 'Retained');
        expect(
          focus.hasFocus,
          false,
        ); // Existing Canvas TextField is read-only.
        expect(_messages(tester).contains('entire nonzero tile'), width > 280);
      }
    },
  );
  testWidgets(
    'TextField State survives nonfinite geometry without model mutation',
    (tester) async {
      final field = _node(3, 'flutter.material.TextField', {});
      Object tile(Object value) =>
          _tile({'minVerticalPadding': value}, {'title': field});
      await _pump(tester, tile(_n(4)));
      final state = tester.state<EditableTextState>(find.byType(EditableText));
      for (final value in [
        _e('double', 'nan'),
        _n(6),
        _e('double', 'infinity'),
        _n(8),
      ]) {
        final model = tile(value), before = jsonEncode(tile(value));
        await _pump(tester, model);
        expect(
          tester.state<EditableTextState>(find.byType(EditableText)),
          same(state),
        );
        expect(jsonEncode(model), before);
      }
    },
  );
}
