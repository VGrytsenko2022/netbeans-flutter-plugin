package dev.flutter.netbeans.dart;

import java.util.Objects;

/**
 * Detects explicit core imports without mistaking comments or string contents for directives.
 * Like the Dart analyzer, implicit-core suppression depends on the base import URI,
 * not an alternative URI in a conditional import.
 */
final class DartCoreImportScope {
    private DartCoreImportScope() { }

    static boolean hasExplicitCoreImport(String source) {
        Lexer lexer = new Lexer(Objects.requireNonNull(source, "source"));
        Token token;
        while ((token = lexer.next()) != null) {
            if (!token.string() && token.value().equals("import")) {
                Token uri = lexer.next();
                StringBuilder value = new StringBuilder();
                while (uri != null && uri.string()) {
                    value.append(uri.value());
                    uri = lexer.next();
                }
                if (value.toString().equals("dart:core")) return true;
            }
        }
        return false;
    }

    private record Token(String value, boolean string) { }

    private static final class Lexer {
        private final String source;
        private int offset;

        Lexer(String source) { this.source = source; }

        Token next() {
            trivia();
            if (offset >= source.length()) return null;
            char ch = source.charAt(offset);
            if ((ch == 'r' || ch == 'R') && offset + 1 < source.length() && quote(source.charAt(offset + 1))) {
                offset++;
                return new Token(string(true, 0), true);
            }
            if (quote(ch)) return new Token(string(false, 0), true);
            int start = offset++;
            if (Character.isLetter(ch) || ch == '_' || ch == '$') {
                while (offset < source.length() && identifier(source.charAt(offset))) offset++;
            }
            return new Token(source.substring(start, offset), false);
        }

        private void trivia() {
            while (offset < source.length()) {
                if (Character.isWhitespace(source.charAt(offset)) || source.charAt(offset) == '\uFEFF') {
                    offset++;
                } else if (source.startsWith("//", offset) || offset == 0 && source.startsWith("#!", offset)) {
                    while (offset < source.length() && source.charAt(offset) != '\n' && source.charAt(offset) != '\r') offset++;
                } else if (source.startsWith("/*", offset)) {
                    offset += 2;
                    int depth = 1;
                    while (depth > 0 && offset < source.length()) {
                        if (source.startsWith("/*", offset)) { depth++; offset += 2; }
                        else if (source.startsWith("*/", offset)) { depth--; offset += 2; }
                        else offset++;
                    }
                    if (depth != 0) throw malformed();
                } else {
                    break;
                }
            }
        }

        private String string(boolean raw, int nesting) {
            if (nesting > 128) throw malformed();
            char delimiter = source.charAt(offset);
            int width = source.startsWith(String.valueOf(delimiter).repeat(3), offset) ? 3 : 1;
            String end = String.valueOf(delimiter).repeat(width);
            offset += width;
            if (width == 3) trimOpeningLine();
            StringBuilder value = new StringBuilder();
            while (offset < source.length()) {
                if (source.startsWith(end, offset)) {
                    offset += width;
                    return value.toString();
                }
                char ch = source.charAt(offset++);
                if (!raw && ch == '\\') {
                    if (offset >= source.length()) throw malformed();
                    char escape = source.charAt(offset++);
                    switch (escape) {
                        case 'x' -> value.appendCodePoint(hex(2));
                        case 'u' -> {
                            if (offset < source.length() && source.charAt(offset) == '{') {
                                offset++;
                                int start = offset;
                                while (offset < source.length() && source.charAt(offset) != '}') offset++;
                                int length = offset - start;
                                if (offset >= source.length() || length < 1 || length > 6) throw malformed();
                                offset = start;
                                int codePoint = hex(length);
                                if (!Character.isValidCodePoint(codePoint)) throw malformed();
                                value.appendCodePoint(codePoint);
                                offset++;
                            } else {
                                value.appendCodePoint(hex(4));
                            }
                        }
                        case 'n' -> value.append('\n');
                        case 'r' -> value.append('\r');
                        case 't' -> value.append('\t');
                        case 'b' -> value.append('\b');
                        case 'f' -> value.append('\f');
                        case 'v' -> value.append((char) 11);
                        default -> value.append(escape);
                    }
                } else if (!raw && ch == '$' && offset < source.length() && source.charAt(offset) == '{') {
                    offset++;
                    interpolation(nesting + 1);
                    value.append('$').append('{').append('}');
                } else {
                    value.append(ch);
                }
            }
            throw malformed();
        }

        private void trimOpeningLine() {
            for (int cursor = offset; cursor < source.length(); cursor++) {
                char ch = source.charAt(cursor);
                if (ch == '\n' || ch == '\r') {
                    offset = cursor + 1;
                    if (ch == '\r' && offset < source.length() && source.charAt(offset) == '\n') offset++;
                    return;
                }
                if (ch == ' ' || ch == '\t') continue;
                if (ch == '\\' && cursor + 1 < source.length()
                        && " \t\r\n".indexOf(source.charAt(cursor + 1)) >= 0) continue;
                return;
            }
        }

        private void interpolation(int nesting) {
            if (nesting > 128) throw malformed();
            int depth = 1;
            while (offset < source.length()) {
                trivia();
                if (offset >= source.length()) break;
                char ch = source.charAt(offset++);
                if (ch == '{') depth++;
                else if (ch == '}' && --depth == 0) return;
                else if (quote(ch)) {
                    offset--;
                    string(false, nesting);
                } else if ((ch == 'r' || ch == 'R') && offset < source.length() && quote(source.charAt(offset))) {
                    string(true, nesting);
                }
            }
            throw malformed();
        }

        private int hex(int count) {
            if (offset + count > source.length()) throw malformed();
            int result = 0;
            for (int i = 0; i < count; i++) {
                int digit = Character.digit(source.charAt(offset++), 16);
                if (digit < 0) throw malformed();
                result = result * 16 + digit;
            }
            return result;
        }

        private static boolean quote(char ch) { return ch == '\'' || ch == '"'; }
        private static boolean identifier(char ch) {
            return Character.isLetterOrDigit(ch) || ch == '_' || ch == '$';
        }
        private static IllegalArgumentException malformed() {
            return new IllegalArgumentException("Cannot determine explicit dart:core import scope from malformed Dart source.");
        }
    }
}
