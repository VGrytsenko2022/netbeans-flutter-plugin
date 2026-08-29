package dev.flutter.netbeans.project.theme;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Strict transformation of the fresh Flutter counter template's root MaterialApp. */
final class FlutterGeneratedMainThemeTransformer {
    private static final String MATERIAL_IMPORT = "import 'package:flutter/material.dart';";
    private static final String THEME_IMPORT = "import 'theme/app_theme.dart';";

    String transform(String source) throws IOException {
        String newline = source.contains("\r\n") ? "\r\n" : "\n";
        int materialImportStart = uniqueWholeLine(source, MATERIAL_IMPORT);
        if (wholeLineCount(source, THEME_IMPORT) != 0) {
            throw new IOException("Generated lib/main.dart already imports "
                    + "theme/app_theme.dart");
        }
        int materialImportEnd = lineSeparatorEnd(source, materialImportStart);
        if (materialImportEnd < 0) {
            throw new IOException("Generated lib/main.dart material import has no line ending");
        }

        List<Token> tokens = tokenize(source);
        int materialOpen = findUniqueRootMaterialApp(tokens);
        int materialClose = matching(tokens, materialOpen, "(", ")");
        Property theme = null;
        for (int index = materialOpen + 1; index < materialClose; index++) {
            Token token = tokens.get(index);
            if (!isDirectMaterialArgument(tokens, materialOpen, index)) {
                continue;
            }
            if ((token.text().equals("darkTheme") || token.text().equals("themeMode"))
                    && isSymbol(tokens, index + 1, ":")) {
                throw new IOException("Generated root MaterialApp already declares "
                        + token.text());
            }
            if (token.text().equals("theme") && isSymbol(tokens, index + 1, ":")) {
                if (theme != null) {
                    throw new IOException(
                            "Generated root MaterialApp contains more than one theme property");
                }
                int expression = index + 2;
                if (expression + 1 >= tokens.size()
                        || !tokens.get(expression).text().equals("ThemeData")
                        || !isSymbol(tokens, expression + 1, "(")) {
                    throw new IOException(
                            "Generated root MaterialApp theme is not a ThemeData expression");
                }
                int expressionClose = matching(tokens, expression + 1, "(", ")");
                int comma = expressionClose + 1;
                if (!isSymbol(tokens, comma, ",")) {
                    throw new IOException(
                            "Generated root MaterialApp theme has no terminal comma");
                }
                theme = new Property(
                        token.start(),
                        tokens.get(expression).start(),
                        tokens.get(expressionClose).end(),
                        tokens.get(comma).end());
            }
        }
        if (theme == null) {
            throw new IOException(
                    "Generated root MaterialApp has no unique ThemeData theme property");
        }

        String indent = lineIndent(source, theme.fieldStart());
        int themeLineEnd = lineSeparatorEnd(source, theme.commaEnd());
        if (themeLineEnd < 0 || !source.substring(theme.commaEnd(),
                themeLineEnd - newline.length()).isBlank()) {
            throw new IOException(
                    "Generated root MaterialApp theme has unsupported trailing content");
        }

        StringBuilder transformed = new StringBuilder(source);
        transformed.insert(themeLineEnd,
                indent + "darkTheme: AppTheme.dark," + newline
                + indent + "themeMode: AppTheme.mode," + newline);
        transformed.replace(theme.expressionStart(), theme.expressionEnd(), "AppTheme.light");
        transformed.insert(materialImportEnd, THEME_IMPORT + newline);
        return transformed.toString();
    }

    private static int findUniqueRootMaterialApp(List<Token> tokens) throws IOException {
        int found = -1;
        for (int index = 0; index + 2 < tokens.size(); index++) {
            if (tokens.get(index).text().equals("return")
                    && tokens.get(index + 1).text().equals("MaterialApp")
                    && tokens.get(index + 2).text().equals("(")) {
                if (found >= 0) {
                    throw new IOException(
                            "Generated lib/main.dart contains more than one returned MaterialApp");
                }
                found = index + 2;
            }
        }
        if (found < 0) {
            throw new IOException(
                    "Generated lib/main.dart has no supported returned MaterialApp");
        }
        return found;
    }

    private static boolean isDirectMaterialArgument(
            List<Token> tokens, int materialOpen, int candidate) throws IOException {
        int parentheses = 1;
        int brackets = 0;
        int braces = 0;
        for (int index = materialOpen + 1; index < candidate; index++) {
            String token = tokens.get(index).text();
            switch (token) {
                case "(" -> parentheses++;
                case ")" -> parentheses--;
                case "[" -> brackets++;
                case "]" -> brackets--;
                case "{" -> braces++;
                case "}" -> braces--;
                default -> { }
            }
            if (parentheses < 1 || brackets < 0 || braces < 0) {
                throw new IOException("Generated lib/main.dart has unbalanced delimiters");
            }
        }
        return parentheses == 1 && brackets == 0 && braces == 0;
    }

    private static int matching(
            List<Token> tokens, int openIndex, String open, String close) throws IOException {
        if (!isSymbol(tokens, openIndex, open)) {
            throw new IOException("Expected opening delimiter " + open);
        }
        int depth = 0;
        for (int index = openIndex; index < tokens.size(); index++) {
            String token = tokens.get(index).text();
            if (token.equals(open)) {
                depth++;
            } else if (token.equals(close) && --depth == 0) {
                return index;
            }
        }
        throw new IOException("Generated lib/main.dart has an unmatched " + open);
    }

    private static boolean isSymbol(List<Token> tokens, int index, String value) {
        return index >= 0 && index < tokens.size() && tokens.get(index).text().equals(value);
    }

    private static List<Token> tokenize(String source) throws IOException {
        List<Token> tokens = new ArrayList<>();
        int index = 0;
        while (index < source.length()) {
            char current = source.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
                continue;
            }
            if (current == '/' && index + 1 < source.length()
                    && source.charAt(index + 1) == '/') {
                index += 2;
                while (index < source.length() && source.charAt(index) != '\n') {
                    index++;
                }
                continue;
            }
            if (current == '/' && index + 1 < source.length()
                    && source.charAt(index + 1) == '*') {
                index = skipBlockComment(source, index);
                continue;
            }
            boolean raw = (current == 'r' || current == 'R')
                    && index + 1 < source.length()
                    && (source.charAt(index + 1) == '\'' || source.charAt(index + 1) == '"');
            if (current == '\'' || current == '"' || raw) {
                int start = index;
                index = skipString(source, index, raw);
                tokens.add(new Token(source.substring(start, index), start, index));
                continue;
            }
            if (Character.isLetter(current) || current == '_' || current == '$') {
                int start = index++;
                while (index < source.length()) {
                    char next = source.charAt(index);
                    if (!Character.isLetterOrDigit(next) && next != '_' && next != '$') {
                        break;
                    }
                    index++;
                }
                tokens.add(new Token(source.substring(start, index), start, index));
                continue;
            }
            tokens.add(new Token(String.valueOf(current), index, index + 1));
            index++;
        }
        return List.copyOf(tokens);
    }

    private static int skipBlockComment(String source, int start) throws IOException {
        int depth = 1;
        int index = start + 2;
        while (index + 1 < source.length()) {
            if (source.charAt(index) == '/' && source.charAt(index + 1) == '*') {
                depth++;
                index += 2;
            } else if (source.charAt(index) == '*' && source.charAt(index + 1) == '/') {
                depth--;
                index += 2;
                if (depth == 0) {
                    return index;
                }
            } else {
                index++;
            }
        }
        throw new IOException("Generated lib/main.dart contains an unterminated block comment");
    }

    private static int skipString(String source, int start, boolean raw) throws IOException {
        int quoteIndex = raw ? start + 1 : start;
        char quote = source.charAt(quoteIndex);
        boolean triple = quoteIndex + 2 < source.length()
                && source.charAt(quoteIndex + 1) == quote
                && source.charAt(quoteIndex + 2) == quote;
        int index = quoteIndex + (triple ? 3 : 1);
        while (index < source.length()) {
            if (!raw && source.charAt(index) == '\\') {
                index += Math.min(2, source.length() - index);
                continue;
            }
            if (triple) {
                if (index + 2 < source.length()
                        && source.charAt(index) == quote
                        && source.charAt(index + 1) == quote
                        && source.charAt(index + 2) == quote) {
                    return index + 3;
                }
            } else if (source.charAt(index) == quote) {
                return index + 1;
            }
            index++;
        }
        throw new IOException("Generated lib/main.dart contains an unterminated string");
    }

    private static int uniqueWholeLine(String source, String expected) throws IOException {
        int count = wholeLineCount(source, expected);
        if (count != 1) {
            throw new IOException("Generated lib/main.dart must contain exactly one line: "
                    + expected);
        }
        int cursor = 0;
        while (cursor <= source.length()) {
            int end = source.indexOf('\n', cursor);
            if (end < 0) {
                end = source.length();
            }
            String line = source.substring(cursor, end);
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            if (line.equals(expected)) {
                return cursor;
            }
            if (end == source.length()) {
                break;
            }
            cursor = end + 1;
        }
        throw new IOException("Generated lib/main.dart does not contain " + expected);
    }

    private static int wholeLineCount(String source, String expected) {
        int count = 0;
        int cursor = 0;
        while (cursor <= source.length()) {
            int end = source.indexOf('\n', cursor);
            if (end < 0) {
                end = source.length();
            }
            String line = source.substring(cursor, end);
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            if (line.equals(expected)) {
                count++;
            }
            if (end == source.length()) {
                break;
            }
            cursor = end + 1;
        }
        return count;
    }

    private static int lineSeparatorEnd(String source, int position) {
        int newline = source.indexOf('\n', position);
        return newline < 0 ? -1 : newline + 1;
    }

    private static String lineIndent(String source, int position) {
        int lineStart = source.lastIndexOf('\n', Math.max(0, position - 1)) + 1;
        int cursor = lineStart;
        while (cursor < position
                && (source.charAt(cursor) == ' ' || source.charAt(cursor) == '\t')) {
            cursor++;
        }
        return source.substring(lineStart, cursor);
    }

    private record Token(String text, int start, int end) {
    }

    private record Property(
            int fieldStart,
            int expressionStart,
            int expressionEnd,
            int commaEnd) {
    }
}
