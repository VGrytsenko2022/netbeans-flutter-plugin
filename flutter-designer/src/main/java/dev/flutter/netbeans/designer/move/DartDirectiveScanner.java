package dev.flutter.netbeans.designer.move;

import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bounded lexical scanner for Dart directive URI literals.
 *
 * <p>This is deliberately not a Dart parser. It ignores comments and ordinary
 * strings, but recognizes every URI branch of import/export/part directives
 * and the URI form of {@code part of}. Anything that makes a directive URI
 * ambiguous is rejected rather than guessed.</p>
 */
public final class DartDirectiveScanner {
    private final DartMoveDependencyLimits limits;

    public DartDirectiveScanner() {
        this(DartMoveDependencyLimits.defaults());
    }

    public DartDirectiveScanner(DartMoveDependencyLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public DartMoveDependencyLimits limits() {
        return limits;
    }

    public DartDirectiveScanResult scan(DartMoveSourceSnapshot source) {
        Objects.requireNonNull(source, "source");
        if (source.size() > limits.maxSourceBytes()) {
            return rejected(
                    source.projectRelativePath(),
                    DartMoveDependencyDiagnostic.Code.SOURCE_TOO_LARGE,
                    -1,
                    "",
                    "Dart dependency scan rejected "
                    + source.projectRelativePath() + ": " + source.size()
                    + " bytes exceeds the per-file limit of "
                    + limits.maxSourceBytes() + ".");
        }

        String text;
        try {
            text = decodeStrictUtf8(source.copyBytes());
        } catch (CharacterCodingException invalidUtf8) {
            return rejected(
                    source.projectRelativePath(),
                    DartMoveDependencyDiagnostic.Code.INVALID_UTF8,
                    -1,
                    "",
                    "Dart dependency scan rejected "
                    + source.projectRelativePath()
                    + ": the source is not strict UTF-8.");
        }

        try {
            Parser parser = new Parser(source.projectRelativePath(), text, limits);
            return new DartDirectiveScanResult.Parsed(parser.scan());
        } catch (ScanFailure failure) {
            return new DartDirectiveScanResult.Rejected(failure.diagnostic());
        }
    }

    private static String decodeStrictUtf8(byte[] bytes)
            throws CharacterCodingException {
        return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString();
    }

    private static DartDirectiveScanResult.Rejected rejected(
            String sourcePath,
            DartMoveDependencyDiagnostic.Code code,
            int offset,
            String uri,
            String message) {
        return new DartDirectiveScanResult.Rejected(
                new DartMoveDependencyDiagnostic(
                        code, sourcePath, offset, uri, message));
    }

    private static final class Parser {
        private final String sourcePath;
        private final DartMoveDependencyLimits limits;
        private final Lexer lexer;
        private final List<DartDirectiveReference> references = new ArrayList<>();

        private Parser(
                String sourcePath,
                String text,
                DartMoveDependencyLimits limits) {
            this.sourcePath = sourcePath;
            this.limits = limits;
            lexer = new Lexer(sourcePath, text, limits.maxLexicalNesting());
        }

        private List<DartDirectiveReference> scan() throws ScanFailure {
            while (true) {
                Token token = lexer.next();
                if (token.kind() == TokenKind.EOF) {
                    return List.copyOf(references);
                }
                if (token.isIdentifier("import")) {
                    parseConfigurableDirective(DartDirectiveKind.IMPORT, true);
                } else if (token.isIdentifier("export")) {
                    parseConfigurableDirective(DartDirectiveKind.EXPORT, false);
                } else if (token.isIdentifier("part")) {
                    parsePartDirective();
                }
            }
        }

        private void parseConfigurableDirective(
                DartDirectiveKind kind,
                boolean allowImportQualifiers) throws ScanFailure {
            addUri(kind, requireUriToken(kind), false);
            while (true) {
                Token token = lexer.next();
                if (token.isSymbol(";")) {
                    return;
                }
                if (token.kind() == TokenKind.EOF) {
                    throw malformed(token, kind + " directive has no terminating ';'.");
                }
                if (token.isIdentifier("if")) {
                    skipConditionalTest(kind);
                    addUri(kind, requireUriToken(kind), true);
                } else if (allowImportQualifiers
                        && token.isIdentifier("deferred")) {
                    requireIdentifier("as", kind + " deferred clause requires 'as'.");
                    requireAnyIdentifier(kind + " import prefix is missing.");
                } else if (allowImportQualifiers && token.isIdentifier("as")) {
                    requireAnyIdentifier(kind + " import prefix is missing.");
                } else if (token.isIdentifier("show")
                        || token.isIdentifier("hide")) {
                    parseCombinator(kind);
                } else {
                    throw malformed(token,
                            kind + " directive contains an unsupported token before ';'.");
                }
            }
        }

        private void skipConditionalTest(DartDirectiveKind kind)
                throws ScanFailure {
            Token open = lexer.next();
            if (!open.isSymbol("(")) {
                throw malformed(open,
                        kind + " conditional URI requires a parenthesized test.");
            }
            int depth = 1;
            while (depth > 0) {
                Token token = lexer.next();
                if (token.kind() == TokenKind.EOF || token.isSymbol(";")) {
                    throw malformed(token,
                            kind + " conditional URI has an unclosed test.");
                }
                if (token.isSymbol("(")) {
                    depth++;
                    if (depth > limits.maxLexicalNesting()) {
                        throw malformed(token,
                                kind + " conditional test exceeds the nesting limit.");
                    }
                } else if (token.isSymbol(")")) {
                    depth--;
                }
            }
        }

        private void parseCombinator(DartDirectiveKind kind)
                throws ScanFailure {
            requireAnyIdentifier(kind + " combinator must name at least one symbol.");
            while (true) {
                Token token = lexer.next();
                if (!token.isSymbol(",")) {
                    lexer.push(token);
                    return;
                }
                requireAnyIdentifier(kind + " combinator has a trailing comma.");
            }
        }

        private void parsePartDirective() throws ScanFailure {
            Token token = lexer.next();
            if (token.isIdentifier("of")) {
                parsePartOf();
                return;
            }
            addUri(DartDirectiveKind.PART, requireUriToken(
                    DartDirectiveKind.PART, token), false);
            requireSemicolon(DartDirectiveKind.PART);
        }

        private void parsePartOf() throws ScanFailure {
            Token token = lexer.next();
            if (token.kind() == TokenKind.STRING) {
                addUri(DartDirectiveKind.PART_OF, token, false);
                requireSemicolon(DartDirectiveKind.PART_OF);
                return;
            }
            if (token.kind() != TokenKind.IDENTIFIER) {
                throw malformed(token,
                        "PART_OF directive requires a URI or qualified library name.");
            }
            while (true) {
                Token next = lexer.next();
                if (next.isSymbol(";")) {
                    return;
                }
                if (!next.isSymbol(".")) {
                    throw malformed(next,
                            "PART_OF library name must be a dotted identifier.");
                }
                requireAnyIdentifier("PART_OF library name ends after '.'.");
            }
        }

        private Token requireUriToken(DartDirectiveKind kind)
                throws ScanFailure {
            return requireUriToken(kind, lexer.next());
        }

        private Token requireUriToken(DartDirectiveKind kind, Token token)
                throws ScanFailure {
            if (token.kind() != TokenKind.STRING) {
                throw malformed(token, kind + " directive requires a URI literal.");
            }
            return token;
        }

        private void requireSemicolon(DartDirectiveKind kind)
                throws ScanFailure {
            Token token = lexer.next();
            if (!token.isSymbol(";")) {
                throw malformed(token, kind + " directive must end after its URI.");
            }
        }

        private void requireIdentifier(
                String expected,
                String message) throws ScanFailure {
            Token token = lexer.next();
            if (!token.isIdentifier(expected)) {
                throw malformed(token, message);
            }
        }

        private void requireAnyIdentifier(String message) throws ScanFailure {
            Token token = lexer.next();
            if (token.kind() != TokenKind.IDENTIFIER) {
                throw malformed(token, message);
            }
        }

        private void addUri(
                DartDirectiveKind kind,
                Token token,
                boolean conditional) throws ScanFailure {
            if (token.tripleQuoted()
                    || token.hasEscape()
                    || token.hasInterpolation()) {
                throw failure(
                        DartMoveDependencyDiagnostic.Code.UNSUPPORTED_URI_LITERAL,
                        token.start(),
                        token.text(),
                        kind + " URI in " + sourcePath
                        + " uses a triple-quoted, escaped, or interpolated literal; "
                        + "the move check does not guess its value.");
            }
            if (token.text().isEmpty()) {
                throw failure(
                        DartMoveDependencyDiagnostic.Code.UNSUPPORTED_URI_LITERAL,
                        token.start(),
                        "",
                        kind + " URI in " + sourcePath + " is empty.");
            }
            if (token.text().length() > limits.maxUriCharacters()) {
                throw failure(
                        DartMoveDependencyDiagnostic.Code.UNSUPPORTED_URI_LITERAL,
                        token.start(),
                        "",
                        kind + " URI in " + sourcePath + " exceeds "
                        + limits.maxUriCharacters() + " characters.");
            }
            if (references.size() >= limits.maxDirectivesPerSource()) {
                throw failure(
                        DartMoveDependencyDiagnostic.Code.TOO_MANY_DIRECTIVES,
                        token.start(),
                        token.text(),
                        "Dart dependency scan rejected " + sourcePath + ": more than "
                        + limits.maxDirectivesPerSource()
                        + " directive URI literals were found.");
            }
            references.add(new DartDirectiveReference(
                    sourcePath,
                    kind,
                    token.text(),
                    token.start(),
                    conditional));
        }

        private ScanFailure malformed(Token token, String reason) {
            return failure(
                    DartMoveDependencyDiagnostic.Code.MALFORMED_DIRECTIVE,
                    token.start(),
                    token.kind() == TokenKind.STRING ? token.text() : "",
                    "Dart dependency scan rejected " + sourcePath + " at offset "
                    + token.start() + ": " + reason);
        }

        private ScanFailure failure(
                DartMoveDependencyDiagnostic.Code code,
                int offset,
                String uri,
                String message) {
            return new ScanFailure(new DartMoveDependencyDiagnostic(
                    code, sourcePath, offset, uri, message));
        }
    }

    private enum TokenKind {
        IDENTIFIER,
        STRING,
        SYMBOL,
        EOF
    }

    private record Token(
            TokenKind kind,
            String text,
            int start,
            boolean tripleQuoted,
            boolean hasEscape,
            boolean hasInterpolation) {

        private static Token identifier(String text, int start) {
            return new Token(TokenKind.IDENTIFIER, text, start, false, false, false);
        }

        private static Token symbol(char symbol, int start) {
            return new Token(
                    TokenKind.SYMBOL,
                    Character.toString(symbol),
                    start,
                    false,
                    false,
                    false);
        }

        private static Token eof(int offset) {
            return new Token(TokenKind.EOF, "", offset, false, false, false);
        }

        private boolean isIdentifier(String candidate) {
            return kind == TokenKind.IDENTIFIER && text.equals(candidate);
        }

        private boolean isSymbol(String candidate) {
            return kind == TokenKind.SYMBOL && text.equals(candidate);
        }
    }

    private static final class Lexer {
        private final String sourcePath;
        private final String text;
        private final int maxNesting;
        private int index;
        private Token pushed;

        private Lexer(String sourcePath, String text, int maxNesting) {
            this.sourcePath = sourcePath;
            this.text = text;
            this.maxNesting = maxNesting;
        }

        private void push(Token token) {
            if (pushed != null) {
                throw new IllegalStateException("Only one token of pushback is supported");
            }
            pushed = token;
        }

        private Token next() throws ScanFailure {
            if (pushed != null) {
                Token result = pushed;
                pushed = null;
                return result;
            }
            skipTrivia();
            if (index >= text.length()) {
                return Token.eof(index);
            }
            int start = index;
            char current = text.charAt(index);
            if ((current == 'r' || current == 'R')
                    && index + 1 < text.length()
                    && isQuote(text.charAt(index + 1))) {
                index++;
                return scanString(start, true, 0);
            }
            if (isQuote(current)) {
                return scanString(start, false, 0);
            }
            if (isIdentifierStart(current)) {
                index++;
                while (index < text.length()
                        && isIdentifierPart(text.charAt(index))) {
                    index++;
                }
                return Token.identifier(text.substring(start, index), start);
            }
            index++;
            return Token.symbol(current, start);
        }

        private void skipTrivia() throws ScanFailure {
            while (index < text.length()) {
                char current = text.charAt(index);
                if (Character.isWhitespace(current) || current == '\ufeff') {
                    index++;
                } else if (startsWith("//", index)) {
                    index += 2;
                    while (index < text.length()
                            && text.charAt(index) != '\n'
                            && text.charAt(index) != '\r') {
                        index++;
                    }
                } else if (startsWith("/*", index)) {
                    skipBlockComment();
                } else {
                    return;
                }
            }
        }

        private void skipBlockComment() throws ScanFailure {
            int start = index;
            index += 2;
            int depth = 1;
            while (index < text.length()) {
                if (startsWith("/*", index)) {
                    depth++;
                    if (depth > maxNesting) {
                        throw lexicalFailure(start,
                                "nested block comment exceeds the lexical nesting limit.");
                    }
                    index += 2;
                } else if (startsWith("*/", index)) {
                    index += 2;
                    depth--;
                    if (depth == 0) {
                        return;
                    }
                } else {
                    index++;
                }
            }
            throw lexicalFailure(start, "block comment is not terminated.");
        }

        private Token scanString(
                int tokenStart,
                boolean raw,
                int interpolationNesting) throws ScanFailure {
            int quoteStart = index;
            char quote = text.charAt(index);
            boolean triple = startsWith(
                    new String(new char[]{quote, quote, quote}), index);
            index += triple ? 3 : 1;
            StringBuilder content = new StringBuilder();
            boolean escaped = false;
            boolean interpolated = false;
            while (index < text.length()) {
                if (isTerminator(quote, triple)) {
                    index += triple ? 3 : 1;
                    return new Token(
                            TokenKind.STRING,
                            content.toString(),
                            tokenStart,
                            triple,
                            escaped,
                            interpolated);
                }
                char current = text.charAt(index);
                if (!triple && (current == '\n' || current == '\r')) {
                    throw lexicalFailure(quoteStart,
                            "single-line string literal is not terminated.");
                }
                if (!raw && current == '\\') {
                    escaped = true;
                    index++;
                    if (index >= text.length()) {
                        throw lexicalFailure(quoteStart,
                                "string escape is not terminated.");
                    }
                    if (text.charAt(index) == '\r'
                            && index + 1 < text.length()
                            && text.charAt(index + 1) == '\n') {
                        index += 2;
                    } else {
                        index++;
                    }
                } else if (!raw && current == '$') {
                    interpolated = true;
                    index++;
                    if (index < text.length() && text.charAt(index) == '{') {
                        index++;
                        skipInterpolation(quoteStart, interpolationNesting + 1);
                    } else {
                        while (index < text.length()
                                && isIdentifierPart(text.charAt(index))) {
                            index++;
                        }
                    }
                } else {
                    content.append(current);
                    index++;
                }
            }
            throw lexicalFailure(quoteStart, "string literal is not terminated.");
        }

        private void skipInterpolation(int stringStart, int nesting)
                throws ScanFailure {
            if (nesting > maxNesting) {
                throw lexicalFailure(stringStart,
                        "string interpolation exceeds the lexical nesting limit.");
            }
            int braces = 1;
            while (index < text.length()) {
                if (startsWith("//", index)) {
                    index += 2;
                    while (index < text.length()
                            && text.charAt(index) != '\n'
                            && text.charAt(index) != '\r') {
                        index++;
                    }
                    continue;
                }
                if (startsWith("/*", index)) {
                    skipBlockComment();
                    continue;
                }
                char current = text.charAt(index);
                if ((current == 'r' || current == 'R')
                        && index + 1 < text.length()
                        && isQuote(text.charAt(index + 1))) {
                    int start = index;
                    index++;
                    scanString(start, true, nesting);
                } else if (isQuote(current)) {
                    scanString(index, false, nesting);
                } else if (current == '{') {
                    braces++;
                    if (braces > maxNesting) {
                        throw lexicalFailure(stringStart,
                                "string interpolation exceeds the lexical nesting limit.");
                    }
                    index++;
                } else if (current == '}') {
                    index++;
                    braces--;
                    if (braces == 0) {
                        return;
                    }
                } else {
                    index++;
                }
            }
            throw lexicalFailure(stringStart,
                    "string interpolation is not terminated.");
        }

        private boolean isTerminator(char quote, boolean triple) {
            if (triple) {
                return index + 2 < text.length()
                        && text.charAt(index) == quote
                        && text.charAt(index + 1) == quote
                        && text.charAt(index + 2) == quote;
            }
            return text.charAt(index) == quote;
        }

        private boolean startsWith(String value, int at) {
            return text.regionMatches(at, value, 0, value.length());
        }

        private ScanFailure lexicalFailure(int offset, String reason) {
            return new ScanFailure(new DartMoveDependencyDiagnostic(
                    DartMoveDependencyDiagnostic.Code.MALFORMED_DART_LEXEME,
                    sourcePath,
                    offset,
                    "",
                    "Dart dependency scan rejected " + sourcePath + " at offset "
                    + offset + ": " + reason));
        }

        private static boolean isQuote(char value) {
            return value == '\'' || value == '"';
        }

        private static boolean isIdentifierStart(char value) {
            return value == '_' || value == '$' || Character.isLetter(value);
        }

        private static boolean isIdentifierPart(char value) {
            return isIdentifierStart(value) || Character.isDigit(value);
        }
    }

    private static final class ScanFailure extends Exception {
        private final DartMoveDependencyDiagnostic diagnostic;

        private ScanFailure(DartMoveDependencyDiagnostic diagnostic) {
            super(diagnostic.message());
            this.diagnostic = diagnostic;
        }

        private DartMoveDependencyDiagnostic diagnostic() {
            return diagnostic;
        }
    }
}
