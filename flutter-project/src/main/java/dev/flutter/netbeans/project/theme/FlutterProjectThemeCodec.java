package dev.flutter.netbeans.project.theme;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Strict bounded reader and deterministic writer for {@code project.fdtheme}. */
public final class FlutterProjectThemeCodec {
    /** Bounded for the complete 64-theme v5 catalog, including every typed override. */
    public static final int MAX_DESCRIPTOR_BYTES = 2 * 1024 * 1024;
    private static final int MAX_JSON_DEPTH = 24;
    private static final int MAX_STRING_LENGTH = 4_096;
    private static final int MAX_TOKENS = 262_144;
    private static final Pattern ARGB = Pattern.compile("0x[0-9A-F]{8}");

    private final JsonFactory factory;

    public FlutterProjectThemeCodec() {
        factory = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxNestingDepth(MAX_JSON_DEPTH)
                        .maxStringLength(MAX_STRING_LENGTH)
                        .maxNumberLength(32)
                        .build())
                .build();
    }

    /** Encodes canonical UTF-8 JSON in stable field and theme order with one final LF. */
    public byte[] encode(FlutterProjectTheme projectTheme) throws IOException {
        Objects.requireNonNull(projectTheme, "projectTheme");
        ByteArrayOutputStream output = new ByteArrayOutputStream(2_048);
        DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);
        try (JsonGenerator json = factory.createGenerator(output)) {
            json.setPrettyPrinter(printer);
            json.writeStartObject();
            json.writeStringField("format", FlutterProjectThemePaths.FORMAT);
            json.writeNumberField("schemaVersion", FlutterProjectThemePaths.SCHEMA_VERSION);
            json.writeBooleanField("enabled", projectTheme.enabled());
            json.writeStringField("defaultMode", projectTheme.defaultMode().wireName());
            json.writeStringField("lightThemeId", projectTheme.lightThemeId());
            json.writeStringField("darkThemeId", projectTheme.darkThemeId());
            json.writeArrayFieldStart("themes");
            for (FlutterProjectThemeDefinition theme : projectTheme.themes()) {
                json.writeStartObject();
                json.writeStringField("id", theme.id());
                json.writeBooleanField("enabled", theme.enabled());
                json.writeStringField("displayName", theme.displayName());
                json.writeStringField("brightness", theme.brightness().wireName());
                json.writeStringField("seedArgb", theme.seedArgbLiteral());
                writeColorScheme(json, theme.overrides().colorScheme());
                writeTextTheme(json, theme.overrides().textTheme());
                writeComponentColors(json, theme.overrides().componentColors());
                json.writeEndObject();
            }
            json.writeEndArray();
            json.writeObjectFieldStart("generated");
            json.writeStringField("dartFile", projectTheme.generated().dartFile());
            json.writeStringField("sha256", projectTheme.generated().sha256());
            json.writeEndObject();
            json.writeEndObject();
            json.writeRaw('\n');
        }
        byte[] bytes = output.toByteArray();
        if (bytes.length > MAX_DESCRIPTOR_BYTES) {
            throw new IOException("Encoded project theme descriptor exceeds "
                    + MAX_DESCRIPTOR_BYTES + " bytes");
        }
        return bytes;
    }

    /** Decodes and validates one bounded schema-v1 through schema-v5 descriptor. */
    public FlutterProjectTheme decode(byte[] bytes) throws IOException {
        Objects.requireNonNull(bytes, "bytes");
        if (bytes.length == 0 || bytes.length > MAX_DESCRIPTOR_BYTES) {
            throw new IOException("Project theme descriptor must contain 1 to "
                    + MAX_DESCRIPTOR_BYTES + " bytes");
        }
        try (JsonParser json = factory.createParser(bytes)) {
            TokenBudget budget = new TokenBudget();
            requireToken(next(json, budget), JsonToken.START_OBJECT, "root object");
            Set<String> fields = new HashSet<>();
            String format = null;
            Integer schemaVersion = null;
            Boolean enabled = null;
            FlutterThemeMode defaultMode = null;
            String lightThemeId = null;
            String darkThemeId = null;
            List<ParsedTheme> parsedThemes = null;
            FlutterGeneratedThemeArtifact generated = null;
            while (next(json, budget) != JsonToken.END_OBJECT) {
                requireToken(json.currentToken(), JsonToken.FIELD_NAME, "root field");
                String field = json.currentName();
                if (!fields.add(field)) {
                    throw invalid("Duplicate root field: " + field, null);
                }
                JsonToken value = next(json, budget);
                switch (field) {
                    case "format" -> format = requireString(json, value, field);
                    case "schemaVersion" -> schemaVersion = requireInteger(json, value, field);
                    case "enabled" -> enabled = requireBoolean(json, value, field);
                    case "defaultMode" -> defaultMode = parseMode(
                            requireString(json, value, field), field);
                    case "lightThemeId" -> lightThemeId = requireString(json, value, field);
                    case "darkThemeId" -> darkThemeId = requireString(json, value, field);
                    case "themes" -> parsedThemes = readThemes(json, value, budget);
                    case "generated" -> generated = readGenerated(json, value, budget);
                    default -> throw invalid("Unknown root field: " + field, null);
                }
            }
            if (next(json, budget) != null) {
                throw invalid("Content follows the root object", null);
            }
            if (!FlutterProjectThemePaths.FORMAT.equals(format)) {
                throw invalid("format must be " + FlutterProjectThemePaths.FORMAT, null);
            }
            if (schemaVersion == null
                    || (schemaVersion != FlutterProjectThemePaths.LEGACY_SCHEMA_VERSION
                    && schemaVersion != FlutterProjectThemePaths.GLOBAL_ENABLED_SCHEMA_VERSION
                    && schemaVersion != FlutterProjectThemePaths.PER_THEME_ENABLED_SCHEMA_VERSION
                    && schemaVersion != FlutterProjectThemePaths.OVERRIDES_SCHEMA_VERSION
                    && schemaVersion != FlutterProjectThemePaths.SCHEMA_VERSION)) {
                throw invalid("schemaVersion must be "
                        + FlutterProjectThemePaths.LEGACY_SCHEMA_VERSION + ", "
                        + FlutterProjectThemePaths.GLOBAL_ENABLED_SCHEMA_VERSION + ", "
                        + FlutterProjectThemePaths.PER_THEME_ENABLED_SCHEMA_VERSION + ", "
                        + FlutterProjectThemePaths.OVERRIDES_SCHEMA_VERSION + ", or "
                        + FlutterProjectThemePaths.SCHEMA_VERSION, null);
            }
            if (schemaVersion == FlutterProjectThemePaths.LEGACY_SCHEMA_VERSION) {
                if (enabled != null) {
                    throw invalid("enabled is not part of schemaVersion "
                            + FlutterProjectThemePaths.LEGACY_SCHEMA_VERSION, null);
                }
                enabled = Boolean.TRUE;
            } else {
                requirePresent(enabled, "enabled");
            }
            requirePresent(defaultMode, "defaultMode");
            requirePresent(lightThemeId, "lightThemeId");
            requirePresent(darkThemeId, "darkThemeId");
            requirePresent(parsedThemes, "themes");
            requirePresent(generated, "generated");
            try {
                List<FlutterProjectThemeDefinition> themes = materializeThemes(
                        parsedThemes, schemaVersion);
                return new FlutterProjectTheme(
                        enabled, defaultMode, lightThemeId, darkThemeId, themes, generated);
            } catch (IllegalArgumentException ex) {
                throw invalid(ex.getMessage(), ex);
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw invalid("Invalid project theme JSON: " + ex.getOriginalMessage(), ex);
        }
    }

    /** Reads a descriptor file without following a symbolic-link descriptor. */
    public FlutterProjectTheme read(Path descriptor) throws IOException {
        Objects.requireNonNull(descriptor, "descriptor");
        if (!Files.isRegularFile(descriptor, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(descriptor)) {
            throw new IOException("Project theme descriptor is not a regular non-symbolic file: "
                    + descriptor);
        }
        long size = Files.size(descriptor);
        if (size <= 0 || size > MAX_DESCRIPTOR_BYTES) {
            throw new IOException("Project theme descriptor " + descriptor
                    + " must contain 1 to " + MAX_DESCRIPTOR_BYTES + " bytes");
        }
        return decode(Files.readAllBytes(descriptor));
    }

    private static List<ParsedTheme> readThemes(
            JsonParser json, JsonToken token, TokenBudget budget) throws IOException {
        requireToken(token, JsonToken.START_ARRAY, "themes array");
        List<ParsedTheme> result = new ArrayList<>();
        JsonToken item;
        while ((item = next(json, budget)) != JsonToken.END_ARRAY) {
            if (result.size() >= FlutterProjectTheme.MAX_THEMES) {
                throw invalid("themes contains more than "
                        + FlutterProjectTheme.MAX_THEMES + " entries", null);
            }
            result.add(readTheme(json, item, budget));
        }
        return List.copyOf(result);
    }

    private static ParsedTheme readTheme(
            JsonParser json, JsonToken token, TokenBudget budget) throws IOException {
        requireToken(token, JsonToken.START_OBJECT, "theme object");
        Set<String> fields = new HashSet<>();
        String id = null;
        Boolean enabled = null;
        String displayName = null;
        FlutterThemeBrightness brightness = null;
        Integer seedArgb = null;
        Map<FlutterMaterialColorRole, Integer> colorScheme = null;
        Map<FlutterMaterialTextStyleRole, FlutterThemeTextStyleOverride> textTheme = null;
        Map<FlutterThemeComponentColorRole, FlutterThemeColorValue> componentColors = null;
        while (next(json, budget) != JsonToken.END_OBJECT) {
            requireToken(json.currentToken(), JsonToken.FIELD_NAME, "theme field");
            String field = json.currentName();
            if (!fields.add(field)) {
                throw invalid("Duplicate theme field: " + field, null);
            }
            JsonToken value = next(json, budget);
            switch (field) {
                case "id" -> id = requireString(json, value, field);
                case "enabled" -> enabled = requireBoolean(json, value, field);
                case "displayName" -> displayName = requireString(json, value, field);
                case "brightness" -> brightness = parseBrightness(
                        requireString(json, value, field), field);
                case "seedArgb" -> seedArgb = parseArgb(
                        requireString(json, value, field), field);
                case "colorScheme" -> colorScheme = readColorScheme(json, value, budget);
                case "textTheme" -> textTheme = readTextTheme(json, value, budget);
                case "components" -> componentColors = readComponentColors(json, value, budget);
                default -> throw invalid("Unknown theme field: " + field, null);
            }
        }
        requirePresent(id, "themes[].id");
        requirePresent(displayName, "themes[].displayName");
        requirePresent(brightness, "themes[].brightness");
        requirePresent(seedArgb, "themes[].seedArgb");
        return new ParsedTheme(
                id, displayName, brightness, seedArgb, enabled, colorScheme, textTheme,
                componentColors);
    }

    private static List<FlutterProjectThemeDefinition> materializeThemes(
            List<ParsedTheme> parsedThemes, int schemaVersion) throws IOException {
        boolean itemEnabledSupported = schemaVersion
                >= FlutterProjectThemePaths.PER_THEME_ENABLED_SCHEMA_VERSION;
        boolean overridesSupported = schemaVersion
                >= FlutterProjectThemePaths.OVERRIDES_SCHEMA_VERSION;
        boolean componentsSupported = schemaVersion == FlutterProjectThemePaths.SCHEMA_VERSION;
        List<FlutterProjectThemeDefinition> result = new ArrayList<>(parsedThemes.size());
        for (ParsedTheme parsed : parsedThemes) {
            if (!itemEnabledSupported && parsed.enabled() != null) {
                throw invalid("themes[].enabled is not part of schemaVersion "
                        + schemaVersion, null);
            }
            if (itemEnabledSupported && parsed.enabled() == null) {
                throw invalid("Missing required field: themes[].enabled", null);
            }
            if (!overridesSupported
                    && (parsed.colorScheme() != null || parsed.textTheme() != null
                    || parsed.componentColors() != null)) {
                throw invalid("themes[].colorScheme and themes[].textTheme are not part of "
                        + "schemaVersion " + schemaVersion, null);
            }
            if (overridesSupported && parsed.colorScheme() == null) {
                throw invalid("Missing required field: themes[].colorScheme", null);
            }
            if (overridesSupported && parsed.textTheme() == null) {
                throw invalid("Missing required field: themes[].textTheme", null);
            }
            if (!componentsSupported && parsed.componentColors() != null) {
                throw invalid("themes[].components is not part of schemaVersion "
                        + schemaVersion, null);
            }
            if (componentsSupported && parsed.componentColors() == null) {
                throw invalid("Missing required field: themes[].components", null);
            }
            try {
                result.add(new FlutterProjectThemeDefinition(
                        parsed.id(),
                        parsed.displayName(),
                        parsed.brightness(),
                        parsed.seedArgb(),
                        itemEnabledSupported ? parsed.enabled() : true,
                        overridesSupported
                                ? new FlutterThemeOverrides(
                                        parsed.colorScheme(), parsed.textTheme(),
                                        componentsSupported
                                                ? parsed.componentColors()
                                                : Map.of())
                                : FlutterThemeOverrides.EMPTY));
            } catch (IllegalArgumentException ex) {
                throw invalid(ex.getMessage(), ex);
            }
        }
        return List.copyOf(result);
    }

    private static FlutterGeneratedThemeArtifact readGenerated(
            JsonParser json, JsonToken token, TokenBudget budget) throws IOException {
        requireToken(token, JsonToken.START_OBJECT, "generated object");
        Set<String> fields = new HashSet<>();
        String dartFile = null;
        String sha256 = null;
        while (next(json, budget) != JsonToken.END_OBJECT) {
            requireToken(json.currentToken(), JsonToken.FIELD_NAME, "generated field");
            String field = json.currentName();
            if (!fields.add(field)) {
                throw invalid("Duplicate generated field: " + field, null);
            }
            JsonToken value = next(json, budget);
            switch (field) {
                case "dartFile" -> dartFile = requireString(json, value, field);
                case "sha256" -> sha256 = requireString(json, value, field);
                default -> throw invalid("Unknown generated field: " + field, null);
            }
        }
        requirePresent(dartFile, "generated.dartFile");
        requirePresent(sha256, "generated.sha256");
        try {
            return new FlutterGeneratedThemeArtifact(dartFile, sha256);
        } catch (IllegalArgumentException ex) {
            throw invalid(ex.getMessage(), ex);
        }
    }

    private static FlutterThemeMode parseMode(String value, String field) throws IOException {
        try {
            return FlutterThemeMode.fromWireName(value);
        } catch (IllegalArgumentException ex) {
            throw invalid(field + " has unsupported value: " + value, ex);
        }
    }

    private static FlutterThemeBrightness parseBrightness(String value, String field)
            throws IOException {
        try {
            return FlutterThemeBrightness.fromWireName(value);
        } catch (IllegalArgumentException ex) {
            throw invalid(field + " has unsupported value: " + value, ex);
        }
    }

    private static int parseArgb(String value, String field) throws IOException {
        if (!ARGB.matcher(value).matches()) {
            throw invalid(field + " must use exact uppercase 0xAARRGGBB form", null);
        }
        return (int) Long.parseUnsignedLong(value.substring(2), 16);
    }

    private static double requireDouble(JsonParser json, JsonToken token, String field)
            throws IOException {
        if (token != JsonToken.VALUE_NUMBER_FLOAT && token != JsonToken.VALUE_NUMBER_INT) {
            throw invalid("Expected " + field + " number but found " + token, null);
        }
        double value = json.getDoubleValue();
        if (!Double.isFinite(value)) {
            throw invalid(field + " must be finite", null);
        }
        return value;
    }

    private static String requireString(JsonParser json, JsonToken token, String field)
            throws IOException {
        requireToken(token, JsonToken.VALUE_STRING, field + " string");
        return json.getText();
    }

    private static int requireInteger(JsonParser json, JsonToken token, String field)
            throws IOException {
        requireToken(token, JsonToken.VALUE_NUMBER_INT, field + " integer");
        try {
            return json.getIntValue();
        } catch (RuntimeException ex) {
            throw invalid(field + " is outside the supported integer range", ex);
        }
    }

    private static boolean requireBoolean(JsonParser json, JsonToken token, String field)
            throws IOException {
        if (token == JsonToken.VALUE_TRUE) {
            return true;
        }
        if (token == JsonToken.VALUE_FALSE) {
            return false;
        }
        throw invalid("Expected " + field + " boolean but found " + token, null);
    }

    private static JsonToken next(JsonParser json, TokenBudget budget) throws IOException {
        JsonToken token = json.nextToken();
        if (token != null && ++budget.tokens > MAX_TOKENS) {
            throw invalid("Project theme JSON exceeds " + MAX_TOKENS + " tokens", null);
        }
        return token;
    }

    private static void requireToken(JsonToken actual, JsonToken expected, String description)
            throws IOException {
        if (actual != expected) {
            throw invalid("Expected " + description + " but found " + actual, null);
        }
    }

    private static void requirePresent(Object value, String field) throws IOException {
        if (value == null) {
            throw invalid("Missing required field: " + field, null);
        }
    }

    private static IOException invalid(String message, Throwable cause) {
        return cause == null ? new IOException(message) : new IOException(message, cause);
    }

    private static final class TokenBudget {
        private int tokens;
    }

    private record ParsedTheme(
            String id,
            String displayName,
            FlutterThemeBrightness brightness,
            int seedArgb,
            Boolean enabled,
            Map<FlutterMaterialColorRole, Integer> colorScheme,
            Map<FlutterMaterialTextStyleRole, FlutterThemeTextStyleOverride> textTheme,
            Map<FlutterThemeComponentColorRole, FlutterThemeColorValue> componentColors) {
    }

    private static void writeColorScheme(
            JsonGenerator json, Map<FlutterMaterialColorRole, Integer> overrides)
            throws IOException {
        json.writeObjectFieldStart("colorScheme");
        for (FlutterMaterialColorRole role : FlutterMaterialColorRole.values()) {
            Integer argb = overrides.get(role);
            if (argb != null) {
                json.writeStringField(role.wireName(),
                        FlutterThemeTextStyleOverride.argbLiteral(argb));
            }
        }
        json.writeEndObject();
    }

    private static void writeTextTheme(
            JsonGenerator json,
            Map<FlutterMaterialTextStyleRole, FlutterThemeTextStyleOverride> overrides)
            throws IOException {
        json.writeObjectFieldStart("textTheme");
        for (FlutterMaterialTextStyleRole role : FlutterMaterialTextStyleRole.values()) {
            FlutterThemeTextStyleOverride style = overrides.get(role);
            if (style == null) {
                continue;
            }
            json.writeObjectFieldStart(role.wireName());
            writeThemeColor(json, "color", style.color());
            writeThemeColor(json, "backgroundColor", style.backgroundColor());
            writeDouble(json, "fontSize", style.fontSize());
            if (style.fontWeight().isPresent()) {
                json.writeStringField(
                        "fontWeight", style.fontWeight().orElseThrow().wireName());
            }
            if (style.fontStyle().isPresent()) {
                json.writeStringField(
                        "fontStyle", style.fontStyle().orElseThrow().wireName());
            }
            writeDouble(json, "letterSpacing", style.letterSpacing());
            writeDouble(json, "wordSpacing", style.wordSpacing());
            writeDouble(json, "height", style.height());
            if (style.fontFamily().isPresent()) {
                json.writeStringField("fontFamily", style.fontFamily().orElseThrow());
            }
            if (style.decoration().isPresent()) {
                json.writeArrayFieldStart("decoration");
                Set<FlutterThemeTextDecorationLine> lines = style.decoration().orElseThrow();
                for (FlutterThemeTextDecorationLine line
                        : FlutterThemeTextDecorationLine.values()) {
                    if (lines.contains(line)) {
                        json.writeString(line.wireName());
                    }
                }
                json.writeEndArray();
            }
            writeThemeColor(json, "decorationColor", style.decorationColor());
            if (style.decorationStyle().isPresent()) {
                json.writeStringField(
                        "decorationStyle", style.decorationStyle().orElseThrow().wireName());
            }
            writeDouble(json, "decorationThickness", style.decorationThickness());
            json.writeEndObject();
        }
        json.writeEndObject();
    }

    private static void writeComponentColors(
            JsonGenerator json,
            Map<FlutterThemeComponentColorRole, FlutterThemeColorValue> overrides)
            throws IOException {
        json.writeObjectFieldStart("components");
        for (FlutterThemeComponentColorRole role
                : FlutterThemeComponentColorRole.values()) {
            FlutterThemeColorValue value = overrides.get(role);
            if (value != null) {
                writeThemeColor(json, role.wireName(), Optional.of(value));
            }
        }
        json.writeEndObject();
    }

    private static void writeThemeColor(
            JsonGenerator json, String field, Optional<FlutterThemeColorValue> value)
            throws IOException {
        if (value.isEmpty()) {
            return;
        }
        json.writeObjectFieldStart(field);
        switch (value.orElseThrow()) {
            case FlutterThemeColorValue.Literal literal -> {
                json.writeStringField("kind", "argb");
                json.writeStringField("argb", literal.argbLiteral());
            }
            case FlutterThemeColorValue.ColorRole role -> {
                json.writeStringField("kind", "colorScheme");
                json.writeStringField("role", role.role().wireName());
            }
        }
        json.writeEndObject();
    }

    private static void writeDouble(
            JsonGenerator json, String field, Optional<Double> value) throws IOException {
        if (value.isPresent()) {
            json.writeNumberField(field, value.orElseThrow());
        }
    }

    private static Map<FlutterMaterialColorRole, Integer> readColorScheme(
            JsonParser json, JsonToken token, TokenBudget budget) throws IOException {
        requireToken(token, JsonToken.START_OBJECT, "colorScheme object");
        EnumMap<FlutterMaterialColorRole, Integer> result =
                new EnumMap<>(FlutterMaterialColorRole.class);
        while (next(json, budget) != JsonToken.END_OBJECT) {
            requireToken(json.currentToken(), JsonToken.FIELD_NAME, "ColorScheme role");
            String wireName = json.currentName();
            FlutterMaterialColorRole role;
            try {
                role = FlutterMaterialColorRole.fromWireName(wireName);
            } catch (IllegalArgumentException failure) {
                throw invalid(failure.getMessage(), failure);
            }
            String literal = requireString(json, next(json, budget),
                    "colorScheme." + wireName);
            result.put(role, parseArgb(literal, "colorScheme." + wireName));
        }
        return Map.copyOf(result);
    }

    private static Map<FlutterMaterialTextStyleRole, FlutterThemeTextStyleOverride>
            readTextTheme(JsonParser json, JsonToken token, TokenBudget budget)
            throws IOException {
        requireToken(token, JsonToken.START_OBJECT, "textTheme object");
        EnumMap<FlutterMaterialTextStyleRole, FlutterThemeTextStyleOverride> result =
                new EnumMap<>(FlutterMaterialTextStyleRole.class);
        while (next(json, budget) != JsonToken.END_OBJECT) {
            requireToken(json.currentToken(), JsonToken.FIELD_NAME, "TextTheme role");
            String wireName = json.currentName();
            FlutterMaterialTextStyleRole role;
            try {
                role = FlutterMaterialTextStyleRole.fromWireName(wireName);
            } catch (IllegalArgumentException failure) {
                throw invalid(failure.getMessage(), failure);
            }
            FlutterThemeTextStyleOverride style = readTextStyle(
                    json, next(json, budget), budget, wireName);
            if (style.isEmpty()) {
                throw invalid("textTheme." + wireName + " must contain at least one override", null);
            }
            result.put(role, style);
        }
        return Map.copyOf(result);
    }

    private static Map<FlutterThemeComponentColorRole, FlutterThemeColorValue>
            readComponentColors(JsonParser json, JsonToken token, TokenBudget budget)
            throws IOException {
        requireToken(token, JsonToken.START_OBJECT, "components object");
        EnumMap<FlutterThemeComponentColorRole, FlutterThemeColorValue> result =
                new EnumMap<>(FlutterThemeComponentColorRole.class);
        while (next(json, budget) != JsonToken.END_OBJECT) {
            requireToken(json.currentToken(), JsonToken.FIELD_NAME,
                    "component color role");
            String wireName = json.currentName();
            FlutterThemeComponentColorRole role;
            try {
                role = FlutterThemeComponentColorRole.fromWireName(wireName);
            } catch (IllegalArgumentException failure) {
                throw invalid(failure.getMessage(), failure);
            }
            FlutterThemeColorValue value = readThemeColor(
                    json, next(json, budget), budget,
                    "components." + wireName);
            if (result.put(role, value) != null) {
                throw invalid("Duplicate component color role: " + wireName, null);
            }
        }
        return Map.copyOf(result);
    }

    private static FlutterThemeTextStyleOverride readTextStyle(
            JsonParser json, JsonToken token, TokenBudget budget, String role)
            throws IOException {
        requireToken(token, JsonToken.START_OBJECT, "TextStyle override object");
        Set<String> fields = new HashSet<>();
        FlutterThemeColorValue color = null;
        FlutterThemeColorValue backgroundColor = null;
        Double fontSize = null;
        FlutterThemeFontWeight fontWeight = null;
        FlutterThemeFontStyle fontStyle = null;
        Double letterSpacing = null;
        Double wordSpacing = null;
        Double height = null;
        String fontFamily = null;
        Set<FlutterThemeTextDecorationLine> decoration = null;
        FlutterThemeColorValue decorationColor = null;
        FlutterThemeTextDecorationStyle decorationStyle = null;
        Double decorationThickness = null;
        while (next(json, budget) != JsonToken.END_OBJECT) {
            requireToken(json.currentToken(), JsonToken.FIELD_NAME, "TextStyle field");
            String field = json.currentName();
            if (!fields.add(field)) {
                throw invalid("Duplicate TextStyle field: " + field, null);
            }
            JsonToken value = next(json, budget);
            String path = "textTheme." + role + "." + field;
            try {
                switch (field) {
                    case "color" -> color = readThemeColor(json, value, budget, path);
                    case "backgroundColor" -> backgroundColor =
                            readThemeColor(json, value, budget, path);
                    case "fontSize" -> fontSize = requireDouble(json, value, path);
                    case "fontWeight" -> fontWeight = FlutterThemeFontWeight.fromWireName(
                            requireString(json, value, path));
                    case "fontStyle" -> fontStyle = FlutterThemeFontStyle.fromWireName(
                            requireString(json, value, path));
                    case "letterSpacing" -> letterSpacing = requireDouble(json, value, path);
                    case "wordSpacing" -> wordSpacing = requireDouble(json, value, path);
                    case "height" -> height = requireDouble(json, value, path);
                    case "fontFamily" -> fontFamily = requireString(json, value, path);
                    case "decoration" -> decoration = readDecoration(
                            json, value, budget, path);
                    case "decorationColor" -> decorationColor =
                            readThemeColor(json, value, budget, path);
                    case "decorationStyle" -> decorationStyle =
                            FlutterThemeTextDecorationStyle.fromWireName(
                                    requireString(json, value, path));
                    case "decorationThickness" -> decorationThickness =
                            requireDouble(json, value, path);
                    default -> throw invalid("Unknown TextStyle field: " + field, null);
                }
            } catch (IllegalArgumentException failure) {
                throw invalid(path + " has unsupported value: " + failure.getMessage(), failure);
            }
        }
        try {
            return new FlutterThemeTextStyleOverride(
                    Optional.ofNullable(color), Optional.ofNullable(backgroundColor),
                    Optional.ofNullable(fontSize), Optional.ofNullable(fontWeight),
                    Optional.ofNullable(fontStyle), Optional.ofNullable(letterSpacing),
                    Optional.ofNullable(wordSpacing), Optional.ofNullable(height),
                    Optional.ofNullable(fontFamily), Optional.ofNullable(decoration),
                    Optional.ofNullable(decorationColor),
                    Optional.ofNullable(decorationStyle), Optional.ofNullable(decorationThickness));
        } catch (IllegalArgumentException failure) {
            throw invalid("Invalid textTheme." + role + ": " + failure.getMessage(), failure);
        }
    }

    private static FlutterThemeColorValue readThemeColor(
            JsonParser json, JsonToken token, TokenBudget budget, String path)
            throws IOException {
        requireToken(token, JsonToken.START_OBJECT, path + " color object");
        Set<String> fields = new HashSet<>();
        String kind = null;
        String argb = null;
        String role = null;
        while (next(json, budget) != JsonToken.END_OBJECT) {
            requireToken(json.currentToken(), JsonToken.FIELD_NAME, path + " color field");
            String field = json.currentName();
            if (!fields.add(field)) {
                throw invalid("Duplicate " + path + " color field: " + field, null);
            }
            String value = requireString(json, next(json, budget), path + "." + field);
            switch (field) {
                case "kind" -> kind = value;
                case "argb" -> argb = value;
                case "role" -> role = value;
                default -> throw invalid("Unknown " + path + " color field: " + field, null);
            }
        }
        if ("argb".equals(kind)) {
            requirePresent(argb, path + ".argb");
            if (role != null) {
                throw invalid(path + ".role is not valid for an argb color", null);
            }
            return new FlutterThemeColorValue.Literal(parseArgb(argb, path + ".argb"));
        }
        if ("colorScheme".equals(kind)) {
            requirePresent(role, path + ".role");
            if (argb != null) {
                throw invalid(path + ".argb is not valid for a ColorScheme role", null);
            }
            try {
                return new FlutterThemeColorValue.ColorRole(
                        FlutterMaterialColorRole.fromWireName(role));
            } catch (IllegalArgumentException failure) {
                throw invalid(path + ".role has unsupported value: " + role, failure);
            }
        }
        throw invalid(path + ".kind must be argb or colorScheme", null);
    }

    private static Set<FlutterThemeTextDecorationLine> readDecoration(
            JsonParser json, JsonToken token, TokenBudget budget, String path)
            throws IOException {
        requireToken(token, JsonToken.START_ARRAY, path + " array");
        EnumSet<FlutterThemeTextDecorationLine> result =
                EnumSet.noneOf(FlutterThemeTextDecorationLine.class);
        while (next(json, budget) != JsonToken.END_ARRAY) {
            String value = requireString(json, json.currentToken(), path + " item");
            try {
                FlutterThemeTextDecorationLine line =
                        FlutterThemeTextDecorationLine.fromWireName(value);
                if (!result.add(line)) {
                    throw invalid(path + " contains duplicate value: " + value, null);
                }
            } catch (IllegalArgumentException failure) {
                throw invalid(path + " has unsupported value: " + value, failure);
            }
        }
        return Set.copyOf(result);
    }

}
