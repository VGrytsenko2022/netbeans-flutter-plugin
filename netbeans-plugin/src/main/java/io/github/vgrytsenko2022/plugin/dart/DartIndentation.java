package io.github.vgrytsenko2022.plugin.dart;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Segment;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.editor.indent.api.IndentUtils;

/** Lexer-aware indentation decisions shared by the editor typing hooks. */
final class DartIndentation {
    static final int KEEP = -1;
    static final int MAX_PREFIX_SCAN_TOKENS = 4_096;
    static final int MAX_PREFIX_SCAN_CHARACTERS = 128 * 1_024;
    private static final int MAX_LOCAL_LOOKBACK_LINES = 64;
    private static final int MAX_LOCAL_LOOKBACK_CHARACTERS = 16 * 1_024;

    private DartIndentation() {
    }

    static List<Decision> plan(
            Document document,
            int startOffset,
            int endOffset,
            int caretOffset,
            int indentSize) throws BadLocationException {
        return render(document, () -> planLocked(
                document, startOffset, endOffset, caretOffset, indentSize));
    }

    private static List<Decision> planLocked(
            Document document,
            int startOffset,
            int endOffset,
            int caretOffset,
            int indentSize) throws BadLocationException {
        int length = document.getLength();
        int boundedStart = Math.max(0, Math.min(startOffset, length));
        int boundedEnd = Math.max(boundedStart, Math.min(endOffset, length));
        int boundedCaret = Math.max(0, Math.min(caretOffset, length));
        Element root = document.getDefaultRootElement();
        int firstLineIndex = root.getElementIndex(boundedStart);
        int lastLineIndex = root.getElementIndex(boundedEnd);
        int caretLineIndex = root.getElementIndex(boundedCaret);
        int effectiveIndentSize = Math.max(1, indentSize);

        TokenSequence<DartTokenId> sequence = tokenSequence(document);
        if (sequence == null) {
            return localPlanLocked(
                    document,
                    root,
                    firstLineIndex,
                    lastLineIndex,
                    caretLineIndex,
                    effectiveIndentSize);
        }
        TokenCursor cursor = TokenCursor.start(sequence);
        TokenInfo previous = null;
        Deque<Character> delimiters = new ArrayDeque<>();
        List<Decision> decisions = new ArrayList<>(lastLineIndex - firstLineIndex + 1);

        for (int lineIndex = firstLineIndex; lineIndex <= lastLineIndex; lineIndex++) {
            Element line = root.getElement(lineIndex);
            int lineStart = Math.min(line.getStartOffset(), length);
            int lineEnd = Math.min(line.getEndOffset(), length);
            if (lineStart > MAX_PREFIX_SCAN_CHARACTERS) {
                appendLocalDecisionsLocked(
                        document,
                        root,
                        lineIndex,
                        lastLineIndex,
                        caretLineIndex,
                        effectiveIndentSize,
                        decisions);
                return decisions;
            }
            while (cursor.current() != null && cursor.current().offset() < lineStart) {
                if (cursor.visitedTokens() >= MAX_PREFIX_SCAN_TOKENS) {
                    appendLocalDecisionsLocked(
                            document,
                            root,
                            lineIndex,
                            lastLineIndex,
                            caretLineIndex,
                            effectiveIndentSize,
                            decisions);
                    return decisions;
                }
                previous = cursor.current();
                updateDelimiters(delimiters, previous);
                cursor.advance();
            }

            if (previous != null
                    && previous.offset() < lineStart
                    && previous.end() > lineStart
                    && isProtected(previous.id())) {
                decisions.add(new Decision(lineStart, KEEP));
                continue;
            }

            Segment lineText = segment(document, lineStart, lineEnd - lineStart);
            int firstContentRelative = firstNonWhitespace(lineText);
            if (firstContentRelative < 0) {
                decisions.add(new Decision(
                        lineStart,
                        lineIndex == caretLineIndex
                                ? delimiters.size() * effectiveIndentSize
                                : KEEP));
                continue;
            }

            int firstContent = lineStart + firstContentRelative;
            int depth = depthAfterLeadingClosers(
                    document,
                    lineText,
                    lineStart,
                    firstContent,
                    delimiters);
            decisions.add(new Decision(lineStart, depth * effectiveIndentSize));
        }
        return decisions;
    }

    private static List<Decision> localPlanLocked(
            Document document,
            Element root,
            int firstLineIndex,
            int lastLineIndex,
            int caretLineIndex,
            int indentSize) throws BadLocationException {
        List<Decision> decisions = new ArrayList<>(lastLineIndex - firstLineIndex + 1);
        appendLocalDecisionsLocked(
                document,
                root,
                firstLineIndex,
                lastLineIndex,
                caretLineIndex,
                indentSize,
                decisions);
        return decisions;
    }

    private static void appendLocalDecisionsLocked(
            Document document,
            Element root,
            int firstLineIndex,
            int lastLineIndex,
            int caretLineIndex,
            int indentSize,
            List<Decision> decisions) throws BadLocationException {
        for (int lineIndex = firstLineIndex; lineIndex <= lastLineIndex; lineIndex++) {
            decisions.add(localDecisionLocked(
                    document, root, lineIndex, caretLineIndex, indentSize));
        }
    }

    private static Decision localDecisionLocked(
            Document document,
            Element root,
            int lineIndex,
            int caretLineIndex,
            int indentSize) throws BadLocationException {
        int length = document.getLength();
        Element line = root.getElement(lineIndex);
        int lineStart = Math.min(line.getStartOffset(), length);
        int lineEnd = Math.min(line.getEndOffset(), length);
        if (isProtectedInteriorLocked(document, lineStart)) {
            return new Decision(lineStart, KEEP);
        }

        Segment lineText = segment(document, lineStart, lineEnd - lineStart);
        int firstContentRelative = firstNonWhitespace(lineText);
        if (firstContentRelative < 0 && lineIndex != caretLineIndex) {
            return new Decision(lineStart, KEEP);
        }

        LineAnchor anchor = previousMeaningfulLineLocked(document, root, lineIndex);
        int baseIndent = anchor == null
                ? IndentUtils.lineIndent(document, lineStart)
                : anchor.indent();
        int openingDepth = anchor == null
                ? 0
                : unmatchedOpeningDepthLocked(document, anchor.start(), anchor.end());
        int closingDepth = firstContentRelative < 0
                ? 0
                : leadingClosingDepthLocked(
                        document, lineText, lineStart, firstContentRelative);
        int indent = Math.max(0, baseIndent + (openingDepth - closingDepth) * indentSize);
        return new Decision(lineStart, indent);
    }

    private static LineAnchor previousMeaningfulLineLocked(
            Document document,
            Element root,
            int lineIndex) throws BadLocationException {
        int currentStart = Math.min(
                root.getElement(lineIndex).getStartOffset(), document.getLength());
        int checkedLines = 0;
        for (int candidateIndex = lineIndex - 1;
                candidateIndex >= 0 && checkedLines < MAX_LOCAL_LOOKBACK_LINES;
                candidateIndex--, checkedLines++) {
            Element candidate = root.getElement(candidateIndex);
            int start = Math.min(candidate.getStartOffset(), document.getLength());
            if (currentStart - start > MAX_LOCAL_LOOKBACK_CHARACTERS) {
                break;
            }
            int end = Math.min(candidate.getEndOffset(), document.getLength());
            Segment text = segment(document, start, end - start);
            if (firstNonWhitespace(text) < 0 || isProtectedInteriorLocked(document, start)) {
                continue;
            }
            return new LineAnchor(start, end, IndentUtils.lineIndent(document, start));
        }
        return null;
    }

    private static int unmatchedOpeningDepthLocked(
            Document document,
            int lineStart,
            int lineEnd) {
        TokenSequence<DartTokenId> sequence = tokenSequence(document);
        if (sequence == null) {
            return 0;
        }
        Deque<Character> delimiters = new ArrayDeque<>();
        sequence.move(lineStart);
        if (!sequence.moveNext()) {
            return 0;
        }
        do {
            if (sequence.offset() >= lineEnd) {
                break;
            }
            updateDelimiters(delimiters, TokenInfo.from(sequence));
        } while (sequence.moveNext());
        return delimiters.size();
    }

    private static int leadingClosingDepthLocked(
            Document document,
            Segment lineText,
            int lineStart,
            int firstContentRelative) throws BadLocationException {
        int depth = 0;
        int relative = firstContentRelative;
        while (relative < lineText.length()) {
            while (relative < lineText.length()
                    && Character.isWhitespace(lineText.charAt(relative))) {
                relative++;
            }
            if (relative >= lineText.length()) {
                break;
            }
            char character = lineText.charAt(relative);
            if (!isClosing(character)
                    || !isCodeSeparatorAtLocked(document, lineStart + relative, character)) {
                break;
            }
            depth++;
            relative++;
        }
        return depth;
    }

    private static boolean isProtectedInteriorLocked(Document document, int lineStart) {
        TokenInfo token = tokenAtLocked(document, lineStart);
        return token != null
                && token.offset() < lineStart
                && token.end() > lineStart
                && isProtected(token.id());
    }

    static boolean shouldExpandBreak(Document document, int offset)
            throws BadLocationException {
        return render(document, () -> offset > 0
                && offset < document.getLength()
                && isCodeSeparatorAtLocked(document, offset - 1, '{')
                && isCodeSeparatorAtLocked(document, offset, '}'));
    }

    static boolean isCodeSeparatorAt(Document document, int offset, char expected)
            throws BadLocationException {
        return render(document, () -> isCodeSeparatorAtLocked(document, offset, expected));
    }

    private static boolean isCodeSeparatorAtLocked(
            Document document,
            int offset,
            char expected) throws BadLocationException {
        if (offset < 0 || offset >= document.getLength()
                || document.getText(offset, 1).charAt(0) != expected) {
            return false;
        }
        TokenInfo token = tokenAtLocked(document, offset);
        return token != null
                && token.offset() == offset
                && token.id() == DartTokenId.SEPARATOR
                && token.delimiter() == expected;
    }

    private static TokenInfo tokenAtLocked(Document document, int offset) {
        if (offset < 0 || offset >= document.getLength()) {
            return null;
        }
        TokenSequence<DartTokenId> sequence = tokenSequence(document);
        if (sequence == null) {
            return null;
        }
        sequence.move(offset);
        if (!sequence.moveNext()) {
            return null;
        }
        TokenInfo token = TokenInfo.from(sequence);
        return token.offset() <= offset && token.end() > offset ? token : null;
    }

    static boolean isFirstNonWhitespaceOnLine(Document document, int offset)
            throws BadLocationException {
        return render(document, () ->
                isFirstNonWhitespaceOnLineLocked(document, offset));
    }

    private static boolean isFirstNonWhitespaceOnLineLocked(
            Document document,
            int offset) throws BadLocationException {
        if (offset < 0 || offset >= document.getLength()) {
            return false;
        }
        int start = lineStartLocked(document, offset);
        Segment prefix = segment(document, start, offset - start);
        for (int index = 0; index < prefix.length(); index++) {
            if (!Character.isWhitespace(prefix.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    static int lineStart(Document document, int offset) {
        int[] result = new int[1];
        document.render(() -> result[0] = lineStartLocked(document, offset));
        return result[0];
    }

    private static int lineStartLocked(Document document, int offset) {
        int bounded = Math.max(0, Math.min(offset, document.getLength()));
        Element root = document.getDefaultRootElement();
        return Math.min(root.getElement(root.getElementIndex(bounded)).getStartOffset(),
                document.getLength());
    }

    private static TokenSequence<DartTokenId> tokenSequence(Document document) {
        TokenHierarchy<?> hierarchy = TokenHierarchy.get(document);
        return hierarchy == null ? null : hierarchy.tokenSequence(DartTokenId.language());
    }

    private static Segment segment(Document document, int offset, int length)
            throws BadLocationException {
        Segment result = new Segment();
        document.getText(offset, length, result);
        return result;
    }

    private static int firstNonWhitespace(Segment text) {
        for (int index = 0; index < text.length(); index++) {
            if (!Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private static void updateDelimiters(Deque<Character> delimiters, TokenInfo token) {
        if (token.id() != DartTokenId.SEPARATOR || token.delimiter() == 0) {
            return;
        }
        char delimiter = token.delimiter();
        if (isOpening(delimiter)) {
            delimiters.push(delimiter);
        } else if (isClosing(delimiter)
                && !delimiters.isEmpty()
                && matches(delimiters.peek(), delimiter)) {
            delimiters.pop();
        }
    }

    private static int depthAfterLeadingClosers(
            Document document,
            Segment lineText,
            int lineStart,
            int firstContent,
            Deque<Character> delimiters) throws BadLocationException {
        Deque<Character> remaining = new ArrayDeque<>(delimiters);
        int relative = firstContent - lineStart;
        while (relative < lineText.length()) {
            while (relative < lineText.length()
                    && Character.isWhitespace(lineText.charAt(relative))) {
                relative++;
            }
            if (relative >= lineText.length()) {
                break;
            }
            int offset = lineStart + relative;
            char character = lineText.charAt(relative);
            if (!isClosing(character)
                    || remaining.isEmpty()
                    || !matches(remaining.peek(), character)
                    || !isCodeSeparatorAtLocked(document, offset, character)) {
                break;
            }
            remaining.pop();
            relative++;
        }
        return remaining.size();
    }

    private static boolean isProtected(DartTokenId id) {
        return id == DartTokenId.STRING
                || id == DartTokenId.COMMENT
                || id == DartTokenId.DOC_COMMENT
                || id == DartTokenId.ERROR;
    }

    private static boolean isOpening(char value) {
        return value == '{' || value == '[' || value == '(';
    }

    private static boolean isClosing(char value) {
        return value == '}' || value == ']' || value == ')';
    }

    private static boolean matches(char opening, char closing) {
        return opening == '{' && closing == '}'
                || opening == '[' && closing == ']'
                || opening == '(' && closing == ')';
    }

    private static <T> T render(Document document, DocumentOperation<T> operation)
            throws BadLocationException {
        Object[] result = new Object[1];
        BadLocationException[] failure = new BadLocationException[1];
        document.render(() -> {
            try {
                result[0] = operation.run();
            } catch (BadLocationException ex) {
                failure[0] = ex;
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
        @SuppressWarnings("unchecked")
        T typedResult = (T) result[0];
        return typedResult;
    }

    record Decision(int lineStart, int indent) {
    }

    private record LineAnchor(int start, int end, int indent) {
    }

    @FunctionalInterface
    private interface DocumentOperation<T> {
        T run() throws BadLocationException;
    }

    private record TokenInfo(int offset, int end, DartTokenId id, char delimiter) {
        static TokenInfo from(TokenSequence<DartTokenId> sequence) {
            boolean separator = sequence.token().id() == DartTokenId.SEPARATOR
                    && sequence.token().length() == 1;
            return new TokenInfo(
                    sequence.offset(),
                    sequence.offset() + sequence.token().length(),
                    sequence.token().id(),
                    separator ? sequence.token().text().charAt(0) : 0);
        }
    }

    private static final class TokenCursor {
        private final TokenSequence<DartTokenId> sequence;
        private TokenInfo current;
        private int visitedTokens;

        private TokenCursor(TokenSequence<DartTokenId> sequence) {
            this.sequence = sequence;
            advance();
        }

        static TokenCursor start(TokenSequence<DartTokenId> sequence) {
            if (sequence != null) {
                sequence.moveStart();
            }
            return new TokenCursor(sequence);
        }

        TokenInfo current() {
            return current;
        }

        int visitedTokens() {
            return visitedTokens;
        }

        void advance() {
            if (sequence != null && sequence.moveNext()) {
                current = TokenInfo.from(sequence);
                visitedTokens++;
            } else {
                current = null;
            }
        }
    }
}
