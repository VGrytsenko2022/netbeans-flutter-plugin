import 'dart:convert';
import 'dart:typed_data';

const canvasModelFormat = 'netbeans-flutter-canvas-model';
const canvasModelProtocolVersion = 1;
const maxCanvasSequence = 9007199254740991;

final _stableIdPattern = RegExp(
  r'^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
);

class CanvasModel {
  const CanvasModel({
    required this.sessionId,
    required this.presentationSequence,
    required this.documentId,
    required this.logicalRevisionId,
    required this.profile,
    required this.root,
    required this.widgetIds,
  });

  final String sessionId;
  final int presentationSequence;
  final String documentId;
  final int logicalRevisionId;
  final CanvasProfile profile;
  final CanvasNode root;
  final Set<String> widgetIds;

  static CanvasModel decode(Uint8List bytes) {
    final Object? decoded;
    try {
      decoded = jsonDecode(utf8.decode(bytes, allowMalformed: false));
    } on Object catch (error) {
      throw FormatException('Canvas model is not valid UTF-8 JSON: $error');
    }
    final object = _object(decoded, r'$');
    _exactKeys(object, r'$', const {
      'format',
      'protocolVersion',
      'sessionId',
      'presentationSequence',
      'documentId',
      'logicalRevisionId',
      'profile',
      'root',
    });
    _expect(
      object['format'] == canvasModelFormat,
      'Canvas model format is not supported.',
    );
    _expect(
      object['protocolVersion'] == canvasModelProtocolVersion,
      'Canvas model protocol version is not supported.',
    );
    final sessionId = _stableId(object['sessionId'], r'$/sessionId');
    final presentationSequence = _sequence(
      object['presentationSequence'],
      r'$/presentationSequence',
    );
    final documentId = _stableId(object['documentId'], r'$/documentId');
    final logicalRevisionId = _sequence(
      object['logicalRevisionId'],
      r'$/logicalRevisionId',
    );
    final profile = CanvasProfile.decode(object['profile']);
    final budget = _NodeBudget();
    final root = CanvasNode._decode(object['root'], budget, 0, r'$/root');
    return CanvasModel(
      sessionId: sessionId,
      presentationSequence: presentationSequence,
      documentId: documentId,
      logicalRevisionId: logicalRevisionId,
      profile: profile,
      root: root,
      widgetIds: Set.unmodifiable(budget.ids),
    );
  }
}

class CanvasProfile {
  const CanvasProfile({
    required this.previewMode,
    required this.targetPlatform,
    required this.logicalWidth,
    required this.logicalHeight,
    required this.devicePixelRatio,
    required this.brightness,
    required this.themeIdentity,
    required this.locale,
    required this.textScaleFactor,
  });

  final String previewMode;
  final String targetPlatform;
  final double logicalWidth;
  final double logicalHeight;
  final double devicePixelRatio;
  final String brightness;
  final String themeIdentity;
  final String locale;
  final double textScaleFactor;

  static CanvasProfile decode(Object? value) {
    final object = _object(value, r'$/profile');
    _exactKeys(object, r'$/profile', const {
      'previewMode',
      'targetPlatform',
      'logicalWidth',
      'logicalHeight',
      'devicePixelRatio',
      'brightness',
      'themeIdentity',
      'locale',
      'textScaleFactor',
    });
    final previewMode = _enumText(
      object['previewMode'],
      r'$/profile/previewMode',
      const {'mobile', 'tablet', 'desktop', 'web'},
    );
    final targetPlatform = _enumText(
      object['targetPlatform'],
      r'$/profile/targetPlatform',
      const {'android', 'ios', 'windows', 'macos', 'linux', 'web'},
    );
    final logicalWidth = _finiteNumber(
      object['logicalWidth'],
      r'$/profile/logicalWidth',
      minimumExclusive: 0,
      maximum: 10000,
    );
    final logicalHeight = _finiteNumber(
      object['logicalHeight'],
      r'$/profile/logicalHeight',
      minimumExclusive: 0,
      maximum: 10000,
    );
    final devicePixelRatio = _finiteNumber(
      object['devicePixelRatio'],
      r'$/profile/devicePixelRatio',
      minimumExclusive: 0,
      maximum: 10,
    );
    final physicalWidth = (logicalWidth * devicePixelRatio).ceil();
    final physicalHeight = (logicalHeight * devicePixelRatio).ceil();
    _expect(
      physicalWidth <= 4096 &&
          physicalHeight <= 4096 &&
          physicalWidth * physicalHeight <= 8388608,
      'Canvas profile exceeds the negotiated physical surface bounds.',
    );
    final brightness = _enumText(
      object['brightness'],
      r'$/profile/brightness',
      const {'light', 'dark'},
    );
    final themeIdentity = _boundedText(
      object['themeIdentity'],
      r'$/profile/themeIdentity',
      1,
      128,
    );
    final locale = _boundedText(object['locale'], r'$/profile/locale', 2, 64);
    _expect(
      RegExp(r'^[A-Za-z]{2,8}(?:[-_][A-Za-z0-9]{2,8})*$').hasMatch(locale),
      'Canvas locale is not a supported BCP-47-style language tag.',
    );
    final textScaleFactor = _finiteNumber(
      object['textScaleFactor'],
      r'$/profile/textScaleFactor',
      minimumExclusive: 0,
      maximum: 5,
    );
    return CanvasProfile(
      previewMode: previewMode,
      targetPlatform: targetPlatform,
      logicalWidth: logicalWidth,
      logicalHeight: logicalHeight,
      devicePixelRatio: devicePixelRatio,
      brightness: brightness,
      themeIdentity: themeIdentity,
      locale: locale.replaceAll('_', '-'),
      textScaleFactor: textScaleFactor,
    );
  }
}

class CanvasNode {
  const CanvasNode({
    required this.id,
    required this.type,
    required this.properties,
    required this.slots,
  });

  final String id;
  final String type;
  final Map<String, CanvasValue> properties;
  final Map<String, CanvasSlot> slots;

  CanvasSlot? slot(String name) => slots[name];

  static CanvasNode _decode(
    Object? value,
    _NodeBudget budget,
    int depth,
    String path,
  ) {
    _expect(depth <= 256, 'Canvas widget tree exceeds the maximum depth.');
    _expect(budget.count < 10000, 'Canvas widget tree has too many nodes.');
    final object = _object(value, path);
    _exactKeys(object, path, const {'id', 'type', 'properties', 'slots'});
    final id = _stableId(object['id'], '$path/id');
    _expect(budget.ids.add(id), 'Canvas widget ids must be unique.');
    budget.count++;
    final type = _boundedText(object['type'], '$path/type', 1, 255);
    _expect(
      _widgetSpecifications.containsKey(type),
      'Unsupported Canvas widget type: $type',
    );

    final rawProperties = _object(object['properties'], '$path/properties');
    final specification = _widgetSpecifications[type]!;
    _expect(
      rawProperties.length <= specification.properties.length,
      'Canvas node has too many properties: $path',
    );
    final properties = <String, CanvasValue>{};
    for (final entry in rawProperties.entries) {
      final property = specification.properties[entry.key];
      _expect(property != null, 'Unsupported property $type.${entry.key}.');
      properties[entry.key] = CanvasValue._decode(
        entry.value,
        property!,
        '$path/properties/${entry.key}',
      );
    }
    _validatePropertyRelationships(type, properties, path, budget);

    final rawSlots = _object(object['slots'], '$path/slots');
    _expect(
      rawSlots.length <= specification.slots.length,
      'Canvas node has too many slots: $path',
    );
    final slots = <String, CanvasSlot>{};
    for (final entry in rawSlots.entries) {
      final expectedKind = specification.slots[entry.key];
      _expect(expectedKind != null, 'Unsupported slot $type.${entry.key}.');
      slots[entry.key] = CanvasSlot._decode(
        entry.value,
        expectedKind!,
        budget,
        depth + 1,
        '$path/slots/${entry.key}',
      );
    }
    return CanvasNode(
      id: id,
      type: type,
      properties: Map.unmodifiable(properties),
      slots: Map.unmodifiable(slots),
    );
  }
}

class CanvasSlot {
  const CanvasSlot(this.kind, this.children);

  final String kind;
  final List<CanvasNode> children;

  CanvasNode? get child => children.isEmpty ? null : children.first;

  static CanvasSlot _decode(
    Object? value,
    String expectedKind,
    _NodeBudget budget,
    int depth,
    String path,
  ) {
    final object = _object(value, path);
    if (expectedKind == 'single') {
      _exactKeys(object, path, const {'kind', 'child'});
      _expect(
        object['kind'] == 'single',
        'Canvas single slot kind is invalid: $path',
      );
      final child = object['child'];
      return CanvasSlot(
        'single',
        child == null
            ? const []
            : [CanvasNode._decode(child, budget, depth, '$path/child')],
      );
    }
    _exactKeys(object, path, const {'kind', 'children'});
    _expect(
      object['kind'] == 'list',
      'Canvas list slot kind is invalid: $path',
    );
    final rawChildren = object['children'];
    _expect(
      rawChildren is List<Object?>,
      'Canvas list slot children must be an array: $path',
    );
    final children = rawChildren as List<Object?>;
    _expect(
      children.length <= 10000,
      'Canvas list slot has too many children: $path',
    );
    return CanvasSlot(
      'list',
      List.unmodifiable([
        for (var index = 0; index < children.length; index++)
          CanvasNode._decode(
            children[index],
            budget,
            depth,
            '$path/children/$index',
          ),
      ]),
    );
  }
}

class CanvasValue {
  const CanvasValue(this.kind, this.value);

  final String kind;
  final Object value;

  static CanvasValue _decode(Object? value, _PropertySpec spec, String path) {
    final object = _object(value, path);
    final kind = object['kind'];
    _expect(kind is String, 'Canvas property kind must be a string: $path');
    _expect(
      spec.kinds.contains(kind),
      'Canvas property kind is not allowed at $path.',
    );
    switch (kind) {
      case 'string':
        _exactKeys(object, path, const {'kind', 'value'});
        return CanvasValue(
          kind as String,
          _boundedPropertyText(object['value'], '$path/value', 65536),
        );
      case 'boolean':
        _exactKeys(object, path, const {'kind', 'value'});
        _expect(
          object['value'] is bool,
          'Canvas boolean value is invalid: $path',
        );
        return CanvasValue(kind as String, object['value']! as bool);
      case 'integer':
        _exactKeys(object, path, const {'kind', 'value'});
        final integer = object['value'];
        _expect(integer is int, 'Canvas integer value is invalid: $path');
        _expect(
          (integer as int).abs() <= maxCanvasSequence,
          'Canvas integer value exceeds the interoperable range: $path',
        );
        if (spec.minimum != null) {
          _expect(
            integer >= spec.minimum!,
            'Canvas integer value is below its minimum: $path',
          );
        }
        return CanvasValue(kind as String, integer);
      case 'double':
        _exactKeys(object, path, const {'kind', 'value'});
        final number = _finiteNumber(object['value'], '$path/value');
        if (spec.minimum != null) {
          _expect(
            number >= spec.minimum!,
            'Canvas number is below its minimum: $path',
          );
        }
        return CanvasValue(kind as String, number);
      case 'enum':
        _exactKeys(object, path, const {'kind', 'type', 'value'});
        final enumType = _boundedText(object['type'], '$path/type', 1, 255);
        final enumValue = _boundedText(object['value'], '$path/value', 1, 255);
        _expect(
          enumType == spec.enumType,
          'Canvas enum type is not allowed at $path.',
        );
        _expect(
          spec.enumValues.contains(enumValue),
          'Canvas enum value is not allowed at $path.',
        );
        return CanvasValue(
          kind as String,
          CanvasEnumValue(enumType, enumValue),
        );
      case 'color':
        _exactKeys(object, path, const {'kind', 'argb'});
        final argb = _boundedText(object['argb'], '$path/argb', 10, 10);
        _expect(
          RegExp(r'^0x[0-9A-F]{8}$').hasMatch(argb),
          'Canvas ARGB color is invalid: $path',
        );
        return CanvasValue(
          kind as String,
          int.parse(argb.substring(2), radix: 16),
        );
      case 'edgeInsets':
        _exactKeys(object, path, const {
          'kind',
          'left',
          'top',
          'right',
          'bottom',
        });
        return CanvasValue(
          kind as String,
          CanvasEdgeInsets(
            _finiteNumber(object['left'], '$path/left'),
            _finiteNumber(object['top'], '$path/top'),
            _finiteNumber(object['right'], '$path/right'),
            _finiteNumber(object['bottom'], '$path/bottom'),
          ),
        );
      default:
        throw FormatException('Unsupported Canvas property kind at $path.');
    }
  }
}

class CanvasEnumValue {
  const CanvasEnumValue(this.type, this.value);
  final String type;
  final String value;
}

class CanvasEdgeInsets {
  const CanvasEdgeInsets(this.left, this.top, this.right, this.bottom);
  final double left;
  final double top;
  final double right;
  final double bottom;
}

class _WidgetSpec {
  const _WidgetSpec(this.properties, this.slots);
  final Map<String, _PropertySpec> properties;
  final Map<String, String> slots;
}

class _PropertySpec {
  const _PropertySpec(
    this.kinds, {
    this.enumType,
    this.enumValues = const {},
    this.minimum,
  });
  final Set<String> kinds;
  final String? enumType;
  final Set<String> enumValues;
  final num? minimum;
}

const _widgetSpecifications = <String, _WidgetSpec>{
  'flutter.material.Scaffold': _WidgetSpec(
    {
      'backgroundColor': _PropertySpec({'color'}),
      'resizeToAvoidBottomInset': _PropertySpec({'boolean'}),
    },
    {'appBar': 'single', 'body': 'single', 'floatingActionButton': 'single'},
  ),
  'flutter.widgets.Column': _WidgetSpec(
    {
      'mainAxisAlignment': _PropertySpec(
        {'enum'},
        enumType: 'MainAxisAlignment',
        enumValues: {
          'start',
          'end',
          'center',
          'spaceBetween',
          'spaceAround',
          'spaceEvenly',
        },
      ),
      'mainAxisSize': _PropertySpec(
        {'enum'},
        enumType: 'MainAxisSize',
        enumValues: {'min', 'max'},
      ),
      'crossAxisAlignment': _PropertySpec(
        {'enum'},
        enumType: 'CrossAxisAlignment',
        enumValues: {'start', 'end', 'center', 'stretch', 'baseline'},
      ),
      'textDirection': _PropertySpec(
        {'enum'},
        enumType: 'TextDirection',
        enumValues: {'rtl', 'ltr'},
      ),
      'verticalDirection': _PropertySpec(
        {'enum'},
        enumType: 'VerticalDirection',
        enumValues: {'up', 'down'},
      ),
      'textBaseline': _PropertySpec(
        {'enum'},
        enumType: 'TextBaseline',
        enumValues: {'alphabetic', 'ideographic'},
      ),
      'spacing': _PropertySpec({'double'}, minimum: 0),
    },
    {'children': 'list'},
  ),
  'flutter.widgets.Row': _WidgetSpec(
    {
      'mainAxisAlignment': _PropertySpec(
        {'enum'},
        enumType: 'MainAxisAlignment',
        enumValues: {
          'start',
          'end',
          'center',
          'spaceBetween',
          'spaceAround',
          'spaceEvenly',
        },
      ),
      'mainAxisSize': _PropertySpec(
        {'enum'},
        enumType: 'MainAxisSize',
        enumValues: {'min', 'max'},
      ),
      'crossAxisAlignment': _PropertySpec(
        {'enum'},
        enumType: 'CrossAxisAlignment',
        enumValues: {'start', 'end', 'center', 'stretch', 'baseline'},
      ),
      'textDirection': _PropertySpec(
        {'enum'},
        enumType: 'TextDirection',
        enumValues: {'rtl', 'ltr'},
      ),
      'verticalDirection': _PropertySpec(
        {'enum'},
        enumType: 'VerticalDirection',
        enumValues: {'up', 'down'},
      ),
      'textBaseline': _PropertySpec(
        {'enum'},
        enumType: 'TextBaseline',
        enumValues: {'alphabetic', 'ideographic'},
      ),
      'spacing': _PropertySpec({'double'}, minimum: 0),
    },
    {'children': 'list'},
  ),
  'flutter.widgets.Padding': _WidgetSpec(
    {
      'padding': _PropertySpec({'edgeInsets'}),
    },
    {'child': 'single'},
  ),
  'flutter.widgets.Center': _WidgetSpec(
    {
      'widthFactor': _PropertySpec({'integer', 'double'}, minimum: 0),
      'heightFactor': _PropertySpec({'integer', 'double'}, minimum: 0),
    },
    {'child': 'single'},
  ),
  'flutter.widgets.Text': _WidgetSpec({
    'data': _PropertySpec({'string'}),
    'textAlign': _PropertySpec(
      {'enum'},
      enumType: 'TextAlign',
      enumValues: {'start', 'end', 'left', 'right', 'center', 'justify'},
    ),
    'textDirection': _PropertySpec(
      {'enum'},
      enumType: 'TextDirection',
      enumValues: {'rtl', 'ltr'},
    ),
    'softWrap': _PropertySpec({'boolean'}),
    'maxLines': _PropertySpec({'integer'}, minimum: 1),
    'overflow': _PropertySpec(
      {'enum'},
      enumType: 'TextOverflow',
      enumValues: {'clip', 'fade', 'ellipsis', 'visible'},
    ),
    'semanticsLabel': _PropertySpec({'string'}),
    'semanticsIdentifier': _PropertySpec({'string'}),
    'textWidthBasis': _PropertySpec(
      {'enum'},
      enumType: 'TextWidthBasis',
      enumValues: {'parent', 'longestLine'},
    ),
    'selectionColor': _PropertySpec({'color'}),
  }, {}),
};

class _NodeBudget {
  int count = 0;
  final ids = <String>{};
  final semanticsIdentifierPaths = <String, String>{};
}

void _validatePropertyRelationships(
  String type,
  Map<String, CanvasValue> properties,
  String path,
  _NodeBudget budget,
) {
  if (type == 'flutter.widgets.Column' || type == 'flutter.widgets.Row') {
    final crossAxisAlignment = properties['crossAxisAlignment']?.value;
    if (crossAxisAlignment is CanvasEnumValue &&
        crossAxisAlignment.value == 'baseline') {
      _expect(
        properties.containsKey('textBaseline'),
        'Canvas $type requires textBaseline when crossAxisAlignment is baseline: '
        '$path/properties/textBaseline',
      );
    }
  }

  if (type != 'flutter.widgets.Text') {
    return;
  }
  final semanticsIdentifier = properties['semanticsIdentifier']?.value;
  if (semanticsIdentifier is! String) {
    return;
  }
  final propertyPath = '$path/properties/semanticsIdentifier';
  final firstPath = budget.semanticsIdentifierPaths[semanticsIdentifier];
  _expect(
    firstPath == null,
    'Canvas Text semanticsIdentifier must be unique: $propertyPath duplicates '
    '$firstPath',
  );
  budget.semanticsIdentifierPaths[semanticsIdentifier] = propertyPath;
}

Map<String, Object?> _object(Object? value, String path) {
  _expect(
    value is Map<String, Object?>,
    'Canvas value must be an object: $path',
  );
  return value! as Map<String, Object?>;
}

void _exactKeys(
  Map<String, Object?> object,
  String path,
  Set<String> expected,
) {
  _expect(
    object.length == expected.length &&
        object.keys.toSet().containsAll(expected),
    'Canvas object has missing or unknown fields: $path',
  );
}

String _stableId(Object? value, String path) {
  final text = _boundedText(value, path, 36, 36);
  _expect(_stableIdPattern.hasMatch(text), 'Canvas identity is invalid: $path');
  return text;
}

int _sequence(Object? value, String path) {
  _expect(value is int, 'Canvas sequence must be an integer: $path');
  final sequence = value! as int;
  _expect(
    sequence >= 0 && sequence <= maxCanvasSequence,
    'Canvas sequence is outside the interoperable range: $path',
  );
  return sequence;
}

String _enumText(Object? value, String path, Set<String> allowed) {
  final text = _boundedText(value, path, 1, 32).toLowerCase();
  _expect(allowed.contains(text), 'Canvas enum value is not allowed: $path');
  return text;
}

String _boundedText(Object? value, String path, int minimum, int maximum) {
  _expect(value is String, 'Canvas value must be a string: $path');
  final text = value! as String;
  _expect(
    text.runes.length >= minimum && text.runes.length <= maximum,
    'Canvas string length is outside its bounds: $path',
  );
  _expect(
    !text.runes.any(
      (codePoint) =>
          codePoint < 0x20 || (codePoint >= 0x7f && codePoint <= 0x9f),
    ),
    'Canvas string contains control characters: $path',
  );
  return text;
}

String _boundedPropertyText(Object? value, String path, int maximum) {
  _expect(value is String, 'Canvas value must be a string: $path');
  final text = value! as String;
  _expect(
    text.runes.length <= maximum,
    'Canvas string length is outside its bounds: $path',
  );
  return text;
}

double _finiteNumber(
  Object? value,
  String path, {
  num? minimumExclusive,
  num? maximum,
}) {
  _expect(value is num, 'Canvas value must be a number: $path');
  final number = (value! as num).toDouble();
  _expect(number.isFinite, 'Canvas number must be finite: $path');
  if (minimumExclusive != null) {
    _expect(
      number > minimumExclusive,
      'Canvas number is below its minimum: $path',
    );
  }
  if (maximum != null) {
    _expect(number <= maximum, 'Canvas number exceeds its maximum: $path');
  }
  return number;
}

Never _fail(String message) => throw FormatException(message);

void _expect(bool condition, String message) {
  if (!condition) {
    _fail(message);
  }
}
