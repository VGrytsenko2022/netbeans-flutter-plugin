package dev.flutter.netbeans.plugin.dart;

import java.util.Set;
import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;

/** Incremental Dart lexer with nested comments and string interpolation support. */
final class DartLexer implements Lexer<DartTokenId> {
    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "as", "assert", "async", "await", "base", "break",
            "case", "catch", "class", "const", "continue", "covariant",
            "default", "deferred", "do", "else", "enum", "export", "extends",
            "extension", "external", "factory", "final", "finally", "for", "get",
            "hide", "if", "implements", "import", "in", "interface", "is", "late",
            "library", "mixin", "new", "of", "on", "operator", "part", "required",
            "rethrow", "return", "sealed", "set", "show", "static", "super", "switch",
            "sync", "this", "throw", "try", "type", "typedef", "var", "when", "while",
            "with", "yield");
    private static final Set<String> BUILT_IN_TYPES = Set.of(
            "bool", "double", "dynamic", "Function", "Future", "int", "Iterable",
            "List", "Map", "Never", "Null", "num", "Object", "Record", "Runes",
            "Set", "Stream", "String", "Symbol", "void");
    private static final Set<String> LITERALS = Set.of("true", "false", "null");

    private final LexerInput input;
    private final TokenFactory<DartTokenId> tokens;
    private Mode mode;
    private CodeState codeState;
    private StringState stringState;
    private int blockCommentDepth;
    private int blockCommentPrevious;
    private boolean documentationComment;

    DartLexer(LexerRestartInfo<DartTokenId> info) {
        input = info.input();
        tokens = info.tokenFactory();
        Object saved = info.state();
        if (saved instanceof RestartState state) {
            mode = state.mode();
            codeState = state.codeState();
            stringState = state.stringState();
            blockCommentDepth = state.blockCommentDepth();
            blockCommentPrevious = state.blockCommentPrevious();
            documentationComment = state.documentationComment();
        } else {
            mode = Mode.CODE;
            codeState = CodeState.topLevel();
        }
    }

    @Override
    public Token<DartTokenId> nextToken() {
        return switch (mode) {
            case CODE -> lexCode();
            case STRING -> lexString();
            case BLOCK_COMMENT -> lexBlockComment();
        };
    }

    private Token<DartTokenId> lexCode() {
        int character = input.read();
        if (character == LexerInput.EOF) {
            return null;
        }
        if (Character.isWhitespace(character)) {
            return whitespace();
        }
        if (consumeIdentifierStart(character)) {
            if (character == 'r') {
                Token<DartTokenId> rawString = rawStringIfPresent();
                if (rawString != null) {
                    return rawString;
                }
            }
            return identifier();
        }
        if (isDecimalDigit(character)) {
            return number(character);
        }
        return switch (character) {
            case '\'', '"' -> beginString(character, false);
            case '/' -> slashOrComment();
            case '#' -> hashOrScriptComment();
            case '@' -> token(DartTokenId.ANNOTATION);
            case '(', ')', '[', ']', ';', ',', ':' -> token(DartTokenId.SEPARATOR);
            case '{' -> openBrace();
            case '}' -> closeBrace();
            case '.' -> dotOrNumber();
            default -> isOperatorCharacter(character)
                    ? operator()
                    : token(DartTokenId.ERROR);
        };
    }

    private Token<DartTokenId> whitespace() {
        int character;
        while ((character = input.read()) != LexerInput.EOF) {
            if (!Character.isWhitespace(character)) {
                input.backup(1);
                break;
            }
        }
        return token(DartTokenId.WHITESPACE);
    }

    private Token<DartTokenId> identifier() {
        int character;
        while ((character = input.read()) != LexerInput.EOF) {
            if (!consumeIdentifierPart(character)) {
                input.backup(1);
                break;
            }
        }
        String value = input.readText().toString();
        if (LITERALS.contains(value)) {
            return token(DartTokenId.LITERAL);
        }
        if (BUILT_IN_TYPES.contains(value)) {
            return token(DartTokenId.BUILT_IN_TYPE);
        }
        return token(KEYWORDS.contains(value) ? DartTokenId.KEYWORD : DartTokenId.IDENTIFIER);
    }

    private Token<DartTokenId> rawStringIfPresent() {
        int quote = input.read();
        if (quote != '\'' && quote != '"') {
            if (quote != LexerInput.EOF) {
                input.backup(1);
            }
            return null;
        }
        return beginString(quote, true);
    }

    private Token<DartTokenId> beginString(int quote, boolean raw) {
        if (prepareString(quote, raw)) {
            return lexString();
        }
        return token(DartTokenId.STRING);
    }

    /** Returns true when more string content follows the consumed opening delimiter. */
    private boolean prepareString(int quote, boolean raw) {
        CodeState returnCode = codeState;
        int second = input.read();
        if (second == quote) {
            int third = input.read();
            if (third == quote) {
                stringState = new StringState(quote, true, raw, returnCode);
                mode = Mode.STRING;
                return true;
            }
            if (third != LexerInput.EOF) {
                input.backup(1);
            }
            return false;
        }
        if (second != LexerInput.EOF) {
            input.backup(1);
        }
        stringState = new StringState(quote, false, raw, returnCode);
        mode = Mode.STRING;
        return true;
    }

    private Token<DartTokenId> lexString() {
        StringState current = stringState;
        while (true) {
            int character = input.read();
            if (character == LexerInput.EOF) {
                return hasReadCharacters() ? token(DartTokenId.ERROR) : null;
            }
            if (!current.raw() && character == '\\') {
                int escaped = input.read();
                if (escaped == LexerInput.EOF) {
                    return token(DartTokenId.ERROR);
                }
                continue;
            }
            if (!current.triple() && (character == '\r' || character == '\n')) {
                restoreCodeAfterString(current);
                return token(DartTokenId.ERROR);
            }
            if (character == current.quote()) {
                if (!current.triple()) {
                    restoreCodeAfterString(current);
                    return token(DartTokenId.STRING);
                }
                int second = input.read();
                if (second == current.quote()) {
                    int third = input.read();
                    if (third == current.quote()) {
                        restoreCodeAfterString(current);
                        return token(DartTokenId.STRING);
                    }
                    if (third != LexerInput.EOF) {
                        input.backup(1);
                    }
                } else if (second != LexerInput.EOF) {
                    input.backup(1);
                }
                continue;
            }
            if (!current.raw() && character == '$') {
                if (actualReadLength() > 1) {
                    input.backup(1);
                    return token(DartTokenId.STRING);
                }
                int next = input.read();
                if (next == '{') {
                    codeState = new CodeState(1, current);
                    stringState = null;
                    mode = Mode.CODE;
                    return token(DartTokenId.STRING_INTERPOLATION);
                }
                if (consumeIdentifierStart(next)) {
                    while ((next = input.read()) != LexerInput.EOF) {
                        if (!consumeIdentifierPart(next)) {
                            input.backup(1);
                            break;
                        }
                    }
                    return token(DartTokenId.STRING_INTERPOLATION);
                }
                if (next == LexerInput.EOF) {
                    return token(DartTokenId.ERROR);
                }
                input.backup(1);
            }
        }
    }

    private void restoreCodeAfterString(StringState completed) {
        codeState = completed.returnCode();
        stringState = null;
        mode = Mode.CODE;
    }

    private Token<DartTokenId> slashOrComment() {
        int next = input.read();
        if (next == '/') {
            int third = input.read();
            boolean documentation = third == '/';
            return lineComment(documentation);
        }
        if (next == '*') {
            int third = input.read();
            documentationComment = third == '*';
            if (third != LexerInput.EOF) {
                input.backup(1);
            }
            mode = Mode.BLOCK_COMMENT;
            blockCommentDepth = 1;
            blockCommentPrevious = 0;
            return lexBlockComment();
        }
        if (next != LexerInput.EOF) {
            input.backup(1);
        }
        return operator();
    }

    private Token<DartTokenId> hashOrScriptComment() {
        int next = input.read();
        if (next == '!') {
            return lineComment(false);
        }
        if (next != LexerInput.EOF) {
            input.backup(1);
        }
        return operator();
    }

    private Token<DartTokenId> lineComment(boolean documentation) {
        int character;
        while ((character = input.read()) != LexerInput.EOF) {
            if (character == '\n') {
                break;
            }
            if (character == '\r') {
                int next = input.read();
                if (next != '\n' && next != LexerInput.EOF) {
                    input.backup(1);
                }
                break;
            }
        }
        return token(documentation ? DartTokenId.DOC_COMMENT : DartTokenId.COMMENT);
    }

    private Token<DartTokenId> lexBlockComment() {
        int previous = blockCommentPrevious;
        int character;
        while ((character = input.read()) != LexerInput.EOF) {
            if (previous == '/' && character == '*') {
                blockCommentDepth++;
                previous = 0;
                continue;
            }
            if (previous == '*' && character == '/') {
                blockCommentDepth--;
                previous = 0;
                if (blockCommentDepth == 0) {
                    mode = Mode.CODE;
                    blockCommentPrevious = 0;
                    return token(documentationComment
                            ? DartTokenId.DOC_COMMENT
                            : DartTokenId.COMMENT);
                }
                continue;
            }
            previous = character;
        }
        blockCommentPrevious = previous;
        return hasReadCharacters()
                ? token(documentationComment ? DartTokenId.DOC_COMMENT : DartTokenId.COMMENT)
                : null;
    }

    private Token<DartTokenId> number(int first) {
        if (first == '0') {
            int prefix = input.read();
            if (prefix == 'x' || prefix == 'X') {
                consumeDigits(16);
                return token(DartTokenId.NUMBER);
            }
            if (prefix == 'b' || prefix == 'B') {
                consumeDigits(2);
                return token(DartTokenId.NUMBER);
            }
            if (prefix != LexerInput.EOF) {
                input.backup(1);
            }
        }
        consumeDecimalDigits();
        int character = input.read();
        if (character == '.') {
            int next = input.read();
            if (isDecimalDigit(next)) {
                consumeDecimalDigits();
            } else {
                if (next != LexerInput.EOF) {
                    input.backup(1);
                }
                input.backup(1);
            }
        } else if (character != LexerInput.EOF) {
            input.backup(1);
        }
        consumeExponentIfPresent();
        return token(DartTokenId.NUMBER);
    }

    private Token<DartTokenId> dotOrNumber() {
        int next = input.read();
        if (isDecimalDigit(next)) {
            consumeDecimalDigits();
            consumeExponentIfPresent();
            return token(DartTokenId.NUMBER);
        }
        if (next != LexerInput.EOF) {
            input.backup(1);
        }
        return operator();
    }

    private void consumeDigits(int radix) {
        int character;
        while ((character = input.read()) != LexerInput.EOF) {
            if (character != '_' && !isAsciiDigitForRadix(character, radix)) {
                input.backup(1);
                break;
            }
        }
    }

    private void consumeDecimalDigits() {
        int character;
        while ((character = input.read()) != LexerInput.EOF) {
            if (character != '_' && !isDecimalDigit(character)) {
                input.backup(1);
                break;
            }
        }
    }

    private void consumeExponentIfPresent() {
        int exponent = input.read();
        if (exponent != 'e' && exponent != 'E') {
            if (exponent != LexerInput.EOF) {
                input.backup(1);
            }
            return;
        }
        int signOrDigit = input.read();
        if (signOrDigit == '+' || signOrDigit == '-') {
            consumeDecimalDigits();
        } else if (isDecimalDigit(signOrDigit)) {
            consumeDecimalDigits();
        } else if (signOrDigit != LexerInput.EOF) {
            input.backup(1);
        }
    }

    private Token<DartTokenId> openBrace() {
        if (codeState.interpolationDepth() > 0) {
            codeState = new CodeState(
                    codeState.interpolationDepth() + 1,
                    codeState.resumeString());
        }
        return token(DartTokenId.SEPARATOR);
    }

    private Token<DartTokenId> closeBrace() {
        if (codeState.interpolationDepth() == 1) {
            stringState = codeState.resumeString();
            codeState = stringState.returnCode();
            mode = Mode.STRING;
            return token(DartTokenId.STRING_INTERPOLATION);
        }
        if (codeState.interpolationDepth() > 1) {
            codeState = new CodeState(
                    codeState.interpolationDepth() - 1,
                    codeState.resumeString());
        }
        return token(DartTokenId.SEPARATOR);
    }

    private Token<DartTokenId> operator() {
        int character;
        while ((character = input.read()) != LexerInput.EOF) {
            if (!isOperatorCharacter(character)) {
                input.backup(1);
                break;
            }
        }
        return token(DartTokenId.OPERATOR);
    }

    private Token<DartTokenId> token(DartTokenId id) {
        return tokens.createToken(id);
    }

    private int actualReadLength() {
        // LexerInput.readLength() already excludes a previously read EOF marker.
        return input.readLength();
    }

    private boolean hasReadCharacters() {
        return actualReadLength() > 0;
    }

    private boolean consumeIdentifierStart(int character) {
        return consumeIdentifierCodePoint(character, true);
    }

    private boolean consumeIdentifierPart(int character) {
        return consumeIdentifierCodePoint(character, false);
    }

    /**
     * Checks one UTF-16 code point and leaves a valid low surrogate consumed.
     * If a high surrogate is not followed by a matching low surrogate, the
     * look-ahead character is backed up so the caller can end the token before
     * the malformed code unit.
     */
    private boolean consumeIdentifierCodePoint(int first, boolean start) {
        if (first == LexerInput.EOF || first == '$') {
            return false;
        }
        if (!Character.isHighSurrogate((char) first)) {
            return first == '_'
                    || (start
                            ? Character.isUnicodeIdentifierStart(first)
                            : Character.isUnicodeIdentifierPart(first));
        }

        int second = input.read();
        if (second != LexerInput.EOF && Character.isLowSurrogate((char) second)) {
            int codePoint = Character.toCodePoint((char) first, (char) second);
            boolean identifierCodePoint = start
                    ? Character.isUnicodeIdentifierStart(codePoint)
                    : Character.isUnicodeIdentifierPart(codePoint);
            if (identifierCodePoint) {
                return true;
            }
        }
        // LexerInput counts EOF as one backed-up item as well.
        input.backup(1);
        return false;
    }

    private static boolean isDecimalDigit(int character) {
        return character >= '0' && character <= '9';
    }

    private static boolean isAsciiDigitForRadix(int character, int radix) {
        if (isDecimalDigit(character)) {
            return character - '0' < radix;
        }
        if (radix > 10) {
            int lower = Character.toLowerCase(character);
            return lower >= 'a' && lower < 'a' + (radix - 10);
        }
        return false;
    }

    private static boolean isOperatorCharacter(int character) {
        return switch (character) {
            case '+', '-', '*', '/', '%', '=', '!', '<', '>', '&', '|', '^', '~',
                 '?', '.', '#' -> true;
            default -> false;
        };
    }

    @Override
    public Object state() {
        if (mode == Mode.CODE
                && codeState.interpolationDepth() == 0
                && codeState.resumeString() == null) {
            return null;
        }
        return new RestartState(
                mode,
                codeState,
                stringState,
                blockCommentDepth,
                blockCommentPrevious,
                documentationComment);
    }

    @Override
    public void release() {
        // No external resources.
    }

    private enum Mode {
        CODE,
        STRING,
        BLOCK_COMMENT
    }

    private record CodeState(int interpolationDepth, StringState resumeString) {
        static CodeState topLevel() {
            return new CodeState(0, null);
        }
    }

    private record StringState(int quote, boolean triple, boolean raw, CodeState returnCode) {
    }

    private record RestartState(
            Mode mode,
            CodeState codeState,
            StringState stringState,
            int blockCommentDepth,
            int blockCommentPrevious,
            boolean documentationComment) {
    }
}
