package dev.flutter.netbeans.plugin.designer.guard;

import dev.flutter.netbeans.plugin.dart.DartTokenId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.api.editor.guards.GuardedSection;
import org.netbeans.api.editor.guards.SimpleSection;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;

/** Parser and writer for the persisted Flutter designer guard markers. */
final class DartGuardedSectionMarkers {

    static final String OPEN_PREFIX = "// <netbeans-flutter-designer region=\"";
    static final String OPEN_SUFFIX = "\">";
    static final String CLOSE_MARKER = "// </netbeans-flutter-designer>";
    private static final String MARKER_TOKEN = "netbeans-flutter-designer";
    private static final int MAX_REGION_ID_LENGTH = 128;

    private DartGuardedSectionMarkers() {
    }

    static ParseResult parse(char[] input) {
        char[] original = input.clone();
        char[] masked = input.clone();
        List<Region> regions = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        Set<Integer> lineCommentStarts = lineCommentStarts(input);
        OpenMarker open = null;

        int lineStart = 0;
        while (lineStart < input.length) {
            int contentEnd = lineContentEnd(input, lineStart);
            int nextLine = nextLineOffset(input, contentEnd);
            String line = new String(input, lineStart, contentEnd - lineStart);
            Marker marker = marker(line, lineStart, lineCommentStarts);

            if (marker.kind() == MarkerKind.MALFORMED) {
                return ParseResult.invalid(original);
            }
            if (marker.kind() == MarkerKind.OPEN) {
                if (open != null || !ids.add(marker.regionId())) {
                    return ParseResult.invalid(original);
                }
                open = new OpenMarker(
                        marker.regionId(),
                        lineStart,
                        lineStart + marker.indentationLength(),
                        contentEnd);
            } else if (marker.kind() == MarkerKind.CLOSE) {
                if (open == null) {
                    return ParseResult.invalid(original);
                }
                Arrays.fill(masked, open.markerStart(), open.contentEnd(), ' ');
                Arrays.fill(masked, lineStart + marker.indentationLength(), contentEnd, ' ');
                regions.add(new Region(open.regionId(), open.lineStart(), contentEnd - 1));
                open = null;
            }

            if (nextLine <= lineStart) {
                break;
            }
            lineStart = nextLine;
        }

        if (open != null) {
            return ParseResult.invalid(original);
        }
        return new ParseResult(true, masked, List.copyOf(regions));
    }

    static char[] write(List<GuardedSection> sections, char[] content) {
        List<Region> regions = new ArrayList<>(sections.size());
        Set<String> ids = new HashSet<>();
        for (GuardedSection section : sections) {
            if (!(section instanceof SimpleSection)
                    || !section.isValid()
                    || !validRegionId(section.getName())
                    || !ids.add(section.getName())) {
                return null;
            }
            int start = section.getStartPosition().getOffset();
            int end = section.getEndPosition().getOffset();
            regions.add(new Region(section.getName(), start, end));
        }
        List<Region> ordered = new ArrayList<>(regions);
        ordered.sort(Comparator.comparingInt(Region::startOffset));
        if (!validRegions(ordered, content.length)) {
            return null;
        }
        return writeValidatedRegions(ordered, content);
    }

    static char[] writeRegions(List<Region> regions, char[] content) {
        if (regions.isEmpty()) {
            return content.clone();
        }

        List<Region> ordered = new ArrayList<>(regions);
        ordered.sort(Comparator.comparingInt(Region::startOffset));
        if (!validRegions(ordered, content.length)) {
            return content.clone();
        }

        return writeValidatedRegions(ordered, content);
    }

    static int payloadStartOffset(char[] content, Region region) {
        int openingLineEnd = lineContentEnd(content, region.startOffset());
        return nextLineOffset(content, openingLineEnd);
    }

    static int payloadEndOffset(char[] content, Region region) {
        return lineStart(content, region.endOffset());
    }

    private static char[] writeValidatedRegions(List<Region> ordered, char[] content) {
        StringBuilder output = new StringBuilder(content.length + ordered.size() * 96);
        int cursor = 0;
        for (Region region : ordered) {
            output.append(content, cursor, region.startOffset() - cursor);
            appendRegion(output, content, region);
            cursor = region.endOffset() + 1;
        }
        output.append(content, cursor, content.length - cursor);
        char[] restored = new char[output.length()];
        output.getChars(0, output.length(), restored, 0);
        return restored;
    }

    private static void appendRegion(StringBuilder output, char[] content, Region region) {
        int start = region.startOffset();
        int endExclusive = region.endOffset() + 1;
        String openMarker = openingMarker(region.id());

        int firstContentEnd = lineContentEnd(content, start);
        int lastLineStart = lineStart(content, region.endOffset());
        int openingMarkerStart = firstContentEnd - openMarker.length();
        int closingMarkerStart = endExclusive - CLOSE_MARKER.length();
        boolean hasOpeningPlaceholder = openingMarkerStart >= start
                && containsOnlyIndentation(content, start, openingMarkerStart)
                && containsOnlySpaces(content, openingMarkerStart, firstContentEnd);
        boolean hasClosingPlaceholder = closingMarkerStart >= lastLineStart
                && containsOnlyIndentation(content, lastLineStart, closingMarkerStart)
                && containsOnlySpaces(content, closingMarkerStart, endExclusive);
        String separator = nearestLineSeparator(content, start, endExclusive);

        if (hasOpeningPlaceholder) {
            output.append(content, start, openingMarkerStart - start);
            output.append(openMarker);
            start = firstContentEnd;
        } else {
            output.append(openMarker).append(separator);
        }

        int bodyEnd = hasClosingPlaceholder ? lastLineStart : endExclusive;
        if (hasClosingPlaceholder) {
            output.append(content, start, bodyEnd - start);
            output.append(content, lastLineStart, closingMarkerStart - lastLineStart);
            output.append(CLOSE_MARKER);
        } else {
            int trailingContentEnd = beforeTrailingLineSeparator(
                    content, start, bodyEnd);
            int trailingIndentationStart = lineStart(
                    content, trailingContentEnd);
            boolean hasTrailingIndentation = trailingIndentationStart
                    < trailingContentEnd
                    && trailingIndentationStart > start
                    && containsOnlyIndentation(
                            content, trailingIndentationStart, trailingContentEnd);
            int sourceEnd = hasTrailingIndentation
                    ? trailingIndentationStart
                    : bodyEnd;
            output.append(content, start, sourceEnd - start);
            if (sourceEnd > start
                    && !endsWithLineSeparator(content, start, sourceEnd)) {
                output.append(separator);
            }
            if (hasTrailingIndentation) {
                output.append(
                        content,
                        trailingIndentationStart,
                        trailingContentEnd - trailingIndentationStart);
            }
            output.append(CLOSE_MARKER);
        }
    }

    private static boolean validRegions(List<Region> regions, int contentLength) {
        if (validateRegions(regions) != null) {
            return false;
        }
        for (Region region : regions) {
            if (region.endOffset() >= contentLength) {
                return false;
            }
        }
        return true;
    }

    static String validateRegions(List<Region> regions) {
        Set<String> ids = new HashSet<>();
        int previousEnd = -1;
        for (Region region : regions) {
            if (!validRegionId(region.id())
                    || !ids.add(region.id())) {
                return "guarded-section names must be unique valid region IDs";
            }
            if (region.startOffset() < 0
                    || region.endOffset() < region.startOffset()) {
                return "guarded-section offsets are invalid";
            }
            if (region.startOffset() <= previousEnd) {
                return "guarded sections overlap";
            }
            previousEnd = region.endOffset();
        }
        return null;
    }

    private static Marker marker(
            String line,
            int lineStart,
            Set<Integer> lineCommentStarts) {
        int indentationLength = 0;
        if (lineStart == 0 && !line.isEmpty() && line.charAt(0) == '\uFEFF') {
            indentationLength++;
        }
        while (indentationLength < line.length()) {
            char c = line.charAt(indentationLength);
            if (c != ' ' && c != '\t') {
                break;
            }
            indentationLength++;
        }
        String markerText = line.substring(indentationLength);
        if (!looksLikeDesignerMarkerComment(markerText)) {
            return new Marker(MarkerKind.NONE, "", 0);
        }

        // An exact marker-looking line is structural only when Dart lexes its
        // leading // as a normal line-comment token. This deliberately excludes
        // identical text inside strings, raw/triple strings, block comments and
        // documentation comments.
        int markerStart = lineStart + indentationLength;
        if (!lineCommentStarts.contains(markerStart)) {
            return new Marker(MarkerKind.NONE, "", 0);
        }

        if (markerText.equals(CLOSE_MARKER)) {
            return new Marker(MarkerKind.CLOSE, "", indentationLength);
        }
        if (markerText.startsWith(OPEN_PREFIX) && markerText.endsWith(OPEN_SUFFIX)) {
            String id = markerText.substring(
                    OPEN_PREFIX.length(),
                    markerText.length() - OPEN_SUFFIX.length());
            if (validRegionId(id) && markerText.equals(openingMarker(id))) {
                return new Marker(MarkerKind.OPEN, id, indentationLength);
            }
            return new Marker(MarkerKind.MALFORMED, "", 0);
        }
        return new Marker(MarkerKind.MALFORMED, "", 0);
    }

    private static Set<Integer> lineCommentStarts(char[] input) {
        TokenHierarchy<CharSequence> hierarchy = TokenHierarchy.create(
                new String(input),
                DartTokenId.language());
        TokenSequence<DartTokenId> sequence = hierarchy.tokenSequence(DartTokenId.language());
        if (sequence == null) {
            return Set.of();
        }

        Set<Integer> starts = new HashSet<>();
        int interpolationDepth = 0;
        while (sequence.moveNext()) {
            if (sequence.token().id() == DartTokenId.STRING_INTERPOLATION) {
                String interpolation = sequence.token().text().toString();
                if (interpolation.equals("${")) {
                    interpolationDepth++;
                } else if (interpolation.equals("}") && interpolationDepth > 0) {
                    interpolationDepth--;
                }
            } else if (interpolationDepth == 0
                    && sequence.token().id() == DartTokenId.COMMENT
                    && sequence.token().text().length() >= 2
                    && sequence.token().text().charAt(0) == '/'
                    && sequence.token().text().charAt(1) == '/') {
                starts.add(sequence.offset());
            }
        }
        return Set.copyOf(starts);
    }

    private static boolean looksLikeDesignerMarkerComment(String text) {
        if (!text.startsWith("//")) {
            return false;
        }
        String comment = text.substring(2).stripLeading();
        return comment.startsWith("<" + MARKER_TOKEN)
                || comment.startsWith("</" + MARKER_TOKEN);
    }

    private static boolean validRegionId(String id) {
        if (id == null || id.isEmpty() || id.length() > MAX_REGION_ID_LENGTH) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (!((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '.' || c == '_' || c == ':' || c == '-')) {
                return false;
            }
        }
        return true;
    }

    private static String openingMarker(String id) {
        return OPEN_PREFIX + id + OPEN_SUFFIX;
    }

    private static int lineContentEnd(char[] content, int start) {
        int offset = start;
        while (offset < content.length && content[offset] != '\r' && content[offset] != '\n') {
            offset++;
        }
        return offset;
    }

    private static int nextLineOffset(char[] content, int contentEnd) {
        if (contentEnd >= content.length) {
            return content.length;
        }
        if (content[contentEnd] == '\r'
                && contentEnd + 1 < content.length
                && content[contentEnd + 1] == '\n') {
            return contentEnd + 2;
        }
        return contentEnd + 1;
    }

    private static int lineStart(char[] content, int offset) {
        int result = Math.min(offset, content.length);
        while (result > 0) {
            char previous = content[result - 1];
            if (previous == '\n' || previous == '\r') {
                break;
            }
            result--;
        }
        return result;
    }

    private static int beforeTrailingLineSeparator(
            char[] content, int start, int end) {
        if (end <= start) {
            return end;
        }
        int result = end;
        if (content[result - 1] == '\n') {
            result--;
            if (result > start && content[result - 1] == '\r') {
                result--;
            }
        } else if (content[result - 1] == '\r') {
            result--;
        }
        return result;
    }

    private static boolean containsOnlySpaces(char[] content, int start, int end) {
        for (int i = start; i < end; i++) {
            if (content[i] != ' ') {
                return false;
            }
        }
        return true;
    }

    private static boolean containsOnlyIndentation(char[] content, int start, int end) {
        for (int i = start; i < end; i++) {
            if (content[i] != ' '
                    && content[i] != '\t'
                    && !(i == 0 && content[i] == '\uFEFF')) {
                return false;
            }
        }
        return true;
    }

    private static String nearestLineSeparator(char[] content, int start, int end) {
        for (int i = start; i < end; i++) {
            if (content[i] == '\r') {
                return i + 1 < content.length && content[i + 1] == '\n' ? "\r\n" : "\r";
            }
            if (content[i] == '\n') {
                return "\n";
            }
        }
        for (int i = end; i < content.length; i++) {
            if (content[i] == '\r') {
                return i + 1 < content.length && content[i + 1] == '\n' ? "\r\n" : "\r";
            }
            if (content[i] == '\n') {
                return "\n";
            }
        }
        for (int i = start - 1; i >= 0; i--) {
            if (content[i] == '\n') {
                return i > 0 && content[i - 1] == '\r' ? "\r\n" : "\n";
            }
            if (content[i] == '\r') {
                return "\r";
            }
        }
        return "\n";
    }

    private static boolean endsWithLineSeparator(char[] content, int start, int end) {
        return end > start && (content[end - 1] == '\n' || content[end - 1] == '\r');
    }

    record Region(String id, int startOffset, int endOffset) {
    }

    record ParseResult(boolean valid, char[] content, List<Region> regions) {
        static ParseResult invalid(char[] original) {
            return new ParseResult(false, original, List.of());
        }
    }

    private record OpenMarker(String regionId, int lineStart, int markerStart, int contentEnd) {
    }

    private record Marker(MarkerKind kind, String regionId, int indentationLength) {
    }

    private enum MarkerKind {
        NONE,
        OPEN,
        CLOSE,
        MALFORMED
    }
}
