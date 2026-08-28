import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:netbeans_flutter_canvas_runner/src/canvas_model.dart';

void main() {
  test('decodes the strict six-widget read-only projection', () {
    final model = CanvasModel.decode(modelBytesForViewTest());

    expect(model.profile.previewMode, 'mobile');
    expect(model.profile.targetPlatform, 'windows');
    expect(model.root.type, 'flutter.material.Scaffold');
    expect(model.widgetIds, hasLength(8));
    expect(
      model.root.slot('body')!.child!.slot('children')!.children,
      hasLength(2),
    );
    final column = model.root.slot('body')!.child!;
    expect(column.properties['spacing']!.value, 12.5);
    expect(
      (column.properties['crossAxisAlignment']!.value as CanvasEnumValue).value,
      'baseline',
    );
    final text = column
        .slot('children')!
        .children
        .first
        .slot('child')!
        .child!
        .slot('child')!
        .child!;
    expect(text.properties['semanticsIdentifier']!.value, 'primary-greeting');
    expect(text.properties['selectionColor']!.value, 0xff336699);
  });

  test('rejects baseline alignment without an exact text baseline', () {
    final json = _modelJson();
    final properties = _column(json)['properties']! as Map<String, Object?>;
    properties.remove('textBaseline');

    expect(() => _decode(json), throwsFormatException);
  });

  test('rejects negative or non-double flex spacing', () {
    final negative = _modelJson();
    (_column(negative)['properties']! as Map<String, Object?>)['spacing'] = {
      'kind': 'double',
      'value': -0.5,
    };
    expect(() => _decode(negative), throwsFormatException);

    final integer = _modelJson();
    (_column(integer)['properties']! as Map<String, Object?>)['spacing'] = {
      'kind': 'integer',
      'value': 1,
    };
    expect(() => _decode(integer), throwsFormatException);
  });

  test('rejects duplicate Text semantics identifiers across the tree', () {
    final json = _modelJson();
    final texts = _textNodes(_column(json));
    final firstProperties = texts[0]['properties']! as Map<String, Object?>;
    final secondProperties = texts[1]['properties']! as Map<String, Object?>;
    secondProperties['semanticsIdentifier'] = Map<String, Object?>.from(
      firstProperties['semanticsIdentifier']! as Map<String, Object?>,
    );

    expect(() => _decode(json), throwsFormatException);
  });

  test('rejects executable or unknown property values', () {
    final json = _modelJson();
    final text =
        (((json['root'] as Map<String, Object?>)['slots']
                    as Map<String, Object?>)['body']
                as Map<String, Object?>)['child']
            as Map<String, Object?>;
    final properties = text['properties'] as Map<String, Object?>;
    properties['onTap'] = {'kind': 'dartExpression', 'value': 'launch()'};

    expect(
      () =>
          CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(json)))),
      throwsFormatException,
    );
  });

  test('rejects duplicate stable widget identities', () {
    final json = _modelJson();
    final root = json['root'] as Map<String, Object?>;
    final column =
        ((root['slots'] as Map<String, Object?>)['body']
                as Map<String, Object?>)['child']
            as Map<String, Object?>;
    final children =
        ((column['slots'] as Map<String, Object?>)['children']
                as Map<String, Object?>)['children']
            as List<Object?>;
    (children[1] as Map<String, Object?>)['id'] =
        (children[0] as Map<String, Object?>)['id'];

    expect(
      () =>
          CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(json)))),
      throwsFormatException,
    );
  });

  test(
    'admits the exact target-platform vocabulary and rejects unknown targets',
    () {
      for (final platform in const [
        'android',
        'ios',
        'windows',
        'macos',
        'linux',
        'web',
      ]) {
        final json = _modelJson();
        (json['profile']! as Map<String, Object?>)['targetPlatform'] = platform;
        expect(
          CanvasModel.decode(
            Uint8List.fromList(utf8.encode(jsonEncode(json))),
          ).profile.targetPlatform,
          platform,
        );
      }

      final invalid = _modelJson();
      (invalid['profile']! as Map<String, Object?>)['targetPlatform'] =
          'browser';
      expect(
        () => CanvasModel.decode(
          Uint8List.fromList(utf8.encode(jsonEncode(invalid))),
        ),
        throwsFormatException,
      );
    },
  );
}

Uint8List modelBytesForViewTest() =>
    Uint8List.fromList(utf8.encode(jsonEncode(_modelJson())));

Map<String, Object?> _modelJson() => {
  'format': 'netbeans-flutter-canvas-model',
  'protocolVersion': 1,
  'sessionId': '80ef60ed-b108-4674-99a6-c1f3102f01ab',
  'presentationSequence': 4,
  'documentId': 'd2d37c77-8510-4bd0-9280-a72e5bc3871e',
  'logicalRevisionId': 2,
  'profile': {
    'previewMode': 'MOBILE',
    'targetPlatform': 'WINDOWS',
    'logicalWidth': 390,
    'logicalHeight': 844,
    'devicePixelRatio': 1,
    'brightness': 'LIGHT',
    'themeIdentity': 'material-default',
    'locale': 'en-US',
    'textScaleFactor': 1,
  },
  'root': _node(
    '6e88bff4-8d73-48aa-92b5-87aa3344f6a7',
    'flutter.material.Scaffold',
    properties: {
      'backgroundColor': {'kind': 'color', 'argb': '0xFFFFFFFF'},
      'resizeToAvoidBottomInset': {'kind': 'boolean', 'value': true},
    },
    slots: {
      'body': _single(
        _node(
          '4c04dc44-7381-4ca8-826f-8c23bc2351d1',
          'flutter.widgets.Column',
          properties: {
            'mainAxisAlignment': {
              'kind': 'enum',
              'type': 'MainAxisAlignment',
              'value': 'center',
            },
            'mainAxisSize': {
              'kind': 'enum',
              'type': 'MainAxisSize',
              'value': 'min',
            },
            'crossAxisAlignment': {
              'kind': 'enum',
              'type': 'CrossAxisAlignment',
              'value': 'baseline',
            },
            'textDirection': {
              'kind': 'enum',
              'type': 'TextDirection',
              'value': 'ltr',
            },
            'verticalDirection': {
              'kind': 'enum',
              'type': 'VerticalDirection',
              'value': 'down',
            },
            'textBaseline': {
              'kind': 'enum',
              'type': 'TextBaseline',
              'value': 'alphabetic',
            },
            'spacing': {'kind': 'double', 'value': 12.5},
          },
          slots: {
            'children': _list([
              _node(
                '0f78bed5-2fba-42cd-914e-a3abbd2c48c3',
                'flutter.widgets.Padding',
                properties: {
                  'padding': {
                    'kind': 'edgeInsets',
                    'left': 16,
                    'top': 16,
                    'right': 16,
                    'bottom': 16,
                  },
                },
                slots: {
                  'child': _single(
                    _node(
                      '733ef462-41d7-4849-b780-5d9520666ae4',
                      'flutter.widgets.Center',
                      properties: {
                        'widthFactor': {'kind': 'double', 'value': 1.0},
                      },
                      slots: {
                        'child': _single(
                          _node(
                            '5ab6c203-3d32-489c-9d7a-7c14f29637cb',
                            'flutter.widgets.Text',
                            properties: {
                              'data': {'kind': 'string', 'value': 'Hello'},
                              'textAlign': {
                                'kind': 'enum',
                                'type': 'TextAlign',
                                'value': 'center',
                              },
                              'textDirection': {
                                'kind': 'enum',
                                'type': 'TextDirection',
                                'value': 'ltr',
                              },
                              'softWrap': {'kind': 'boolean', 'value': false},
                              'overflow': {
                                'kind': 'enum',
                                'type': 'TextOverflow',
                                'value': 'ellipsis',
                              },
                              'maxLines': {'kind': 'integer', 'value': 2},
                              'semanticsLabel': {
                                'kind': 'string',
                                'value': 'Primary greeting',
                              },
                              'semanticsIdentifier': {
                                'kind': 'string',
                                'value': 'primary-greeting',
                              },
                              'textWidthBasis': {
                                'kind': 'enum',
                                'type': 'TextWidthBasis',
                                'value': 'longestLine',
                              },
                              'selectionColor': {
                                'kind': 'color',
                                'argb': '0xFF336699',
                              },
                            },
                          ),
                        ),
                      },
                    ),
                  ),
                },
              ),
              _node(
                '1035b7df-df9b-442b-9af2-72b4c90f1462',
                'flutter.widgets.Row',
                properties: {
                  'mainAxisAlignment': {
                    'kind': 'enum',
                    'type': 'MainAxisAlignment',
                    'value': 'end',
                  },
                  'mainAxisSize': {
                    'kind': 'enum',
                    'type': 'MainAxisSize',
                    'value': 'min',
                  },
                  'crossAxisAlignment': {
                    'kind': 'enum',
                    'type': 'CrossAxisAlignment',
                    'value': 'baseline',
                  },
                  'textDirection': {
                    'kind': 'enum',
                    'type': 'TextDirection',
                    'value': 'rtl',
                  },
                  'verticalDirection': {
                    'kind': 'enum',
                    'type': 'VerticalDirection',
                    'value': 'up',
                  },
                  'textBaseline': {
                    'kind': 'enum',
                    'type': 'TextBaseline',
                    'value': 'ideographic',
                  },
                  'spacing': {'kind': 'double', 'value': 4.0},
                },
                slots: {
                  'children': _list([
                    _node(
                      '64260967-f830-4e3c-bbd1-f81cb79db092',
                      'flutter.widgets.Text',
                      properties: {
                        'data': {'kind': 'string', 'value': 'World'},
                        'semanticsIdentifier': {
                          'kind': 'string',
                          'value': 'secondary-greeting',
                        },
                      },
                    ),
                  ]),
                },
              ),
            ]),
          },
        ),
      ),
      'floatingActionButton': _single(
        _node(
          'd341f7e9-c3d5-47f2-a91d-25963065f9f8',
          'flutter.widgets.Text',
          properties: {
            'data': {'kind': 'string', 'value': 'Action'},
            'semanticsIdentifier': {
              'kind': 'string',
              'value': 'floating-action-label',
            },
          },
        ),
      ),
    },
  ),
};

Map<String, Object?> _node(
  String id,
  String type, {
  Map<String, Object?> properties = const {},
  Map<String, Object?> slots = const {},
}) => {'id': id, 'type': type, 'properties': properties, 'slots': slots};

Map<String, Object?> _single(Map<String, Object?>? child) => {
  'kind': 'single',
  'child': child,
};

Map<String, Object?> _list(List<Map<String, Object?>> children) => {
  'kind': 'list',
  'children': children,
};

CanvasModel _decode(Map<String, Object?> json) =>
    CanvasModel.decode(Uint8List.fromList(utf8.encode(jsonEncode(json))));

Map<String, Object?> _column(Map<String, Object?> json) {
  final root = json['root']! as Map<String, Object?>;
  final body =
      (root['slots']! as Map<String, Object?>)['body']! as Map<String, Object?>;
  return body['child']! as Map<String, Object?>;
}

List<Map<String, Object?>> _textNodes(Map<String, Object?> node) {
  final result = <Map<String, Object?>>[];
  void visit(Map<String, Object?> current) {
    if (current['type'] == 'flutter.widgets.Text') {
      result.add(current);
    }
    final slots = current['slots']! as Map<String, Object?>;
    for (final rawSlot in slots.values) {
      final slot = rawSlot! as Map<String, Object?>;
      if (slot['kind'] == 'single') {
        final child = slot['child'];
        if (child is Map<String, Object?>) {
          visit(child);
        }
      } else {
        for (final child in slot['children']! as List<Object?>) {
          visit(child! as Map<String, Object?>);
        }
      }
    }
  }

  visit(node);
  return result;
}
