import 'dart:convert';
import 'dart:typed_data';

const canvasModelFormat = 'netbeans-flutter-canvas-model';
const canvasModelProtocolVersion = 4;
const maxCanvasSequence = 9007199254740991;

const canvasColorSchemeThemeTokens = <String>{
  'material.colorScheme.primary',
  'material.colorScheme.onPrimary',
  'material.colorScheme.primaryContainer',
  'material.colorScheme.onPrimaryContainer',
  'material.colorScheme.primaryFixed',
  'material.colorScheme.primaryFixedDim',
  'material.colorScheme.onPrimaryFixed',
  'material.colorScheme.onPrimaryFixedVariant',
  'material.colorScheme.secondary',
  'material.colorScheme.onSecondary',
  'material.colorScheme.secondaryContainer',
  'material.colorScheme.onSecondaryContainer',
  'material.colorScheme.secondaryFixed',
  'material.colorScheme.secondaryFixedDim',
  'material.colorScheme.onSecondaryFixed',
  'material.colorScheme.onSecondaryFixedVariant',
  'material.colorScheme.tertiary',
  'material.colorScheme.onTertiary',
  'material.colorScheme.tertiaryContainer',
  'material.colorScheme.onTertiaryContainer',
  'material.colorScheme.tertiaryFixed',
  'material.colorScheme.tertiaryFixedDim',
  'material.colorScheme.onTertiaryFixed',
  'material.colorScheme.onTertiaryFixedVariant',
  'material.colorScheme.error',
  'material.colorScheme.onError',
  'material.colorScheme.errorContainer',
  'material.colorScheme.onErrorContainer',
  'material.colorScheme.surface',
  'material.colorScheme.onSurface',
  'material.colorScheme.surfaceDim',
  'material.colorScheme.surfaceBright',
  'material.colorScheme.surfaceContainerLowest',
  'material.colorScheme.surfaceContainerLow',
  'material.colorScheme.surfaceContainer',
  'material.colorScheme.surfaceContainerHigh',
  'material.colorScheme.surfaceContainerHighest',
  'material.colorScheme.onSurfaceVariant',
  'material.colorScheme.outline',
  'material.colorScheme.outlineVariant',
  'material.colorScheme.shadow',
  'material.colorScheme.scrim',
  'material.colorScheme.inverseSurface',
  'material.colorScheme.onInverseSurface',
  'material.colorScheme.inversePrimary',
  'material.colorScheme.surfaceTint',
};

const canvasTextThemeTokens = <String>{
  'material.textTheme.displayLarge',
  'material.textTheme.displayMedium',
  'material.textTheme.displaySmall',
  'material.textTheme.headlineLarge',
  'material.textTheme.headlineMedium',
  'material.textTheme.headlineSmall',
  'material.textTheme.titleLarge',
  'material.textTheme.titleMedium',
  'material.textTheme.titleSmall',
  'material.textTheme.bodyLarge',
  'material.textTheme.bodyMedium',
  'material.textTheme.bodySmall',
  'material.textTheme.labelLarge',
  'material.textTheme.labelMedium',
  'material.textTheme.labelSmall',
};

const canvasColorSchemeRoles = <String>{
  'primary',
  'onPrimary',
  'primaryContainer',
  'onPrimaryContainer',
  'primaryFixed',
  'primaryFixedDim',
  'onPrimaryFixed',
  'onPrimaryFixedVariant',
  'secondary',
  'onSecondary',
  'secondaryContainer',
  'onSecondaryContainer',
  'secondaryFixed',
  'secondaryFixedDim',
  'onSecondaryFixed',
  'onSecondaryFixedVariant',
  'tertiary',
  'onTertiary',
  'tertiaryContainer',
  'onTertiaryContainer',
  'tertiaryFixed',
  'tertiaryFixedDim',
  'onTertiaryFixed',
  'onTertiaryFixedVariant',
  'error',
  'onError',
  'errorContainer',
  'onErrorContainer',
  'surface',
  'onSurface',
  'surfaceDim',
  'surfaceBright',
  'surfaceContainerLowest',
  'surfaceContainerLow',
  'surfaceContainer',
  'surfaceContainerHigh',
  'surfaceContainerHighest',
  'onSurfaceVariant',
  'outline',
  'outlineVariant',
  'shadow',
  'scrim',
  'inverseSurface',
  'onInverseSurface',
  'inversePrimary',
  'surfaceTint',
};

const canvasTextThemeRoles = <String>{
  'displayLarge',
  'displayMedium',
  'displaySmall',
  'headlineLarge',
  'headlineMedium',
  'headlineSmall',
  'titleLarge',
  'titleMedium',
  'titleSmall',
  'bodyLarge',
  'bodyMedium',
  'bodySmall',
  'labelLarge',
  'labelMedium',
  'labelSmall',
};

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
    required this.theme,
    required this.locale,
    required this.textScaleFactor,
  });

  final String previewMode;
  final String targetPlatform;
  final double logicalWidth;
  final double logicalHeight;
  final double devicePixelRatio;
  final CanvasTheme theme;
  final String locale;
  final double textScaleFactor;

  String get brightness => theme.brightness;

  static CanvasProfile decode(Object? value) {
    final object = _object(value, r'$/profile');
    _exactKeys(object, r'$/profile', const {
      'previewMode',
      'targetPlatform',
      'logicalWidth',
      'logicalHeight',
      'devicePixelRatio',
      'theme',
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
    final theme = CanvasTheme.decode(object['theme']);
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
      theme: theme,
      locale: locale.replaceAll('_', '-'),
      textScaleFactor: textScaleFactor,
    );
  }
}

class CanvasTheme {
  const CanvasTheme({
    required this.definitionId,
    required this.seedArgb,
    required this.brightness,
    required this.digestIdentity,
    required this.colorScheme,
    required this.textTheme,
  });

  final String definitionId;
  final int seedArgb;
  final String brightness;
  final String digestIdentity;
  final Map<String, int> colorScheme;
  final Map<String, CanvasThemeTextStyleOverride> textTheme;

  static CanvasTheme decode(Object? value) {
    final object = _object(value, r'$/profile/theme');
    _exactKeys(object, r'$/profile/theme', const {
      'definitionId',
      'seedArgb',
      'brightness',
      'digestIdentity',
      'colorScheme',
      'textTheme',
    });
    final definitionId = _boundedText(
      object['definitionId'],
      r'$/profile/theme/definitionId',
      1,
      64,
    );
    _expect(
      RegExp(
        r'^(?:[a-z][a-z0-9_]*|material\.default\.(?:light|dark))$',
      ).hasMatch(definitionId),
      'Canvas theme definition id is not canonical.',
    );
    final seedLiteral = _boundedText(
      object['seedArgb'],
      r'$/profile/theme/seedArgb',
      10,
      10,
    );
    _expect(
      RegExp(r'^0xFF[0-9A-F]{6}$').hasMatch(seedLiteral),
      'Canvas theme seed ARGB value must be opaque 0xFFRRGGBB.',
    );
    final brightness = _enumText(
      object['brightness'],
      r'$/profile/theme/brightness',
      const {'light', 'dark'},
    );
    final digestIdentity = _boundedText(
      object['digestIdentity'],
      r'$/profile/theme/digestIdentity',
      64,
      64,
    );
    _expect(
      RegExp(r'^[0-9A-F]{64}$').hasMatch(digestIdentity),
      'Canvas theme digest identity must be an uppercase SHA-256 value.',
    );
    final colorScheme = _decodeThemeColorScheme(object['colorScheme']);
    final textTheme = _decodeThemeTextTheme(object['textTheme']);
    return CanvasTheme(
      definitionId: definitionId,
      seedArgb: int.parse(seedLiteral.substring(2), radix: 16),
      brightness: brightness,
      digestIdentity: digestIdentity,
      colorScheme: Map.unmodifiable(colorScheme),
      textTheme: Map.unmodifiable(textTheme),
    );
  }
}

sealed class CanvasThemeColorValue {
  const CanvasThemeColorValue();

  static CanvasThemeColorValue decode(Object? value, String path) {
    final object = _object(value, path);
    final kind = _boundedText(object['kind'], '$path/kind', 4, 11);
    if (kind == 'argb') {
      _exactKeys(object, path, const {'kind', 'argb'});
      final literal = _boundedText(object['argb'], '$path/argb', 10, 10);
      _expect(
        RegExp(r'^0x[0-9A-F]{8}$').hasMatch(literal),
        'Canvas theme color must use canonical 0xAARRGGBB: $path',
      );
      return CanvasThemeLiteralColor(
        int.parse(literal.substring(2), radix: 16),
      );
    }
    if (kind == 'colorScheme') {
      _exactKeys(object, path, const {'kind', 'role'});
      final role = _boundedText(object['role'], '$path/role', 1, 64);
      _expect(
        canvasColorSchemeRoles.contains(role),
        'Canvas theme color references an unknown ColorScheme role: $path',
      );
      return CanvasThemeRoleColor(role);
    }
    _fail('Canvas theme color kind is not supported: $path');
  }
}

class CanvasThemeLiteralColor extends CanvasThemeColorValue {
  const CanvasThemeLiteralColor(this.argb);
  final int argb;
}

class CanvasThemeRoleColor extends CanvasThemeColorValue {
  const CanvasThemeRoleColor(this.role);
  final String role;
}

class CanvasThemeTextStyleOverride {
  const CanvasThemeTextStyleOverride({
    this.color,
    this.backgroundColor,
    this.fontSize,
    this.fontWeight,
    this.fontStyle,
    this.letterSpacing,
    this.wordSpacing,
    this.height,
    this.fontFamily,
    this.decoration,
    this.decorationColor,
    this.decorationStyle,
    this.decorationThickness,
  });

  final CanvasThemeColorValue? color;
  final CanvasThemeColorValue? backgroundColor;
  final double? fontSize;
  final String? fontWeight;
  final String? fontStyle;
  final double? letterSpacing;
  final double? wordSpacing;
  final double? height;
  final String? fontFamily;
  final Set<String>? decoration;
  final CanvasThemeColorValue? decorationColor;
  final String? decorationStyle;
  final double? decorationThickness;

  static CanvasThemeTextStyleOverride decode(Object? value, String path) {
    final object = _object(value, path);
    _allowedKeys(object, path, const {
      'color',
      'backgroundColor',
      'fontSize',
      'fontWeight',
      'fontStyle',
      'letterSpacing',
      'wordSpacing',
      'height',
      'fontFamily',
      'decoration',
      'decorationColor',
      'decorationStyle',
      'decorationThickness',
    });
    _expect(
      object.isNotEmpty,
      'Canvas TextTheme role override is empty: $path',
    );
    final fontFamily = object.containsKey('fontFamily')
        ? _boundedText(object['fontFamily'], '$path/fontFamily', 1, 128)
        : null;
    if (fontFamily != null) {
      _expect(
        fontFamily.trim() == fontFamily,
        'Canvas theme font family has surrounding whitespace: $path',
      );
    }
    return CanvasThemeTextStyleOverride(
      color: object.containsKey('color')
          ? CanvasThemeColorValue.decode(object['color'], '$path/color')
          : null,
      backgroundColor: object.containsKey('backgroundColor')
          ? CanvasThemeColorValue.decode(
              object['backgroundColor'],
              '$path/backgroundColor',
            )
          : null,
      fontSize: object.containsKey('fontSize')
          ? _finiteNumber(object['fontSize'], '$path/fontSize', minimum: 0)
          : null,
      fontWeight: object.containsKey('fontWeight')
          ? _exactEnumText(object['fontWeight'], '$path/fontWeight', const {
              'w100',
              'w200',
              'w300',
              'w400',
              'w500',
              'w600',
              'w700',
              'w800',
              'w900',
            })
          : null,
      fontStyle: object.containsKey('fontStyle')
          ? _exactEnumText(object['fontStyle'], '$path/fontStyle', const {
              'normal',
              'italic',
            })
          : null,
      letterSpacing: object.containsKey('letterSpacing')
          ? _finiteNumber(object['letterSpacing'], '$path/letterSpacing')
          : null,
      wordSpacing: object.containsKey('wordSpacing')
          ? _finiteNumber(object['wordSpacing'], '$path/wordSpacing')
          : null,
      height: object.containsKey('height')
          ? _finiteNumber(object['height'], '$path/height', minimumExclusive: 0)
          : null,
      fontFamily: fontFamily,
      decoration: object.containsKey('decoration')
          ? _decodeThemeDecoration(object['decoration'], '$path/decoration')
          : null,
      decorationColor: object.containsKey('decorationColor')
          ? CanvasThemeColorValue.decode(
              object['decorationColor'],
              '$path/decorationColor',
            )
          : null,
      decorationStyle: object.containsKey('decorationStyle')
          ? _exactEnumText(
              object['decorationStyle'],
              '$path/decorationStyle',
              const {'solid', 'double', 'dotted', 'dashed', 'wavy'},
            )
          : null,
      decorationThickness: object.containsKey('decorationThickness')
          ? _finiteNumber(
              object['decorationThickness'],
              '$path/decorationThickness',
              minimum: 0,
            )
          : null,
    );
  }
}

Map<String, int> _decodeThemeColorScheme(Object? value) {
  final object = _object(value, r'$/profile/theme/colorScheme');
  _expect(
    object.length <= canvasColorSchemeRoles.length,
    'Canvas theme has too many ColorScheme overrides.',
  );
  final result = <String, int>{};
  for (final entry in object.entries) {
    _expect(
      canvasColorSchemeRoles.contains(entry.key),
      'Canvas theme has an unknown ColorScheme role: ${entry.key}',
    );
    final literal = _boundedText(
      entry.value,
      r'$/profile/theme/colorScheme/${entry.key}',
      10,
      10,
    );
    _expect(
      RegExp(r'^0x[0-9A-F]{8}$').hasMatch(literal),
      'Canvas ColorScheme override must use canonical 0xAARRGGBB.',
    );
    result[entry.key] = int.parse(literal.substring(2), radix: 16);
  }
  return result;
}

Map<String, CanvasThemeTextStyleOverride> _decodeThemeTextTheme(Object? value) {
  final object = _object(value, r'$/profile/theme/textTheme');
  _expect(
    object.length <= canvasTextThemeRoles.length,
    'Canvas theme has too many TextTheme overrides.',
  );
  final result = <String, CanvasThemeTextStyleOverride>{};
  for (final entry in object.entries) {
    _expect(
      canvasTextThemeRoles.contains(entry.key),
      'Canvas theme has an unknown TextTheme role: ${entry.key}',
    );
    result[entry.key] = CanvasThemeTextStyleOverride.decode(
      entry.value,
      r'$/profile/theme/textTheme/${entry.key}',
    );
  }
  return result;
}

Set<String> _decodeThemeDecoration(Object? value, String path) {
  _expect(
    value is List<Object?>,
    'Canvas theme decoration must be an array: $path',
  );
  final result = <String>{};
  for (final item in value! as List<Object?>) {
    final line = _exactEnumText(item, '$path/${result.length}', const {
      'underline',
      'overline',
      'lineThrough',
    });
    _expect(
      result.add(line),
      'Canvas theme decoration contains duplicates: $path',
    );
  }
  _expect(
    result.length <= 3,
    'Canvas theme decoration has too many lines: $path',
  );
  return Set.unmodifiable(result);
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
    for (final requiredProperty in specification.requiredProperties) {
      _expect(
        properties.containsKey(requiredProperty),
        'Canvas node is missing required property '
        '$type.$requiredProperty: $path/properties/$requiredProperty',
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
          _boundedPropertyText(
            object['value'],
            '$path/value',
            spec.minimumStringLength,
            spec.maximumStringLength,
          ),
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
            spec.minimumExclusive
                ? number > spec.minimum!
                : number >= spec.minimum!,
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
        final left = _finiteNumber(object['left'], '$path/left');
        final top = _finiteNumber(object['top'], '$path/top');
        final right = _finiteNumber(object['right'], '$path/right');
        final bottom = _finiteNumber(object['bottom'], '$path/bottom');
        if (spec.minimum != null) {
          for (final side in {
            'left': left,
            'top': top,
            'right': right,
            'bottom': bottom,
          }.entries) {
            _expect(
              spec.minimumExclusive
                  ? side.value > spec.minimum!
                  : side.value >= spec.minimum!,
              'Canvas edge inset is below its minimum: $path/${side.key}',
            );
          }
        }
        return CanvasValue(
          kind as String,
          CanvasEdgeInsets(left, top, right, bottom),
        );
      case 'themeToken':
        _exactKeys(object, path, const {'kind', 'token'});
        final token = _themeToken(object['token'], '$path/token');
        _expect(
          spec.themeTokens.contains(token),
          'Canvas theme token is not allowed at $path.',
        );
        return CanvasValue(kind as String, CanvasThemeToken(token));
      case 'paint':
        return CanvasValue(kind as String, _decodePaint(object, path));
      case 'shadowList':
        return CanvasValue(kind as String, _decodeShadows(object, path));
      case 'fontFeatureList':
        return CanvasValue(kind as String, _decodeFontFeatures(object, path));
      case 'fontVariationList':
        return CanvasValue(kind as String, _decodeFontVariations(object, path));
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

class CanvasThemeToken {
  const CanvasThemeToken(this.wireId);
  final String wireId;

  String get role => wireId.substring(wireId.lastIndexOf('.') + 1);
}

sealed class CanvasColorSource {
  const CanvasColorSource();
}

class CanvasLiteralColor extends CanvasColorSource {
  const CanvasLiteralColor(this.argb);
  final int argb;
}

class CanvasThemeColor extends CanvasColorSource {
  const CanvasThemeColor(this.token);
  final CanvasThemeToken token;
}

class CanvasPaint {
  const CanvasPaint({
    required this.color,
    required this.blendMode,
    required this.style,
    required this.strokeWidth,
    required this.strokeCap,
    required this.strokeJoin,
    required this.strokeMiterLimit,
    required this.antiAlias,
    required this.filterQuality,
    required this.invertColors,
    required this.maskFilter,
  });

  final CanvasColorSource color;
  final String blendMode;
  final String style;
  final double strokeWidth;
  final String strokeCap;
  final String strokeJoin;
  final double strokeMiterLimit;
  final bool antiAlias;
  final String filterQuality;
  final bool invertColors;
  final CanvasBlurMask? maskFilter;
}

class CanvasBlurMask {
  const CanvasBlurMask(this.style, this.sigma);
  final String style;
  final double sigma;
}

class CanvasShadowValue {
  const CanvasShadowValue({
    required this.id,
    required this.color,
    required this.offsetX,
    required this.offsetY,
    required this.blurRadius,
  });
  final String id;
  final CanvasColorSource color;
  final double offsetX;
  final double offsetY;
  final double blurRadius;
}

class CanvasFontFeatureValue {
  const CanvasFontFeatureValue(this.id, this.tag, this.value);
  final String id;
  final String tag;
  final int value;
}

class CanvasFontVariationValue {
  const CanvasFontVariationValue(this.id, this.axis, this.value);
  final String id;
  final String axis;
  final double value;
}

CanvasPaint _decodePaint(Map<String, Object?> object, String path) {
  const required = {
    'kind',
    'color',
    'blendMode',
    'style',
    'strokeWidth',
    'strokeCap',
    'strokeJoin',
    'strokeMiterLimit',
    'antiAlias',
    'filterQuality',
    'invertColors',
  };
  _exactKeys(
    object,
    path,
    object.containsKey('maskFilter') ? {...required, 'maskFilter'} : required,
  );
  final strokeWidth = _finiteNumber(
    object['strokeWidth'],
    '$path/strokeWidth',
    minimum: 0,
  );
  final strokeMiterLimit = _finiteNumber(
    object['strokeMiterLimit'],
    '$path/strokeMiterLimit',
    minimum: 0,
  );
  final antiAlias = object['antiAlias'];
  final invertColors = object['invertColors'];
  _expect(antiAlias is bool, 'Canvas Paint antiAlias is invalid: $path');
  _expect(invertColors is bool, 'Canvas Paint invertColors is invalid: $path');
  CanvasBlurMask? mask;
  if (object.containsKey('maskFilter')) {
    final rawMask = _object(object['maskFilter'], '$path/maskFilter');
    _exactKeys(rawMask, '$path/maskFilter', const {'style', 'sigma'});
    mask = CanvasBlurMask(
      _exactEnumText(rawMask['style'], '$path/maskFilter/style', const {
        'normal',
        'solid',
        'outer',
        'inner',
      }),
      _finiteNumber(
        rawMask['sigma'],
        '$path/maskFilter/sigma',
        minimumExclusive: 0,
      ),
    );
  }
  return CanvasPaint(
    color: _decodeColorSource(object['color'], '$path/color'),
    blendMode: _exactEnumText(
      object['blendMode'],
      '$path/blendMode',
      _blendModes,
    ),
    style: _exactEnumText(object['style'], '$path/style', const {
      'fill',
      'stroke',
    }),
    strokeWidth: strokeWidth,
    strokeCap: _exactEnumText(object['strokeCap'], '$path/strokeCap', const {
      'butt',
      'round',
      'square',
    }),
    strokeJoin: _exactEnumText(object['strokeJoin'], '$path/strokeJoin', const {
      'miter',
      'round',
      'bevel',
    }),
    strokeMiterLimit: strokeMiterLimit,
    antiAlias: antiAlias as bool,
    filterQuality: _exactEnumText(
      object['filterQuality'],
      '$path/filterQuality',
      const {'none', 'low', 'medium', 'high'},
    ),
    invertColors: invertColors as bool,
    maskFilter: mask,
  );
}

List<CanvasShadowValue> _decodeShadows(
  Map<String, Object?> object,
  String path,
) {
  _exactKeys(object, path, const {'kind', 'items'});
  final rawItems = _boundedItems(object['items'], '$path/items');
  final ids = <String>{};
  return List.unmodifiable([
    for (var index = 0; index < rawItems.length; index++)
      _decodeShadow(rawItems[index], '$path/items/$index', ids),
  ]);
}

CanvasShadowValue _decodeShadow(Object? value, String path, Set<String> ids) {
  final object = _object(value, path);
  _exactKeys(object, path, const {
    'id',
    'color',
    'offsetX',
    'offsetY',
    'blurRadius',
  });
  final id = _stableId(object['id'], '$path/id');
  _expect(ids.add(id), 'Canvas Shadow ids must be unique: $path/id');
  return CanvasShadowValue(
    id: id,
    color: _decodeColorSource(object['color'], '$path/color'),
    offsetX: _finiteNumber(object['offsetX'], '$path/offsetX'),
    offsetY: _finiteNumber(object['offsetY'], '$path/offsetY'),
    blurRadius: _finiteNumber(
      object['blurRadius'],
      '$path/blurRadius',
      minimum: 0,
    ),
  );
}

List<CanvasFontFeatureValue> _decodeFontFeatures(
  Map<String, Object?> object,
  String path,
) {
  _exactKeys(object, path, const {'kind', 'items'});
  final rawItems = _boundedItems(object['items'], '$path/items');
  final ids = <String>{};
  final tags = <String>{};
  return List.unmodifiable([
    for (var index = 0; index < rawItems.length; index++)
      _decodeFontFeature(rawItems[index], '$path/items/$index', ids, tags),
  ]);
}

CanvasFontFeatureValue _decodeFontFeature(
  Object? value,
  String path,
  Set<String> ids,
  Set<String> tags,
) {
  final object = _object(value, path);
  _exactKeys(object, path, const {'id', 'tag', 'value'});
  final id = _stableId(object['id'], '$path/id');
  _expect(ids.add(id), 'Canvas FontFeature ids must be unique: $path/id');
  final tag = _openTypeTag(object['tag'], '$path/tag');
  _expect(tags.add(tag), 'Canvas FontFeature tags must be unique: $path/tag');
  final featureValue = object['value'];
  _expect(
    featureValue is int && featureValue >= 0 && featureValue <= 0x7fffffff,
    'Canvas FontFeature value is invalid: $path/value',
  );
  return CanvasFontFeatureValue(id, tag, featureValue as int);
}

List<CanvasFontVariationValue> _decodeFontVariations(
  Map<String, Object?> object,
  String path,
) {
  _exactKeys(object, path, const {'kind', 'items'});
  final rawItems = _boundedItems(object['items'], '$path/items');
  final ids = <String>{};
  final axes = <String>{};
  return List.unmodifiable([
    for (var index = 0; index < rawItems.length; index++)
      _decodeFontVariation(rawItems[index], '$path/items/$index', ids, axes),
  ]);
}

CanvasFontVariationValue _decodeFontVariation(
  Object? value,
  String path,
  Set<String> ids,
  Set<String> axes,
) {
  final object = _object(value, path);
  _exactKeys(object, path, const {'id', 'axis', 'value'});
  final id = _stableId(object['id'], '$path/id');
  _expect(ids.add(id), 'Canvas FontVariation ids must be unique: $path/id');
  final axis = _openTypeTag(object['axis'], '$path/axis');
  _expect(
    axes.add(axis),
    'Canvas FontVariation axes must be unique: $path/axis',
  );
  final variationValue = _finiteNumber(
    object['value'],
    '$path/value',
    minimum: -32768,
  );
  _expect(
    variationValue < 32768,
    'Canvas FontVariation value is outside the 16.16 range: $path/value',
  );
  _validateRegisteredVariationAxis(axis, variationValue, '$path/value');
  return CanvasFontVariationValue(id, axis, variationValue);
}

CanvasColorSource _decodeColorSource(Object? value, String path) {
  final object = _object(value, path);
  final kind = object['kind'];
  if (kind == 'literal') {
    _exactKeys(object, path, const {'kind', 'argb'});
    final argb = _boundedText(object['argb'], '$path/argb', 10, 10);
    _expect(
      RegExp(r'^0x[0-9A-F]{8}$').hasMatch(argb),
      'Canvas nested ARGB color is invalid: $path/argb',
    );
    return CanvasLiteralColor(int.parse(argb.substring(2), radix: 16));
  }
  _expect(kind == 'theme', 'Canvas color source kind is invalid: $path/kind');
  _exactKeys(object, path, const {'kind', 'token'});
  final token = _themeToken(object['token'], '$path/token');
  _expect(
    canvasColorSchemeThemeTokens.contains(token),
    'Canvas color source theme token is not reviewed: $path/token',
  );
  return CanvasThemeColor(CanvasThemeToken(token));
}

List<Object?> _boundedItems(Object? value, String path) {
  _expect(
    value is List<Object?>,
    'Canvas structured items must be an array: $path',
  );
  final items = value! as List<Object?>;
  _expect(
    items.length <= 256,
    'Canvas structured list exceeds 256 items: $path',
  );
  return items;
}

String _openTypeTag(Object? value, String path) {
  final tag = _boundedText(value, path, 4, 4);
  _expect(
    tag.codeUnits.every((unit) => unit >= 0x20 && unit <= 0x7e),
    'Canvas OpenType tag must contain four printable ASCII characters: $path',
  );
  return tag;
}

String _themeToken(Object? value, String path) {
  final token = _boundedText(value, path, 1, 128);
  _expect(
    RegExp(
      r'^material\.(?:colorScheme|textTheme)\.[a-z][A-Za-z0-9]*$',
    ).hasMatch(token),
    'Canvas Material theme token is invalid: $path',
  );
  return token;
}

void _validateRegisteredVariationAxis(String axis, double value, String path) {
  final valid = switch (axis) {
    'ital' => value >= 0 && value <= 1,
    'opsz' => value > 0 && value < 32768,
    'slnt' => value > -90 && value < 90,
    'wdth' => value >= 0 && value < 32768,
    'wght' => value >= 1 && value <= 1000,
    _ => true,
  };
  _expect(valid, 'Canvas registered font axis value is invalid: $path');
}

const _blendModes = <String>{
  'clear',
  'src',
  'dst',
  'srcOver',
  'dstOver',
  'srcIn',
  'dstIn',
  'srcOut',
  'dstOut',
  'srcATop',
  'dstATop',
  'xor',
  'plus',
  'modulate',
  'screen',
  'overlay',
  'darken',
  'lighten',
  'colorDodge',
  'colorBurn',
  'hardLight',
  'softLight',
  'difference',
  'exclusion',
  'multiply',
  'hue',
  'saturation',
  'color',
  'luminosity',
};

class _WidgetSpec {
  const _WidgetSpec(
    this.properties,
    this.slots, {
    this.requiredProperties = const {},
  });
  final Map<String, _PropertySpec> properties;
  final Map<String, String> slots;
  final Set<String> requiredProperties;
}

class _PropertySpec {
  const _PropertySpec(
    this.kinds, {
    this.enumType,
    this.enumValues = const {},
    this.minimum,
    this.minimumExclusive = false,
    this.minimumStringLength = 0,
    this.maximumStringLength = 65536,
    this.themeTokens = const {},
  });
  final Set<String> kinds;
  final String? enumType;
  final Set<String> enumValues;
  final num? minimum;
  final bool minimumExclusive;
  final int minimumStringLength;
  final int maximumStringLength;
  final Set<String> themeTokens;
}

const _fontWeightProperty = _PropertySpec(
  {'enum'},
  enumType: 'FontWeight',
  enumValues: {
    'w100',
    'w200',
    'w300',
    'w400',
    'w500',
    'w600',
    'w700',
    'w800',
    'w900',
  },
);
const _fontStyleProperty = _PropertySpec(
  {'enum'},
  enumType: 'FontStyle',
  enumValues: {'normal', 'italic'},
);
const _textBaselineProperty = _PropertySpec(
  {'enum'},
  enumType: 'TextBaseline',
  enumValues: {'alphabetic', 'ideographic'},
);
const _textLeadingDistributionProperty = _PropertySpec(
  {'enum'},
  enumType: 'TextLeadingDistribution',
  enumValues: {'proportional', 'even'},
);
const _textOverflowProperty = _PropertySpec(
  {'enum'},
  enumType: 'TextOverflow',
  enumValues: {'clip', 'fade', 'ellipsis', 'visible'},
);
const _fontNameProperty = _PropertySpec(
  {'string'},
  minimumStringLength: 1,
  maximumStringLength: 256,
);
const _fontFamilyFallbackProperty = _PropertySpec({
  'string',
}, maximumStringLength: 4096);

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
      'padding': _PropertySpec({'edgeInsets'}, minimum: 0),
    },
    {'child': 'single'},
    requiredProperties: {'padding'},
  ),
  'flutter.widgets.Center': _WidgetSpec(
    {
      'widthFactor': _PropertySpec({'integer', 'double'}, minimum: 0),
      'heightFactor': _PropertySpec({'integer', 'double'}, minimum: 0),
    },
    {'child': 'single'},
  ),
  'flutter.widgets.Text': _WidgetSpec(
    {
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
      'overflow': _textOverflowProperty,
      'semanticsLabel': _PropertySpec({'string'}),
      'semanticsIdentifier': _PropertySpec({'string'}),
      'textWidthBasis': _PropertySpec(
        {'enum'},
        enumType: 'TextWidthBasis',
        enumValues: {'parent', 'longestLine'},
      ),
      'selectionColor': _PropertySpec({
        'color',
        'themeToken',
      }, themeTokens: canvasColorSchemeThemeTokens),
      'localeLanguageCode': _PropertySpec({'string'}),
      'localeScriptCode': _PropertySpec({'string'}),
      'localeCountryCode': _PropertySpec({'string'}),
      'textScalerFactor': _PropertySpec({'double'}, minimum: 0),
      'textHeightApplyFirstAscent': _PropertySpec({'boolean'}),
      'textHeightApplyLastDescent': _PropertySpec({'boolean'}),
      'textHeightLeadingDistribution': _textLeadingDistributionProperty,
      'styleInherit': _PropertySpec({'boolean'}),
      'styleThemeTextStyle': _PropertySpec({
        'themeToken',
      }, themeTokens: canvasTextThemeTokens),
      'styleColor': _PropertySpec({
        'color',
        'themeToken',
      }, themeTokens: canvasColorSchemeThemeTokens),
      'styleBackgroundColor': _PropertySpec({
        'color',
        'themeToken',
      }, themeTokens: canvasColorSchemeThemeTokens),
      'styleFontSize': _PropertySpec({'double'}, minimum: 0),
      'styleFontWeight': _fontWeightProperty,
      'styleFontStyle': _fontStyleProperty,
      'styleLetterSpacing': _PropertySpec({'double'}),
      'styleWordSpacing': _PropertySpec({'double'}),
      'styleTextBaseline': _textBaselineProperty,
      'styleHeight': _PropertySpec({'double'}),
      'styleLeadingDistribution': _textLeadingDistributionProperty,
      'styleLocaleLanguageCode': _PropertySpec({'string'}),
      'styleLocaleScriptCode': _PropertySpec({'string'}),
      'styleLocaleCountryCode': _PropertySpec({'string'}),
      'styleDecorationUnderline': _PropertySpec({'boolean'}),
      'styleDecorationOverline': _PropertySpec({'boolean'}),
      'styleDecorationLineThrough': _PropertySpec({'boolean'}),
      'styleForeground': _PropertySpec({'paint'}),
      'styleBackground': _PropertySpec({'paint'}),
      'styleShadows': _PropertySpec({'shadowList'}),
      'styleFontFeatures': _PropertySpec({'fontFeatureList'}),
      'styleFontVariations': _PropertySpec({'fontVariationList'}),
      'styleDecorationColor': _PropertySpec({
        'color',
        'themeToken',
      }, themeTokens: canvasColorSchemeThemeTokens),
      'styleDecorationStyle': _PropertySpec(
        {'enum'},
        enumType: 'TextDecorationStyle',
        enumValues: {'solid', 'double', 'dotted', 'dashed', 'wavy'},
      ),
      'styleDecorationThickness': _PropertySpec({'double'}),
      'styleDebugLabel': _PropertySpec({'string'}),
      'styleFontFamily': _fontNameProperty,
      'styleFontFamilyFallback': _fontFamilyFallbackProperty,
      'stylePackage': _fontNameProperty,
      'styleOverflow': _textOverflowProperty,
      'strutFontFamily': _fontNameProperty,
      'strutFontFamilyFallback': _fontFamilyFallbackProperty,
      'strutFontSize': _PropertySpec(
        {'double'},
        minimum: 0,
        minimumExclusive: true,
      ),
      'strutHeight': _PropertySpec({'double'}),
      'strutLeadingDistribution': _textLeadingDistributionProperty,
      'strutLeading': _PropertySpec({'double'}, minimum: 0),
      'strutFontWeight': _fontWeightProperty,
      'strutFontStyle': _fontStyleProperty,
      'strutForceHeight': _PropertySpec({'boolean'}),
      'strutDebugLabel': _PropertySpec({'string'}),
      'strutPackage': _fontNameProperty,
    },
    {},
    requiredProperties: {'data'},
  ),
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

  _validateLocaleSubtag(
    properties,
    'localeLanguageCode',
    RegExp(r'^(?:[a-z]{2,3}|[a-z]{5,8})$'),
    path,
  );
  _validateLocaleSubtag(
    properties,
    'localeScriptCode',
    RegExp(r'^[A-Z][a-z]{3}$'),
    path,
  );
  _validateLocaleSubtag(
    properties,
    'localeCountryCode',
    RegExp(r'^(?:[A-Z]{2}|[0-9]{3})$'),
    path,
  );
  _validateLocaleSubtag(
    properties,
    'styleLocaleLanguageCode',
    RegExp(r'^(?:[a-z]{2,3}|[a-z]{5,8})$'),
    path,
  );
  _validateLocaleSubtag(
    properties,
    'styleLocaleScriptCode',
    RegExp(r'^[A-Z][a-z]{3}$'),
    path,
  );
  _validateLocaleSubtag(
    properties,
    'styleLocaleCountryCode',
    RegExp(r'^(?:[A-Z]{2}|[0-9]{3})$'),
    path,
  );

  _validateFontPackageRelationship(
    properties,
    packageName: 'stylePackage',
    fontFamilyName: 'styleFontFamily',
    fallbackName: 'styleFontFamilyFallback',
    path: path,
  );
  _expect(
    !(properties.containsKey('styleColor') &&
        properties.containsKey('styleForeground')),
    'Canvas Text styleColor and styleForeground are mutually exclusive: '
    '$path/properties/styleForeground',
  );
  _expect(
    !(properties.containsKey('styleBackgroundColor') &&
        properties.containsKey('styleBackground')),
    'Canvas Text styleBackgroundColor and styleBackground are mutually exclusive: '
    '$path/properties/styleBackground',
  );
  _validateFontPackageRelationship(
    properties,
    packageName: 'strutPackage',
    fontFamilyName: 'strutFontFamily',
    fallbackName: 'strutFontFamilyFallback',
    path: path,
  );

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

void _validateLocaleSubtag(
  Map<String, CanvasValue> properties,
  String name,
  RegExp pattern,
  String path,
) {
  final value = properties[name]?.value;
  if (value == null) {
    return;
  }
  _expect(
    value is String && pattern.hasMatch(value),
    'Canvas Text locale subtag is invalid: $path/properties/$name',
  );
}

void _validateFontPackageRelationship(
  Map<String, CanvasValue> properties, {
  required String packageName,
  required String fontFamilyName,
  required String fallbackName,
  required String path,
}) {
  final package = properties[packageName]?.value;
  if (package == null) {
    return;
  }
  _expect(
    package is String && package.trim().isNotEmpty,
    'Canvas Text font package must be non-empty: '
    '$path/properties/$packageName',
  );
  _expect(
    _hasNonEmptyString(properties, fontFamilyName) ||
        _hasNonEmptyLine(properties, fallbackName),
    'Canvas Text $packageName requires $fontFamilyName or $fallbackName: '
    '$path/properties/$packageName',
  );
}

bool _hasNonEmptyString(Map<String, CanvasValue> properties, String name) {
  final value = properties[name]?.value;
  return value is String && value.trim().isNotEmpty;
}

bool _hasNonEmptyLine(Map<String, CanvasValue> properties, String name) {
  final value = properties[name]?.value;
  return value is String &&
      value.split(RegExp(r'\r\n?|\n')).any((line) => line.trim().isNotEmpty);
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

void _allowedKeys(
  Map<String, Object?> object,
  String path,
  Set<String> allowed,
) {
  _expect(
    object.keys.every(allowed.contains),
    'Canvas object has unknown fields: $path',
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

String _exactEnumText(Object? value, String path, Set<String> allowed) {
  final text = _boundedText(value, path, 1, 32);
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

String _boundedPropertyText(
  Object? value,
  String path,
  int minimum,
  int maximum,
) {
  _expect(value is String, 'Canvas value must be a string: $path');
  final text = value! as String;
  _expect(
    text.runes.length >= minimum && text.runes.length <= maximum,
    'Canvas string length is outside its bounds: $path',
  );
  return text;
}

double _finiteNumber(
  Object? value,
  String path, {
  num? minimum,
  num? minimumExclusive,
  num? maximum,
}) {
  _expect(
    minimum == null || minimumExclusive == null,
    'Canvas number decoder has conflicting minimum bounds: $path',
  );
  _expect(value is num, 'Canvas value must be a number: $path');
  final number = (value! as num).toDouble();
  _expect(number.isFinite, 'Canvas number must be finite: $path');
  if (minimum != null) {
    _expect(number >= minimum, 'Canvas number is below its minimum: $path');
  }
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
