package io.github.vgrytsenko2022.designer.catalog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Immutable, versioned registry of the Material {@code Icons.*} constants that
 * ship with the reviewed Flutter SDK.
 *
 * <p>The bundled data is self-contained: runtime use does not read the user's
 * Flutter SDK. Generated projects still need
 * {@code flutter.uses-material-design: true} in {@code pubspec.yaml} so that
 * Flutter bundles the {@code MaterialIcons} font.</p>
 */
public final class MaterialIconRegistry {
    public static final String FLUTTER_VERSION = "3.44.8";
    public static final String FLUTTER_REVISION = "058e0af2c2";
    public static final String FONT_FAMILY = "MaterialIcons";
    public static final String PROJECT_REQUIREMENT =
            "flutter.uses-material-design: true";
    public static final int BUNDLED_ICON_COUNT = 8_825;
    public static final int MAX_SEARCH_RESULTS = 100;

    private static final String RESOURCE_PATH =
            "/io/github/vgrytsenko2022/designer/catalog/material-icons-3.44.8.tsv";
    private static final String FORMAT_MAGIC =
            "# netbeans-flutter-material-icons-v1";
    private static final String SOURCE_PATH =
            "packages/flutter/lib/src/material/icons.dart";
    private static final String SOURCE_SHA256 =
            "ba88e3e23962ada6537523aa113811d9719b988412815bf084f50a0aa78137f0";
    private static final String COLUMN_HEADER =
            "# columns=name\tcodePointHex\tmatchTextDirection";
    private static final int HEADER_LINE_COUNT = 9;
    private static final int MAX_RESOURCE_BYTES = 512 * 1024;
    private static final int MAX_LINE_LENGTH = 256;
    private static final int MAX_ICON_NAME_LENGTH = 96;
    private static final int MAX_QUERY_LENGTH = 128;
    private static final Pattern ICON_NAME =
            Pattern.compile("[a-z][a-z0-9_]*");
    private static final Pattern HEX_CODE_POINT =
            Pattern.compile("[0-9a-f]{1,6}");

    private final SourceMetadata metadata;
    private final List<MaterialIcon> entries;
    private final Map<String, MaterialIcon> byName;
    private final Map<Long, MaterialIcon> byCodePointAndDirection;

    private MaterialIconRegistry(
            SourceMetadata metadata,
            List<MaterialIcon> entries,
            Map<String, MaterialIcon> byName) {
        this.metadata = metadata;
        this.entries = List.copyOf(entries);
        this.byName = Collections.unmodifiableMap(new LinkedHashMap<>(byName));
        LinkedHashMap<Long, MaterialIcon> values = new LinkedHashMap<>();
        for (MaterialIcon icon : entries) {
            values.putIfAbsent(valueKey(
                    icon.codePoint(), icon.matchTextDirection()), icon);
        }
        this.byCodePointAndDirection = Collections.unmodifiableMap(values);
    }

    /** Returns the lazily loaded, strictly validated bundled registry. */
    public static MaterialIconRegistry bundled() {
        return BundledHolder.INSTANCE;
    }

    public SourceMetadata metadata() {
        return metadata;
    }

    /** Entries in deterministic identifier order. */
    public List<MaterialIcon> entries() {
        return entries;
    }

    public Optional<MaterialIcon> find(String name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(byName.get(name));
    }

    /** Finds the canonical first icon for one exact serialized IconData value. */
    public Optional<MaterialIcon> find(
            int codePoint, boolean matchTextDirection) {
        if (!Character.isValidCodePoint(codePoint)) {
            return Optional.empty();
        }
        return Optional.ofNullable(byCodePointAndDirection.get(
                valueKey(codePoint, matchTextDirection)));
    }

    /**
     * Searches by identifier using deterministic exact, prefix, token-prefix,
     * then substring ranking.
     *
     * <p>Whitespace and punctuation in the query are normalized to identifier
     * separators. Results are immutable and capped to keep editor interaction
     * bounded.</p>
     */
    public List<MaterialIcon> search(String query, int limit) {
        Objects.requireNonNull(query, "query");
        if (query.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    "Material icon search query exceeds " + MAX_QUERY_LENGTH + " characters");
        }
        if (limit < 1 || limit > MAX_SEARCH_RESULTS) {
            throw new IllegalArgumentException(
                    "Material icon search limit must be between 1 and "
                            + MAX_SEARCH_RESULTS);
        }

        String normalized = normalizeQuery(query);
        if (normalized.isEmpty()) {
            return List.copyOf(entries.subList(0, Math.min(limit, entries.size())));
        }

        ArrayList<RankedIcon> matches = new ArrayList<>();
        for (MaterialIcon icon : entries) {
            int rank = matchRank(icon.name(), normalized);
            if (rank >= 0) {
                matches.add(new RankedIcon(icon, rank));
            }
        }
        matches.sort(Comparator
                .comparingInt(RankedIcon::rank)
                .thenComparingInt(match -> match.icon().name().length())
                .thenComparing(match -> match.icon().name()));

        int resultSize = Math.min(limit, matches.size());
        ArrayList<MaterialIcon> result = new ArrayList<>(resultSize);
        for (int index = 0; index < resultSize; index++) {
            result.add(matches.get(index).icon());
        }
        return List.copyOf(result);
    }

    static MaterialIconRegistry readForTesting(InputStream input, int expectedCount) {
        return read(input, expectedCount, "test material icon registry");
    }

    private static MaterialIconRegistry loadBundled() {
        InputStream resource = MaterialIconRegistry.class.getResourceAsStream(RESOURCE_PATH);
        if (resource == null) {
            throw new IllegalStateException(
                    "Bundled Material icon registry is missing: " + RESOURCE_PATH);
        }
        try (resource) {
            return read(resource, BUNDLED_ICON_COUNT, RESOURCE_PATH);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not close bundled Material icon registry: " + RESOURCE_PATH,
                    exception);
        }
    }

    private static MaterialIconRegistry read(
            InputStream input,
            int expectedCount,
            String description) {
        Objects.requireNonNull(input, "input");
        if (expectedCount < 1 || expectedCount > BUNDLED_ICON_COUNT) {
            throw new IllegalArgumentException("Invalid expected Material icon count: " + expectedCount);
        }

        String source = decodeUtf8(readBounded(input, description), description);
        if (!source.endsWith("\n")) {
            throw corrupt(description, "resource must end with a newline");
        }
        if (source.indexOf('\0') >= 0) {
            throw corrupt(description, "resource contains a NUL character");
        }
        // Git may materialize text resources with CRLF on Windows. Normalize
        // complete CRLF pairs, but continue to reject a bare carriage return.
        source = source.replace("\r\n", "\n");
        if (source.indexOf('\r') >= 0) {
            throw corrupt(description, "resource contains a bare carriage return");
        }

        String[] lines = source.substring(0, source.length() - 1).split("\n", -1);
        if (lines.length < HEADER_LINE_COUNT + 1) {
            throw corrupt(description, "resource is missing headers or icon rows");
        }
        for (int index = 0; index < lines.length; index++) {
            if (lines[index].isEmpty()) {
                throw corrupt(description, "blank line at " + (index + 1));
            }
            if (lines[index].length() > MAX_LINE_LENGTH) {
                throw corrupt(description, "line " + (index + 1) + " exceeds "
                        + MAX_LINE_LENGTH + " characters");
            }
        }

        requireLine(lines, 0, FORMAT_MAGIC, description);
        requireLine(lines, 1, "# flutter.version=" + FLUTTER_VERSION, description);
        requireLine(lines, 2, "# flutter.revision=" + FLUTTER_REVISION, description);
        requireLine(lines, 3, "# source.path=" + SOURCE_PATH, description);
        requireLine(lines, 4, "# source.sha256=" + SOURCE_SHA256, description);
        requireLine(lines, 5, "# source.count=" + expectedCount, description);
        requireLine(lines, 6, "# font.family=" + FONT_FAMILY, description);
        requireLine(lines, 7, "# project.requirement=" + PROJECT_REQUIREMENT, description);
        requireLine(lines, 8, COLUMN_HEADER, description);

        int actualCount = lines.length - HEADER_LINE_COUNT;
        if (actualCount != expectedCount) {
            throw corrupt(description, "declared " + expectedCount
                    + " icons but contains " + actualCount);
        }

        ArrayList<MaterialIcon> parsed = new ArrayList<>(actualCount);
        LinkedHashMap<String, MaterialIcon> parsedByName = new LinkedHashMap<>(actualCount);
        String previousName = null;
        for (int lineIndex = HEADER_LINE_COUNT; lineIndex < lines.length; lineIndex++) {
            String[] columns = lines[lineIndex].split("\t", -1);
            if (columns.length != 3) {
                throw corrupt(description, "line " + (lineIndex + 1)
                        + " must contain exactly three TSV columns");
            }

            String name = columns[0];
            if (name.length() > MAX_ICON_NAME_LENGTH || !ICON_NAME.matcher(name).matches()) {
                throw corrupt(description, "invalid icon identifier at line " + (lineIndex + 1));
            }
            if (parsedByName.containsKey(name)) {
                throw corrupt(description, "duplicate icon identifier: " + name);
            }
            if (previousName != null && previousName.compareTo(name) >= 0) {
                throw corrupt(description, "icon identifiers are not strictly sorted at: " + name);
            }

            int codePoint = parseCodePoint(columns[1], description, lineIndex + 1);
            boolean matchTextDirection = switch (columns[2]) {
                case "0" -> false;
                case "1" -> true;
                default -> throw corrupt(description,
                        "invalid matchTextDirection at line " + (lineIndex + 1));
            };

            MaterialIcon icon = new MaterialIcon(name, codePoint, matchTextDirection);
            parsed.add(icon);
            parsedByName.put(name, icon);
            previousName = name;
        }

        SourceMetadata metadata = new SourceMetadata(
                FLUTTER_VERSION,
                FLUTTER_REVISION,
                SOURCE_PATH,
                SOURCE_SHA256,
                expectedCount,
                FONT_FAMILY,
                PROJECT_REQUIREMENT);
        return new MaterialIconRegistry(metadata, parsed, parsedByName);
    }

    private static byte[] readBounded(InputStream input, String description) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                total += read;
                if (total > MAX_RESOURCE_BYTES) {
                    throw corrupt(description, "resource exceeds "
                            + MAX_RESOURCE_BYTES + " bytes");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Could not read " + description + ": " + exception.getMessage(),
                    exception);
        }
    }

    private static String decodeUtf8(byte[] bytes, String description) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw corrupt(description, "resource is not valid UTF-8");
        }
    }

    private static int parseCodePoint(String value, String description, int lineNumber) {
        if (!HEX_CODE_POINT.matcher(value).matches()) {
            throw corrupt(description, "invalid code point at line " + lineNumber);
        }
        int codePoint;
        try {
            codePoint = Integer.parseInt(value, 16);
        } catch (NumberFormatException exception) {
            throw corrupt(description, "invalid code point at line " + lineNumber);
        }
        if (codePoint == 0
                || !Character.isValidCodePoint(codePoint)
                || (codePoint >= Character.MIN_SURROGATE
                && codePoint <= Character.MAX_SURROGATE)
                || !value.equals(Integer.toHexString(codePoint))) {
            throw corrupt(description, "non-canonical code point at line " + lineNumber);
        }
        return codePoint;
    }

    private static void requireLine(
            String[] lines,
            int index,
            String expected,
            String description) {
        if (!expected.equals(lines[index])) {
            throw corrupt(description, "invalid header at line " + (index + 1));
        }
    }

    private static String normalizeQuery(String query) {
        String lowercase = query.trim().toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder(lowercase.length());
        boolean separatorPending = false;
        for (int index = 0; index < lowercase.length(); index++) {
            char character = lowercase.charAt(index);
            if ((character >= 'a' && character <= 'z')
                    || (character >= '0' && character <= '9')) {
                if (separatorPending && !normalized.isEmpty()) {
                    normalized.append('_');
                }
                normalized.append(character);
                separatorPending = false;
            } else {
                separatorPending = !normalized.isEmpty();
            }
        }
        return normalized.toString();
    }

    private static long valueKey(int codePoint, boolean matchTextDirection) {
        return ((long) codePoint << 1) | (matchTextDirection ? 1L : 0L);
    }

    private static int matchRank(String name, String query) {
        if (name.equals(query)) {
            return 0;
        }
        if (name.startsWith(query)) {
            return 1;
        }
        int tokenStart = 0;
        while (tokenStart < name.length()) {
            if (name.startsWith(query, tokenStart)) {
                return 2;
            }
            int separator = name.indexOf('_', tokenStart);
            if (separator < 0) {
                break;
            }
            tokenStart = separator + 1;
        }
        return name.contains(query) ? 3 : -1;
    }

    private static IllegalArgumentException corrupt(String description, String reason) {
        return new IllegalArgumentException("Corrupt " + description + ": " + reason);
    }

    /** Exact source identity embedded in the resource. */
    public record SourceMetadata(
            String flutterVersion,
            String flutterRevision,
            String sourcePath,
            String sourceSha256,
            int iconCount,
            String fontFamily,
            String projectRequirement) {
    }

    /** One static {@code Icons.*} constant from the reviewed Flutter SDK. */
    public record MaterialIcon(
            String name,
            int codePoint,
            boolean matchTextDirection) {

        public MaterialIcon {
            Objects.requireNonNull(name, "name");
            if (name.length() > MAX_ICON_NAME_LENGTH || !ICON_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException("Invalid Material icon identifier: " + name);
            }
            if (codePoint == 0
                    || !Character.isValidCodePoint(codePoint)
                    || (codePoint >= Character.MIN_SURROGATE
                    && codePoint <= Character.MAX_SURROGATE)) {
                throw new IllegalArgumentException(
                        "Invalid Material icon code point: " + codePoint);
            }
        }

        public String dartExpression() {
            return "Icons." + name;
        }

        public String fontFamily() {
            return FONT_FAMILY;
        }

        /**
         * Neutral selector text derived only from the Dart identifier.
         *
         * <p>This is UI presentation text, never an inferred Flutter
         * {@code semanticLabel}; accessibility copy remains an explicit user
         * decision.</p>
         */
        public String displayLabel() {
            String words = name.replace('_', ' ');
            if (words.isEmpty() || !Character.isLetter(words.charAt(0))) {
                return words;
            }
            return Character.toUpperCase(words.charAt(0)) + words.substring(1);
        }
    }

    private record RankedIcon(MaterialIcon icon, int rank) {
    }

    private static final class BundledHolder {
        private static final MaterialIconRegistry INSTANCE = loadBundled();

        private BundledHolder() {
        }
    }
}
