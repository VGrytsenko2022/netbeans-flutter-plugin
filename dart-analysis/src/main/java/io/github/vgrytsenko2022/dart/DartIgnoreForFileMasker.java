package io.github.vgrytsenko2022.dart;

import java.util.Objects;

/**
 * Equal-length masking for active Dart {@code // ignore_for_file:} directives.
 * String literals and nested block comments are deliberately not treated as
 * directives; interpolation expressions are scanned as Dart code.
 */
final class DartIgnoreForFileMasker {
    private static final String DIRECTIVE = "ignore_for_file";

    private DartIgnoreForFileMasker() {
    }

    static Result mask(String source) {
        Objects.requireNonNull(source, "source");
        char[] masked = source.toCharArray();
        Scan scan = scanCode(source, masked, 0, false);
        return new Result(new String(masked), scan.directives());
    }

    private static Scan scanCode(
            String source,
            char[] masked,
            int start,
            boolean interpolation) {
        int directives = 0;
        int braces = 0;
        int index = start;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (interpolation && current == '}' && braces == 0) {
                return new Scan(index + 1, directives);
            }
            if (current == '{') {
                braces++;
                index++;
                continue;
            }
            if (interpolation && current == '}') {
                braces--;
                index++;
                continue;
            }
            if (current == '/' && index + 1 < source.length()) {
                char next = source.charAt(index + 1);
                if (next == '/') {
                    int end = lineEnd(source, index + 2);
                    directives += maskDirective(source, masked, index + 2, end);
                    index = end;
                    continue;
                }
                if (next == '*') {
                    index = blockCommentEnd(source, index + 2);
                    continue;
                }
            }
            boolean raw = (current == 'r' || current == 'R')
                    && index + 1 < source.length()
                    && isQuote(source.charAt(index + 1))
                    && (index == 0 || !isIdentifierPart(source.charAt(index - 1)));
            if (raw) {
                Scan string = stringEnd(source, masked, index + 1, true);
                index = string.index();
                directives += string.directives();
                continue;
            }
            if (isQuote(current)) {
                Scan string = stringEnd(source, masked, index, false);
                index = string.index();
                directives += string.directives();
                continue;
            }
            index++;
        }
        return new Scan(index, directives);
    }

    private static Scan stringEnd(
            String source,
            char[] masked,
            int quoteOffset,
            boolean raw) {
        char quote = source.charAt(quoteOffset);
        boolean triple = quoteOffset + 2 < source.length()
                && source.charAt(quoteOffset + 1) == quote
                && source.charAt(quoteOffset + 2) == quote;
        int delimiterLength = triple ? 3 : 1;
        int index = quoteOffset + delimiterLength;
        int directives = 0;
        while (index < source.length()) {
            if (matchesDelimiter(source, index, quote, delimiterLength)) {
                return new Scan(index + delimiterLength, directives);
            }
            char current = source.charAt(index);
            if (!raw && current == '\\') {
                index = Math.min(source.length(), index + 2);
                continue;
            }
            if (!raw && current == '$' && index + 1 < source.length()
                    && source.charAt(index + 1) == '{') {
                Scan interpolation = scanCode(source, masked, index + 2, true);
                index = interpolation.index();
                directives += interpolation.directives();
                continue;
            }
            if (!triple && (current == '\n' || current == '\r')) {
                return new Scan(index, directives);
            }
            index++;
        }
        return new Scan(index, directives);
    }

    private static int maskDirective(
            String source,
            char[] masked,
            int commentStart,
            int commentEnd) {
        int index = commentStart;
        while (index < commentEnd && Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        int directiveEnd = index + DIRECTIVE.length();
        if (directiveEnd > commentEnd
                || !source.regionMatches(index, DIRECTIVE, 0, DIRECTIVE.length())) {
            return 0;
        }
        int colon = directiveEnd;
        while (colon < commentEnd && Character.isWhitespace(source.charAt(colon))) {
            colon++;
        }
        if (colon >= commentEnd || source.charAt(colon) != ':') {
            return 0;
        }
        for (int offset = index; offset < directiveEnd; offset++) {
            masked[offset] = ' ';
        }
        return 1;
    }

    private static int lineEnd(String source, int start) {
        int index = start;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (current == '\n' || current == '\r') {
                break;
            }
            index++;
        }
        return index;
    }

    private static int blockCommentEnd(String source, int start) {
        int depth = 1;
        int index = start;
        while (index < source.length() && depth > 0) {
            if (index + 1 < source.length()
                    && source.charAt(index) == '/'
                    && source.charAt(index + 1) == '*') {
                depth++;
                index += 2;
            } else if (index + 1 < source.length()
                    && source.charAt(index) == '*'
                    && source.charAt(index + 1) == '/') {
                depth--;
                index += 2;
            } else {
                index++;
            }
        }
        return index;
    }

    private static boolean matchesDelimiter(
            String source,
            int index,
            char quote,
            int length) {
        if (index + length > source.length()) {
            return false;
        }
        for (int offset = 0; offset < length; offset++) {
            if (source.charAt(index + offset) != quote) {
                return false;
            }
        }
        return true;
    }

    private static boolean isQuote(char value) {
        return value == '\'' || value == '"';
    }

    private static boolean isIdentifierPart(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    private record Scan(int index, int directives) {
    }

    record Result(String content, int directivesMasked) {
        Result {
            Objects.requireNonNull(content, "content");
            if (directivesMasked < 0) {
                throw new IllegalArgumentException(
                        "directivesMasked must not be negative");
            }
        }
    }
}
