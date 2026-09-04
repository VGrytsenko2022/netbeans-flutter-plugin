import 'dart:convert';
import 'dart:typed_data';

import 'material_icon_registry.dart';

const canvasModelFormat = 'netbeans-flutter-canvas-model';
const canvasModelProtocolVersion = 14;
const maxCanvasSequence = 9007199254740991;
const _maxCanvasIconCodePoint = 0x10ffff;
const _canvasIconSurrogateStart = 0xd800;
const _canvasIconSurrogateEnd = 0xdfff;
const _maxCanvasIconMetadataLength = 256;
const _maxCanvasIconFontFamilyFallbacks = 32;
const _canvasIconDataConstraintFingerprint =
    'iconData:0:1114111:55296:57343:256:32:1:1:'
    '061C,200E,200F,2028-202E,2066-2069,FEFF';
const _canvasMaterialIconConstraintFingerprint =
    'materialIcons:3.44.8:058e0af2c2:8825:'
    'ba88e3e23962ada6537523aa113811d9719b988412815bf084f50a0aa78137f0';

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

const canvasThemeComponentColorRoles = <String>{
  'scaffold.backgroundColor',
  'appBar.backgroundColor',
  'appBar.foregroundColor',
  'appBar.shadowColor',
  'appBar.surfaceTintColor',
  'icon.color',
  'elevatedButton.backgroundColor.default',
  'elevatedButton.backgroundColor.disabled',
  'elevatedButton.backgroundColor.pressed',
  'elevatedButton.backgroundColor.hovered',
  'elevatedButton.backgroundColor.focused',
  'elevatedButton.foregroundColor.default',
  'elevatedButton.foregroundColor.disabled',
  'elevatedButton.foregroundColor.pressed',
  'elevatedButton.foregroundColor.hovered',
  'elevatedButton.foregroundColor.focused',
  'elevatedButton.overlayColor.default',
  'elevatedButton.overlayColor.disabled',
  'elevatedButton.overlayColor.pressed',
  'elevatedButton.overlayColor.hovered',
  'elevatedButton.overlayColor.focused',
  'elevatedButton.shadowColor.default',
  'elevatedButton.shadowColor.disabled',
  'elevatedButton.shadowColor.pressed',
  'elevatedButton.shadowColor.hovered',
  'elevatedButton.shadowColor.focused',
  'elevatedButton.surfaceTintColor.default',
  'elevatedButton.surfaceTintColor.disabled',
  'elevatedButton.surfaceTintColor.pressed',
  'elevatedButton.surfaceTintColor.hovered',
  'elevatedButton.surfaceTintColor.focused',
  'elevatedButton.iconColor.default',
  'elevatedButton.iconColor.disabled',
  'elevatedButton.iconColor.pressed',
  'elevatedButton.iconColor.hovered',
  'elevatedButton.iconColor.focused',
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
    required this.imageResourceIds,
  });

  final String sessionId;
  final int presentationSequence;
  final String documentId;
  final int logicalRevisionId;
  final CanvasProfile profile;
  final CanvasNode root;
  final Set<String> widgetIds;
  final Set<String> imageResourceIds;

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
    _expect(
      !_isFlexRestrictedWidgetType(root.type),
      'Canvas ${root.type} must be a direct child of Row.children or '
      'Column.children: \$/root',
    );
    return CanvasModel(
      sessionId: sessionId,
      presentationSequence: presentationSequence,
      documentId: documentId,
      logicalRevisionId: logicalRevisionId,
      profile: profile,
      root: root,
      widgetIds: Set.unmodifiable(budget.ids),
      imageResourceIds: Set.unmodifiable(budget.imageResourceIds),
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
    this.components = const <String, CanvasThemeColorValue>{},
  });

  final String definitionId;
  final int seedArgb;
  final String brightness;
  final String digestIdentity;
  final Map<String, int> colorScheme;
  final Map<String, CanvasThemeTextStyleOverride> textTheme;
  final Map<String, CanvasThemeColorValue> components;

  static CanvasTheme decode(Object? value) {
    final object = _object(value, r'$/profile/theme');
    _exactKeys(object, r'$/profile/theme', const {
      'definitionId',
      'seedArgb',
      'brightness',
      'digestIdentity',
      'colorScheme',
      'textTheme',
      'components',
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
    final components = _decodeThemeComponentColors(object['components']);
    return CanvasTheme(
      definitionId: definitionId,
      seedArgb: int.parse(seedLiteral.substring(2), radix: 16),
      brightness: brightness,
      digestIdentity: digestIdentity,
      colorScheme: Map.unmodifiable(colorScheme),
      textTheme: Map.unmodifiable(textTheme),
      components: Map.unmodifiable(components),
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

Map<String, CanvasThemeColorValue> _decodeThemeComponentColors(Object? value) {
  final object = _object(value, r'$/profile/theme/components');
  _expect(
    object.length <= canvasThemeComponentColorRoles.length,
    'Canvas theme has too many component color overrides.',
  );
  final result = <String, CanvasThemeColorValue>{};
  for (final entry in object.entries) {
    _expect(
      canvasThemeComponentColorRoles.contains(entry.key),
      'Canvas theme has an unknown component color role: ${entry.key}',
    );
    result[entry.key] = CanvasThemeColorValue.decode(
      entry.value,
      r'$/profile/theme/components/${entry.key}',
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
    for (final entry in specification.properties.entries) {
      if (entry.value.required) {
        _expect(
          properties.containsKey(entry.key),
          'Canvas node is missing required property '
          '$type.${entry.key}: $path/properties/${entry.key}',
        );
      }
    }
    _validatePropertyRelationships(type, properties, path, budget);

    final rawSlots = _object(object['slots'], '$path/slots');
    _expect(
      rawSlots.length <= specification.slots.length,
      'Canvas node has too many slots: $path',
    );
    final slots = <String, CanvasSlot>{};
    for (final entry in rawSlots.entries) {
      final slotSpec = specification.slots[entry.key];
      _expect(slotSpec != null, 'Unsupported slot $type.${entry.key}.');
      final reviewedSlotSpec = slotSpec!;
      slots[entry.key] = CanvasSlot._decode(
        entry.value,
        reviewedSlotSpec,
        budget,
        depth + 1,
        '$path/slots/${entry.key}',
      );
      for (final child in slots[entry.key]!.children) {
        _expect(
          reviewedSlotSpec.acceptance.accepts(
            child.type,
            _widgetSpecifications[child.type]!.traits,
          ),
          'Canvas slot rejects child type ${child.type}: '
          '$path/slots/${entry.key}',
        );
        _expect(
          _placementAccepts(type, entry.key, child.type),
          'Canvas ${child.type} must be a direct child of Row.children or '
          'Column.children: $path/slots/${entry.key}',
        );
      }
    }
    for (final entry in specification.slots.entries) {
      if (entry.value.required || entry.value.minimumChildren > 0) {
        _expect(
          slots.containsKey(entry.key),
          'Canvas node is missing required slot '
          '$type.${entry.key}: $path/slots/${entry.key}',
        );
      }
    }
    _validateNodeSlotRelationships(type, properties, slots, path);
    return CanvasNode(
      id: id,
      type: type,
      properties: Map.unmodifiable(properties),
      slots: Map.unmodifiable(slots),
    );
  }
}

void _validateNodeSlotRelationships(
  String type,
  Map<String, CanvasValue> properties,
  Map<String, CanvasSlot> slots,
  String path,
) {
  if (type != 'flutter.widgets.ListView') {
    return;
  }
  final semanticChildCount = properties['semanticChildCount']?.value as int?;
  final childCount = slots['children']?.children.length ?? 0;
  _expect(
    semanticChildCount == null || semanticChildCount <= childCount,
    'Canvas ListView semanticChildCount cannot exceed children.length: '
    '$path/properties/semanticChildCount',
  );
}

class CanvasSlot {
  const CanvasSlot(this.kind, this.children);

  final String kind;
  final List<CanvasNode> children;

  CanvasNode? get child => children.isEmpty ? null : children.first;

  static CanvasSlot _decode(
    Object? value,
    _SlotSpec spec,
    _NodeBudget budget,
    int depth,
    String path,
  ) {
    final object = _object(value, path);
    if (spec.cardinality == 'single') {
      _exactKeys(object, path, const {'kind', 'child'});
      _expect(
        object['kind'] == 'single',
        'Canvas single slot kind is invalid: $path',
      );
      final child = object['child'];
      final result = CanvasSlot(
        'single',
        child == null
            ? const []
            : [CanvasNode._decode(child, budget, depth, '$path/child')],
      );
      _validateChildCount(result.children.length, spec, path);
      return result;
    }
    _expect(
      spec.cardinality == 'list',
      'Canvas slot schema cardinality is invalid: $path',
    );
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
    _validateChildCount(children.length, spec, path);
    final result = CanvasSlot(
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
    return result;
  }

  static void _validateChildCount(int count, _SlotSpec spec, String path) {
    _expect(
      count >= spec.minimumChildren && count <= spec.maximumChildren,
      'Canvas slot child count is outside its bounds: $path',
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
      spec.kinds.contains(kind) ||
          (kind == 'callbackPresence' && spec.kinds.contains('callback')),
      'Canvas property kind is not allowed at $path.',
    );
    switch (kind) {
      case 'string':
        _exactKeys(object, path, const {'kind', 'value'});
        final text = _boundedPropertyText(
          object['value'],
          '$path/value',
          spec.minimumStringLength,
          spec.maximumStringLength,
        );
        if (spec.stringPattern case final pattern?) {
          _expect(
            RegExp('^(?:$pattern)\$').hasMatch(text),
            'Canvas string value does not match its pattern: $path',
          );
        }
        return CanvasValue(kind as String, text);
      case 'boolean':
        _exactKeys(object, path, const {'kind', 'value'});
        _expect(
          object['value'] is bool,
          'Canvas boolean value is invalid: $path',
        );
        return CanvasValue(kind as String, object['value']! as bool);
      case 'callbackPresence':
        _exactKeys(object, path, const {'kind'});
        return CanvasValue(kind as String, true);
      case 'integer':
        _exactKeys(object, path, const {'kind', 'value'});
        final integer = object['value'];
        _expect(integer is int, 'Canvas integer value is invalid: $path');
        _expect(
          (integer as int).abs() <= maxCanvasSequence,
          'Canvas integer value exceeds the interoperable range: $path',
        );
        _validateNumericBounds(integer as num, spec.numericBounds[kind], path);
        return CanvasValue(kind as String, integer);
      case 'double':
        _exactKeys(object, path, const {'kind', 'value'});
        final number = _finiteNumber(object['value'], '$path/value');
        _validateNumericBounds(number, spec.numericBounds[kind], path);
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
        final bounds = _requiredNumericBounds(spec, kind as String, path);
        for (final side in {
          'left': left,
          'top': top,
          'right': right,
          'bottom': bottom,
        }.entries) {
          _expect(
            _withinNumericBounds(side.value, bounds),
            'Canvas edge inset is outside its bounds: $path/${side.key}',
          );
        }
        return CanvasValue(kind, CanvasEdgeInsets(left, top, right, bottom));
      case 'edgeInsetsDirectional':
        _exactKeys(object, path, const {
          'kind',
          'start',
          'top',
          'end',
          'bottom',
        });
        final start = _finiteNumber(object['start'], '$path/start');
        final top = _finiteNumber(object['top'], '$path/top');
        final end = _finiteNumber(object['end'], '$path/end');
        final bottom = _finiteNumber(object['bottom'], '$path/bottom');
        final bounds = _requiredNumericBounds(spec, kind as String, path);
        for (final side in {
          'start': start,
          'top': top,
          'end': end,
          'bottom': bottom,
        }.entries) {
          _expect(
            _withinNumericBounds(side.value, bounds),
            'Canvas directional edge inset is outside its bounds: '
            '$path/${side.key}',
          );
        }
        return CanvasValue(
          kind,
          CanvasEdgeInsetsDirectional(start, top, end, bottom),
        );
      case 'iconData':
        final iconData = _decodeIconData(object, path);
        if (spec.materialIconsOnly) {
          _expect(
            iconData.codePoint == null ||
                (iconData.fontFamily == 'MaterialIcons' &&
                    iconData.fontPackage == null &&
                    iconData.fontFamilyFallback.isEmpty &&
                    isReviewedMaterialIconDataPair(
                      iconData.codePoint!,
                      iconData.matchTextDirection,
                    )),
            'Canvas built-in IconData must be None or use the reviewed '
            'MaterialIcons registry: $path',
          );
        }
        return CanvasValue(kind as String, iconData);
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
      case 'alignmentGeometry':
        return CanvasValue(
          kind as String,
          _decodeAlignmentGeometry(object, path),
        );
      case 'offset':
        return CanvasValue(kind as String, _decodeOffset(object, path));
      case 'size':
        return CanvasValue(kind as String, _decodeSize(object, path));
      case 'boxConstraints':
        return CanvasValue(kind as String, _decodeBoxConstraints(object, path));
      case 'matrix4':
        return CanvasValue(kind as String, _decodeMatrix4(object, path));
      case 'imageProvider':
        _exactKeys(object, path, const {'kind', 'value'});
        return CanvasValue(
          kind as String,
          _decodeImageProvider(object['value'], '$path/value'),
        );
      case 'boxDecoration':
        return CanvasValue(
          kind as String,
          _decodeBoxDecoration(object, spec, path),
        );
      default:
        throw FormatException('Unsupported Canvas property kind at $path.');
    }
  }
}

void _validateNumericBounds(num value, _NumericBounds? bounds, String path) {
  _expect(bounds != null, 'Canvas numeric schema is incomplete: $path');
  _expect(
    _withinNumericBounds(value, bounds!),
    'Canvas numeric value is outside its bounds: $path',
  );
}

_NumericBounds _requiredNumericBounds(
  _PropertySpec spec,
  String kind,
  String path,
) {
  final bounds = spec.numericBounds[kind];
  _expect(bounds != null, 'Canvas numeric schema is incomplete: $path');
  return bounds!;
}

bool _withinNumericBounds(num value, _NumericBounds bounds) {
  final minimum = bounds.minimum;
  if (minimum != null &&
      (bounds.minimumInclusive ? value < minimum : value <= minimum)) {
    return false;
  }
  final maximum = bounds.maximum;
  if (maximum != null &&
      (bounds.maximumInclusive ? value > maximum : value >= maximum)) {
    return false;
  }
  return true;
}

class CanvasEnumValue {
  const CanvasEnumValue(this.type, this.value);
  final String type;
  final String value;
}

class CanvasIconDataValue {
  const CanvasIconDataValue({
    required this.codePoint,
    required this.fontFamily,
    required this.fontPackage,
    required this.matchTextDirection,
    required this.fontFamilyFallback,
  });

  final int? codePoint;
  final String? fontFamily;
  final String? fontPackage;
  final bool matchTextDirection;
  final List<String> fontFamilyFallback;
}

class CanvasEdgeInsets {
  const CanvasEdgeInsets(this.left, this.top, this.right, this.bottom);
  final double left;
  final double top;
  final double right;
  final double bottom;
}

class CanvasEdgeInsetsDirectional {
  const CanvasEdgeInsetsDirectional(
    this.start,
    this.top,
    this.end,
    this.bottom,
  );
  final double start;
  final double top;
  final double end;
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

class CanvasAlignmentGeometryValue {
  const CanvasAlignmentGeometryValue({
    required this.basis,
    required this.horizontal,
    required this.vertical,
  });

  final String basis;
  final double horizontal;
  final double vertical;
}

class CanvasOffsetValue {
  const CanvasOffsetValue({required this.dx, required this.dy});

  final double dx;
  final double dy;
}

class CanvasSizeValue {
  const CanvasSizeValue({required this.width, required this.height});

  final double width;
  final double height;
}

class CanvasBoxConstraintsValue {
  const CanvasBoxConstraintsValue({
    required this.minWidth,
    required this.maxWidth,
    required this.minHeight,
    required this.maxHeight,
  });

  final double? minWidth;
  final double? maxWidth;
  final double? minHeight;
  final double? maxHeight;
}

class CanvasMatrix4Value {
  const CanvasMatrix4Value(this.storage);

  final List<double> storage;
}

class CanvasImageProviderValue {
  const CanvasImageProviderValue({
    required this.providerKind,
    required this.assetName,
    required this.packageName,
    required this.exactScale,
    required this.resize,
    required this.resolution,
  });

  final String providerKind;
  final String assetName;
  final String? packageName;
  final double? exactScale;
  final CanvasResizeImageValue? resize;
  final CanvasImageResolutionValue resolution;
}

class CanvasResizeImageValue {
  const CanvasResizeImageValue({
    required this.width,
    required this.height,
    required this.policy,
    required this.allowUpscaling,
  });

  final int? width;
  final int? height;
  final String policy;
  final bool allowUpscaling;
}

sealed class CanvasImageResolutionValue {
  const CanvasImageResolutionValue();
}

class CanvasResolvedImageValue extends CanvasImageResolutionValue {
  const CanvasResolvedImageValue({
    required this.resourceId,
    required this.resolvedScale,
  });

  final String resourceId;
  final double resolvedScale;
}

class CanvasUnavailableImageValue extends CanvasImageResolutionValue {
  const CanvasUnavailableImageValue({required this.code, required this.reason});

  final String code;
  final String reason;
}

class CanvasDecorationImageValue {
  const CanvasDecorationImageValue({
    required this.image,
    required this.onError,
    required this.colorFilter,
    required this.fit,
    required this.alignment,
    required this.centerSlice,
    required this.repeat,
    required this.matchTextDirection,
    required this.scale,
    required this.opacity,
    required this.filterQuality,
    required this.invertColors,
    required this.isAntiAlias,
  });

  final CanvasImageProviderValue image;

  /// Presence only. The isolated runner never receives or executes a handler.
  final bool onError;
  final CanvasColorFilterValue? colorFilter;
  final String? fit;
  final CanvasAlignmentGeometryValue alignment;
  final CanvasRectValue? centerSlice;
  final String repeat;
  final bool matchTextDirection;
  final double scale;
  final double opacity;
  final String filterQuality;
  final bool invertColors;
  final bool isAntiAlias;
}

sealed class CanvasColorFilterValue {
  const CanvasColorFilterValue();
}

class CanvasModeColorFilterValue extends CanvasColorFilterValue {
  const CanvasModeColorFilterValue({
    required this.color,
    required this.blendMode,
  });

  final CanvasColorSource color;
  final String blendMode;
}

class CanvasMatrixColorFilterValue extends CanvasColorFilterValue {
  const CanvasMatrixColorFilterValue(this.values);

  final List<double> values;
}

class CanvasLinearToSrgbGammaColorFilterValue extends CanvasColorFilterValue {
  const CanvasLinearToSrgbGammaColorFilterValue();
}

class CanvasSrgbToLinearGammaColorFilterValue extends CanvasColorFilterValue {
  const CanvasSrgbToLinearGammaColorFilterValue();
}

class CanvasSaturationColorFilterValue extends CanvasColorFilterValue {
  const CanvasSaturationColorFilterValue(this.value);

  final double value;
}

class CanvasRectValue {
  const CanvasRectValue({
    required this.left,
    required this.top,
    required this.right,
    required this.bottom,
  });

  final double left;
  final double top;
  final double right;
  final double bottom;
}

class CanvasBoxDecorationValue {
  const CanvasBoxDecorationValue({
    required this.color,
    required this.image,
    required this.border,
    required this.borderRadius,
    required this.boxShadow,
    required this.gradient,
    required this.backgroundBlendMode,
    required this.shape,
  });

  final CanvasColorSource? color;
  final CanvasDecorationImageValue? image;
  final CanvasBoxBorderValue? border;
  final CanvasBorderRadiusGeometryValue? borderRadius;
  final List<CanvasBoxShadowValue> boxShadow;
  final CanvasBoxGradientValue? gradient;
  final String? backgroundBlendMode;
  final String shape;
}

sealed class CanvasBoxBorderValue {
  const CanvasBoxBorderValue();

  List<CanvasBorderSideValue> get sides;
}

class CanvasPhysicalBoxBorderValue extends CanvasBoxBorderValue {
  const CanvasPhysicalBoxBorderValue({
    required this.top,
    required this.right,
    required this.bottom,
    required this.left,
  });

  final CanvasBorderSideValue top;
  final CanvasBorderSideValue right;
  final CanvasBorderSideValue bottom;
  final CanvasBorderSideValue left;

  @override
  List<CanvasBorderSideValue> get sides => [top, right, bottom, left];
}

class CanvasDirectionalBoxBorderValue extends CanvasBoxBorderValue {
  const CanvasDirectionalBoxBorderValue({
    required this.top,
    required this.start,
    required this.end,
    required this.bottom,
  });

  final CanvasBorderSideValue top;
  final CanvasBorderSideValue start;
  final CanvasBorderSideValue end;
  final CanvasBorderSideValue bottom;

  @override
  List<CanvasBorderSideValue> get sides => [top, start, end, bottom];
}

class CanvasBorderSideValue {
  const CanvasBorderSideValue({
    required this.color,
    required this.width,
    required this.style,
    required this.strokeAlign,
  });

  final CanvasColorSource color;
  final double width;
  final String style;
  final double strokeAlign;
}

sealed class CanvasBorderRadiusGeometryValue {
  const CanvasBorderRadiusGeometryValue();

  List<CanvasRadiusValue> get radii;
}

class CanvasPhysicalBorderRadiusValue extends CanvasBorderRadiusGeometryValue {
  const CanvasPhysicalBorderRadiusValue({
    required this.topLeft,
    required this.topRight,
    required this.bottomRight,
    required this.bottomLeft,
  });

  final CanvasRadiusValue topLeft;
  final CanvasRadiusValue topRight;
  final CanvasRadiusValue bottomRight;
  final CanvasRadiusValue bottomLeft;

  @override
  List<CanvasRadiusValue> get radii => [
    topLeft,
    topRight,
    bottomRight,
    bottomLeft,
  ];
}

class CanvasDirectionalBorderRadiusValue
    extends CanvasBorderRadiusGeometryValue {
  const CanvasDirectionalBorderRadiusValue({
    required this.topStart,
    required this.topEnd,
    required this.bottomEnd,
    required this.bottomStart,
  });

  final CanvasRadiusValue topStart;
  final CanvasRadiusValue topEnd;
  final CanvasRadiusValue bottomEnd;
  final CanvasRadiusValue bottomStart;

  @override
  List<CanvasRadiusValue> get radii => [
    topStart,
    topEnd,
    bottomEnd,
    bottomStart,
  ];
}

class CanvasRadiusValue {
  const CanvasRadiusValue(this.x, this.y);

  final double x;
  final double y;
}

class CanvasBoxShadowValue {
  const CanvasBoxShadowValue({
    required this.id,
    required this.color,
    required this.offsetX,
    required this.offsetY,
    required this.blurRadius,
    required this.spreadRadius,
    required this.blurStyle,
  });

  final String id;
  final CanvasColorSource color;
  final double offsetX;
  final double offsetY;
  final double blurRadius;
  final double spreadRadius;
  final String blurStyle;
}

sealed class CanvasBoxGradientValue {
  const CanvasBoxGradientValue({
    required this.stops,
    required this.tileMode,
    required this.rotationRadians,
  });

  final List<CanvasGradientStopValue> stops;
  final String tileMode;
  final double? rotationRadians;
}

class CanvasLinearGradientValue extends CanvasBoxGradientValue {
  const CanvasLinearGradientValue({
    required this.begin,
    required this.end,
    required super.stops,
    required super.tileMode,
    required super.rotationRadians,
  });

  final CanvasAlignmentGeometryValue begin;
  final CanvasAlignmentGeometryValue end;
}

class CanvasRadialGradientValue extends CanvasBoxGradientValue {
  const CanvasRadialGradientValue({
    required this.center,
    required this.radius,
    required this.focal,
    required this.focalRadius,
    required super.stops,
    required super.tileMode,
    required super.rotationRadians,
  });

  final CanvasAlignmentGeometryValue center;
  final double radius;
  final CanvasAlignmentGeometryValue? focal;
  final double focalRadius;
}

class CanvasSweepGradientValue extends CanvasBoxGradientValue {
  const CanvasSweepGradientValue({
    required this.center,
    required this.startAngle,
    required this.endAngle,
    required super.stops,
    required super.tileMode,
    required super.rotationRadians,
  });

  final CanvasAlignmentGeometryValue center;
  final double startAngle;
  final double endAngle;
}

class CanvasGradientStopValue {
  const CanvasGradientStopValue({
    required this.id,
    required this.color,
    required this.stop,
  });

  final String id;
  final CanvasColorSource color;
  final double stop;
}

CanvasIconDataValue _decodeIconData(Map<String, Object?> object, String path) {
  _exactKeys(object, path, const {
    'kind',
    'codePoint',
    'fontFamily',
    'fontPackage',
    'matchTextDirection',
    'fontFamilyFallback',
  });
  final rawCodePoint = object['codePoint'];
  _expect(
    rawCodePoint == null || rawCodePoint is int,
    'Canvas IconData codePoint must be an integer or null: $path/codePoint',
  );
  final codePoint = rawCodePoint as int?;
  if (codePoint != null) {
    _expect(
      codePoint >= 0 &&
          codePoint <= _maxCanvasIconCodePoint &&
          (codePoint < _canvasIconSurrogateStart ||
              codePoint > _canvasIconSurrogateEnd),
      'Canvas IconData codePoint must be a Unicode scalar: $path/codePoint',
    );
  }
  final fontFamily = _nullableIconMetadata(
    object['fontFamily'],
    '$path/fontFamily',
  );
  final fontPackage = _nullableIconMetadata(
    object['fontPackage'],
    '$path/fontPackage',
  );
  _expect(
    object['matchTextDirection'] is bool,
    'Canvas IconData matchTextDirection must be a boolean: '
    '$path/matchTextDirection',
  );
  final matchTextDirection = object['matchTextDirection']! as bool;
  final rawFallback = object['fontFamilyFallback'];
  _expect(
    rawFallback is List<Object?>,
    'Canvas IconData fontFamilyFallback must be an array: '
    '$path/fontFamilyFallback',
  );
  final fallbackValues = rawFallback! as List<Object?>;
  _expect(
    fallbackValues.length <= _maxCanvasIconFontFamilyFallbacks,
    'Canvas IconData fontFamilyFallback has too many entries: '
    '$path/fontFamilyFallback',
  );
  final fontFamilyFallback = <String>[];
  final uniqueFallbacks = <String>{};
  for (var index = 0; index < fallbackValues.length; index++) {
    final fallback = _requiredIconMetadata(
      fallbackValues[index],
      '$path/fontFamilyFallback/$index',
    );
    _expect(
      uniqueFallbacks.add(fallback),
      'Canvas IconData fontFamilyFallback entries must be unique: '
      '$path/fontFamilyFallback/$index',
    );
    fontFamilyFallback.add(fallback);
  }
  _expect(
    fontPackage == null || fontFamily != null,
    'Canvas IconData fontPackage requires fontFamily: $path/fontPackage',
  );
  if (codePoint == null) {
    _expect(
      fontFamily == null &&
          fontPackage == null &&
          !matchTextDirection &&
          fontFamilyFallback.isEmpty,
      'Canvas null IconData cannot carry font metadata: $path',
    );
  }
  return CanvasIconDataValue(
    codePoint: codePoint,
    fontFamily: fontFamily,
    fontPackage: fontPackage,
    matchTextDirection: matchTextDirection,
    fontFamilyFallback: List.unmodifiable(fontFamilyFallback),
  );
}

String? _nullableIconMetadata(Object? value, String path) =>
    value == null ? null : _requiredIconMetadata(value, path);

String _requiredIconMetadata(Object? value, String path) {
  _expect(value is String, 'Canvas IconData metadata must be a string: $path');
  final text = value! as String;
  final codePoints = _validatedUnicodeCodePoints(text, path);
  _expect(
    codePoints.isNotEmpty && codePoints.length <= _maxCanvasIconMetadataLength,
    'Canvas IconData metadata length is outside 1..'
    '$_maxCanvasIconMetadataLength: $path',
  );
  _expect(
    !_isCanvasMetadataWhitespace(codePoints.first) &&
        !_isCanvasMetadataWhitespace(codePoints.last),
    'Canvas IconData metadata must not have surrounding whitespace: $path',
  );
  for (final codePoint in codePoints) {
    _expect(
      !_isIsoControl(codePoint) &&
          !_isRejectedIconMetadataBidiControl(codePoint),
      'Canvas IconData metadata must contain printable characters only: $path',
    );
  }
  return text;
}

List<int> _validatedUnicodeCodePoints(String value, String path) {
  final result = <int>[];
  final units = value.codeUnits;
  for (var index = 0; index < units.length; index++) {
    final unit = units[index];
    if (unit >= 0xd800 && unit <= 0xdbff) {
      _expect(
        index + 1 < units.length &&
            units[index + 1] >= 0xdc00 &&
            units[index + 1] <= 0xdfff,
        'Canvas IconData metadata contains an unpaired surrogate: $path',
      );
      final low = units[++index];
      result.add(0x10000 + ((unit - 0xd800) << 10) + (low - 0xdc00));
      continue;
    }
    _expect(
      unit < 0xdc00 || unit > 0xdfff,
      'Canvas IconData metadata contains an unpaired surrogate: $path',
    );
    result.add(unit);
  }
  return result;
}

bool _isIsoControl(int codePoint) =>
    codePoint <= 0x1f || (codePoint >= 0x7f && codePoint <= 0x9f);

bool _isRejectedIconMetadataBidiControl(int codePoint) =>
    codePoint == 0x061c ||
    codePoint == 0x200e ||
    codePoint == 0x200f ||
    (codePoint >= 0x2028 && codePoint <= 0x202e) ||
    (codePoint >= 0x2066 && codePoint <= 0x2069) ||
    codePoint == 0xfeff;

bool _isCanvasMetadataWhitespace(int codePoint) =>
    (codePoint >= 0x09 && codePoint <= 0x0d) ||
    (codePoint >= 0x1c && codePoint <= 0x20) ||
    codePoint == 0x1680 ||
    (codePoint >= 0x2000 && codePoint <= 0x2006) ||
    (codePoint >= 0x2008 && codePoint <= 0x200a) ||
    codePoint == 0x2028 ||
    codePoint == 0x2029 ||
    codePoint == 0x205f ||
    codePoint == 0x3000;

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

CanvasAlignmentGeometryValue _decodeAlignmentGeometry(
  Map<String, Object?> object,
  String path,
) {
  _exactKeys(object, path, const {'kind', 'basis', 'horizontal', 'vertical'});
  return _decodeAlignmentGeometryFields(object, path);
}

CanvasAlignmentGeometryValue _decodeNestedAlignmentGeometry(
  Object? value,
  String path,
) {
  final object = _object(value, path);
  _exactKeys(object, path, const {'basis', 'horizontal', 'vertical'});
  return _decodeAlignmentGeometryFields(object, path);
}

CanvasAlignmentGeometryValue _decodeAlignmentGeometryFields(
  Map<String, Object?> object,
  String path,
) => CanvasAlignmentGeometryValue(
  basis: _exactEnumText(object['basis'], '$path/basis', const {
    'physical',
    'directional',
  }),
  horizontal: _finiteNumber(object['horizontal'], '$path/horizontal'),
  vertical: _finiteNumber(object['vertical'], '$path/vertical'),
);

CanvasOffsetValue _decodeOffset(Map<String, Object?> object, String path) {
  _exactKeys(object, path, const {'kind', 'dx', 'dy'});
  return CanvasOffsetValue(
    dx: _finiteNumber(object['dx'], '$path/dx'),
    dy: _finiteNumber(object['dy'], '$path/dy'),
  );
}

CanvasSizeValue _decodeSize(Map<String, Object?> object, String path) {
  _exactKeys(object, path, const {'kind', 'width', 'height'});
  return CanvasSizeValue(
    width: _finiteNumber(object['width'], '$path/width', minimum: 0),
    height: _finiteNumber(object['height'], '$path/height', minimum: 0),
  );
}

CanvasBoxConstraintsValue _decodeBoxConstraints(
  Map<String, Object?> object,
  String path,
) {
  _exactKeys(object, path, const {
    'kind',
    'minWidth',
    'maxWidth',
    'minHeight',
    'maxHeight',
  });
  final minWidth = object['minWidth'] == null
      ? null
      : _finiteNumber(object['minWidth'], '$path/minWidth', minimum: 0);
  final maxWidth = object['maxWidth'] == null
      ? null
      : _finiteNumber(object['maxWidth'], '$path/maxWidth', minimum: 0);
  final minHeight = object['minHeight'] == null
      ? null
      : _finiteNumber(object['minHeight'], '$path/minHeight', minimum: 0);
  final maxHeight = object['maxHeight'] == null
      ? null
      : _finiteNumber(object['maxHeight'], '$path/maxHeight', minimum: 0);
  _expect(
    minWidth == null
        ? maxWidth == null
        : maxWidth == null || maxWidth >= minWidth,
    'Canvas BoxConstraints maxWidth is less than minWidth: $path',
  );
  _expect(
    minHeight == null
        ? maxHeight == null
        : maxHeight == null || maxHeight >= minHeight,
    'Canvas BoxConstraints maxHeight is less than minHeight: $path',
  );
  return CanvasBoxConstraintsValue(
    minWidth: minWidth,
    maxWidth: maxWidth,
    minHeight: minHeight,
    maxHeight: maxHeight,
  );
}

CanvasMatrix4Value _decodeMatrix4(Map<String, Object?> object, String path) {
  _exactKeys(object, path, const {'kind', 'storage'});
  final rawStorage = object['storage'];
  _expect(
    rawStorage is List<Object?>,
    'Canvas Matrix4 storage must be an array: $path/storage',
  );
  final storage = rawStorage! as List<Object?>;
  _expect(
    storage.length == 16,
    'Canvas Matrix4 storage must contain exactly 16 entries: $path/storage',
  );
  return CanvasMatrix4Value(
    List.unmodifiable([
      for (var index = 0; index < storage.length; index++)
        _finiteNumber(storage[index], '$path/storage/$index'),
    ]),
  );
}

CanvasImageProviderValue _decodeImageProvider(Object? value, String path) {
  final object = _object(value, path);
  _exactKeys(object, path, const {
    'kind',
    'assetName',
    'packageName',
    'exactScale',
    'resize',
    'resolution',
  });
  final providerKind = _exactEnumText(object['kind'], '$path/kind', const {
    'asset',
    'exactAsset',
  });
  final assetName = _boundedPropertyText(
    object['assetName'],
    '$path/assetName',
    1,
    4096,
  );
  _validateImageAssetName(assetName, '$path/assetName');
  final rawPackageName = object['packageName'];
  _expect(
    rawPackageName == null || rawPackageName is String,
    'Canvas image packageName must be a string or null: $path/packageName',
  );
  final packageName = rawPackageName == null
      ? null
      : _boundedText(rawPackageName, '$path/packageName', 1, 64);
  _expect(
    packageName == null ||
        RegExp(r'^[a-z][a-z0-9_]{0,63}$').hasMatch(packageName),
    'Canvas image packageName is not a Dart package name: $path/packageName',
  );
  final exactScale = object['exactScale'] == null
      ? null
      : _finiteNumber(
          object['exactScale'],
          '$path/exactScale',
          minimumExclusive: 0,
        );
  _expect(
    (providerKind == 'exactAsset') == (exactScale != null),
    'Canvas ExactAssetImage alone must carry exactScale: $path/exactScale',
  );
  return CanvasImageProviderValue(
    providerKind: providerKind,
    assetName: assetName,
    packageName: packageName,
    exactScale: exactScale,
    resize: object['resize'] == null
        ? null
        : _decodeResizeImage(object['resize'], '$path/resize'),
    resolution: _decodeImageResolution(
      object['resolution'],
      '$path/resolution',
    ),
  );
}

CanvasResizeImageValue _decodeResizeImage(Object? value, String path) {
  final object = _object(value, path);
  _exactKeys(object, path, const {
    'width',
    'height',
    'policy',
    'allowUpscaling',
  });
  int? dimension(String name) {
    final raw = object[name];
    _expect(
      raw == null || raw is int,
      'Canvas ResizeImage $name must be an integer or null: $path/$name',
    );
    final result = raw as int?;
    _expect(
      result == null || (result >= 1 && result <= 16384),
      'Canvas ResizeImage $name is outside 1..16384: $path/$name',
    );
    return result;
  }

  final width = dimension('width');
  final height = dimension('height');
  _expect(
    width != null || height != null,
    'Canvas ResizeImage requires width or height: $path',
  );
  _expect(
    object['allowUpscaling'] is bool,
    'Canvas ResizeImage allowUpscaling must be a boolean: '
    '$path/allowUpscaling',
  );
  return CanvasResizeImageValue(
    width: width,
    height: height,
    policy: _exactEnumText(object['policy'], '$path/policy', const {
      'exact',
      'fit',
    }),
    allowUpscaling: object['allowUpscaling']! as bool,
  );
}

CanvasImageResolutionValue _decodeImageResolution(Object? value, String path) {
  final object = _object(value, path);
  final kind = _exactEnumText(object['kind'], '$path/kind', const {
    'resolved',
    'unavailable',
  });
  if (kind == 'resolved') {
    _exactKeys(object, path, const {'kind', 'resourceId', 'resolvedScale'});
    final resourceId = _boundedText(
      object['resourceId'],
      '$path/resourceId',
      64,
      64,
    );
    _expect(
      RegExp(r'^[0-9a-f]{64}$').hasMatch(resourceId),
      'Canvas image resourceId must be a lowercase SHA-256 digest: '
      '$path/resourceId',
    );
    return CanvasResolvedImageValue(
      resourceId: resourceId,
      resolvedScale: _finiteNumber(
        object['resolvedScale'],
        '$path/resolvedScale',
        minimumExclusive: 0,
      ),
    );
  }
  _exactKeys(object, path, const {'kind', 'code', 'reason'});
  final reason = _boundedPropertyText(
    object['reason'],
    '$path/reason',
    1,
    1024,
  );
  _expectWellFormedUtf16(reason, '$path/reason');
  _expect(
    reason.codeUnits.length <= 1024,
    'Canvas unavailable image reason exceeds 1024 UTF-16 units: '
    '$path/reason',
  );
  _expect(
    !reason.codeUnits.any(
      (unit) =>
          ((unit <= 0x1f && unit != 0x09 && unit != 0x0a && unit != 0x0d) ||
          (unit >= 0x7f && unit <= 0x9f)),
    ),
    'Canvas unavailable image reason contains a control character: '
    '$path/reason',
  );
  _expect(
    reason.trim().isNotEmpty,
    'Canvas unavailable image reason must be concrete: $path/reason',
  );
  return CanvasUnavailableImageValue(
    code: _exactEnumText(object['code'], '$path/code', const {
      'undeclared',
      'missing',
      'invalidPath',
      'unreadable',
      'unsupportedFormat',
      'corrupt',
      'budgetExceeded',
    }),
    reason: reason,
  );
}

CanvasDecorationImageValue _decodeDecorationImage(
  Object? value,
  _PropertySpec spec,
  String path,
) {
  final object = _object(value, path);
  _exactKeys(object, path, const {
    'image',
    'onError',
    'colorFilter',
    'fit',
    'alignment',
    'centerSlice',
    'repeat',
    'matchTextDirection',
    'scale',
    'opacity',
    'filterQuality',
    'invertColors',
    'isAntiAlias',
  });
  _expect(
    object['onError'] is bool,
    'Canvas DecorationImage onError presence must be a boolean: '
    '$path/onError',
  );
  _expect(
    object['matchTextDirection'] is bool &&
        object['invertColors'] is bool &&
        object['isAntiAlias'] is bool,
    'Canvas DecorationImage boolean field is invalid: $path',
  );
  final fit = object['fit'] == null
      ? null
      : _exactEnumText(object['fit'], '$path/fit', const {
          'fill',
          'contain',
          'cover',
          'fitWidth',
          'fitHeight',
          'none',
          'scaleDown',
        });
  final centerSlice = object['centerSlice'] == null
      ? null
      : _decodeImageRect(object['centerSlice'], '$path/centerSlice');
  _expect(
    centerSlice == null || (fit != 'cover' && fit != 'none'),
    'Canvas DecorationImage centerSlice rejects cover and none fits: '
    '$path/fit',
  );
  return CanvasDecorationImageValue(
    image: _decodeImageProvider(object['image'], '$path/image'),
    onError: object['onError']! as bool,
    colorFilter: object['colorFilter'] == null
        ? null
        : _decodeColorFilter(object['colorFilter'], spec, '$path/colorFilter'),
    fit: fit,
    alignment: _decodeNestedAlignmentGeometry(
      object['alignment'],
      '$path/alignment',
    ),
    centerSlice: centerSlice,
    repeat: _exactEnumText(object['repeat'], '$path/repeat', const {
      'repeat',
      'repeatX',
      'repeatY',
      'noRepeat',
    }),
    matchTextDirection: object['matchTextDirection']! as bool,
    scale: _finiteNumber(object['scale'], '$path/scale', minimumExclusive: 0),
    opacity: _finiteNumber(
      object['opacity'],
      '$path/opacity',
      minimum: 0,
      maximum: 1,
    ),
    filterQuality: _exactEnumText(
      object['filterQuality'],
      '$path/filterQuality',
      const {'none', 'low', 'medium', 'high'},
    ),
    invertColors: object['invertColors']! as bool,
    isAntiAlias: object['isAntiAlias']! as bool,
  );
}

CanvasColorFilterValue _decodeColorFilter(
  Object? value,
  _PropertySpec spec,
  String path,
) {
  final object = _object(value, path);
  final kind = _exactEnumText(object['kind'], '$path/kind', const {
    'mode',
    'matrix',
    'linearToSrgbGamma',
    'srgbToLinearGamma',
    'saturation',
  });
  switch (kind) {
    case 'mode':
      _exactKeys(object, path, const {'kind', 'color', 'blendMode'});
      return CanvasModeColorFilterValue(
        color: _decodeBoxDecorationColor(object['color'], spec, '$path/color'),
        blendMode: _exactEnumText(
          object['blendMode'],
          '$path/blendMode',
          _blendModes,
        ),
      );
    case 'matrix':
      _exactKeys(object, path, const {'kind', 'values'});
      final rawValues = object['values'];
      _expect(
        rawValues is List<Object?> && rawValues.length == 20,
        'Canvas ColorFilter matrix must contain exactly 20 values: '
        '$path/values',
      );
      final values = rawValues! as List<Object?>;
      return CanvasMatrixColorFilterValue(
        List.unmodifiable([
          for (var index = 0; index < values.length; index++)
            _finiteNumber(values[index], '$path/values/$index'),
        ]),
      );
    case 'linearToSrgbGamma':
      _exactKeys(object, path, const {'kind'});
      return const CanvasLinearToSrgbGammaColorFilterValue();
    case 'srgbToLinearGamma':
      _exactKeys(object, path, const {'kind'});
      return const CanvasSrgbToLinearGammaColorFilterValue();
    case 'saturation':
      _exactKeys(object, path, const {'kind', 'value'});
      return CanvasSaturationColorFilterValue(
        _finiteNumber(object['value'], '$path/value'),
      );
  }
  throw StateError('Unreachable Canvas ColorFilter kind.');
}

CanvasRectValue _decodeImageRect(Object? value, String path) {
  final object = _object(value, path);
  _exactKeys(object, path, const {'left', 'top', 'right', 'bottom'});
  final left = _finiteNumber(object['left'], '$path/left', minimum: 0);
  final top = _finiteNumber(object['top'], '$path/top', minimum: 0);
  final right = _finiteNumber(object['right'], '$path/right', minimum: 0);
  final bottom = _finiteNumber(object['bottom'], '$path/bottom', minimum: 0);
  _expect(
    left < right && top < bottom,
    'Canvas DecorationImage centerSlice must have positive width and height: '
    '$path',
  );
  return CanvasRectValue(left: left, top: top, right: right, bottom: bottom);
}

CanvasBoxDecorationValue _decodeBoxDecoration(
  Map<String, Object?> object,
  _PropertySpec spec,
  String path,
) {
  _exactKeys(object, path, const {
    'kind',
    'color',
    'image',
    'border',
    'borderRadius',
    'boxShadow',
    'gradient',
    'backgroundBlendMode',
    'shape',
  });
  final color = object['color'] == null
      ? null
      : _decodeBoxDecorationColor(object['color'], spec, '$path/color');
  final image = object['image'] == null
      ? null
      : _decodeDecorationImage(object['image'], spec, '$path/image');
  final border = object['border'] == null
      ? null
      : _decodeBoxBorder(object['border'], spec, '$path/border');
  final borderRadius = object['borderRadius'] == null
      ? null
      : _decodeBorderRadius(object['borderRadius'], '$path/borderRadius');
  final boxShadow = _decodeBoxShadows(
    object['boxShadow'],
    spec,
    '$path/boxShadow',
  );
  final gradient = object['gradient'] == null
      ? null
      : _decodeBoxGradient(object['gradient'], spec, '$path/gradient');
  final backgroundBlendMode = object['backgroundBlendMode'] == null
      ? null
      : _exactEnumText(
          object['backgroundBlendMode'],
          '$path/backgroundBlendMode',
          _blendModes,
        );
  final shape = _exactEnumText(object['shape'], '$path/shape', const {
    'rectangle',
    'circle',
  });
  _expect(
    shape != 'circle' || borderRadius == null,
    'Canvas circular BoxDecoration cannot have a borderRadius: $path',
  );
  _expect(
    backgroundBlendMode == null || color != null || gradient != null,
    'Canvas BoxDecoration backgroundBlendMode requires color or gradient: '
    '$path',
  );
  _validatePaintSafeBoxBorder(border, borderRadius, shape, path);
  return CanvasBoxDecorationValue(
    color: color,
    image: image,
    border: border,
    borderRadius: borderRadius,
    boxShadow: List.unmodifiable(boxShadow),
    gradient: gradient,
    backgroundBlendMode: backgroundBlendMode,
    shape: shape,
  );
}

CanvasColorSource _decodeBoxDecorationColor(
  Object? value,
  _PropertySpec spec,
  String path,
) {
  final color = _decodeColorSource(value, path);
  if (color is CanvasThemeColor) {
    _expect(
      spec.themeTokens.contains(color.token.wireId),
      'Canvas BoxDecoration theme token is not allowed: $path/token',
    );
  }
  return color;
}

CanvasBoxBorderValue _decodeBoxBorder(
  Object? value,
  _PropertySpec spec,
  String path,
) {
  final object = _object(value, path);
  final kind = _exactEnumText(object['kind'], '$path/kind', const {
    'physical',
    'directional',
  });
  if (kind == 'physical') {
    _exactKeys(object, path, const {'kind', 'top', 'right', 'bottom', 'left'});
    return CanvasPhysicalBoxBorderValue(
      top: _decodeBorderSide(object['top'], spec, '$path/top'),
      right: _decodeBorderSide(object['right'], spec, '$path/right'),
      bottom: _decodeBorderSide(object['bottom'], spec, '$path/bottom'),
      left: _decodeBorderSide(object['left'], spec, '$path/left'),
    );
  }
  _exactKeys(object, path, const {'kind', 'top', 'start', 'end', 'bottom'});
  return CanvasDirectionalBoxBorderValue(
    top: _decodeBorderSide(object['top'], spec, '$path/top'),
    start: _decodeBorderSide(object['start'], spec, '$path/start'),
    end: _decodeBorderSide(object['end'], spec, '$path/end'),
    bottom: _decodeBorderSide(object['bottom'], spec, '$path/bottom'),
  );
}

CanvasBorderSideValue _decodeBorderSide(
  Object? value,
  _PropertySpec spec,
  String path,
) {
  final object = _object(value, path);
  _exactKeys(object, path, const {'color', 'width', 'style', 'strokeAlign'});
  return CanvasBorderSideValue(
    color: _decodeBoxDecorationColor(object['color'], spec, '$path/color'),
    width: _finiteNumber(object['width'], '$path/width', minimum: 0),
    style: _exactEnumText(object['style'], '$path/style', const {
      'none',
      'solid',
    }),
    strokeAlign: _finiteNumber(object['strokeAlign'], '$path/strokeAlign'),
  );
}

CanvasBorderRadiusGeometryValue _decodeBorderRadius(
  Object? value,
  String path,
) {
  final object = _object(value, path);
  final kind = _exactEnumText(object['kind'], '$path/kind', const {
    'physical',
    'directional',
  });
  if (kind == 'physical') {
    _exactKeys(object, path, const {
      'kind',
      'topLeft',
      'topRight',
      'bottomRight',
      'bottomLeft',
    });
    return CanvasPhysicalBorderRadiusValue(
      topLeft: _decodeRadius(object['topLeft'], '$path/topLeft'),
      topRight: _decodeRadius(object['topRight'], '$path/topRight'),
      bottomRight: _decodeRadius(object['bottomRight'], '$path/bottomRight'),
      bottomLeft: _decodeRadius(object['bottomLeft'], '$path/bottomLeft'),
    );
  }
  _exactKeys(object, path, const {
    'kind',
    'topStart',
    'topEnd',
    'bottomEnd',
    'bottomStart',
  });
  return CanvasDirectionalBorderRadiusValue(
    topStart: _decodeRadius(object['topStart'], '$path/topStart'),
    topEnd: _decodeRadius(object['topEnd'], '$path/topEnd'),
    bottomEnd: _decodeRadius(object['bottomEnd'], '$path/bottomEnd'),
    bottomStart: _decodeRadius(object['bottomStart'], '$path/bottomStart'),
  );
}

CanvasRadiusValue _decodeRadius(Object? value, String path) {
  final object = _object(value, path);
  _exactKeys(object, path, const {'x', 'y'});
  return CanvasRadiusValue(
    _finiteNumber(object['x'], '$path/x', minimum: 0),
    _finiteNumber(object['y'], '$path/y', minimum: 0),
  );
}

List<CanvasBoxShadowValue> _decodeBoxShadows(
  Object? value,
  _PropertySpec spec,
  String path,
) {
  final items = _boundedItems(value, path);
  final ids = <String>{};
  return [
    for (var index = 0; index < items.length; index++)
      _decodeBoxShadow(items[index], spec, '$path/$index', ids),
  ];
}

CanvasBoxShadowValue _decodeBoxShadow(
  Object? value,
  _PropertySpec spec,
  String path,
  Set<String> ids,
) {
  final object = _object(value, path);
  _exactKeys(object, path, const {
    'id',
    'color',
    'offsetX',
    'offsetY',
    'blurRadius',
    'spreadRadius',
    'blurStyle',
  });
  final id = _stableId(object['id'], '$path/id');
  _expect(ids.add(id), 'Canvas BoxShadow ids must be unique: $path/id');
  return CanvasBoxShadowValue(
    id: id,
    color: _decodeBoxDecorationColor(object['color'], spec, '$path/color'),
    offsetX: _finiteNumber(object['offsetX'], '$path/offsetX'),
    offsetY: _finiteNumber(object['offsetY'], '$path/offsetY'),
    blurRadius: _finiteNumber(
      object['blurRadius'],
      '$path/blurRadius',
      minimum: 0,
    ),
    spreadRadius: _finiteNumber(object['spreadRadius'], '$path/spreadRadius'),
    blurStyle: _exactEnumText(object['blurStyle'], '$path/blurStyle', const {
      'normal',
      'solid',
      'outer',
      'inner',
    }),
  );
}

CanvasBoxGradientValue _decodeBoxGradient(
  Object? value,
  _PropertySpec spec,
  String path,
) {
  final object = _object(value, path);
  final kind = _exactEnumText(object['kind'], '$path/kind', const {
    'linear',
    'radial',
    'sweep',
  });
  final commonKeys = {'kind', 'stops', 'tileMode', 'rotationRadians'};
  final stops = _decodeGradientStops(object['stops'], spec, '$path/stops');
  final tileMode = _exactEnumText(object['tileMode'], '$path/tileMode', const {
    'clamp',
    'repeated',
    'mirror',
    'decal',
  });
  final rotationRadians = object['rotationRadians'] == null
      ? null
      : _finiteNumber(object['rotationRadians'], '$path/rotationRadians');
  if (kind == 'linear') {
    _exactKeys(object, path, {...commonKeys, 'begin', 'end'});
    return CanvasLinearGradientValue(
      begin: _decodeNestedAlignmentGeometry(object['begin'], '$path/begin'),
      end: _decodeNestedAlignmentGeometry(object['end'], '$path/end'),
      stops: List.unmodifiable(stops),
      tileMode: tileMode,
      rotationRadians: rotationRadians,
    );
  }
  if (kind == 'radial') {
    _exactKeys(object, path, {
      ...commonKeys,
      'center',
      'radius',
      'focal',
      'focalRadius',
    });
    final center = _decodeNestedAlignmentGeometry(
      object['center'],
      '$path/center',
    );
    final focal = object['focal'] == null
        ? null
        : _decodeNestedAlignmentGeometry(object['focal'], '$path/focal');
    final focalRadius = _finiteNumber(
      object['focalRadius'],
      '$path/focalRadius',
      minimum: 0,
    );
    _expect(
      focal != null || focalRadius == 0,
      'Canvas RadialGradient focalRadius requires focal: $path',
    );
    return CanvasRadialGradientValue(
      center: center,
      radius: _finiteNumber(object['radius'], '$path/radius', minimum: 0),
      focal: focal,
      focalRadius: focalRadius,
      stops: List.unmodifiable(stops),
      tileMode: tileMode,
      rotationRadians: rotationRadians,
    );
  }
  _exactKeys(object, path, {...commonKeys, 'center', 'startAngle', 'endAngle'});
  final startAngle = _finiteNumber(object['startAngle'], '$path/startAngle');
  final endAngle = _finiteNumber(object['endAngle'], '$path/endAngle');
  _expect(
    startAngle < endAngle,
    'Canvas SweepGradient startAngle must be less than endAngle: $path',
  );
  return CanvasSweepGradientValue(
    center: _decodeNestedAlignmentGeometry(object['center'], '$path/center'),
    startAngle: startAngle,
    endAngle: endAngle,
    stops: List.unmodifiable(stops),
    tileMode: tileMode,
    rotationRadians: rotationRadians,
  );
}

List<CanvasGradientStopValue> _decodeGradientStops(
  Object? value,
  _PropertySpec spec,
  String path,
) {
  final items = _boundedItems(value, path);
  _expect(
    items.length >= 2,
    'Canvas gradient must contain at least two stops: $path',
  );
  final ids = <String>{};
  final result = <CanvasGradientStopValue>[];
  double? previous;
  for (var index = 0; index < items.length; index++) {
    final itemPath = '$path/$index';
    final object = _object(items[index], itemPath);
    _exactKeys(object, itemPath, const {'id', 'color', 'stop'});
    final id = _stableId(object['id'], '$itemPath/id');
    _expect(
      ids.add(id),
      'Canvas gradient-stop ids must be unique: $itemPath/id',
    );
    final stop = _finiteNumber(
      object['stop'],
      '$itemPath/stop',
      minimum: 0,
      maximum: 1,
    );
    _expect(
      previous == null || stop >= previous,
      'Canvas gradient stops must be non-decreasing: $itemPath/stop',
    );
    previous = stop;
    result.add(
      CanvasGradientStopValue(
        id: id,
        color: _decodeBoxDecorationColor(
          object['color'],
          spec,
          '$itemPath/color',
        ),
        stop: stop,
      ),
    );
  }
  return result;
}

void _validatePaintSafeBoxBorder(
  CanvasBoxBorderValue? border,
  CanvasBorderRadiusGeometryValue? borderRadius,
  String shape,
  String path,
) {
  if (border == null) {
    return;
  }
  final sides = border.sides;
  final first = sides.first;
  if (sides.every((side) => _sameBorderSide(side, first)) ||
      sides.every((side) => side.style == 'none')) {
    return;
  }
  final nonZeroRadius =
      borderRadius?.radii.any((radius) => radius.x != 0 || radius.y != 0) ??
      false;
  final visible = <CanvasBorderSideValue>[
    for (final side in border.sides)
      if (side.style != 'none') side,
  ];
  final oneVisibleColor =
      visible.isNotEmpty &&
      visible.every(
        (side) => _sameColorSource(side.color, visible.first.color),
      );
  final hasHairline = visible.any((side) => side.width == 0);
  if (oneVisibleColor && !hasHairline && (shape == 'circle' || nonZeroRadius)) {
    return;
  }
  _expect(
    shape == 'rectangle' && !nonZeroRadius,
    'Canvas non-uniform border requires uniform visible colors and '
    'non-hairline sides for circles or rounded rectangles: $path/border',
  );
  _expect(
    sides.every((side) => side.strokeAlign == -1),
    'Canvas non-uniform rectangular border requires strokeAlign -1 on every '
    'side: $path/border',
  );
}

bool _sameBorderSide(CanvasBorderSideValue left, CanvasBorderSideValue right) =>
    _sameColorSource(left.color, right.color) &&
    left.width == right.width &&
    left.style == right.style &&
    left.strokeAlign == right.strokeAlign;

bool _sameColorSource(CanvasColorSource left, CanvasColorSource right) =>
    switch ((left, right)) {
      (
        CanvasLiteralColor(:final argb),
        CanvasLiteralColor(argb: final other),
      ) =>
        argb == other,
      (
        CanvasThemeColor(token: final token),
        CanvasThemeColor(token: final other),
      ) =>
        token.wireId == other.wireId,
      _ => false,
    };

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
  const _WidgetSpec(this.properties, this.slots, {this.traits = const {}});
  final Map<String, _PropertySpec> properties;
  final Map<String, _SlotSpec> slots;
  final Set<String> traits;
}

class _PropertySpec {
  const _PropertySpec(
    this.kinds, {
    this.required = false,
    this.creationDefaultFingerprint,
    this.numericBounds = const {},
    this.enumLibraryUri,
    this.enumType,
    this.enumValues = const {},
    this.minimumStringLength = 0,
    this.maximumStringLength = 65536,
    this.explicitStringLength = false,
    this.stringPattern,
    this.themeTokens = const {},
    this.edgeInsetsNonNegative = false,
    this.materialIconsOnly = false,
  });
  final Set<String> kinds;
  final bool required;
  final String? creationDefaultFingerprint;
  final Map<String, _NumericBounds> numericBounds;
  final String? enumLibraryUri;
  final String? enumType;
  final Set<String> enumValues;
  final int minimumStringLength;
  final int maximumStringLength;
  final bool explicitStringLength;
  final String? stringPattern;
  final Set<String> themeTokens;
  final bool edgeInsetsNonNegative;
  final bool materialIconsOnly;
}

class _NumericBounds {
  const _NumericBounds({
    this.minimum,
    this.minimumInclusive = true,
    this.maximum,
    this.maximumInclusive = true,
  });

  final num? minimum;
  final bool minimumInclusive;
  final num? maximum;
  final bool maximumInclusive;
}

class _SlotSpec {
  const _SlotSpec({
    required this.cardinality,
    required this.required,
    required this.minimumChildren,
    required this.maximumChildren,
    this.acceptance = _anySlotAcceptance,
  });

  final String cardinality;
  final bool required;
  final int minimumChildren;
  final int maximumChildren;
  final _SlotAcceptance acceptance;
}

enum _SlotAcceptanceKind { any, requiredTrait, exactTypes }

class _SlotAcceptance {
  const _SlotAcceptance.any()
    : kind = _SlotAcceptanceKind.any,
      requiredTrait = null,
      exactTypes = const {};

  const _SlotAcceptance.requiredTrait(this.requiredTrait)
    : kind = _SlotAcceptanceKind.requiredTrait,
      exactTypes = const {};

  // Reserved by the protocol-v9 closed union even though the current ten
  // widgets use only `any` and `requiredTrait` slot acceptance.
  // ignore: unused_element
  const _SlotAcceptance.exactTypes(this.exactTypes)
    : kind = _SlotAcceptanceKind.exactTypes,
      requiredTrait = null;

  final _SlotAcceptanceKind kind;
  final String? requiredTrait;
  final Set<String> exactTypes;

  bool accepts(String widgetType, Set<String> traits) => switch (kind) {
    _SlotAcceptanceKind.any => true,
    _SlotAcceptanceKind.requiredTrait => traits.contains(requiredTrait),
    _SlotAcceptanceKind.exactTypes => exactTypes.contains(widgetType),
  };

  String fingerprint() => switch (kind) {
    _SlotAcceptanceKind.any => 'any',
    _SlotAcceptanceKind.requiredTrait =>
      'trait:${_base64Fingerprint(requiredTrait!)}',
    _SlotAcceptanceKind.exactTypes =>
      'types:${(exactTypes.toList()..sort()).map(_base64Fingerprint).join(',')}',
  };
}

const _preferredSizeWidgetTrait = 'flutter.widgets.PreferredSizeWidget';
const _anySlotAcceptance = _SlotAcceptance.any();
const _preferredSizeSlotAcceptance = _SlotAcceptance.requiredTrait(
  _preferredSizeWidgetTrait,
);

const _optionalSingleSlot = _SlotSpec(
  cardinality: 'single',
  required: false,
  minimumChildren: 0,
  maximumChildren: 1,
);
const _requiredEmptySingleSlot = _SlotSpec(
  cardinality: 'single',
  required: true,
  minimumChildren: 0,
  maximumChildren: 1,
);
const _requiredSingleSlot = _SlotSpec(
  cardinality: 'single',
  required: true,
  minimumChildren: 1,
  maximumChildren: 1,
);
const _optionalListSlot = _SlotSpec(
  cardinality: 'list',
  required: false,
  minimumChildren: 0,
  maximumChildren: 10000,
);
const _optionalPreferredSizeSingleSlot = _SlotSpec(
  cardinality: 'single',
  required: false,
  minimumChildren: 0,
  maximumChildren: 1,
  acceptance: _preferredSizeSlotAcceptance,
);
const _unboundedDoubleBounds = <String, _NumericBounds>{
  'double': _NumericBounds(),
};
const _nonNegativeDoubleBounds = <String, _NumericBounds>{
  'double': _NumericBounds(minimum: 0),
};
const _positiveDoubleBounds = <String, _NumericBounds>{
  'double': _NumericBounds(minimum: 0, minimumInclusive: false),
};
const _positiveFontVariationBounds = <String, _NumericBounds>{
  'double': _NumericBounds(
    minimum: 0,
    minimumInclusive: false,
    maximum: 32768,
    maximumInclusive: false,
  ),
};
const _gradeFontVariationBounds = <String, _NumericBounds>{
  'double': _NumericBounds(
    minimum: -32768,
    maximum: 32768,
    maximumInclusive: false,
  ),
};
const _zeroToOneDoubleBounds = <String, _NumericBounds>{
  'double': _NumericBounds(minimum: 0, maximum: 1),
};
const _nonNegativeNumberBounds = <String, _NumericBounds>{
  'integer': _NumericBounds(minimum: 0, maximum: maxCanvasSequence),
  'double': _NumericBounds(minimum: 0),
};
const _nonNegativeIntegerBounds = <String, _NumericBounds>{
  'integer': _NumericBounds(minimum: 0, maximum: maxCanvasSequence),
};
const _signedPortableIntegerBounds = <String, _NumericBounds>{
  'integer': _NumericBounds(
    minimum: -maxCanvasSequence,
    maximum: maxCanvasSequence,
  ),
};
const _textFieldMaxLengthIntegerBounds = <String, _NumericBounds>{
  'integer': _NumericBounds(minimum: -1, maximum: maxCanvasSequence),
};
const _positiveIntegerBounds = <String, _NumericBounds>{
  'integer': _NumericBounds(
    minimum: 1,
    maximum: maxCanvasSequence,
    maximumInclusive: true,
  ),
};
const _nonNegativeEdgeInsetsBounds = <String, _NumericBounds>{
  'edgeInsets': _NumericBounds(minimum: 0),
  'edgeInsetsDirectional': _NumericBounds(minimum: 0),
};
const _widgetsLibraryUri = 'package:flutter/widgets.dart';
const _materialLibraryUri = 'package:flutter/material.dart';
const _servicesLibraryUri = 'package:flutter/services.dart';
const _gesturesLibraryUri = 'package:flutter/gestures.dart';
const _renderingLibraryUri = 'package:flutter/rendering.dart';
const _dartUiLibraryUri = 'dart:ui';

const _fontWeightProperty = _PropertySpec(
  {'enum'},
  enumLibraryUri: _widgetsLibraryUri,
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
  enumLibraryUri: _widgetsLibraryUri,
  enumType: 'FontStyle',
  enumValues: {'normal', 'italic'},
);
const _textBaselineProperty = _PropertySpec(
  {'enum'},
  enumLibraryUri: _widgetsLibraryUri,
  enumType: 'TextBaseline',
  enumValues: {'alphabetic', 'ideographic'},
);
const _textLeadingDistributionProperty = _PropertySpec(
  {'enum'},
  enumLibraryUri: _widgetsLibraryUri,
  enumType: 'TextLeadingDistribution',
  enumValues: {'proportional', 'even'},
);
const _textOverflowProperty = _PropertySpec(
  {'enum'},
  enumLibraryUri: _widgetsLibraryUri,
  enumType: 'TextOverflow',
  enumValues: {'clip', 'fade', 'ellipsis', 'visible'},
);
const _fontNameProperty = _PropertySpec(
  {'string'},
  minimumStringLength: 1,
  maximumStringLength: 256,
  explicitStringLength: true,
);
const _fontFamilyFallbackProperty = _PropertySpec(
  {'string'},
  maximumStringLength: 4096,
  explicitStringLength: true,
);

const _themeAwareColorProperty = _PropertySpec({
  'color',
  'themeToken',
}, themeTokens: canvasColorSchemeThemeTokens);
const _brightnessProperty = _PropertySpec(
  {'enum'},
  enumLibraryUri: _widgetsLibraryUri,
  enumType: 'Brightness',
  enumValues: {'light', 'dark'},
);
const _clipBehaviorProperty = _PropertySpec(
  {'enum'},
  enumLibraryUri: _widgetsLibraryUri,
  enumType: 'Clip',
  enumValues: {'none', 'hardEdge', 'antiAlias', 'antiAliasWithSaveLayer'},
);
const _borderStyleProperty = _PropertySpec(
  {'enum'},
  enumLibraryUri: _widgetsLibraryUri,
  enumType: 'BorderStyle',
  enumValues: {'none', 'solid'},
);
const _textDecorationStyleProperty = _PropertySpec(
  {'enum'},
  enumLibraryUri: _widgetsLibraryUri,
  enumType: 'TextDecorationStyle',
  enumValues: {'solid', 'double', 'dotted', 'dashed', 'wavy'},
);
Map<String, _PropertySpec> _prefixedTextStyleProperties(String prefix) => {
  '${prefix}ThemeTextStyle': _PropertySpec({
    'themeToken',
  }, themeTokens: canvasTextThemeTokens),
  '${prefix}Inherit': _PropertySpec({'boolean'}),
  '${prefix}Color': _themeAwareColorProperty,
  '${prefix}BackgroundColor': _themeAwareColorProperty,
  '${prefix}FontSize': _PropertySpec({
    'double',
  }, numericBounds: _nonNegativeDoubleBounds),
  '${prefix}FontWeight': _fontWeightProperty,
  '${prefix}FontStyle': _fontStyleProperty,
  '${prefix}LetterSpacing': _PropertySpec({
    'double',
  }, numericBounds: _unboundedDoubleBounds),
  '${prefix}WordSpacing': _PropertySpec({
    'double',
  }, numericBounds: _unboundedDoubleBounds),
  '${prefix}TextBaseline': _textBaselineProperty,
  '${prefix}Height': _PropertySpec({
    'double',
  }, numericBounds: _unboundedDoubleBounds),
  '${prefix}LeadingDistribution': _textLeadingDistributionProperty,
  '${prefix}LocaleLanguageCode': _PropertySpec({
    'string',
  }, stringPattern: r'(?:[a-z]{2,3}|[a-z]{5,8})'),
  '${prefix}LocaleScriptCode': _PropertySpec({
    'string',
  }, stringPattern: r'[A-Z][a-z]{3}'),
  '${prefix}LocaleCountryCode': _PropertySpec({
    'string',
  }, stringPattern: r'(?:[A-Z]{2}|[0-9]{3})'),
  '${prefix}Foreground': _PropertySpec({'paint'}),
  '${prefix}Background': _PropertySpec({'paint'}),
  '${prefix}Shadows': _PropertySpec({'shadowList'}),
  '${prefix}FontFeatures': _PropertySpec({'fontFeatureList'}),
  '${prefix}FontVariations': _PropertySpec({'fontVariationList'}),
  '${prefix}DecorationUnderline': _PropertySpec({'boolean'}),
  '${prefix}DecorationOverline': _PropertySpec({'boolean'}),
  '${prefix}DecorationLineThrough': _PropertySpec({'boolean'}),
  '${prefix}DecorationColor': _themeAwareColorProperty,
  '${prefix}DecorationStyle': _textDecorationStyleProperty,
  '${prefix}DecorationThickness': _PropertySpec({
    'double',
  }, numericBounds: _unboundedDoubleBounds),
  '${prefix}DebugLabel': _PropertySpec({'string'}),
  '${prefix}FontFamily': _fontNameProperty,
  '${prefix}FontFamilyFallback': _fontFamilyFallbackProperty,
  '${prefix}Package': _fontNameProperty,
  '${prefix}Overflow': _textOverflowProperty,
};

Map<String, _PropertySpec> _elevatedButtonTextStyleProperties(String prefix) =>
    {
      '${prefix}TextTheme': _PropertySpec({
        'themeToken',
      }, themeTokens: canvasTextThemeTokens),
      '${prefix}TextInherit': _PropertySpec({'boolean'}),
      '${prefix}TextBackgroundColor': _themeAwareColorProperty,
      '${prefix}TextFontSize': _PropertySpec({
        'double',
      }, numericBounds: _nonNegativeDoubleBounds),
      '${prefix}TextFontWeight': _fontWeightProperty,
      '${prefix}TextFontStyle': _fontStyleProperty,
      '${prefix}TextLetterSpacing': _PropertySpec({
        'double',
      }, numericBounds: _unboundedDoubleBounds),
      '${prefix}TextWordSpacing': _PropertySpec({
        'double',
      }, numericBounds: _unboundedDoubleBounds),
      '${prefix}TextTextBaseline': _textBaselineProperty,
      '${prefix}TextHeight': _PropertySpec({
        'double',
      }, numericBounds: _unboundedDoubleBounds),
      '${prefix}TextLeadingDistribution': _textLeadingDistributionProperty,
      '${prefix}TextLocaleLanguageCode': _PropertySpec({
        'string',
      }, stringPattern: r'(?:[a-z]{2,3}|[a-z]{5,8})'),
      '${prefix}TextLocaleScriptCode': _PropertySpec({
        'string',
      }, stringPattern: r'[A-Z][a-z]{3}'),
      '${prefix}TextLocaleCountryCode': _PropertySpec({
        'string',
      }, stringPattern: r'(?:[A-Z]{2}|[0-9]{3})'),
      '${prefix}TextBackground': _PropertySpec({'paint'}),
      '${prefix}TextShadows': _PropertySpec({'shadowList'}),
      '${prefix}TextFontFeatures': _PropertySpec({'fontFeatureList'}),
      '${prefix}TextFontVariations': _PropertySpec({'fontVariationList'}),
      '${prefix}TextDecorationUnderline': _PropertySpec({'boolean'}),
      '${prefix}TextDecorationOverline': _PropertySpec({'boolean'}),
      '${prefix}TextDecorationLineThrough': _PropertySpec({'boolean'}),
      '${prefix}TextDecorationColor': _themeAwareColorProperty,
      '${prefix}TextDecorationStyle': _textDecorationStyleProperty,
      '${prefix}TextDecorationThickness': _PropertySpec({
        'double',
      }, numericBounds: _unboundedDoubleBounds),
      '${prefix}TextFontFamily': _fontNameProperty,
      '${prefix}TextFontFamilyFallback': _fontFamilyFallbackProperty,
      '${prefix}TextPackage': _fontNameProperty,
      '${prefix}TextOverflow': _textOverflowProperty,
    };

Map<String, _PropertySpec> _elevatedButtonStateProperties(String prefix) => {
  '${prefix}BackgroundColor': _themeAwareColorProperty,
  '${prefix}ForegroundColor': _themeAwareColorProperty,
  '${prefix}OverlayColor': _themeAwareColorProperty,
  '${prefix}ShadowColor': _themeAwareColorProperty,
  '${prefix}SurfaceTintColor': _themeAwareColorProperty,
  '${prefix}Elevation': _PropertySpec({
    'integer',
    'double',
  }, numericBounds: _nonNegativeNumberBounds),
  '${prefix}Padding': _PropertySpec(
    {'edgeInsets', 'edgeInsetsDirectional'},
    numericBounds: _nonNegativeEdgeInsetsBounds,
    edgeInsetsNonNegative: true,
  ),
  for (final suffix in const [
    'MinimumWidth',
    'MinimumHeight',
    'FixedWidth',
    'FixedHeight',
    'MaximumWidth',
    'MaximumHeight',
  ])
    '$prefix$suffix': _PropertySpec({
      'integer',
      'double',
    }, numericBounds: _nonNegativeNumberBounds),
  '${prefix}IconColor': _themeAwareColorProperty,
  '${prefix}IconSize': _PropertySpec({
    'integer',
    'double',
  }, numericBounds: _nonNegativeNumberBounds),
  '${prefix}SideColor': _themeAwareColorProperty,
  '${prefix}SideWidth': _PropertySpec({
    'double',
  }, numericBounds: _nonNegativeDoubleBounds),
  '${prefix}SideStyle': _borderStyleProperty,
  '${prefix}SideStrokeAlign': _PropertySpec({
    'double',
  }, numericBounds: _unboundedDoubleBounds),
  '${prefix}ShapeKind': _PropertySpec(
    {'string'},
    stringPattern:
        r'(?:roundedRectangle|roundedSuperellipse|stadium|circle|beveledRectangle|continuousRectangle)',
  ),
  '${prefix}ShapeRadiusTopLeft': _PropertySpec({
    'double',
  }, numericBounds: _nonNegativeDoubleBounds),
  '${prefix}ShapeRadiusTopRight': _PropertySpec({
    'double',
  }, numericBounds: _nonNegativeDoubleBounds),
  '${prefix}ShapeRadiusBottomRight': _PropertySpec({
    'double',
  }, numericBounds: _nonNegativeDoubleBounds),
  '${prefix}ShapeRadiusBottomLeft': _PropertySpec({
    'double',
  }, numericBounds: _nonNegativeDoubleBounds),
  '${prefix}ShapeCircleEccentricity': _PropertySpec({
    'double',
  }, numericBounds: _zeroToOneDoubleBounds),
  '${prefix}MouseCursor': _PropertySpec(
    {'string'},
    stringPattern:
        r'(?:none|basic|click|forbidden|wait|progress|contextMenu|help|text|verticalText|cell|precise|move|grab|grabbing|noDrop|alias|copy|disappearing|allScroll|resizeLeftRight|resizeUpDown|resizeUpLeftDownRight|resizeUpRightDownLeft|resizeUp|resizeDown|resizeLeft|resizeRight|resizeUpLeft|resizeUpRight|resizeDownLeft|resizeDownRight|resizeColumn|resizeRow|zoomIn|zoomOut)',
  ),
  ..._elevatedButtonTextStyleProperties(prefix),
};

Map<String, _PropertySpec> _elevatedButtonProperties() => {
  'enabled': _PropertySpec({
    'boolean',
  }, creationDefaultFingerprint: 'boolean:true'),
  'onPressed': _PropertySpec({'callback'}),
  'onLongPress': _PropertySpec({'callback'}),
  'onHover': _PropertySpec({'callback'}),
  'onFocusChange': _PropertySpec({'callback'}),
  'autofocus': _PropertySpec({'boolean'}),
  'clipBehavior': _clipBehaviorProperty,
  for (final prefix in const [
    'style',
    'styleDisabled',
    'stylePressed',
    'styleHovered',
    'styleFocused',
  ])
    ..._elevatedButtonStateProperties(prefix),
  'styleVisualDensityHorizontal': _PropertySpec(
    {'double'},
    numericBounds: const {'double': _NumericBounds(minimum: -4, maximum: 4)},
  ),
  'styleVisualDensityVertical': _PropertySpec(
    {'double'},
    numericBounds: const {'double': _NumericBounds(minimum: -4, maximum: 4)},
  ),
  'styleTapTargetSize': _PropertySpec(
    {'enum'},
    enumLibraryUri: _materialLibraryUri,
    enumType: 'MaterialTapTargetSize',
    enumValues: {'padded', 'shrinkWrap'},
  ),
  'styleAnimationDurationMs': _PropertySpec(
    {'integer'},
    numericBounds: const {
      'integer': _NumericBounds(minimum: 0, maximum: maxCanvasSequence),
    },
  ),
  'styleEnableFeedback': _PropertySpec({'boolean'}),
  'styleAlignmentKind': _PropertySpec({
    'string',
  }, stringPattern: r'(?:physical|directional)'),
  'styleAlignmentX': _PropertySpec({
    'double',
  }, numericBounds: _unboundedDoubleBounds),
  'styleAlignmentY': _PropertySpec({
    'double',
  }, numericBounds: _unboundedDoubleBounds),
  'styleSplashFactory': _PropertySpec({
    'string',
  }, stringPattern: r'(?:inkRipple|inkSplash|inkSparkle|noSplash)'),
};

Map<String, _PropertySpec> _prefixedIconThemeProperties(String prefix) => {
  '${prefix}Size': _PropertySpec({
    'integer',
    'double',
  }, numericBounds: _nonNegativeNumberBounds),
  '${prefix}Fill': _PropertySpec({
    'double',
  }, numericBounds: _zeroToOneDoubleBounds),
  '${prefix}Weight': _PropertySpec({
    'double',
  }, numericBounds: _positiveFontVariationBounds),
  '${prefix}Grade': _PropertySpec({
    'double',
  }, numericBounds: _gradeFontVariationBounds),
  '${prefix}OpticalSize': _PropertySpec({
    'double',
  }, numericBounds: _positiveFontVariationBounds),
  '${prefix}Color': _themeAwareColorProperty,
  '${prefix}Opacity': _PropertySpec({
    'double',
  }, numericBounds: _zeroToOneDoubleBounds),
  '${prefix}Shadows': _PropertySpec({'shadowList'}),
  '${prefix}ApplyTextScaling': _PropertySpec({'boolean'}),
};

Map<String, _PropertySpec> _appBarProperties() => {
  'backgroundColor': _themeAwareColorProperty,
  'centerTitle': _PropertySpec({'boolean'}),
  'elevation': _PropertySpec({
    'integer',
    'double',
  }, numericBounds: _nonNegativeNumberBounds),
  'automaticallyImplyLeading': _PropertySpec({'boolean'}),
  'automaticallyImplyActions': _PropertySpec({'boolean'}),
  'scrolledUnderElevation': _PropertySpec({
    'integer',
    'double',
  }, numericBounds: _nonNegativeNumberBounds),
  'notificationPredicate': _PropertySpec({
    'string',
  }, stringPattern: r'(?:default|depthZero|all)'),
  'shadowColor': _themeAwareColorProperty,
  'surfaceTintColor': _themeAwareColorProperty,
  'foregroundColor': _themeAwareColorProperty,
  'primary': _PropertySpec({'boolean'}),
  'excludeHeaderSemantics': _PropertySpec({'boolean'}),
  'titleSpacing': _PropertySpec({
    'double',
  }, numericBounds: _unboundedDoubleBounds),
  'toolbarOpacity': _PropertySpec({
    'double',
  }, numericBounds: _zeroToOneDoubleBounds),
  'bottomOpacity': _PropertySpec({
    'double',
  }, numericBounds: _zeroToOneDoubleBounds),
  'toolbarHeight': _PropertySpec({
    'integer',
    'double',
  }, numericBounds: _nonNegativeNumberBounds),
  'leadingWidth': _PropertySpec({
    'integer',
    'double',
  }, numericBounds: _nonNegativeNumberBounds),
  'forceMaterialTransparency': _PropertySpec({'boolean'}),
  'useDefaultSemanticsOrder': _PropertySpec({'boolean'}),
  'clipBehavior': _clipBehaviorProperty,
  'actionsPadding': _PropertySpec(
    {'edgeInsets', 'edgeInsetsDirectional'},
    numericBounds: _nonNegativeEdgeInsetsBounds,
    edgeInsetsNonNegative: true,
  ),
  'animateColor': _PropertySpec({'boolean'}),
  'shapeKind': _PropertySpec(
    {'string'},
    stringPattern:
        r'(?:roundedRectangle|stadium|circle|beveledRectangle|continuousRectangle)',
  ),
  'shapeSideColor': _themeAwareColorProperty,
  'shapeSideWidth': _PropertySpec({
    'double',
  }, numericBounds: _nonNegativeDoubleBounds),
  'shapeSideStyle': _borderStyleProperty,
  'shapeSideStrokeAlign': _PropertySpec({
    'double',
  }, numericBounds: _unboundedDoubleBounds),
  'shapeRadiusTopLeft': _PropertySpec({
    'double',
  }, numericBounds: _nonNegativeDoubleBounds),
  'shapeRadiusTopRight': _PropertySpec({
    'double',
  }, numericBounds: _nonNegativeDoubleBounds),
  'shapeRadiusBottomRight': _PropertySpec({
    'double',
  }, numericBounds: _nonNegativeDoubleBounds),
  'shapeRadiusBottomLeft': _PropertySpec({
    'double',
  }, numericBounds: _nonNegativeDoubleBounds),
  'shapeCircleEccentricity': _PropertySpec({
    'double',
  }, numericBounds: _zeroToOneDoubleBounds),
  ..._prefixedIconThemeProperties('iconTheme'),
  ..._prefixedIconThemeProperties('actionsIconTheme'),
  ..._prefixedTextStyleProperties('toolbarTextStyle'),
  ..._prefixedTextStyleProperties('titleTextStyle'),
  'systemOverlayStyleSystemNavigationBarColor': _themeAwareColorProperty,
  'systemOverlayStyleSystemNavigationBarDividerColor': _themeAwareColorProperty,
  'systemOverlayStyleSystemNavigationBarIconBrightness': _brightnessProperty,
  'systemOverlayStyleSystemNavigationBarContrastEnforced': _PropertySpec({
    'boolean',
  }),
  'systemOverlayStyleStatusBarColor': _themeAwareColorProperty,
  'systemOverlayStyleStatusBarBrightness': _brightnessProperty,
  'systemOverlayStyleStatusBarIconBrightness': _brightnessProperty,
  'systemOverlayStyleSystemStatusBarContrastEnforced': _PropertySpec({
    'boolean',
  }),
};

Map<String, _PropertySpec> _scaffoldProperties() => {
  'floatingActionButtonLocation': _PropertySpec(
    {'string'},
    stringPattern:
        r'(?:startTop|miniStartTop|centerTop|miniCenterTop|endTop|miniEndTop|startFloat|miniStartFloat|centerFloat|miniCenterFloat|endFloat|miniEndFloat|startDocked|miniStartDocked|centerDocked|miniCenterDocked|endDocked|miniEndDocked|endContained)',
  ),
  'floatingActionButtonAnimator': _PropertySpec({
    'string',
  }, stringPattern: r'(?:scaling|noAnimation)'),
  'persistentFooterAlignment': _PropertySpec(
    {'string'},
    stringPattern:
        r'(?:topStart|topCenter|topEnd|centerStart|center|centerEnd|bottomStart|bottomCenter|bottomEnd)',
  ),
  'onDrawerChanged': _PropertySpec({'callback'}),
  'onEndDrawerChanged': _PropertySpec({'callback'}),
  'backgroundColor': _themeAwareColorProperty,
  'resizeToAvoidBottomInset': _PropertySpec({'boolean'}),
  'primary': _PropertySpec({'boolean'}),
  'drawerDragStartBehavior': _PropertySpec(
    {'enum'},
    enumLibraryUri: 'package:flutter/gestures.dart',
    enumType: 'DragStartBehavior',
    enumValues: {'down', 'start'},
  ),
  'extendBody': _PropertySpec({'boolean'}),
  'drawerBarrierDismissible': _PropertySpec({'boolean'}),
  'extendBodyBehindAppBar': _PropertySpec({'boolean'}),
  'drawerScrimColor': _themeAwareColorProperty,
  'drawerEdgeDragWidth': _PropertySpec({
    'integer',
    'double',
  }, numericBounds: _nonNegativeNumberBounds),
  'drawerEnableOpenDragGesture': _PropertySpec({'boolean'}),
  'endDrawerEnableOpenDragGesture': _PropertySpec({'boolean'}),
  'restorationId': _PropertySpec(
    {'string'},
    minimumStringLength: 1,
    maximumStringLength: 256,
    explicitStringLength: true,
  ),
};

// Source-parity extraction marker retained for the packaged Java gate:
// const _widgetSpecifications
final _widgetSpecifications = <String, _WidgetSpec>{
  'flutter.material.Scaffold': _WidgetSpec(_scaffoldProperties(), {
    'appBar': _optionalPreferredSizeSingleSlot,
    'body': _optionalSingleSlot,
    'floatingActionButton': _optionalSingleSlot,
  }),
  'flutter.material.AppBar': _WidgetSpec(
    _appBarProperties(),
    const {
      'leading': _optionalSingleSlot,
      'title': _optionalSingleSlot,
      'actions': _optionalListSlot,
      'flexibleSpace': _optionalSingleSlot,
      'bottom': _optionalPreferredSizeSingleSlot,
    },
    traits: const {_preferredSizeWidgetTrait},
  ),
  'flutter.widgets.Align': _WidgetSpec(
    {
      'alignment': _PropertySpec({'alignmentGeometry'}),
      'widthFactor': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
      'heightFactor': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.FractionallySizedBox': _WidgetSpec(
    {
      'alignment': _PropertySpec({'alignmentGeometry'}),
      'widthFactor': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
      'heightFactor': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.FittedBox': _WidgetSpec(
    {
      'fit': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'BoxFit',
        enumValues: {
          'fill',
          'contain',
          'cover',
          'fitWidth',
          'fitHeight',
          'none',
          'scaleDown',
        },
      ),
      'alignment': _PropertySpec({'alignmentGeometry'}),
      'clipBehavior': _clipBehaviorProperty,
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.Expanded': _WidgetSpec(
    {
      'flex': _PropertySpec({
        'integer',
      }, numericBounds: _nonNegativeIntegerBounds),
    },
    {'child': _requiredSingleSlot},
  ),
  'flutter.widgets.Flexible': _WidgetSpec(
    {
      'flex': _PropertySpec({
        'integer',
      }, numericBounds: _nonNegativeIntegerBounds),
      'fit': _PropertySpec(
        {'enum'},
        enumLibraryUri: _renderingLibraryUri,
        enumType: 'FlexFit',
        enumValues: {'loose', 'tight'},
      ),
    },
    {'child': _requiredSingleSlot},
  ),
  'flutter.widgets.AspectRatio': _WidgetSpec(
    {
      'aspectRatio': _PropertySpec(
        {'double'},
        required: true,
        creationDefaultFingerprint: 'double:1',
        numericBounds: _positiveDoubleBounds,
      ),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.Baseline': _WidgetSpec(
    {
      'baseline': _PropertySpec(
        {'double'},
        required: true,
        creationDefaultFingerprint: 'double:24',
        numericBounds: _unboundedDoubleBounds,
      ),
      'baselineType': _PropertySpec(
        {'enum'},
        required: true,
        creationDefaultFingerprint: 'enum:TextBaseline:alphabetic',
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'TextBaseline',
        enumValues: {'alphabetic', 'ideographic'},
      ),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.IntrinsicHeight': _WidgetSpec(const {}, {
    'child': _optionalSingleSlot,
  }),
  'flutter.widgets.IntrinsicWidth': _WidgetSpec(
    {
      'stepWidth': _PropertySpec({
        'double',
      }, numericBounds: _nonNegativeDoubleBounds),
      'stepHeight': _PropertySpec({
        'double',
      }, numericBounds: _nonNegativeDoubleBounds),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.Offstage': _WidgetSpec(
    {
      'offstage': _PropertySpec({'boolean'}),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.SizedOverflowBox': _WidgetSpec(
    {
      'size': _PropertySpec(
        {'size'},
        required: true,
        creationDefaultFingerprint: 'size:100,100',
      ),
      'alignment': _PropertySpec({'alignmentGeometry'}),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.RotatedBox': _WidgetSpec(
    {
      'quarterTurns': _PropertySpec(
        {'integer'},
        required: true,
        creationDefaultFingerprint: 'integer:1',
        numericBounds: _signedPortableIntegerBounds,
      ),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.Transform': _WidgetSpec(
    {
      'transform': _PropertySpec(
        {'matrix4'},
        required: true,
        creationDefaultFingerprint: 'matrix4:1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1',
      ),
      'origin': _PropertySpec({'offset'}),
      'alignment': _PropertySpec({'alignmentGeometry'}),
      'transformHitTests': _PropertySpec({'boolean'}),
      'filterQuality': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'FilterQuality',
        enumValues: {'none', 'low', 'medium', 'high'},
      ),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.Opacity': _WidgetSpec(
    {
      'opacity': _PropertySpec(
        {'double'},
        required: true,
        creationDefaultFingerprint: 'double:1',
        numericBounds: _zeroToOneDoubleBounds,
      ),
      'alwaysIncludeSemantics': _PropertySpec({'boolean'}),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.Column': _WidgetSpec(
    {
      'mainAxisAlignment': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
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
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'MainAxisSize',
        enumValues: {'min', 'max'},
      ),
      'crossAxisAlignment': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'CrossAxisAlignment',
        enumValues: {'start', 'end', 'center', 'stretch', 'baseline'},
      ),
      'textDirection': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'TextDirection',
        enumValues: {'rtl', 'ltr'},
      ),
      'verticalDirection': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'VerticalDirection',
        enumValues: {'up', 'down'},
      ),
      'textBaseline': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'TextBaseline',
        enumValues: {'alphabetic', 'ideographic'},
      ),
      'spacing': _PropertySpec({
        'double',
      }, numericBounds: _nonNegativeDoubleBounds),
    },
    {'children': _optionalListSlot},
  ),
  'flutter.widgets.Row': _WidgetSpec(
    {
      'mainAxisAlignment': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
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
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'MainAxisSize',
        enumValues: {'min', 'max'},
      ),
      'crossAxisAlignment': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'CrossAxisAlignment',
        enumValues: {'start', 'end', 'center', 'stretch', 'baseline'},
      ),
      'textDirection': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'TextDirection',
        enumValues: {'rtl', 'ltr'},
      ),
      'verticalDirection': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'VerticalDirection',
        enumValues: {'up', 'down'},
      ),
      'textBaseline': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'TextBaseline',
        enumValues: {'alphabetic', 'ideographic'},
      ),
      'spacing': _PropertySpec({
        'double',
      }, numericBounds: _nonNegativeDoubleBounds),
    },
    {'children': _optionalListSlot},
  ),
  'flutter.widgets.Wrap': _WidgetSpec(
    {
      'direction': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'Axis',
        enumValues: {'horizontal', 'vertical'},
      ),
      'alignment': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'WrapAlignment',
        enumValues: {
          'start',
          'end',
          'center',
          'spaceBetween',
          'spaceAround',
          'spaceEvenly',
        },
      ),
      'spacing': _PropertySpec({
        'double',
      }, numericBounds: _unboundedDoubleBounds),
      'runAlignment': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'WrapAlignment',
        enumValues: {
          'start',
          'end',
          'center',
          'spaceBetween',
          'spaceAround',
          'spaceEvenly',
        },
      ),
      'runSpacing': _PropertySpec({
        'double',
      }, numericBounds: _unboundedDoubleBounds),
      'crossAxisAlignment': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'WrapCrossAlignment',
        enumValues: {'start', 'end', 'center'},
      ),
      'textDirection': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'TextDirection',
        enumValues: {'rtl', 'ltr'},
      ),
      'verticalDirection': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'VerticalDirection',
        enumValues: {'up', 'down'},
      ),
      'clipBehavior': _clipBehaviorProperty,
    },
    {'children': _optionalListSlot},
  ),
  'flutter.widgets.Stack': _WidgetSpec(
    {
      'alignment': _PropertySpec({'alignmentGeometry'}),
      'textDirection': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'TextDirection',
        enumValues: {'rtl', 'ltr'},
      ),
      'fit': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'StackFit',
        enumValues: {'loose', 'expand', 'passthrough'},
      ),
      'clipBehavior': _clipBehaviorProperty,
    },
    {'children': _optionalListSlot},
  ),
  'flutter.widgets.Padding': _WidgetSpec(
    {
      'padding': _PropertySpec(
        {'edgeInsets', 'edgeInsetsDirectional'},
        required: true,
        creationDefaultFingerprint: 'edgeInsets:16,16,16,16',
        numericBounds: _nonNegativeEdgeInsetsBounds,
        edgeInsetsNonNegative: true,
      ),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.Center': _WidgetSpec(
    {
      'widthFactor': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
      'heightFactor': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.Container': _WidgetSpec(
    {
      'alignment': _PropertySpec({'alignmentGeometry'}),
      'padding': _PropertySpec(
        {'edgeInsets', 'edgeInsetsDirectional'},
        numericBounds: _nonNegativeEdgeInsetsBounds,
        edgeInsetsNonNegative: true,
      ),
      'color': _themeAwareColorProperty,
      'isAntiAlias': _PropertySpec({'boolean'}),
      'decoration': _PropertySpec({
        'boxDecoration',
      }, themeTokens: canvasColorSchemeThemeTokens),
      'foregroundDecoration': _PropertySpec({
        'boxDecoration',
      }, themeTokens: canvasColorSchemeThemeTokens),
      'width': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
      'height': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
      'constraints': _PropertySpec({'boxConstraints'}),
      'margin': _PropertySpec(
        {'edgeInsets', 'edgeInsetsDirectional'},
        numericBounds: _nonNegativeEdgeInsetsBounds,
        edgeInsetsNonNegative: true,
      ),
      'transform': _PropertySpec({'matrix4'}),
      'transformAlignment': _PropertySpec({'alignmentGeometry'}),
      'clipBehavior': _clipBehaviorProperty,
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.ConstrainedBox': _WidgetSpec(
    {
      'constraints': _PropertySpec(
        {'boxConstraints'},
        required: true,
        creationDefaultFingerprint: 'boxConstraints:0,inf,0,inf',
      ),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.UnconstrainedBox': _WidgetSpec(
    {
      'textDirection': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'TextDirection',
        enumValues: {'rtl', 'ltr'},
      ),
      'alignment': _PropertySpec({'alignmentGeometry'}),
      'constrainedAxis': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'Axis',
        enumValues: {'horizontal', 'vertical'},
      ),
      'clipBehavior': _clipBehaviorProperty,
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.LimitedBox': _WidgetSpec(
    {
      'maxWidth': _PropertySpec({
        'double',
      }, numericBounds: _nonNegativeDoubleBounds),
      'maxHeight': _PropertySpec({
        'double',
      }, numericBounds: _nonNegativeDoubleBounds),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.OverflowBox': _WidgetSpec(
    {
      'alignment': _PropertySpec({'alignmentGeometry'}),
      'minWidth': _PropertySpec({
        'double',
      }, numericBounds: _nonNegativeDoubleBounds),
      'maxWidth': _PropertySpec({
        'double',
      }, numericBounds: _nonNegativeDoubleBounds),
      'minHeight': _PropertySpec({
        'double',
      }, numericBounds: _nonNegativeDoubleBounds),
      'maxHeight': _PropertySpec({
        'double',
      }, numericBounds: _nonNegativeDoubleBounds),
      'fit': _PropertySpec(
        {'enum'},
        enumLibraryUri: _renderingLibraryUri,
        enumType: 'OverflowBoxFit',
        enumValues: {'max', 'deferToChild'},
      ),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.Icon': _WidgetSpec({
    'icon': _PropertySpec(
      {'iconData'},
      required: true,
      creationDefaultFingerprint: 'iconData:58873:TWF0ZXJpYWxJY29ucw:-:0:-',
      materialIconsOnly: true,
    ),
    'size': _PropertySpec({
      'integer',
      'double',
    }, numericBounds: _nonNegativeNumberBounds),
    'fill': _PropertySpec({'double'}, numericBounds: _zeroToOneDoubleBounds),
    'weight': _PropertySpec({
      'double',
    }, numericBounds: _positiveFontVariationBounds),
    'grade': _PropertySpec({
      'double',
    }, numericBounds: _gradeFontVariationBounds),
    'opticalSize': _PropertySpec({
      'double',
    }, numericBounds: _positiveFontVariationBounds),
    'color': _PropertySpec({
      'color',
      'themeToken',
    }, themeTokens: canvasColorSchemeThemeTokens),
    'shadows': _PropertySpec({'shadowList'}),
    'semanticLabel': _PropertySpec({'string'}),
    'textDirection': _PropertySpec(
      {'enum'},
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'TextDirection',
      enumValues: {'rtl', 'ltr'},
    ),
    'applyTextScaling': _PropertySpec({'boolean'}),
    'blendMode': _PropertySpec(
      {'enum'},
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'BlendMode',
      enumValues: _blendModes,
    ),
    'fontWeight': _fontWeightProperty,
  }, {}),
  'flutter.widgets.Image': _WidgetSpec({
    'image': _PropertySpec({'imageProvider'}, required: true),
    'frameBuilder': _PropertySpec({'callback'}),
    'loadingBuilder': _PropertySpec({'callback'}),
    'errorBuilder': _PropertySpec({'callback'}),
    'semanticLabel': _PropertySpec({'string'}),
    'excludeFromSemantics': _PropertySpec({'boolean'}),
    'width': _PropertySpec({
      'integer',
      'double',
    }, numericBounds: _nonNegativeNumberBounds),
    'height': _PropertySpec({
      'integer',
      'double',
    }, numericBounds: _nonNegativeNumberBounds),
    'color': _themeAwareColorProperty,
    'opacity': _PropertySpec({'double'}, numericBounds: _zeroToOneDoubleBounds),
    'colorBlendMode': _PropertySpec(
      {'enum'},
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'BlendMode',
      enumValues: _blendModes,
    ),
    'fit': _PropertySpec(
      {'enum'},
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'BoxFit',
      enumValues: {
        'fill',
        'contain',
        'cover',
        'fitWidth',
        'fitHeight',
        'none',
        'scaleDown',
      },
    ),
    'alignment': _PropertySpec({'alignmentGeometry'}),
    'repeat': _PropertySpec(
      {'enum'},
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'ImageRepeat',
      enumValues: {'repeat', 'repeatX', 'repeatY', 'noRepeat'},
    ),
    'centerSliceLeft': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'centerSliceTop': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'centerSliceRight': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'centerSliceBottom': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'matchTextDirection': _PropertySpec({'boolean'}),
    'gaplessPlayback': _PropertySpec({'boolean'}),
    'isAntiAlias': _PropertySpec({'boolean'}),
    'filterQuality': _PropertySpec(
      {'enum'},
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'FilterQuality',
      enumValues: {'none', 'low', 'medium', 'high'},
    ),
  }, {}),
  'flutter.widgets.ListBody': _WidgetSpec(
    {
      'mainAxis': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'Axis',
        enumValues: {'horizontal', 'vertical'},
      ),
      'reverse': _PropertySpec({'boolean'}),
    },
    {'children': _optionalListSlot},
  ),
  'flutter.widgets.ListView': _WidgetSpec(
    {
      'scrollDirection': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'Axis',
        enumValues: {'horizontal', 'vertical'},
      ),
      'reverse': _PropertySpec({'boolean'}),
      'primary': _PropertySpec({'boolean'}),
      'physics': _PropertySpec(
        {'string'},
        stringPattern:
            r'(?:alwaysScrollable|bouncing|clamping|neverScrollable|page|rangeMaintaining)',
      ),
      'shrinkWrap': _PropertySpec({'boolean'}),
      'padding': _PropertySpec(
        {'edgeInsets', 'edgeInsetsDirectional'},
        numericBounds: _nonNegativeEdgeInsetsBounds,
        edgeInsetsNonNegative: true,
      ),
      'itemExtent': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
      'addAutomaticKeepAlives': _PropertySpec({'boolean'}),
      'addRepaintBoundaries': _PropertySpec({'boolean'}),
      'addSemanticIndexes': _PropertySpec({'boolean'}),
      'scrollCacheExtent': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
      'semanticChildCount': _PropertySpec({
        'integer',
      }, numericBounds: _nonNegativeIntegerBounds),
      'dragStartBehavior': _PropertySpec(
        {'enum'},
        enumLibraryUri: _gesturesLibraryUri,
        enumType: 'DragStartBehavior',
        enumValues: {'down', 'start'},
      ),
      'keyboardDismissBehavior': _PropertySpec(
        {'enum'},
        enumLibraryUri: _widgetsLibraryUri,
        enumType: 'ScrollViewKeyboardDismissBehavior',
        enumValues: {'manual', 'onDrag'},
      ),
      'restorationId': _PropertySpec(
        {'string'},
        minimumStringLength: 1,
        maximumStringLength: 256,
        explicitStringLength: true,
      ),
      'clipBehavior': _clipBehaviorProperty,
      'hitTestBehavior': _PropertySpec(
        {'enum'},
        enumLibraryUri: _renderingLibraryUri,
        enumType: 'HitTestBehavior',
        enumValues: {'deferToChild', 'opaque', 'translucent'},
      ),
    },
    {'children': _optionalListSlot},
  ),
  'flutter.widgets.SizedBox': _WidgetSpec(
    {
      'width': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
      'height': _PropertySpec({
        'integer',
        'double',
      }, numericBounds: _nonNegativeNumberBounds),
    },
    {'child': _optionalSingleSlot},
  ),
  'flutter.widgets.Spacer': _WidgetSpec({
    'flex': _PropertySpec({'integer'}, numericBounds: _positiveIntegerBounds),
  }, const {}),
  'flutter.material.ElevatedButton': _WidgetSpec(
    _elevatedButtonProperties(),
    const {'child': _requiredEmptySingleSlot},
  ),
  'flutter.material.TextField': _WidgetSpec({
    'keyboardType': _PropertySpec(
      {'string'},
      stringPattern:
          r'(?:text|multiline|number|numberSigned|numberDecimal|numberSignedDecimal|phone|datetime|emailAddress|url|visiblePassword|name|streetAddress|none|webSearch|twitter)',
    ),
    'textInputAction': _PropertySpec(
      {'enum'},
      enumLibraryUri: _servicesLibraryUri,
      enumType: 'TextInputAction',
      enumValues: {
        'none',
        'unspecified',
        'done',
        'go',
        'search',
        'send',
        'next',
        'previous',
        'continueAction',
        'join',
        'route',
        'emergencyCall',
        'newline',
      },
    ),
    'textCapitalization': _PropertySpec(
      {'enum'},
      enumLibraryUri: _servicesLibraryUri,
      enumType: 'TextCapitalization',
      enumValues: {'words', 'sentences', 'characters', 'none'},
    ),
    'textAlign': _PropertySpec(
      {'enum'},
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'TextAlign',
      enumValues: {'left', 'right', 'center', 'justify', 'start', 'end'},
    ),
    'textAlignVertical': _PropertySpec({
      'string',
    }, stringPattern: r'(?:top|center|bottom)'),
    'textDirection': _PropertySpec(
      {'enum'},
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'TextDirection',
      enumValues: {'rtl', 'ltr'},
    ),
    'readOnly': _PropertySpec({'boolean'}),
    'showCursor': _PropertySpec({'boolean'}),
    'autofocus': _PropertySpec({'boolean'}),
    'obscuringCharacter': _PropertySpec({
      'string',
    }, stringPattern: r'[\u0000-\uD7FF\uE000-\uFFFF]'),
    'obscureText': _PropertySpec({'boolean'}),
    'autocorrect': _PropertySpec({'boolean'}),
    'smartDashesType': _PropertySpec(
      {'enum'},
      enumLibraryUri: _servicesLibraryUri,
      enumType: 'SmartDashesType',
      enumValues: {'disabled', 'enabled'},
    ),
    'smartQuotesType': _PropertySpec(
      {'enum'},
      enumLibraryUri: _servicesLibraryUri,
      enumType: 'SmartQuotesType',
      enumValues: {'disabled', 'enabled'},
    ),
    'enableSuggestions': _PropertySpec({'boolean'}),
    'maxLines': _PropertySpec({
      'integer',
    }, numericBounds: _positiveIntegerBounds),
    'minLines': _PropertySpec({
      'integer',
    }, numericBounds: _positiveIntegerBounds),
    'expands': _PropertySpec({'boolean'}),
    'maxLength': _PropertySpec({
      'integer',
    }, numericBounds: _textFieldMaxLengthIntegerBounds),
    'maxLengthEnforcement': _PropertySpec(
      {'enum'},
      enumLibraryUri: _servicesLibraryUri,
      enumType: 'MaxLengthEnforcement',
      enumValues: {'none', 'enforced', 'truncateAfterCompositionEnds'},
    ),
    'onChanged': _PropertySpec({'callback'}),
    'onEditingComplete': _PropertySpec({'callback'}),
    'onSubmitted': _PropertySpec({'callback'}),
    'onAppPrivateCommand': _PropertySpec({'callback'}),
    'enabled': _PropertySpec({'boolean'}),
    'ignorePointers': _PropertySpec({'boolean'}),
    'cursorWidth': _PropertySpec({
      'integer',
      'double',
    }, numericBounds: _nonNegativeNumberBounds),
    'cursorHeight': _PropertySpec({
      'integer',
      'double',
    }, numericBounds: _nonNegativeNumberBounds),
    'cursorRadiusX': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'cursorRadiusY': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'cursorOpacityAnimates': _PropertySpec({'boolean'}),
    'cursorColor': _themeAwareColorProperty,
    'cursorErrorColor': _themeAwareColorProperty,
    'selectionHeightStyle': _PropertySpec(
      {'enum'},
      enumLibraryUri: _dartUiLibraryUri,
      enumType: 'BoxHeightStyle',
      enumValues: {
        'tight',
        'max',
        'includeLineSpacingMiddle',
        'includeLineSpacingTop',
        'includeLineSpacingBottom',
        'strut',
      },
    ),
    'selectionWidthStyle': _PropertySpec(
      {'enum'},
      enumLibraryUri: _dartUiLibraryUri,
      enumType: 'BoxWidthStyle',
      enumValues: {'tight', 'max'},
    ),
    'keyboardAppearance': _brightnessProperty,
    'scrollPaddingLeft': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'scrollPaddingTop': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'scrollPaddingRight': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'scrollPaddingBottom': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'dragStartBehavior': _PropertySpec(
      {'enum'},
      enumLibraryUri: _gesturesLibraryUri,
      enumType: 'DragStartBehavior',
      enumValues: {'down', 'start'},
    ),
    'enableInteractiveSelection': _PropertySpec({'boolean'}),
    'selectAllOnFocus': _PropertySpec({'boolean'}),
    'onTap': _PropertySpec({'callback'}),
    'onTapAlwaysCalled': _PropertySpec({'boolean'}),
    'onTapOutside': _PropertySpec({'callback'}),
    'onTapUpOutside': _PropertySpec({'callback'}),
    'mouseCursor': _PropertySpec(
      {'string'},
      stringPattern:
          r'(?:none|basic|click|forbidden|wait|progress|contextMenu|help|text|verticalText|cell|precise|move|grab|grabbing|noDrop|alias|copy|disappearing|allScroll|resizeLeftRight|resizeUpDown|resizeUpLeftDownRight|resizeUpRightDownLeft|resizeUp|resizeDown|resizeLeft|resizeRight|resizeUpLeft|resizeUpRight|resizeDownLeft|resizeDownRight|resizeColumn|resizeRow|zoomIn|zoomOut)',
    ),
    'clipBehavior': _clipBehaviorProperty,
    'restorationId': _PropertySpec(
      {'string'},
      minimumStringLength: 1,
      maximumStringLength: 256,
      explicitStringLength: true,
    ),
    'stylusHandwritingEnabled': _PropertySpec({'boolean'}),
    'enableIMEPersonalizedLearning': _PropertySpec({'boolean'}),
    'enableInlinePrediction': _PropertySpec({'boolean'}),
    'canRequestFocus': _PropertySpec({'boolean'}),
  }, {}),
  'flutter.widgets.Text': _WidgetSpec({
    'data': _PropertySpec(
      {'string'},
      required: true,
      creationDefaultFingerprint: 'string:VGV4dA',
    ),
    'textAlign': _PropertySpec(
      {'enum'},
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'TextAlign',
      enumValues: {'start', 'end', 'left', 'right', 'center', 'justify'},
    ),
    'textDirection': _PropertySpec(
      {'enum'},
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'TextDirection',
      enumValues: {'rtl', 'ltr'},
    ),
    'softWrap': _PropertySpec({'boolean'}),
    'maxLines': _PropertySpec({
      'integer',
    }, numericBounds: _positiveIntegerBounds),
    'overflow': _textOverflowProperty,
    'semanticsLabel': _PropertySpec({'string'}),
    'semanticsIdentifier': _PropertySpec({'string'}),
    'textWidthBasis': _PropertySpec(
      {'enum'},
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'TextWidthBasis',
      enumValues: {'parent', 'longestLine'},
    ),
    'selectionColor': _PropertySpec({
      'color',
      'themeToken',
    }, themeTokens: canvasColorSchemeThemeTokens),
    'localeLanguageCode': _PropertySpec({
      'string',
    }, stringPattern: r'(?:[a-z]{2,3}|[a-z]{5,8})'),
    'localeScriptCode': _PropertySpec({
      'string',
    }, stringPattern: r'[A-Z][a-z]{3}'),
    'localeCountryCode': _PropertySpec({
      'string',
    }, stringPattern: r'(?:[A-Z]{2}|[0-9]{3})'),
    'textScalerFactor': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
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
    'styleFontSize': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'styleFontWeight': _fontWeightProperty,
    'styleFontStyle': _fontStyleProperty,
    'styleLetterSpacing': _PropertySpec({
      'double',
    }, numericBounds: _unboundedDoubleBounds),
    'styleWordSpacing': _PropertySpec({
      'double',
    }, numericBounds: _unboundedDoubleBounds),
    'styleTextBaseline': _textBaselineProperty,
    'styleHeight': _PropertySpec({
      'double',
    }, numericBounds: _unboundedDoubleBounds),
    'styleLeadingDistribution': _textLeadingDistributionProperty,
    'styleLocaleLanguageCode': _PropertySpec({
      'string',
    }, stringPattern: r'(?:[a-z]{2,3}|[a-z]{5,8})'),
    'styleLocaleScriptCode': _PropertySpec({
      'string',
    }, stringPattern: r'[A-Z][a-z]{3}'),
    'styleLocaleCountryCode': _PropertySpec({
      'string',
    }, stringPattern: r'(?:[A-Z]{2}|[0-9]{3})'),
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
      enumLibraryUri: _widgetsLibraryUri,
      enumType: 'TextDecorationStyle',
      enumValues: {'solid', 'double', 'dotted', 'dashed', 'wavy'},
    ),
    'styleDecorationThickness': _PropertySpec({
      'double',
    }, numericBounds: _unboundedDoubleBounds),
    'styleDebugLabel': _PropertySpec({'string'}),
    'styleFontFamily': _fontNameProperty,
    'styleFontFamilyFallback': _fontFamilyFallbackProperty,
    'stylePackage': _fontNameProperty,
    'styleOverflow': _textOverflowProperty,
    'strutFontFamily': _fontNameProperty,
    'strutFontFamilyFallback': _fontFamilyFallbackProperty,
    'strutFontSize': _PropertySpec({
      'double',
    }, numericBounds: _positiveDoubleBounds),
    'strutHeight': _PropertySpec({
      'double',
    }, numericBounds: _unboundedDoubleBounds),
    'strutLeadingDistribution': _textLeadingDistributionProperty,
    'strutLeading': _PropertySpec({
      'double',
    }, numericBounds: _nonNegativeDoubleBounds),
    'strutFontWeight': _fontWeightProperty,
    'strutFontStyle': _fontStyleProperty,
    'strutForceHeight': _PropertySpec({'boolean'}),
    'strutDebugLabel': _PropertySpec({'string'}),
    'strutPackage': _fontNameProperty,
  }, {}),
};

const String canvasReviewedWidgetSchemaContract = '''

W|flutter.material.AppBar
P|actionsIconThemeApplyTextScaling|boolean|0|-|-|boolean:any
P|actionsIconThemeColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|actionsIconThemeFill|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|actionsIconThemeGrade|double|0|-|double:-32768:1:32768:0|double:range:-32768:1:32768:0
P|actionsIconThemeOpacity|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|actionsIconThemeOpticalSize|double|0|-|double:0:0:32768:0|double:range:0:0:32768:0
P|actionsIconThemeShadows|shadowList|0|-|-|shadowList:shadowTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|actionsIconThemeSize|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|actionsIconThemeWeight|double|0|-|double:0:0:32768:0|double:range:0:0:32768:0
P|actionsPadding|edgeInsets,edgeInsetsDirectional|0|-|edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|edgeInsets:edgeInsets:1:0:1:*:1;edgeInsetsDirectional:edgeInsets:1:0:1:*:1
P|animateColor|boolean|0|-|-|boolean:any
P|automaticallyImplyActions|boolean|0|-|-|boolean:any
P|automaticallyImplyLeading|boolean|0|-|-|boolean:any
P|backgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|bottomOpacity|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|centerTitle|boolean|0|-|-|boolean:any
P|clipBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none
P|elevation|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|excludeHeaderSemantics|boolean|0|-|-|boolean:any
P|forceMaterialTransparency|boolean|0|-|-|boolean:any
P|foregroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|iconThemeApplyTextScaling|boolean|0|-|-|boolean:any
P|iconThemeColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|iconThemeFill|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|iconThemeGrade|double|0|-|double:-32768:1:32768:0|double:range:-32768:1:32768:0
P|iconThemeOpacity|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|iconThemeOpticalSize|double|0|-|double:0:0:32768:0|double:range:0:0:32768:0
P|iconThemeShadows|shadowList|0|-|-|shadowList:shadowTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|iconThemeSize|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|iconThemeWeight|double|0|-|double:0:0:32768:0|double:range:0:0:32768:0
P|leadingWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|notificationPredicate|string|0|-|-|string:pattern:KD86ZGVmYXVsdHxkZXB0aFplcm98YWxsKQ
P|primary|boolean|0|-|-|boolean:any
P|scrolledUnderElevation|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|shadowColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|shapeCircleEccentricity|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|shapeKind|string|0|-|-|string:pattern:KD86cm91bmRlZFJlY3RhbmdsZXxzdGFkaXVtfGNpcmNsZXxiZXZlbGVkUmVjdGFuZ2xlfGNvbnRpbnVvdXNSZWN0YW5nbGUp
P|shapeRadiusBottomLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|shapeRadiusBottomRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|shapeRadiusTopLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|shapeRadiusTopRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|shapeSideColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|shapeSideStrokeAlign|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|shapeSideStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:BorderStyle:none,solid
P|shapeSideWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|surfaceTintColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|systemOverlayStyleStatusBarBrightness|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Brightness:dark,light
P|systemOverlayStyleStatusBarColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|systemOverlayStyleStatusBarIconBrightness|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Brightness:dark,light
P|systemOverlayStyleSystemNavigationBarColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|systemOverlayStyleSystemNavigationBarContrastEnforced|boolean|0|-|-|boolean:any
P|systemOverlayStyleSystemNavigationBarDividerColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|systemOverlayStyleSystemNavigationBarIconBrightness|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Brightness:dark,light
P|systemOverlayStyleSystemStatusBarContrastEnforced|boolean|0|-|-|boolean:any
P|titleSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|titleTextStyleBackground|paint|0|-|-|paint:paintTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|titleTextStyleBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|titleTextStyleColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|titleTextStyleDebugLabel|string|0|-|-|string:any
P|titleTextStyleDecorationColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|titleTextStyleDecorationLineThrough|boolean|0|-|-|boolean:any
P|titleTextStyleDecorationOverline|boolean|0|-|-|boolean:any
P|titleTextStyleDecorationStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDecorationStyle:dashed,dotted,double,solid,wavy
P|titleTextStyleDecorationThickness|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|titleTextStyleDecorationUnderline|boolean|0|-|-|boolean:any
P|titleTextStyleFontFamily|string|0|-|-|string:length:1:256
P|titleTextStyleFontFamilyFallback|string|0|-|-|string:length:0:4096
P|titleTextStyleFontFeatures|fontFeatureList|0|-|-|fontFeatureList:any
P|titleTextStyleFontSize|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|titleTextStyleFontStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontStyle:italic,normal
P|titleTextStyleFontVariations|fontVariationList|0|-|-|fontVariationList:fontVariationList
P|titleTextStyleFontWeight|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontWeight:w100,w200,w300,w400,w500,w600,w700,w800,w900
P|titleTextStyleForeground|paint|0|-|-|paint:paintTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|titleTextStyleHeight|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|titleTextStyleInherit|boolean|0|-|-|boolean:any
P|titleTextStyleLeadingDistribution|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextLeadingDistribution:even,proportional
P|titleTextStyleLetterSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|titleTextStyleLocaleCountryCode|string|0|-|-|string:pattern:KD86W0EtWl17Mn18WzAtOV17M30p
P|titleTextStyleLocaleLanguageCode|string|0|-|-|string:pattern:KD86W2Etel17MiwzfXxbYS16XXs1LDh9KQ
P|titleTextStyleLocaleScriptCode|string|0|-|-|string:pattern:W0EtWl1bYS16XXszfQ
P|titleTextStyleOverflow|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextOverflow:clip,ellipsis,fade,visible
P|titleTextStylePackage|string|0|-|-|string:length:1:256
P|titleTextStyleShadows|shadowList|0|-|-|shadowList:shadowTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|titleTextStyleTextBaseline|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextBaseline:alphabetic,ideographic
P|titleTextStyleThemeTextStyle|themeToken|0|-|-|themeToken:tokens:material.textTheme.bodyLarge,material.textTheme.bodyMedium,material.textTheme.bodySmall,material.textTheme.displayLarge,material.textTheme.displayMedium,material.textTheme.displaySmall,material.textTheme.headlineLarge,material.textTheme.headlineMedium,material.textTheme.headlineSmall,material.textTheme.labelLarge,material.textTheme.labelMedium,material.textTheme.labelSmall,material.textTheme.titleLarge,material.textTheme.titleMedium,material.textTheme.titleSmall
P|titleTextStyleWordSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|toolbarHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|toolbarOpacity|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|toolbarTextStyleBackground|paint|0|-|-|paint:paintTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|toolbarTextStyleBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|toolbarTextStyleColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|toolbarTextStyleDebugLabel|string|0|-|-|string:any
P|toolbarTextStyleDecorationColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|toolbarTextStyleDecorationLineThrough|boolean|0|-|-|boolean:any
P|toolbarTextStyleDecorationOverline|boolean|0|-|-|boolean:any
P|toolbarTextStyleDecorationStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDecorationStyle:dashed,dotted,double,solid,wavy
P|toolbarTextStyleDecorationThickness|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|toolbarTextStyleDecorationUnderline|boolean|0|-|-|boolean:any
P|toolbarTextStyleFontFamily|string|0|-|-|string:length:1:256
P|toolbarTextStyleFontFamilyFallback|string|0|-|-|string:length:0:4096
P|toolbarTextStyleFontFeatures|fontFeatureList|0|-|-|fontFeatureList:any
P|toolbarTextStyleFontSize|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|toolbarTextStyleFontStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontStyle:italic,normal
P|toolbarTextStyleFontVariations|fontVariationList|0|-|-|fontVariationList:fontVariationList
P|toolbarTextStyleFontWeight|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontWeight:w100,w200,w300,w400,w500,w600,w700,w800,w900
P|toolbarTextStyleForeground|paint|0|-|-|paint:paintTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|toolbarTextStyleHeight|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|toolbarTextStyleInherit|boolean|0|-|-|boolean:any
P|toolbarTextStyleLeadingDistribution|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextLeadingDistribution:even,proportional
P|toolbarTextStyleLetterSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|toolbarTextStyleLocaleCountryCode|string|0|-|-|string:pattern:KD86W0EtWl17Mn18WzAtOV17M30p
P|toolbarTextStyleLocaleLanguageCode|string|0|-|-|string:pattern:KD86W2Etel17MiwzfXxbYS16XXs1LDh9KQ
P|toolbarTextStyleLocaleScriptCode|string|0|-|-|string:pattern:W0EtWl1bYS16XXszfQ
P|toolbarTextStyleOverflow|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextOverflow:clip,ellipsis,fade,visible
P|toolbarTextStylePackage|string|0|-|-|string:length:1:256
P|toolbarTextStyleShadows|shadowList|0|-|-|shadowList:shadowTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|toolbarTextStyleTextBaseline|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextBaseline:alphabetic,ideographic
P|toolbarTextStyleThemeTextStyle|themeToken|0|-|-|themeToken:tokens:material.textTheme.bodyLarge,material.textTheme.bodyMedium,material.textTheme.bodySmall,material.textTheme.displayLarge,material.textTheme.displayMedium,material.textTheme.displaySmall,material.textTheme.headlineLarge,material.textTheme.headlineMedium,material.textTheme.headlineSmall,material.textTheme.labelLarge,material.textTheme.labelMedium,material.textTheme.labelSmall,material.textTheme.titleLarge,material.textTheme.titleMedium,material.textTheme.titleSmall
P|toolbarTextStyleWordSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|useDefaultSemanticsOrder|boolean|0|-|-|boolean:any
S|actions|list|0|0|10000|any
S|bottom|single|0|0|1|trait:Zmx1dHRlci53aWRnZXRzLlByZWZlcnJlZFNpemVXaWRnZXQ
S|flexibleSpace|single|0|0|1|any
S|leading|single|0|0|1|any
S|title|single|0|0|1|any
W|flutter.material.ElevatedButton
P|autofocus|boolean|0|-|-|boolean:any
P|clipBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none
P|enabled|boolean|0|boolean:true|-|boolean:any
P|onFocusChange|callback|0|-|-|callback:callbackReference
P|onHover|callback|0|-|-|callback:callbackReference
P|onLongPress|callback|0|-|-|callback:callbackReference
P|onPressed|callback|0|-|-|callback:callbackReference
P|styleAlignmentKind|string|0|-|-|string:pattern:KD86cGh5c2ljYWx8ZGlyZWN0aW9uYWwp
P|styleAlignmentX|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleAlignmentY|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleAnimationDurationMs|integer|0|-|integer:0:1:9007199254740991:1|integer:range:0:1:9007199254740991:1
P|styleBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledElevation|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleDisabledFixedHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleDisabledFixedWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleDisabledForegroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledIconColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledIconSize|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleDisabledMaximumHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleDisabledMaximumWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleDisabledMinimumHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleDisabledMinimumWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleDisabledMouseCursor|string|0|-|-|string:pattern:KD86bm9uZXxiYXNpY3xjbGlja3xmb3JiaWRkZW58d2FpdHxwcm9ncmVzc3xjb250ZXh0TWVudXxoZWxwfHRleHR8dmVydGljYWxUZXh0fGNlbGx8cHJlY2lzZXxtb3ZlfGdyYWJ8Z3JhYmJpbmd8bm9Ecm9wfGFsaWFzfGNvcHl8ZGlzYXBwZWFyaW5nfGFsbFNjcm9sbHxyZXNpemVMZWZ0UmlnaHR8cmVzaXplVXBEb3dufHJlc2l6ZVVwTGVmdERvd25SaWdodHxyZXNpemVVcFJpZ2h0RG93bkxlZnR8cmVzaXplVXB8cmVzaXplRG93bnxyZXNpemVMZWZ0fHJlc2l6ZVJpZ2h0fHJlc2l6ZVVwTGVmdHxyZXNpemVVcFJpZ2h0fHJlc2l6ZURvd25MZWZ0fHJlc2l6ZURvd25SaWdodHxyZXNpemVDb2x1bW58cmVzaXplUm93fHpvb21Jbnx6b29tT3V0KQ
P|styleDisabledOverlayColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledPadding|edgeInsets,edgeInsetsDirectional|0|-|edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|edgeInsets:edgeInsets:1:0:1:*:1;edgeInsetsDirectional:edgeInsets:1:0:1:*:1
P|styleDisabledShadowColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledShapeCircleEccentricity|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|styleDisabledShapeKind|string|0|-|-|string:pattern:KD86cm91bmRlZFJlY3RhbmdsZXxyb3VuZGVkU3VwZXJlbGxpcHNlfHN0YWRpdW18Y2lyY2xlfGJldmVsZWRSZWN0YW5nbGV8Y29udGludW91c1JlY3RhbmdsZSk
P|styleDisabledShapeRadiusBottomLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleDisabledShapeRadiusBottomRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleDisabledShapeRadiusTopLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleDisabledShapeRadiusTopRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleDisabledSideColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledSideStrokeAlign|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleDisabledSideStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:BorderStyle:none,solid
P|styleDisabledSideWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleDisabledSurfaceTintColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledTextBackground|paint|0|-|-|paint:paintTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledTextBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledTextDecorationColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledTextDecorationLineThrough|boolean|0|-|-|boolean:any
P|styleDisabledTextDecorationOverline|boolean|0|-|-|boolean:any
P|styleDisabledTextDecorationStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDecorationStyle:dashed,dotted,double,solid,wavy
P|styleDisabledTextDecorationThickness|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleDisabledTextDecorationUnderline|boolean|0|-|-|boolean:any
P|styleDisabledTextFontFamily|string|0|-|-|string:length:1:256
P|styleDisabledTextFontFamilyFallback|string|0|-|-|string:length:0:4096
P|styleDisabledTextFontFeatures|fontFeatureList|0|-|-|fontFeatureList:any
P|styleDisabledTextFontSize|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleDisabledTextFontStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontStyle:italic,normal
P|styleDisabledTextFontVariations|fontVariationList|0|-|-|fontVariationList:fontVariationList
P|styleDisabledTextFontWeight|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontWeight:w100,w200,w300,w400,w500,w600,w700,w800,w900
P|styleDisabledTextHeight|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleDisabledTextInherit|boolean|0|-|-|boolean:any
P|styleDisabledTextLeadingDistribution|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextLeadingDistribution:even,proportional
P|styleDisabledTextLetterSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleDisabledTextLocaleCountryCode|string|0|-|-|string:pattern:KD86W0EtWl17Mn18WzAtOV17M30p
P|styleDisabledTextLocaleLanguageCode|string|0|-|-|string:pattern:KD86W2Etel17MiwzfXxbYS16XXs1LDh9KQ
P|styleDisabledTextLocaleScriptCode|string|0|-|-|string:pattern:W0EtWl1bYS16XXszfQ
P|styleDisabledTextOverflow|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextOverflow:clip,ellipsis,fade,visible
P|styleDisabledTextPackage|string|0|-|-|string:length:1:256
P|styleDisabledTextShadows|shadowList|0|-|-|shadowList:shadowTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDisabledTextTextBaseline|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextBaseline:alphabetic,ideographic
P|styleDisabledTextTheme|themeToken|0|-|-|themeToken:tokens:material.textTheme.bodyLarge,material.textTheme.bodyMedium,material.textTheme.bodySmall,material.textTheme.displayLarge,material.textTheme.displayMedium,material.textTheme.displaySmall,material.textTheme.headlineLarge,material.textTheme.headlineMedium,material.textTheme.headlineSmall,material.textTheme.labelLarge,material.textTheme.labelMedium,material.textTheme.labelSmall,material.textTheme.titleLarge,material.textTheme.titleMedium,material.textTheme.titleSmall
P|styleDisabledTextWordSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleElevation|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleEnableFeedback|boolean|0|-|-|boolean:any
P|styleFixedHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleFixedWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleFocusedBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleFocusedElevation|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleFocusedFixedHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleFocusedFixedWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleFocusedForegroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleFocusedIconColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleFocusedIconSize|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleFocusedMaximumHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleFocusedMaximumWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleFocusedMinimumHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleFocusedMinimumWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleFocusedMouseCursor|string|0|-|-|string:pattern:KD86bm9uZXxiYXNpY3xjbGlja3xmb3JiaWRkZW58d2FpdHxwcm9ncmVzc3xjb250ZXh0TWVudXxoZWxwfHRleHR8dmVydGljYWxUZXh0fGNlbGx8cHJlY2lzZXxtb3ZlfGdyYWJ8Z3JhYmJpbmd8bm9Ecm9wfGFsaWFzfGNvcHl8ZGlzYXBwZWFyaW5nfGFsbFNjcm9sbHxyZXNpemVMZWZ0UmlnaHR8cmVzaXplVXBEb3dufHJlc2l6ZVVwTGVmdERvd25SaWdodHxyZXNpemVVcFJpZ2h0RG93bkxlZnR8cmVzaXplVXB8cmVzaXplRG93bnxyZXNpemVMZWZ0fHJlc2l6ZVJpZ2h0fHJlc2l6ZVVwTGVmdHxyZXNpemVVcFJpZ2h0fHJlc2l6ZURvd25MZWZ0fHJlc2l6ZURvd25SaWdodHxyZXNpemVDb2x1bW58cmVzaXplUm93fHpvb21Jbnx6b29tT3V0KQ
P|styleFocusedOverlayColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleFocusedPadding|edgeInsets,edgeInsetsDirectional|0|-|edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|edgeInsets:edgeInsets:1:0:1:*:1;edgeInsetsDirectional:edgeInsets:1:0:1:*:1
P|styleFocusedShadowColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleFocusedShapeCircleEccentricity|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|styleFocusedShapeKind|string|0|-|-|string:pattern:KD86cm91bmRlZFJlY3RhbmdsZXxyb3VuZGVkU3VwZXJlbGxpcHNlfHN0YWRpdW18Y2lyY2xlfGJldmVsZWRSZWN0YW5nbGV8Y29udGludW91c1JlY3RhbmdsZSk
P|styleFocusedShapeRadiusBottomLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleFocusedShapeRadiusBottomRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleFocusedShapeRadiusTopLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleFocusedShapeRadiusTopRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleFocusedSideColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleFocusedSideStrokeAlign|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleFocusedSideStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:BorderStyle:none,solid
P|styleFocusedSideWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleFocusedSurfaceTintColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleFocusedTextBackground|paint|0|-|-|paint:paintTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleFocusedTextBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleFocusedTextDecorationColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleFocusedTextDecorationLineThrough|boolean|0|-|-|boolean:any
P|styleFocusedTextDecorationOverline|boolean|0|-|-|boolean:any
P|styleFocusedTextDecorationStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDecorationStyle:dashed,dotted,double,solid,wavy
P|styleFocusedTextDecorationThickness|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleFocusedTextDecorationUnderline|boolean|0|-|-|boolean:any
P|styleFocusedTextFontFamily|string|0|-|-|string:length:1:256
P|styleFocusedTextFontFamilyFallback|string|0|-|-|string:length:0:4096
P|styleFocusedTextFontFeatures|fontFeatureList|0|-|-|fontFeatureList:any
P|styleFocusedTextFontSize|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleFocusedTextFontStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontStyle:italic,normal
P|styleFocusedTextFontVariations|fontVariationList|0|-|-|fontVariationList:fontVariationList
P|styleFocusedTextFontWeight|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontWeight:w100,w200,w300,w400,w500,w600,w700,w800,w900
P|styleFocusedTextHeight|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleFocusedTextInherit|boolean|0|-|-|boolean:any
P|styleFocusedTextLeadingDistribution|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextLeadingDistribution:even,proportional
P|styleFocusedTextLetterSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleFocusedTextLocaleCountryCode|string|0|-|-|string:pattern:KD86W0EtWl17Mn18WzAtOV17M30p
P|styleFocusedTextLocaleLanguageCode|string|0|-|-|string:pattern:KD86W2Etel17MiwzfXxbYS16XXs1LDh9KQ
P|styleFocusedTextLocaleScriptCode|string|0|-|-|string:pattern:W0EtWl1bYS16XXszfQ
P|styleFocusedTextOverflow|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextOverflow:clip,ellipsis,fade,visible
P|styleFocusedTextPackage|string|0|-|-|string:length:1:256
P|styleFocusedTextShadows|shadowList|0|-|-|shadowList:shadowTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleFocusedTextTextBaseline|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextBaseline:alphabetic,ideographic
P|styleFocusedTextTheme|themeToken|0|-|-|themeToken:tokens:material.textTheme.bodyLarge,material.textTheme.bodyMedium,material.textTheme.bodySmall,material.textTheme.displayLarge,material.textTheme.displayMedium,material.textTheme.displaySmall,material.textTheme.headlineLarge,material.textTheme.headlineMedium,material.textTheme.headlineSmall,material.textTheme.labelLarge,material.textTheme.labelMedium,material.textTheme.labelSmall,material.textTheme.titleLarge,material.textTheme.titleMedium,material.textTheme.titleSmall
P|styleFocusedTextWordSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleForegroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredElevation|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleHoveredFixedHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleHoveredFixedWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleHoveredForegroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredIconColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredIconSize|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleHoveredMaximumHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleHoveredMaximumWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleHoveredMinimumHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleHoveredMinimumWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleHoveredMouseCursor|string|0|-|-|string:pattern:KD86bm9uZXxiYXNpY3xjbGlja3xmb3JiaWRkZW58d2FpdHxwcm9ncmVzc3xjb250ZXh0TWVudXxoZWxwfHRleHR8dmVydGljYWxUZXh0fGNlbGx8cHJlY2lzZXxtb3ZlfGdyYWJ8Z3JhYmJpbmd8bm9Ecm9wfGFsaWFzfGNvcHl8ZGlzYXBwZWFyaW5nfGFsbFNjcm9sbHxyZXNpemVMZWZ0UmlnaHR8cmVzaXplVXBEb3dufHJlc2l6ZVVwTGVmdERvd25SaWdodHxyZXNpemVVcFJpZ2h0RG93bkxlZnR8cmVzaXplVXB8cmVzaXplRG93bnxyZXNpemVMZWZ0fHJlc2l6ZVJpZ2h0fHJlc2l6ZVVwTGVmdHxyZXNpemVVcFJpZ2h0fHJlc2l6ZURvd25MZWZ0fHJlc2l6ZURvd25SaWdodHxyZXNpemVDb2x1bW58cmVzaXplUm93fHpvb21Jbnx6b29tT3V0KQ
P|styleHoveredOverlayColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredPadding|edgeInsets,edgeInsetsDirectional|0|-|edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|edgeInsets:edgeInsets:1:0:1:*:1;edgeInsetsDirectional:edgeInsets:1:0:1:*:1
P|styleHoveredShadowColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredShapeCircleEccentricity|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|styleHoveredShapeKind|string|0|-|-|string:pattern:KD86cm91bmRlZFJlY3RhbmdsZXxyb3VuZGVkU3VwZXJlbGxpcHNlfHN0YWRpdW18Y2lyY2xlfGJldmVsZWRSZWN0YW5nbGV8Y29udGludW91c1JlY3RhbmdsZSk
P|styleHoveredShapeRadiusBottomLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleHoveredShapeRadiusBottomRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleHoveredShapeRadiusTopLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleHoveredShapeRadiusTopRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleHoveredSideColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredSideStrokeAlign|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleHoveredSideStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:BorderStyle:none,solid
P|styleHoveredSideWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleHoveredSurfaceTintColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredTextBackground|paint|0|-|-|paint:paintTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredTextBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredTextDecorationColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredTextDecorationLineThrough|boolean|0|-|-|boolean:any
P|styleHoveredTextDecorationOverline|boolean|0|-|-|boolean:any
P|styleHoveredTextDecorationStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDecorationStyle:dashed,dotted,double,solid,wavy
P|styleHoveredTextDecorationThickness|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleHoveredTextDecorationUnderline|boolean|0|-|-|boolean:any
P|styleHoveredTextFontFamily|string|0|-|-|string:length:1:256
P|styleHoveredTextFontFamilyFallback|string|0|-|-|string:length:0:4096
P|styleHoveredTextFontFeatures|fontFeatureList|0|-|-|fontFeatureList:any
P|styleHoveredTextFontSize|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleHoveredTextFontStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontStyle:italic,normal
P|styleHoveredTextFontVariations|fontVariationList|0|-|-|fontVariationList:fontVariationList
P|styleHoveredTextFontWeight|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontWeight:w100,w200,w300,w400,w500,w600,w700,w800,w900
P|styleHoveredTextHeight|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleHoveredTextInherit|boolean|0|-|-|boolean:any
P|styleHoveredTextLeadingDistribution|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextLeadingDistribution:even,proportional
P|styleHoveredTextLetterSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleHoveredTextLocaleCountryCode|string|0|-|-|string:pattern:KD86W0EtWl17Mn18WzAtOV17M30p
P|styleHoveredTextLocaleLanguageCode|string|0|-|-|string:pattern:KD86W2Etel17MiwzfXxbYS16XXs1LDh9KQ
P|styleHoveredTextLocaleScriptCode|string|0|-|-|string:pattern:W0EtWl1bYS16XXszfQ
P|styleHoveredTextOverflow|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextOverflow:clip,ellipsis,fade,visible
P|styleHoveredTextPackage|string|0|-|-|string:length:1:256
P|styleHoveredTextShadows|shadowList|0|-|-|shadowList:shadowTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHoveredTextTextBaseline|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextBaseline:alphabetic,ideographic
P|styleHoveredTextTheme|themeToken|0|-|-|themeToken:tokens:material.textTheme.bodyLarge,material.textTheme.bodyMedium,material.textTheme.bodySmall,material.textTheme.displayLarge,material.textTheme.displayMedium,material.textTheme.displaySmall,material.textTheme.headlineLarge,material.textTheme.headlineMedium,material.textTheme.headlineSmall,material.textTheme.labelLarge,material.textTheme.labelMedium,material.textTheme.labelSmall,material.textTheme.titleLarge,material.textTheme.titleMedium,material.textTheme.titleSmall
P|styleHoveredTextWordSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleIconColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleIconSize|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleMaximumHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleMaximumWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleMinimumHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleMinimumWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|styleMouseCursor|string|0|-|-|string:pattern:KD86bm9uZXxiYXNpY3xjbGlja3xmb3JiaWRkZW58d2FpdHxwcm9ncmVzc3xjb250ZXh0TWVudXxoZWxwfHRleHR8dmVydGljYWxUZXh0fGNlbGx8cHJlY2lzZXxtb3ZlfGdyYWJ8Z3JhYmJpbmd8bm9Ecm9wfGFsaWFzfGNvcHl8ZGlzYXBwZWFyaW5nfGFsbFNjcm9sbHxyZXNpemVMZWZ0UmlnaHR8cmVzaXplVXBEb3dufHJlc2l6ZVVwTGVmdERvd25SaWdodHxyZXNpemVVcFJpZ2h0RG93bkxlZnR8cmVzaXplVXB8cmVzaXplRG93bnxyZXNpemVMZWZ0fHJlc2l6ZVJpZ2h0fHJlc2l6ZVVwTGVmdHxyZXNpemVVcFJpZ2h0fHJlc2l6ZURvd25MZWZ0fHJlc2l6ZURvd25SaWdodHxyZXNpemVDb2x1bW58cmVzaXplUm93fHpvb21Jbnx6b29tT3V0KQ
P|styleOverlayColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePadding|edgeInsets,edgeInsetsDirectional|0|-|edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|edgeInsets:edgeInsets:1:0:1:*:1;edgeInsetsDirectional:edgeInsets:1:0:1:*:1
P|stylePressedBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePressedElevation|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|stylePressedFixedHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|stylePressedFixedWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|stylePressedForegroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePressedIconColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePressedIconSize|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|stylePressedMaximumHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|stylePressedMaximumWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|stylePressedMinimumHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|stylePressedMinimumWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|stylePressedMouseCursor|string|0|-|-|string:pattern:KD86bm9uZXxiYXNpY3xjbGlja3xmb3JiaWRkZW58d2FpdHxwcm9ncmVzc3xjb250ZXh0TWVudXxoZWxwfHRleHR8dmVydGljYWxUZXh0fGNlbGx8cHJlY2lzZXxtb3ZlfGdyYWJ8Z3JhYmJpbmd8bm9Ecm9wfGFsaWFzfGNvcHl8ZGlzYXBwZWFyaW5nfGFsbFNjcm9sbHxyZXNpemVMZWZ0UmlnaHR8cmVzaXplVXBEb3dufHJlc2l6ZVVwTGVmdERvd25SaWdodHxyZXNpemVVcFJpZ2h0RG93bkxlZnR8cmVzaXplVXB8cmVzaXplRG93bnxyZXNpemVMZWZ0fHJlc2l6ZVJpZ2h0fHJlc2l6ZVVwTGVmdHxyZXNpemVVcFJpZ2h0fHJlc2l6ZURvd25MZWZ0fHJlc2l6ZURvd25SaWdodHxyZXNpemVDb2x1bW58cmVzaXplUm93fHpvb21Jbnx6b29tT3V0KQ
P|stylePressedOverlayColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePressedPadding|edgeInsets,edgeInsetsDirectional|0|-|edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|edgeInsets:edgeInsets:1:0:1:*:1;edgeInsetsDirectional:edgeInsets:1:0:1:*:1
P|stylePressedShadowColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePressedShapeCircleEccentricity|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|stylePressedShapeKind|string|0|-|-|string:pattern:KD86cm91bmRlZFJlY3RhbmdsZXxyb3VuZGVkU3VwZXJlbGxpcHNlfHN0YWRpdW18Y2lyY2xlfGJldmVsZWRSZWN0YW5nbGV8Y29udGludW91c1JlY3RhbmdsZSk
P|stylePressedShapeRadiusBottomLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|stylePressedShapeRadiusBottomRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|stylePressedShapeRadiusTopLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|stylePressedShapeRadiusTopRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|stylePressedSideColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePressedSideStrokeAlign|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|stylePressedSideStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:BorderStyle:none,solid
P|stylePressedSideWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|stylePressedSurfaceTintColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePressedTextBackground|paint|0|-|-|paint:paintTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePressedTextBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePressedTextDecorationColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePressedTextDecorationLineThrough|boolean|0|-|-|boolean:any
P|stylePressedTextDecorationOverline|boolean|0|-|-|boolean:any
P|stylePressedTextDecorationStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDecorationStyle:dashed,dotted,double,solid,wavy
P|stylePressedTextDecorationThickness|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|stylePressedTextDecorationUnderline|boolean|0|-|-|boolean:any
P|stylePressedTextFontFamily|string|0|-|-|string:length:1:256
P|stylePressedTextFontFamilyFallback|string|0|-|-|string:length:0:4096
P|stylePressedTextFontFeatures|fontFeatureList|0|-|-|fontFeatureList:any
P|stylePressedTextFontSize|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|stylePressedTextFontStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontStyle:italic,normal
P|stylePressedTextFontVariations|fontVariationList|0|-|-|fontVariationList:fontVariationList
P|stylePressedTextFontWeight|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontWeight:w100,w200,w300,w400,w500,w600,w700,w800,w900
P|stylePressedTextHeight|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|stylePressedTextInherit|boolean|0|-|-|boolean:any
P|stylePressedTextLeadingDistribution|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextLeadingDistribution:even,proportional
P|stylePressedTextLetterSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|stylePressedTextLocaleCountryCode|string|0|-|-|string:pattern:KD86W0EtWl17Mn18WzAtOV17M30p
P|stylePressedTextLocaleLanguageCode|string|0|-|-|string:pattern:KD86W2Etel17MiwzfXxbYS16XXs1LDh9KQ
P|stylePressedTextLocaleScriptCode|string|0|-|-|string:pattern:W0EtWl1bYS16XXszfQ
P|stylePressedTextOverflow|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextOverflow:clip,ellipsis,fade,visible
P|stylePressedTextPackage|string|0|-|-|string:length:1:256
P|stylePressedTextShadows|shadowList|0|-|-|shadowList:shadowTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|stylePressedTextTextBaseline|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextBaseline:alphabetic,ideographic
P|stylePressedTextTheme|themeToken|0|-|-|themeToken:tokens:material.textTheme.bodyLarge,material.textTheme.bodyMedium,material.textTheme.bodySmall,material.textTheme.displayLarge,material.textTheme.displayMedium,material.textTheme.displaySmall,material.textTheme.headlineLarge,material.textTheme.headlineMedium,material.textTheme.headlineSmall,material.textTheme.labelLarge,material.textTheme.labelMedium,material.textTheme.labelSmall,material.textTheme.titleLarge,material.textTheme.titleMedium,material.textTheme.titleSmall
P|stylePressedTextWordSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleShadowColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleShapeCircleEccentricity|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|styleShapeKind|string|0|-|-|string:pattern:KD86cm91bmRlZFJlY3RhbmdsZXxyb3VuZGVkU3VwZXJlbGxpcHNlfHN0YWRpdW18Y2lyY2xlfGJldmVsZWRSZWN0YW5nbGV8Y29udGludW91c1JlY3RhbmdsZSk
P|styleShapeRadiusBottomLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleShapeRadiusBottomRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleShapeRadiusTopLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleShapeRadiusTopRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleSideColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleSideStrokeAlign|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleSideStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:BorderStyle:none,solid
P|styleSideWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleSplashFactory|string|0|-|-|string:pattern:KD86aW5rUmlwcGxlfGlua1NwbGFzaHxpbmtTcGFya2xlfG5vU3BsYXNoKQ
P|styleSurfaceTintColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleTapTargetSize|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL21hdGVyaWFsLmRhcnQ:MaterialTapTargetSize:padded,shrinkWrap
P|styleTextBackground|paint|0|-|-|paint:paintTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleTextBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleTextDecorationColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleTextDecorationLineThrough|boolean|0|-|-|boolean:any
P|styleTextDecorationOverline|boolean|0|-|-|boolean:any
P|styleTextDecorationStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDecorationStyle:dashed,dotted,double,solid,wavy
P|styleTextDecorationThickness|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleTextDecorationUnderline|boolean|0|-|-|boolean:any
P|styleTextFontFamily|string|0|-|-|string:length:1:256
P|styleTextFontFamilyFallback|string|0|-|-|string:length:0:4096
P|styleTextFontFeatures|fontFeatureList|0|-|-|fontFeatureList:any
P|styleTextFontSize|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleTextFontStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontStyle:italic,normal
P|styleTextFontVariations|fontVariationList|0|-|-|fontVariationList:fontVariationList
P|styleTextFontWeight|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontWeight:w100,w200,w300,w400,w500,w600,w700,w800,w900
P|styleTextHeight|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleTextInherit|boolean|0|-|-|boolean:any
P|styleTextLeadingDistribution|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextLeadingDistribution:even,proportional
P|styleTextLetterSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleTextLocaleCountryCode|string|0|-|-|string:pattern:KD86W0EtWl17Mn18WzAtOV17M30p
P|styleTextLocaleLanguageCode|string|0|-|-|string:pattern:KD86W2Etel17MiwzfXxbYS16XXs1LDh9KQ
P|styleTextLocaleScriptCode|string|0|-|-|string:pattern:W0EtWl1bYS16XXszfQ
P|styleTextOverflow|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextOverflow:clip,ellipsis,fade,visible
P|styleTextPackage|string|0|-|-|string:length:1:256
P|styleTextShadows|shadowList|0|-|-|shadowList:shadowTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleTextTextBaseline|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextBaseline:alphabetic,ideographic
P|styleTextTheme|themeToken|0|-|-|themeToken:tokens:material.textTheme.bodyLarge,material.textTheme.bodyMedium,material.textTheme.bodySmall,material.textTheme.displayLarge,material.textTheme.displayMedium,material.textTheme.displaySmall,material.textTheme.headlineLarge,material.textTheme.headlineMedium,material.textTheme.headlineSmall,material.textTheme.labelLarge,material.textTheme.labelMedium,material.textTheme.labelSmall,material.textTheme.titleLarge,material.textTheme.titleMedium,material.textTheme.titleSmall
P|styleTextWordSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleVisualDensityHorizontal|double|0|-|double:-4:1:4:1|double:range:-4:1:4:1
P|styleVisualDensityVertical|double|0|-|double:-4:1:4:1|double:range:-4:1:4:1
S|child|single|1|0|1|any
W|flutter.material.Scaffold
P|backgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|drawerBarrierDismissible|boolean|0|-|-|boolean:any
P|drawerDragStartBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL2dlc3R1cmVzLmRhcnQ:DragStartBehavior:down,start
P|drawerEdgeDragWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|drawerEnableOpenDragGesture|boolean|0|-|-|boolean:any
P|drawerScrimColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|endDrawerEnableOpenDragGesture|boolean|0|-|-|boolean:any
P|extendBody|boolean|0|-|-|boolean:any
P|extendBodyBehindAppBar|boolean|0|-|-|boolean:any
P|floatingActionButtonAnimator|string|0|-|-|string:pattern:KD86c2NhbGluZ3xub0FuaW1hdGlvbik
P|floatingActionButtonLocation|string|0|-|-|string:pattern:KD86c3RhcnRUb3B8bWluaVN0YXJ0VG9wfGNlbnRlclRvcHxtaW5pQ2VudGVyVG9wfGVuZFRvcHxtaW5pRW5kVG9wfHN0YXJ0RmxvYXR8bWluaVN0YXJ0RmxvYXR8Y2VudGVyRmxvYXR8bWluaUNlbnRlckZsb2F0fGVuZEZsb2F0fG1pbmlFbmRGbG9hdHxzdGFydERvY2tlZHxtaW5pU3RhcnREb2NrZWR8Y2VudGVyRG9ja2VkfG1pbmlDZW50ZXJEb2NrZWR8ZW5kRG9ja2VkfG1pbmlFbmREb2NrZWR8ZW5kQ29udGFpbmVkKQ
P|onDrawerChanged|callback|0|-|-|callback:callbackReference
P|onEndDrawerChanged|callback|0|-|-|callback:callbackReference
P|persistentFooterAlignment|string|0|-|-|string:pattern:KD86dG9wU3RhcnR8dG9wQ2VudGVyfHRvcEVuZHxjZW50ZXJTdGFydHxjZW50ZXJ8Y2VudGVyRW5kfGJvdHRvbVN0YXJ0fGJvdHRvbUNlbnRlcnxib3R0b21FbmQp
P|primary|boolean|0|-|-|boolean:any
P|resizeToAvoidBottomInset|boolean|0|-|-|boolean:any
P|restorationId|string|0|-|-|string:length:1:256
S|appBar|single|0|0|1|trait:Zmx1dHRlci53aWRnZXRzLlByZWZlcnJlZFNpemVXaWRnZXQ
S|body|single|0|0|1|any
S|floatingActionButton|single|0|0|1|any
W|flutter.material.TextField
P|autocorrect|boolean|0|-|-|boolean:any
P|autofocus|boolean|0|-|-|boolean:any
P|canRequestFocus|boolean|0|-|-|boolean:any
P|clipBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none
P|cursorColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|cursorErrorColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|cursorHeight|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|cursorOpacityAnimates|boolean|0|-|-|boolean:any
P|cursorRadiusX|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|cursorRadiusY|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|cursorWidth|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|dragStartBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL2dlc3R1cmVzLmRhcnQ:DragStartBehavior:down,start
P|enableIMEPersonalizedLearning|boolean|0|-|-|boolean:any
P|enableInlinePrediction|boolean|0|-|-|boolean:any
P|enableInteractiveSelection|boolean|0|-|-|boolean:any
P|enableSuggestions|boolean|0|-|-|boolean:any
P|enabled|boolean|0|-|-|boolean:any
P|expands|boolean|0|-|-|boolean:any
P|ignorePointers|boolean|0|-|-|boolean:any
P|keyboardAppearance|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Brightness:dark,light
P|keyboardType|string|0|-|-|string:pattern:KD86dGV4dHxtdWx0aWxpbmV8bnVtYmVyfG51bWJlclNpZ25lZHxudW1iZXJEZWNpbWFsfG51bWJlclNpZ25lZERlY2ltYWx8cGhvbmV8ZGF0ZXRpbWV8ZW1haWxBZGRyZXNzfHVybHx2aXNpYmxlUGFzc3dvcmR8bmFtZXxzdHJlZXRBZGRyZXNzfG5vbmV8d2ViU2VhcmNofHR3aXR0ZXIp
P|maxLength|integer|0|-|integer:-1:1:9007199254740991:1|integer:range:-1:1:9007199254740991:1
P|maxLengthEnforcement|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3NlcnZpY2VzLmRhcnQ:MaxLengthEnforcement:enforced,none,truncateAfterCompositionEnds
P|maxLines|integer|0|-|integer:1:1:9007199254740991:1|integer:range:1:1:9007199254740991:1
P|minLines|integer|0|-|integer:1:1:9007199254740991:1|integer:range:1:1:9007199254740991:1
P|mouseCursor|string|0|-|-|string:pattern:KD86bm9uZXxiYXNpY3xjbGlja3xmb3JiaWRkZW58d2FpdHxwcm9ncmVzc3xjb250ZXh0TWVudXxoZWxwfHRleHR8dmVydGljYWxUZXh0fGNlbGx8cHJlY2lzZXxtb3ZlfGdyYWJ8Z3JhYmJpbmd8bm9Ecm9wfGFsaWFzfGNvcHl8ZGlzYXBwZWFyaW5nfGFsbFNjcm9sbHxyZXNpemVMZWZ0UmlnaHR8cmVzaXplVXBEb3dufHJlc2l6ZVVwTGVmdERvd25SaWdodHxyZXNpemVVcFJpZ2h0RG93bkxlZnR8cmVzaXplVXB8cmVzaXplRG93bnxyZXNpemVMZWZ0fHJlc2l6ZVJpZ2h0fHJlc2l6ZVVwTGVmdHxyZXNpemVVcFJpZ2h0fHJlc2l6ZURvd25MZWZ0fHJlc2l6ZURvd25SaWdodHxyZXNpemVDb2x1bW58cmVzaXplUm93fHpvb21Jbnx6b29tT3V0KQ
P|obscureText|boolean|0|-|-|boolean:any
P|obscuringCharacter|string|0|-|-|string:pattern:W1x1MDAwMC1cdUQ3RkZcdUUwMDAtXHVGRkZGXQ
P|onAppPrivateCommand|callback|0|-|-|callback:callbackReference
P|onChanged|callback|0|-|-|callback:callbackReference
P|onEditingComplete|callback|0|-|-|callback:callbackReference
P|onSubmitted|callback|0|-|-|callback:callbackReference
P|onTap|callback|0|-|-|callback:callbackReference
P|onTapAlwaysCalled|boolean|0|-|-|boolean:any
P|onTapOutside|callback|0|-|-|callback:callbackReference
P|onTapUpOutside|callback|0|-|-|callback:callbackReference
P|readOnly|boolean|0|-|-|boolean:any
P|restorationId|string|0|-|-|string:length:1:256
P|scrollPaddingBottom|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|scrollPaddingLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|scrollPaddingRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|scrollPaddingTop|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|selectAllOnFocus|boolean|0|-|-|boolean:any
P|selectionHeightStyle|enum|0|-|-|enum:enum:ZGFydDp1aQ:BoxHeightStyle:includeLineSpacingBottom,includeLineSpacingMiddle,includeLineSpacingTop,max,strut,tight
P|selectionWidthStyle|enum|0|-|-|enum:enum:ZGFydDp1aQ:BoxWidthStyle:max,tight
P|showCursor|boolean|0|-|-|boolean:any
P|smartDashesType|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3NlcnZpY2VzLmRhcnQ:SmartDashesType:disabled,enabled
P|smartQuotesType|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3NlcnZpY2VzLmRhcnQ:SmartQuotesType:disabled,enabled
P|stylusHandwritingEnabled|boolean|0|-|-|boolean:any
P|textAlign|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextAlign:center,end,justify,left,right,start
P|textAlignVertical|string|0|-|-|string:pattern:KD86dG9wfGNlbnRlcnxib3R0b20p
P|textCapitalization|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3NlcnZpY2VzLmRhcnQ:TextCapitalization:characters,none,sentences,words
P|textDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDirection:ltr,rtl
P|textInputAction|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3NlcnZpY2VzLmRhcnQ:TextInputAction:continueAction,done,emergencyCall,go,join,newline,next,none,previous,route,search,send,unspecified
W|flutter.widgets.Align
P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|heightFactor|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|widthFactor|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
S|child|single|0|0|1|any
W|flutter.widgets.AspectRatio
P|aspectRatio|double|1|double:1|double:0:0:*:1|double:range:0:0:*:1
S|child|single|0|0|1|any
W|flutter.widgets.Baseline
P|baseline|double|1|double:24|double:*:1:*:1|double:range:*:1:*:1
P|baselineType|enum|1|enum:TextBaseline:alphabetic|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextBaseline:alphabetic,ideographic
S|child|single|0|0|1|any
W|flutter.widgets.Center
P|heightFactor|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|widthFactor|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
S|child|single|0|0|1|any
W|flutter.widgets.Column
P|crossAxisAlignment|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:CrossAxisAlignment:baseline,center,end,start,stretch
P|mainAxisAlignment|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:MainAxisAlignment:center,end,spaceAround,spaceBetween,spaceEvenly,start
P|mainAxisSize|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:MainAxisSize:max,min
P|spacing|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|textBaseline|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextBaseline:alphabetic,ideographic
P|textDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDirection:ltr,rtl
P|verticalDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:VerticalDirection:down,up
S|children|list|0|0|10000|any
W|flutter.widgets.ConstrainedBox
P|constraints|boxConstraints|1|boxConstraints:0,inf,0,inf|-|boxConstraints:boxConstraints:v2:finiteOrPositiveInfinity
S|child|single|0|0|1|any
W|flutter.widgets.Container
P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|clipBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none
P|color|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|constraints|boxConstraints|0|-|-|boxConstraints:boxConstraints:v2:finiteOrPositiveInfinity
P|decoration|boxDecoration|0|-|-|boxDecoration:boxDecoration:v2:imageProvider:v1:asset,exactAsset:package:exactScale:resize(1..16384,exact,fit,allowUpscaling):decorationImage:v1:onError,colorFilter(mode,matrix20,linearToSrgbGamma,srgbToLinearGamma,saturation),fit,alignment,centerSlice,repeat,matchTextDirection,scale,opacity,filterQuality,invertColors,isAntiAlias:centerSliceFit(except:cover,none):theme=material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|foregroundDecoration|boxDecoration|0|-|-|boxDecoration:boxDecoration:v2:imageProvider:v1:asset,exactAsset:package:exactScale:resize(1..16384,exact,fit,allowUpscaling):decorationImage:v1:onError,colorFilter(mode,matrix20,linearToSrgbGamma,srgbToLinearGamma,saturation),fit,alignment,centerSlice,repeat,matchTextDirection,scale,opacity,filterQuality,invertColors,isAntiAlias:centerSliceFit(except:cover,none):theme=material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|height|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|isAntiAlias|boolean|0|-|-|boolean:any
P|margin|edgeInsets,edgeInsetsDirectional|0|-|edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|edgeInsets:edgeInsets:1:0:1:*:1;edgeInsetsDirectional:edgeInsets:1:0:1:*:1
P|padding|edgeInsets,edgeInsetsDirectional|0|-|edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|edgeInsets:edgeInsets:1:0:1:*:1;edgeInsetsDirectional:edgeInsets:1:0:1:*:1
P|transform|matrix4|0|-|-|matrix4:matrix4
P|transformAlignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|width|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
S|child|single|0|0|1|any
W|flutter.widgets.Expanded
P|flex|integer|0|-|integer:0:1:9007199254740991:1|integer:range:0:1:9007199254740991:1
S|child|single|1|1|1|any
R|flutter.widgets.Expanded|directParentSlot|flutter.widgets.Column|children
R|flutter.widgets.Expanded|directParentSlot|flutter.widgets.Row|children
C|flutter.widgets.Expanded|paletteCreate|wrapExistingChild|child
W|flutter.widgets.FittedBox
P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|clipBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none
P|fit|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:BoxFit:contain,cover,fill,fitHeight,fitWidth,none,scaleDown
S|child|single|0|0|1|any
W|flutter.widgets.Flexible
P|fit|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:FlexFit:loose,tight
P|flex|integer|0|-|integer:0:1:9007199254740991:1|integer:range:0:1:9007199254740991:1
S|child|single|1|1|1|any
R|flutter.widgets.Flexible|directParentSlot|flutter.widgets.Column|children
R|flutter.widgets.Flexible|directParentSlot|flutter.widgets.Row|children
C|flutter.widgets.Flexible|paletteCreate|wrapExistingChild|child
W|flutter.widgets.FractionallySizedBox
P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|heightFactor|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|widthFactor|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
S|child|single|0|0|1|any
W|flutter.widgets.Icon
P|applyTextScaling|boolean|0|-|-|boolean:any
P|blendMode|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:BlendMode:clear,color,colorBurn,colorDodge,darken,difference,dst,dstATop,dstIn,dstOut,dstOver,exclusion,hardLight,hue,lighten,luminosity,modulate,multiply,overlay,plus,saturation,screen,softLight,src,srcATop,srcIn,srcOut,srcOver,xor
P|color|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|fill|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|fontWeight|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontWeight:w100,w200,w300,w400,w500,w600,w700,w800,w900
P|grade|double|0|-|double:-32768:1:32768:0|double:range:-32768:1:32768:0
P|icon|iconData|1|iconData:58873:TWF0ZXJpYWxJY29ucw:-:0:-|-|iconData:materialIcons:3.44.8:058e0af2c2:8825:ba88e3e23962ada6537523aa113811d9719b988412815bf084f50a0aa78137f0
P|opticalSize|double|0|-|double:0:0:32768:0|double:range:0:0:32768:0
P|semanticLabel|string|0|-|-|string:any
P|shadows|shadowList|0|-|-|shadowList:shadowTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|size|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|textDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDirection:ltr,rtl
P|weight|double|0|-|double:0:0:32768:0|double:range:0:0:32768:0
W|flutter.widgets.Image
P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|centerSliceBottom|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|centerSliceLeft|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|centerSliceRight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|centerSliceTop|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|color|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|colorBlendMode|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:BlendMode:clear,color,colorBurn,colorDodge,darken,difference,dst,dstATop,dstIn,dstOut,dstOver,exclusion,hardLight,hue,lighten,luminosity,modulate,multiply,overlay,plus,saturation,screen,softLight,src,srcATop,srcIn,srcOut,srcOver,xor
P|errorBuilder|callback|0|-|-|callback:callbackReference
P|excludeFromSemantics|boolean|0|-|-|boolean:any
P|filterQuality|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FilterQuality:high,low,medium,none
P|fit|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:BoxFit:contain,cover,fill,fitHeight,fitWidth,none,scaleDown
P|frameBuilder|callback|0|-|-|callback:callbackReference
P|gaplessPlayback|boolean|0|-|-|boolean:any
P|height|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|image|imageProvider|1|-|-|imageProvider:imageProvider:v1:asset,exactAsset:package:exactScale:resize(1..16384,exact,fit,allowUpscaling)
P|isAntiAlias|boolean|0|-|-|boolean:any
P|loadingBuilder|callback|0|-|-|callback:callbackReference
P|matchTextDirection|boolean|0|-|-|boolean:any
P|opacity|double|0|-|double:0:1:1:1|double:range:0:1:1:1
P|repeat|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:ImageRepeat:noRepeat,repeat,repeatX,repeatY
P|semanticLabel|string|0|-|-|string:any
P|width|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
W|flutter.widgets.IntrinsicHeight
S|child|single|0|0|1|any
W|flutter.widgets.IntrinsicWidth
P|stepHeight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|stepWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1
S|child|single|0|0|1|any
W|flutter.widgets.LimitedBox
P|maxHeight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|maxWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1
S|child|single|0|0|1|any
W|flutter.widgets.ListBody
P|mainAxis|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Axis:horizontal,vertical
P|reverse|boolean|0|-|-|boolean:any
S|children|list|0|0|10000|any
W|flutter.widgets.ListView
P|addAutomaticKeepAlives|boolean|0|-|-|boolean:any
P|addRepaintBoundaries|boolean|0|-|-|boolean:any
P|addSemanticIndexes|boolean|0|-|-|boolean:any
P|clipBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none
P|dragStartBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL2dlc3R1cmVzLmRhcnQ:DragStartBehavior:down,start
P|hitTestBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:HitTestBehavior:deferToChild,opaque,translucent
P|itemExtent|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|keyboardDismissBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:ScrollViewKeyboardDismissBehavior:manual,onDrag
P|padding|edgeInsets,edgeInsetsDirectional|0|-|edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|edgeInsets:edgeInsets:1:0:1:*:1;edgeInsetsDirectional:edgeInsets:1:0:1:*:1
P|physics|string|0|-|-|string:pattern:KD86YWx3YXlzU2Nyb2xsYWJsZXxib3VuY2luZ3xjbGFtcGluZ3xuZXZlclNjcm9sbGFibGV8cGFnZXxyYW5nZU1haW50YWluaW5nKQ
P|primary|boolean|0|-|-|boolean:any
P|restorationId|string|0|-|-|string:length:1:256
P|reverse|boolean|0|-|-|boolean:any
P|scrollCacheExtent|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|scrollDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Axis:horizontal,vertical
P|semanticChildCount|integer|0|-|integer:0:1:9007199254740991:1|integer:range:0:1:9007199254740991:1
P|shrinkWrap|boolean|0|-|-|boolean:any
S|children|list|0|0|10000|any
W|flutter.widgets.Offstage
P|offstage|boolean|0|-|-|boolean:any
S|child|single|0|0|1|any
W|flutter.widgets.Opacity
P|alwaysIncludeSemantics|boolean|0|-|-|boolean:any
P|opacity|double|1|double:1|double:0:1:1:1|double:range:0:1:1:1
S|child|single|0|0|1|any
W|flutter.widgets.OverflowBox
P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|fit|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3JlbmRlcmluZy5kYXJ0:OverflowBoxFit:deferToChild,max
P|maxHeight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|maxWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|minHeight|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|minWidth|double|0|-|double:0:1:*:1|double:range:0:1:*:1
S|child|single|0|0|1|any
W|flutter.widgets.Padding
P|padding|edgeInsets,edgeInsetsDirectional|1|edgeInsets:16,16,16,16|edgeInsets:0:1:*:1;edgeInsetsDirectional:0:1:*:1|edgeInsets:edgeInsets:1:0:1:*:1;edgeInsetsDirectional:edgeInsets:1:0:1:*:1
S|child|single|0|0|1|any
W|flutter.widgets.RotatedBox
P|quarterTurns|integer|1|integer:1|integer:-9007199254740991:1:9007199254740991:1|integer:range:-9007199254740991:1:9007199254740991:1
S|child|single|0|0|1|any
W|flutter.widgets.Row
P|crossAxisAlignment|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:CrossAxisAlignment:baseline,center,end,start,stretch
P|mainAxisAlignment|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:MainAxisAlignment:center,end,spaceAround,spaceBetween,spaceEvenly,start
P|mainAxisSize|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:MainAxisSize:max,min
P|spacing|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|textBaseline|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextBaseline:alphabetic,ideographic
P|textDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDirection:ltr,rtl
P|verticalDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:VerticalDirection:down,up
S|children|list|0|0|10000|any
W|flutter.widgets.SizedBox
P|height|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
P|width|double,integer|0|-|double:0:1:*:1;integer:0:1:9007199254740991:1|double:range:0:1:*:1;integer:range:0:1:9007199254740991:1
S|child|single|0|0|1|any
W|flutter.widgets.SizedOverflowBox
P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|size|size|1|size:100,100|-|size:size:finiteNonNegative
S|child|single|0|0|1|any
W|flutter.widgets.Spacer
P|flex|integer|0|-|integer:1:1:9007199254740991:1|integer:range:1:1:9007199254740991:1
R|flutter.widgets.Spacer|directParentSlot|flutter.widgets.Column|children
R|flutter.widgets.Spacer|directParentSlot|flutter.widgets.Row|children
W|flutter.widgets.Stack
P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|clipBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none
P|fit|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:StackFit:expand,loose,passthrough
P|textDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDirection:ltr,rtl
S|children|list|0|0|10000|any
W|flutter.widgets.Text
P|data|string|1|string:VGV4dA|-|string:any
P|localeCountryCode|string|0|-|-|string:pattern:KD86W0EtWl17Mn18WzAtOV17M30p
P|localeLanguageCode|string|0|-|-|string:pattern:KD86W2Etel17MiwzfXxbYS16XXs1LDh9KQ
P|localeScriptCode|string|0|-|-|string:pattern:W0EtWl1bYS16XXszfQ
P|maxLines|integer|0|-|integer:1:1:9007199254740991:1|integer:range:1:1:9007199254740991:1
P|overflow|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextOverflow:clip,ellipsis,fade,visible
P|selectionColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|semanticsIdentifier|string|0|-|-|string:any
P|semanticsLabel|string|0|-|-|string:any
P|softWrap|boolean|0|-|-|boolean:any
P|strutDebugLabel|string|0|-|-|string:any
P|strutFontFamily|string|0|-|-|string:length:1:256
P|strutFontFamilyFallback|string|0|-|-|string:length:0:4096
P|strutFontSize|double|0|-|double:0:0:*:1|double:range:0:0:*:1
P|strutFontStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontStyle:italic,normal
P|strutFontWeight|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontWeight:w100,w200,w300,w400,w500,w600,w700,w800,w900
P|strutForceHeight|boolean|0|-|-|boolean:any
P|strutHeight|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|strutLeading|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|strutLeadingDistribution|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextLeadingDistribution:even,proportional
P|strutPackage|string|0|-|-|string:length:1:256
P|styleBackground|paint|0|-|-|paint:paintTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleBackgroundColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDebugLabel|string|0|-|-|string:any
P|styleDecorationColor|color,themeToken|0|-|-|color:any;themeToken:tokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleDecorationLineThrough|boolean|0|-|-|boolean:any
P|styleDecorationOverline|boolean|0|-|-|boolean:any
P|styleDecorationStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDecorationStyle:dashed,dotted,double,solid,wavy
P|styleDecorationThickness|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleDecorationUnderline|boolean|0|-|-|boolean:any
P|styleFontFamily|string|0|-|-|string:length:1:256
P|styleFontFamilyFallback|string|0|-|-|string:length:0:4096
P|styleFontFeatures|fontFeatureList|0|-|-|fontFeatureList:any
P|styleFontSize|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|styleFontStyle|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontStyle:italic,normal
P|styleFontVariations|fontVariationList|0|-|-|fontVariationList:fontVariationList
P|styleFontWeight|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FontWeight:w100,w200,w300,w400,w500,w600,w700,w800,w900
P|styleForeground|paint|0|-|-|paint:paintTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleHeight|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleInherit|boolean|0|-|-|boolean:any
P|styleLeadingDistribution|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextLeadingDistribution:even,proportional
P|styleLetterSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|styleLocaleCountryCode|string|0|-|-|string:pattern:KD86W0EtWl17Mn18WzAtOV17M30p
P|styleLocaleLanguageCode|string|0|-|-|string:pattern:KD86W2Etel17MiwzfXxbYS16XXs1LDh9KQ
P|styleLocaleScriptCode|string|0|-|-|string:pattern:W0EtWl1bYS16XXszfQ
P|styleOverflow|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextOverflow:clip,ellipsis,fade,visible
P|stylePackage|string|0|-|-|string:length:1:256
P|styleShadows|shadowList|0|-|-|shadowList:shadowTokens:material.colorScheme.error,material.colorScheme.errorContainer,material.colorScheme.inversePrimary,material.colorScheme.inverseSurface,material.colorScheme.onError,material.colorScheme.onErrorContainer,material.colorScheme.onInverseSurface,material.colorScheme.onPrimary,material.colorScheme.onPrimaryContainer,material.colorScheme.onPrimaryFixed,material.colorScheme.onPrimaryFixedVariant,material.colorScheme.onSecondary,material.colorScheme.onSecondaryContainer,material.colorScheme.onSecondaryFixed,material.colorScheme.onSecondaryFixedVariant,material.colorScheme.onSurface,material.colorScheme.onSurfaceVariant,material.colorScheme.onTertiary,material.colorScheme.onTertiaryContainer,material.colorScheme.onTertiaryFixed,material.colorScheme.onTertiaryFixedVariant,material.colorScheme.outline,material.colorScheme.outlineVariant,material.colorScheme.primary,material.colorScheme.primaryContainer,material.colorScheme.primaryFixed,material.colorScheme.primaryFixedDim,material.colorScheme.scrim,material.colorScheme.secondary,material.colorScheme.secondaryContainer,material.colorScheme.secondaryFixed,material.colorScheme.secondaryFixedDim,material.colorScheme.shadow,material.colorScheme.surface,material.colorScheme.surfaceBright,material.colorScheme.surfaceContainer,material.colorScheme.surfaceContainerHigh,material.colorScheme.surfaceContainerHighest,material.colorScheme.surfaceContainerLow,material.colorScheme.surfaceContainerLowest,material.colorScheme.surfaceDim,material.colorScheme.surfaceTint,material.colorScheme.tertiary,material.colorScheme.tertiaryContainer,material.colorScheme.tertiaryFixed,material.colorScheme.tertiaryFixedDim
P|styleTextBaseline|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextBaseline:alphabetic,ideographic
P|styleThemeTextStyle|themeToken|0|-|-|themeToken:tokens:material.textTheme.bodyLarge,material.textTheme.bodyMedium,material.textTheme.bodySmall,material.textTheme.displayLarge,material.textTheme.displayMedium,material.textTheme.displaySmall,material.textTheme.headlineLarge,material.textTheme.headlineMedium,material.textTheme.headlineSmall,material.textTheme.labelLarge,material.textTheme.labelMedium,material.textTheme.labelSmall,material.textTheme.titleLarge,material.textTheme.titleMedium,material.textTheme.titleSmall
P|styleWordSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|textAlign|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextAlign:center,end,justify,left,right,start
P|textDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDirection:ltr,rtl
P|textHeightApplyFirstAscent|boolean|0|-|-|boolean:any
P|textHeightApplyLastDescent|boolean|0|-|-|boolean:any
P|textHeightLeadingDistribution|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextLeadingDistribution:even,proportional
P|textScalerFactor|double|0|-|double:0:1:*:1|double:range:0:1:*:1
P|textWidthBasis|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextWidthBasis:longestLine,parent
W|flutter.widgets.Transform
P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|filterQuality|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:FilterQuality:high,low,medium,none
P|origin|offset|0|-|-|offset:offset:finiteSigned
P|transform|matrix4|1|matrix4:1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1|-|matrix4:matrix4
P|transformHitTests|boolean|0|-|-|boolean:any
S|child|single|0|0|1|any
W|flutter.widgets.UnconstrainedBox
P|alignment|alignmentGeometry|0|-|-|alignmentGeometry:alignmentGeometry
P|clipBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none
P|constrainedAxis|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Axis:horizontal,vertical
P|textDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDirection:ltr,rtl
S|child|single|0|0|1|any
W|flutter.widgets.Wrap
P|alignment|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:WrapAlignment:center,end,spaceAround,spaceBetween,spaceEvenly,start
P|clipBehavior|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Clip:antiAlias,antiAliasWithSaveLayer,hardEdge,none
P|crossAxisAlignment|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:WrapCrossAlignment:center,end,start
P|direction|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:Axis:horizontal,vertical
P|runAlignment|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:WrapAlignment:center,end,spaceAround,spaceBetween,spaceEvenly,start
P|runSpacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|spacing|double|0|-|double:*:1:*:1|double:range:*:1:*:1
P|textDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:TextDirection:ltr,rtl
P|verticalDirection|enum|0|-|-|enum:enum:cGFja2FnZTpmbHV0dGVyL3dpZGdldHMuZGFydA:VerticalDirection:down,up
S|children|list|0|0|10000|any
''';

String canvasRuntimeWidgetSchemaContractForTesting() {
  final result = StringBuffer();
  final widgetTypes = _widgetSpecifications.keys.toList()..sort();
  for (final widgetType in widgetTypes) {
    final widget = _widgetSpecifications[widgetType]!;
    result.writeln('W|$widgetType');
    final propertyNames = widget.properties.keys.toList()..sort();
    for (final propertyName in propertyNames) {
      final property = widget.properties[propertyName]!;
      final kinds = property.kinds.toList()..sort();
      final numericEntries = property.numericBounds.entries.toList()
        ..sort((left, right) => left.key.compareTo(right.key));
      final numeric = numericEntries.isEmpty
          ? '-'
          : numericEntries
                .map(
                  (entry) =>
                      '${entry.key}:${_numericBoundsFingerprint(entry.value)}',
                )
                .join(';');
      final constraints = kinds
          .map(
            (kind) => '$kind:${_propertyConstraintFingerprint(property, kind)}',
          )
          .join(';');
      result
        ..write('P|$propertyName|${kinds.join(',')}|')
        ..write(property.required ? '1' : '0')
        ..write('|${property.creationDefaultFingerprint ?? '-'}|')
        ..write(numeric)
        ..writeln('|$constraints');
    }
    final slotNames = widget.slots.keys.toList()..sort();
    for (final slotName in slotNames) {
      final slot = widget.slots[slotName]!;
      result.writeln(
        'S|$slotName|${slot.cardinality}|${slot.required ? 1 : 0}|'
        '${slot.minimumChildren}|${slot.maximumChildren}|'
        '${slot.acceptance.fingerprint()}',
      );
    }
    if (_isFlexRestrictedWidgetType(widgetType)) {
      result
        ..writeln(
          'R|$widgetType|directParentSlot|'
          'flutter.widgets.Column|children',
        )
        ..writeln(
          'R|$widgetType|directParentSlot|'
          'flutter.widgets.Row|children',
        );
      if (_isFlexParentDataWidgetType(widgetType)) {
        result.writeln('C|$widgetType|paletteCreate|wrapExistingChild|child');
      }
    }
  }
  return result.toString();
}

bool _placementAccepts(
  String parentWidgetType,
  String slotName,
  String childWidgetType,
) {
  if (!_isFlexRestrictedWidgetType(childWidgetType)) {
    return true;
  }
  return slotName == 'children' &&
      (parentWidgetType == 'flutter.widgets.Row' ||
          parentWidgetType == 'flutter.widgets.Column');
}

bool _isFlexParentDataWidgetType(String widgetType) =>
    widgetType == 'flutter.widgets.Expanded' ||
    widgetType == 'flutter.widgets.Flexible';

bool _isFlexRestrictedWidgetType(String widgetType) =>
    _isFlexParentDataWidgetType(widgetType) ||
    widgetType == 'flutter.widgets.Spacer';

bool isCanvasReviewedWidgetType(String widgetType) =>
    _widgetSpecifications.containsKey(widgetType);

Set<String> canvasWidgetTraitsForType(String widgetType) =>
    _widgetSpecifications[widgetType]?.traits ?? const {};

String _propertyConstraintFingerprint(_PropertySpec spec, String kind) {
  final numeric = spec.numericBounds[kind];
  if (kind == 'iconData') {
    return spec.materialIconsOnly
        ? _canvasMaterialIconConstraintFingerprint
        : _canvasIconDataConstraintFingerprint;
  }
  if (kind == 'edgeInsets' || kind == 'edgeInsetsDirectional') {
    _expect(numeric != null, 'Canvas EdgeInsets schema is incomplete.');
    return 'edgeInsets:${spec.edgeInsetsNonNegative ? 1 : 0}:'
        '${_numericBoundsFingerprint(numeric!)}';
  }
  if (numeric != null) {
    return 'range:${_numericBoundsFingerprint(numeric)}';
  }
  if (kind == 'enum') {
    final library = spec.enumLibraryUri;
    final type = spec.enumType;
    _expect(
      library != null && type != null && spec.enumValues.isNotEmpty,
      'Canvas enum schema is incomplete.',
    );
    final values = spec.enumValues.toList()..sort();
    return 'enum:${_base64Fingerprint(library!)}:$type:${values.join(',')}';
  }
  if (kind == 'string') {
    if (spec.stringPattern case final pattern?) {
      return 'pattern:${_base64Fingerprint(pattern)}';
    }
    if (spec.explicitStringLength) {
      return 'length:${spec.minimumStringLength}:${spec.maximumStringLength}';
    }
    return 'any';
  }
  if (kind == 'themeToken') {
    final tokens = spec.themeTokens.toList()..sort();
    _expect(tokens.isNotEmpty, 'Canvas theme-token schema is empty.');
    return 'tokens:${tokens.join(',')}';
  }
  if (kind == 'paint') {
    final tokens = canvasColorSchemeThemeTokens.toList()..sort();
    return 'paintTokens:${tokens.join(',')}';
  }
  if (kind == 'shadowList') {
    final tokens = canvasColorSchemeThemeTokens.toList()..sort();
    return 'shadowTokens:${tokens.join(',')}';
  }
  if (kind == 'fontVariationList') {
    return 'fontVariationList';
  }
  if (kind == 'boxConstraints') {
    return 'boxConstraints:v2:finiteOrPositiveInfinity';
  }
  if (kind == 'size') {
    return 'size:finiteNonNegative';
  }
  if (kind == 'alignmentGeometry' || kind == 'matrix4') {
    return kind;
  }
  if (kind == 'offset') {
    return 'offset:finiteSigned';
  }
  if (kind == 'imageProvider') {
    return 'imageProvider:v1:asset,exactAsset:package:exactScale:'
        'resize(1..16384,exact,fit,allowUpscaling)';
  }
  if (kind == 'boxDecoration') {
    final tokens = spec.themeTokens.toList()..sort();
    _expect(tokens.isNotEmpty, 'Canvas BoxDecoration token schema is empty.');
    return 'boxDecoration:v2:'
        'imageProvider:v1:asset,exactAsset:package:exactScale:'
        'resize(1..16384,exact,fit,allowUpscaling):decorationImage:v1:'
        'onError,colorFilter(mode,matrix20,linearToSrgbGamma,'
        'srgbToLinearGamma,saturation),fit,alignment,centerSlice,repeat,'
        'matchTextDirection,scale,opacity,filterQuality,invertColors,'
        'isAntiAlias:centerSliceFit(except:cover,none):theme='
        '${tokens.join(',')}';
  }
  if (kind == 'callback') {
    return 'callbackReference';
  }
  return 'any';
}

String _numericBoundsFingerprint(_NumericBounds bounds) =>
    '${_canonicalBound(bounds.minimum)}:${bounds.minimumInclusive ? 1 : 0}:'
    '${_canonicalBound(bounds.maximum)}:${bounds.maximumInclusive ? 1 : 0}';

String _canonicalBound(num? value) {
  if (value == null) {
    return '*';
  }
  if (value == 0) {
    return '0';
  }
  return value.toString();
}

String _base64Fingerprint(String value) =>
    base64Url.encode(utf8.encode(value)).replaceAll('=', '');

class _NodeBudget {
  int count = 0;
  final ids = <String>{};
  final imageResourceIds = <String>{};
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

  if (type == 'flutter.widgets.OverflowBox') {
    final minWidth = properties['minWidth']?.value as double?;
    final maxWidth = properties['maxWidth']?.value as double?;
    final minHeight = properties['minHeight']?.value as double?;
    final maxHeight = properties['maxHeight']?.value as double?;
    _expect(
      minWidth == null || maxWidth == null || minWidth <= maxWidth,
      'Canvas OverflowBox minWidth cannot exceed maxWidth: '
      '$path/properties/minWidth',
    );
    _expect(
      minHeight == null || maxHeight == null || minHeight <= maxHeight,
      'Canvas OverflowBox minHeight cannot exceed maxHeight: '
      '$path/properties/minHeight',
    );
  }

  if (type == 'flutter.material.AppBar') {
    _validateAppBarRelationships(properties, path);
    return;
  }

  if (type == 'flutter.material.ElevatedButton') {
    _validateElevatedButtonRelationships(properties, path);
    return;
  }

  if (type == 'flutter.widgets.Container') {
    _expect(
      !(properties.containsKey('color') &&
          properties.containsKey('decoration')),
      'Canvas Container color and decoration are mutually exclusive: '
      '$path/properties',
    );
    final clip = properties['clipBehavior']?.value;
    _expect(
      clip is! CanvasEnumValue ||
          clip.value == 'none' ||
          properties.containsKey('decoration'),
      'Canvas Container non-none clipBehavior requires decoration: '
      '$path/properties/clipBehavior',
    );
    for (final name in const ['decoration', 'foregroundDecoration']) {
      final decoration = properties[name]?.value;
      if (decoration is! CanvasBoxDecorationValue) {
        continue;
      }
      final resolution = decoration.image?.image.resolution;
      if (resolution is CanvasResolvedImageValue) {
        budget.imageResourceIds.add(resolution.resourceId);
      }
    }
    return;
  }

  if (type == 'flutter.widgets.Image') {
    final provider = properties['image']?.value;
    _expect(
      provider is CanvasImageProviderValue,
      'Canvas Image requires a direct image provider: $path/properties/image',
    );
    final resolution = (provider! as CanvasImageProviderValue).resolution;
    if (resolution is CanvasResolvedImageValue) {
      budget.imageResourceIds.add(resolution.resourceId);
    }

    const sliceNames = [
      'centerSliceLeft',
      'centerSliceTop',
      'centerSliceRight',
      'centerSliceBottom',
    ];
    final presentSlices = sliceNames
        .where(properties.containsKey)
        .toList(growable: false);
    _expect(
      presentSlices.isEmpty || presentSlices.length == sliceNames.length,
      'Canvas Image centerSlice coordinates must be all present or all '
      'omitted: $path/properties',
    );
    if (presentSlices.isNotEmpty) {
      final left = properties['centerSliceLeft']!.value as double;
      final top = properties['centerSliceTop']!.value as double;
      final right = properties['centerSliceRight']!.value as double;
      final bottom = properties['centerSliceBottom']!.value as double;
      _expect(
        left < right && top < bottom,
        'Canvas Image centerSlice must have positive width and height: '
        '$path/properties',
      );
      final fit = properties['fit']?.value;
      _expect(
        fit is! CanvasEnumValue ||
            (fit.value != 'cover' && fit.value != 'none'),
        'Canvas Image centerSlice rejects cover and none fits: '
        '$path/properties/fit',
      );
    }
    return;
  }

  if (type == 'flutter.material.TextField') {
    const radiusNames = ['cursorRadiusX', 'cursorRadiusY'];
    final presentRadii = radiusNames
        .where(properties.containsKey)
        .toList(growable: false);
    _expect(
      presentRadii.isEmpty || presentRadii.length == radiusNames.length,
      'Canvas TextField cursorRadius coordinates must be all present or all '
      'omitted: $path/properties',
    );

    const paddingNames = [
      'scrollPaddingLeft',
      'scrollPaddingTop',
      'scrollPaddingRight',
      'scrollPaddingBottom',
    ];
    final presentPadding = paddingNames
        .where(properties.containsKey)
        .toList(growable: false);
    _expect(
      presentPadding.isEmpty || presentPadding.length == paddingNames.length,
      'Canvas TextField scrollPadding coordinates must be all present or all '
      'omitted: $path/properties',
    );

    final expands = properties['expands']?.value == true;
    _expect(
      !expands ||
          (!properties.containsKey('maxLines') &&
              !properties.containsKey('minLines')),
      'Canvas TextField expands rejects explicit minLines and maxLines: '
      '$path/properties',
    );
    final maxLines = properties['maxLines']?.value as int? ?? 1;
    final minLines = properties['minLines']?.value as int?;
    _expect(
      minLines == null || minLines <= maxLines,
      'Canvas TextField minLines cannot exceed the effective maxLines: '
      '$path/properties/minLines',
    );
    final obscureText = properties['obscureText']?.value == true;
    _expect(
      !obscureText || (!expands && maxLines == 1),
      'Canvas TextField obscureText requires a non-expanding single line: '
      '$path/properties/obscureText',
    );
    final maxLength = properties['maxLength']?.value as int?;
    _expect(
      maxLength == null || maxLength == -1 || maxLength > 0,
      'Canvas TextField maxLength must be -1 or positive: '
      '$path/properties/maxLength',
    );
    final inputAction = properties['textInputAction']?.value;
    final effectiveMultiline = expands || maxLines != 1;
    _expect(
      inputAction is! CanvasEnumValue ||
          inputAction.value != 'newline' ||
          !effectiveMultiline ||
          properties['keyboardType']?.value != 'text',
      'Canvas TextField multiline newline action rejects the explicit text '
      'keyboard type: $path/properties/keyboardType',
    );
    return;
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

void _validateElevatedButtonRelationships(
  Map<String, CanvasValue> properties,
  String path,
) {
  for (final prefix in const [
    'style',
    'styleDisabled',
    'stylePressed',
    'styleHovered',
    'styleFocused',
  ]) {
    for (final dimension in const ['Width', 'Height']) {
      final minimumName = '${prefix}Minimum$dimension';
      final fixedName = '${prefix}Fixed$dimension';
      final maximumName = '${prefix}Maximum$dimension';
      final minimum = properties[minimumName]?.value as num?;
      final fixed = properties[fixedName]?.value as num?;
      final maximum = properties[maximumName]?.value as num?;
      _expect(
        minimum == null || maximum == null || minimum <= maximum,
        'Canvas ElevatedButton $minimumName must be less than or equal to '
        '$maximumName: $path/properties/$maximumName',
      );
      _expect(
        fixed == null || minimum == null || fixed >= minimum,
        'Canvas ElevatedButton $fixedName must be greater than or equal to '
        '$minimumName: $path/properties/$fixedName',
      );
      _expect(
        fixed == null || maximum == null || fixed <= maximum,
        'Canvas ElevatedButton $fixedName must be less than or equal to '
        '$maximumName: $path/properties/$fixedName',
      );
    }

    final radiusFields = {
      '${prefix}ShapeRadiusTopLeft',
      '${prefix}ShapeRadiusTopRight',
      '${prefix}ShapeRadiusBottomRight',
      '${prefix}ShapeRadiusBottomLeft',
    };
    final eccentricity = '${prefix}ShapeCircleEccentricity';
    final kindName = '${prefix}ShapeKind';
    final localKind = properties[kindName]?.value;
    final kind =
        localKind ??
        (prefix != 'style' && prefix != 'styleDisabled'
            ? properties['styleShapeKind']?.value
            : null);
    final hasShapeLeaf = <String>{
      ...radiusFields,
      eccentricity,
    }.any(properties.containsKey);
    _expect(
      kind is String || !hasShapeLeaf,
      'Canvas ElevatedButton shape fields require an effective local '
      '$kindName: $path/properties/$kindName',
    );
    if (kind is String) {
      final supportsRadius = const {
        'roundedRectangle',
        'roundedSuperellipse',
        'beveledRectangle',
        'continuousRectangle',
      }.contains(kind);
      _expect(
        supportsRadius || !radiusFields.any(properties.containsKey),
        'Canvas ElevatedButton $kind shape does not accept corner radii: '
        '$path/properties/$kindName',
      );
      _expect(
        kind == 'circle' || !properties.containsKey(eccentricity),
        'Canvas ElevatedButton $kind shape does not accept circle '
        'eccentricity: $path/properties/$eccentricity',
      );
    }

    final textPrefix = '${prefix}Text';
    _validateLocaleSubtag(
      properties,
      '${textPrefix}LocaleLanguageCode',
      RegExp(r'^(?:[a-z]{2,3}|[a-z]{5,8})$'),
      path,
    );
    _validateLocaleSubtag(
      properties,
      '${textPrefix}LocaleScriptCode',
      RegExp(r'^[A-Z][a-z]{3}$'),
      path,
    );
    _validateLocaleSubtag(
      properties,
      '${textPrefix}LocaleCountryCode',
      RegExp(r'^(?:[A-Z]{2}|[0-9]{3})$'),
      path,
    );
    _validateElevatedButtonFontPackageRelationship(properties, prefix, path);
    _expect(
      !(properties.containsKey('${textPrefix}BackgroundColor') &&
          properties.containsKey('${textPrefix}Background')),
      'Canvas ElevatedButton ${textPrefix}BackgroundColor and '
      '${textPrefix}Background are mutually exclusive: '
      '$path/properties/${textPrefix}Background',
    );
  }

  const textStatePrefixes = [
    'style',
    'styleDisabled',
    'stylePressed',
    'styleHovered',
    'styleFocused',
  ];
  final hasLocalTextInheritance = textStatePrefixes.any(
    (prefix) =>
        properties.containsKey('${prefix}TextInherit') ||
        properties.containsKey('${prefix}TextTheme'),
  );
  if (hasLocalTextInheritance) {
    final base = properties['styleTextInherit']?.value;
    final disabled = properties['styleDisabledTextInherit']?.value;
    _expect(
      base is bool,
      'Canvas ElevatedButton local TextStyle inheritance requires '
      'styleTextInherit: $path/properties/styleTextInherit',
    );
    _expect(
      disabled is bool && disabled == base,
      'Canvas ElevatedButton disabled TextStyle inherit must equal the base: '
      '$path/properties/styleDisabledTextInherit',
    );
    for (final prefix in textStatePrefixes) {
      final inheritName = '${prefix}TextInherit';
      final themeName = '${prefix}TextTheme';
      final inherit = properties[inheritName]?.value;
      _expect(
        inherit == null || inherit == base,
        'Canvas ElevatedButton $inheritName must equal styleTextInherit: '
        '$path/properties/$inheritName',
      );
      _expect(
        !properties.containsKey(themeName) || inherit == base,
        'Canvas ElevatedButton $themeName requires an explicit matching '
        '$inheritName: $path/properties/$inheritName',
      );
    }
  }

  const alignmentNames = {
    'styleAlignmentKind',
    'styleAlignmentX',
    'styleAlignmentY',
  };
  final alignmentCount = alignmentNames.where(properties.containsKey).length;
  _expect(
    alignmentCount == 0 || alignmentCount == alignmentNames.length,
    'Canvas ElevatedButton alignment requires styleAlignmentKind, '
    'styleAlignmentX, and styleAlignmentY together: '
    '$path/properties/styleAlignmentKind',
  );
}

void _validateAppBarRelationships(
  Map<String, CanvasValue> properties,
  String path,
) {
  const sideFields = {
    'shapeSideColor',
    'shapeSideWidth',
    'shapeSideStyle',
    'shapeSideStrokeAlign',
  };
  const radiusFields = {
    'shapeRadiusTopLeft',
    'shapeRadiusTopRight',
    'shapeRadiusBottomRight',
    'shapeRadiusBottomLeft',
  };
  const eccentricity = 'shapeCircleEccentricity';
  final kind = properties['shapeKind']?.value;
  final hasShapeLeaf = <String>{
    ...sideFields,
    ...radiusFields,
    eccentricity,
  }.any(properties.containsKey);
  _expect(
    kind is String || !hasShapeLeaf,
    'Canvas AppBar shape fields require shapeKind: '
    '$path/properties/shapeKind',
  );
  if (kind is String) {
    final supportsRadius = const {
      'roundedRectangle',
      'beveledRectangle',
      'continuousRectangle',
    }.contains(kind);
    final hasRadius = radiusFields.any(properties.containsKey);
    _expect(
      supportsRadius || !hasRadius,
      'Canvas AppBar $kind shape does not accept corner radii: '
      '$path/properties/shapeKind',
    );
    _expect(
      kind == 'circle' || !properties.containsKey(eccentricity),
      'Canvas AppBar $kind shape does not accept circle eccentricity: '
      '$path/properties/$eccentricity',
    );
  }
  _validateAppBarTextStyleRelationships(
    properties,
    prefix: 'toolbarTextStyle',
    path: path,
  );
  _validateAppBarTextStyleRelationships(
    properties,
    prefix: 'titleTextStyle',
    path: path,
  );
}

void _validateAppBarTextStyleRelationships(
  Map<String, CanvasValue> properties, {
  required String prefix,
  required String path,
}) {
  _validateLocaleSubtag(
    properties,
    '${prefix}LocaleLanguageCode',
    RegExp(r'^(?:[a-z]{2,3}|[a-z]{5,8})$'),
    path,
  );
  _validateLocaleSubtag(
    properties,
    '${prefix}LocaleScriptCode',
    RegExp(r'^[A-Z][a-z]{3}$'),
    path,
  );
  _validateLocaleSubtag(
    properties,
    '${prefix}LocaleCountryCode',
    RegExp(r'^(?:[A-Z]{2}|[0-9]{3})$'),
    path,
  );
  _validateFontPackageRelationship(
    properties,
    packageName: '${prefix}Package',
    fontFamilyName: '${prefix}FontFamily',
    fallbackName: '${prefix}FontFamilyFallback',
    path: path,
  );
  _expect(
    !(properties.containsKey('${prefix}Color') &&
        properties.containsKey('${prefix}Foreground')),
    'Canvas AppBar ${prefix}Color and ${prefix}Foreground are mutually '
    'exclusive: $path/properties/${prefix}Foreground',
  );
  _expect(
    !(properties.containsKey('${prefix}BackgroundColor') &&
        properties.containsKey('${prefix}Background')),
    'Canvas AppBar ${prefix}BackgroundColor and ${prefix}Background are '
    'mutually exclusive: $path/properties/${prefix}Background',
  );
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

void _validateElevatedButtonFontPackageRelationship(
  Map<String, CanvasValue> properties,
  String prefix,
  String path,
) {
  final textPrefix = '${prefix}Text';
  final packageName = '${textPrefix}Package';
  final package = properties[packageName]?.value;
  if (package == null) {
    return;
  }
  _expect(
    package is String && package.trim().isNotEmpty,
    'Canvas Text font package must be non-empty: '
    '$path/properties/$packageName',
  );

  final familyName = '${textPrefix}FontFamily';
  final fallbackName = '${textPrefix}FontFamilyFallback';
  final mayUseBase =
      (prefix == 'styleFocused' ||
          prefix == 'styleHovered' ||
          prefix == 'stylePressed') &&
      properties['${textPrefix}Inherit']?.value != false;
  final hasFamily = properties.containsKey(familyName)
      ? _hasNonEmptyString(properties, familyName)
      : mayUseBase && _hasNonEmptyString(properties, 'styleTextFontFamily');
  final hasFallback = properties.containsKey(fallbackName)
      ? _hasNonEmptyLine(properties, fallbackName)
      : mayUseBase &&
            _hasNonEmptyLine(properties, 'styleTextFontFamilyFallback');
  _expect(
    hasFamily || hasFallback,
    'Canvas ElevatedButton $packageName requires an effective local '
    '$familyName or $fallbackName without crossing disabled/inherit-false '
    'state isolation: $path/properties/$packageName',
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

void _expectWellFormedUtf16(String value, String path) {
  final units = value.codeUnits;
  for (var index = 0; index < units.length; index++) {
    final unit = units[index];
    if (unit >= 0xd800 && unit <= 0xdbff) {
      _expect(
        index + 1 < units.length &&
            units[index + 1] >= 0xdc00 &&
            units[index + 1] <= 0xdfff,
        'Canvas string contains an unpaired surrogate: $path',
      );
      index++;
      continue;
    }
    _expect(
      unit < 0xdc00 || unit > 0xdfff,
      'Canvas string contains an unpaired surrogate: $path',
    );
  }
}

void _validateImageAssetName(String value, String path) {
  _expectWellFormedUtf16(value, path);
  final codeUnits = value.codeUnits;
  _expect(
    !value.runes.every(_isCanvasMetadataWhitespace) &&
        codeUnits.first > 0x20 &&
        codeUnits.last > 0x20,
    'Canvas image assetName must be non-blank without surrounding whitespace: $path',
  );
  _expect(
    !value.startsWith('/') && !value.startsWith('~') && !value.endsWith('/'),
    'Canvas image assetName must be a relative POSIX path: $path',
  );
  _expect(
    value
        .split('/')
        .every(
          (segment) => segment.isNotEmpty && segment != '.' && segment != '..',
        ),
    'Canvas image assetName contains an unsafe path segment: $path',
  );
  _expect(
    !value.runes.any(
      (codePoint) =>
          codePoint == 0x25 ||
          codePoint == 0x5c ||
          codePoint == 0x3a ||
          codePoint <= 0x1f ||
          (codePoint >= 0x7f && codePoint <= 0x9f) ||
          codePoint == 0x061c ||
          codePoint == 0x200e ||
          codePoint == 0x200f ||
          (codePoint >= 0x2028 && codePoint <= 0x202e) ||
          (codePoint >= 0x2066 && codePoint <= 0x2069) ||
          codePoint == 0xfeff,
    ),
    'Canvas image assetName contains an unsafe path character: $path',
  );
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
