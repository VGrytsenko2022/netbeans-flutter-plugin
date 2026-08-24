package dev.flutter.netbeans.plugin.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import org.junit.jupiter.api.Test;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;

class DartLexerTest {
    @Test
    void exposesStableMimeTypeAndTokenCategories() {
        Language<DartTokenId> language = DartTokenId.language();

        assertEquals(DartTokenId.MIME_TYPE, language.mimeType());
        assertEquals(DartTokenId.MIME_TYPE, new DartEditorKit().getContentType());
        assertEquals(Set.copyOf(Arrays.asList(DartTokenId.values())), language.tokenIds());
        assertEquals(
                Arrays.stream(DartTokenId.values())
                        .map(DartTokenId::primaryCategory)
                        .collect(Collectors.toSet()),
                language.tokenCategories());
    }

    @Test
    void tokenizesRepresentativeDartSourceWithoutLosingText() {
        String source = """
                /// Counter documentation.
                @override
                class Counter {
                  final int value = 42;
                  bool enabled = true;
                  void add() { value += 1; }
                }
                """;

        List<LexedToken> tokens = lex(source);

        assertContains(tokens, DartTokenId.DOC_COMMENT, "/// Counter documentation.\n");
        assertContains(tokens, DartTokenId.ANNOTATION, "@");
        assertContains(tokens, DartTokenId.KEYWORD, "class");
        assertContains(tokens, DartTokenId.IDENTIFIER, "Counter");
        assertContains(tokens, DartTokenId.BUILT_IN_TYPE, "int");
        assertContains(tokens, DartTokenId.NUMBER, "42");
        assertContains(tokens, DartTokenId.LITERAL, "true");
        assertContains(tokens, DartTokenId.OPERATOR, "+=");
        assertContains(tokens, DartTokenId.SEPARATOR, "{");
        assertFalse(tokens.stream().anyMatch(token -> token.id() == DartTokenId.ERROR));
    }

    @Test
    void keepsRawStringsWholeAndDoesNotInterpolateThem() {
        String source = "r'' r\"\" r'$name' r\"${value}\"";

        List<LexedToken> tokens = withoutWhitespace(lex(source));

        assertEquals(List.of(
                new LexedToken(DartTokenId.STRING, "r''", 0),
                new LexedToken(DartTokenId.STRING, "r\"\"", 4),
                new LexedToken(DartTokenId.STRING, "r'$name'", 8),
                new LexedToken(DartTokenId.STRING, "r\"${value}\"", 17)), tokens);
        assertFalse(tokens.stream()
                .anyMatch(token -> token.id() == DartTokenId.STRING_INTERPOLATION));
    }

    @Test
    void tokenizesNestedCommentsAndStringInterpolation() {
        String source = "/* outer /* inner */ outer */ \"Hello $name: ${count + 1}\"";

        List<LexedToken> tokens = lex(source);

        assertContains(tokens, DartTokenId.COMMENT, "/* outer /* inner */ outer */");
        assertContains(tokens, DartTokenId.STRING_INTERPOLATION, "$name");
        assertContains(tokens, DartTokenId.STRING_INTERPOLATION, "${");
        assertContains(tokens, DartTokenId.STRING_INTERPOLATION, "}");
        assertContains(tokens, DartTokenId.IDENTIFIER, "count");
        assertFalse(tokens.stream().anyMatch(token -> token.id() == DartTokenId.ERROR));
    }

    @Test
    void preservesUnterminatedConstructsAtEndOfFile() {
        assertEquals(
                List.of(new LexedToken(DartTokenId.ERROR, "'unterminated", 0)),
                lex("'unterminated"));
        assertEquals(
                List.of(new LexedToken(DartTokenId.COMMENT, "/* unterminated", 0)),
                lex("/* unterminated"));
    }

    @Test
    void treatsDollarBeforeClosingQuoteAsStringContent() {
        List<LexedToken> tokens = lex("\"Price: $\"");

        assertFalse(tokens.stream()
                .anyMatch(token -> token.id() == DartTokenId.STRING_INTERPOLATION));
        assertFalse(tokens.stream().anyMatch(token -> token.id() == DartTokenId.ERROR));
        assertTrue(tokens.stream().allMatch(token -> token.id() == DartTokenId.STRING));
    }

    @Test
    void supportsUnicodeIdentifiersAndAdvancesPastMalformedSurrogates() {
        String supplementaryLetter = new String(Character.toChars(0x10400));
        List<LexedToken> identifiers = withoutWhitespace(
                lex("лічильник " + supplementaryLetter + "Value"));

        assertEquals(List.of(
                new LexedToken(DartTokenId.IDENTIFIER, "лічильник", 0),
                new LexedToken(
                        DartTokenId.IDENTIFIER,
                        supplementaryLetter + "Value",
                        "лічильник ".length())), identifiers);

        String malformed = String.valueOf((char) 0xD801) + 'x';
        assertEquals(List.of(
                new LexedToken(DartTokenId.ERROR, String.valueOf((char) 0xD801), 0),
                new LexedToken(DartTokenId.IDENTIFIER, "x", 1)), lex(malformed));
    }

    @Test
    void restoresNestedStringAndBraceStatesInsideInterpolation() {
        String source = "\"value ${build({'key': '${nested}'})}\"";

        List<LexedToken> tokens = lex(source);

        assertEquals(2, tokens.stream()
                .filter(token -> token.id() == DartTokenId.STRING_INTERPOLATION)
                .filter(token -> token.text().equals("${"))
                .count());
        assertEquals(2, tokens.stream()
                .filter(token -> token.id() == DartTokenId.STRING_INTERPOLATION)
                .filter(token -> token.text().equals("}"))
                .count());
        assertContains(tokens, DartTokenId.SEPARATOR, "}");
        assertContains(tokens, DartTokenId.IDENTIFIER, "nested");
        assertFalse(tokens.stream().anyMatch(token -> token.id() == DartTokenId.ERROR));
    }

    @Test
    void recoversWhenBlockCommentIsCompletedAfterEndOfFile() throws BadLocationException {
        TestDocument document = dartDocument("/* open");
        TokenHierarchy<TestDocument> hierarchy = TokenHierarchy.get(document);

        assertEquals(DartTokenId.COMMENT, tokenAt(hierarchy, 0).id());

        document.insertString(document.getLength(), " */\nfinal value = 1;", null);
        String completed = document.getText(0, document.getLength());
        assertEquals(
                DartTokenId.KEYWORD,
                tokenAt(hierarchy, completed.indexOf("final")).id());
    }

    @Test
    void incrementallyRelexesWhenBlockCommentDelimiterIsRemovedAndRestored()
            throws BadLocationException {
        String source = "/* header */\nfinal int value = 1;";
        TestDocument document = dartDocument(source);
        TokenHierarchy<TestDocument> hierarchy = TokenHierarchy.get(document);
        int delimiterOffset = source.indexOf("*/");

        assertEquals(DartTokenId.KEYWORD, tokenAt(hierarchy, source.indexOf("final")).id());

        document.remove(delimiterOffset, 2);
        String unterminated = document.getText(0, document.getLength());
        assertEquals(
                DartTokenId.COMMENT,
                tokenAt(hierarchy, unterminated.indexOf("final")).id());

        document.insertString(delimiterOffset, "*/", null);
        String restored = document.getText(0, document.getLength());
        assertEquals(
                DartTokenId.KEYWORD,
                tokenAt(hierarchy, restored.indexOf("final")).id());
    }

    @Test
    void incrementallyRelexesWhenTripleStringDelimiterIsRemovedAndRestored()
            throws BadLocationException {
        String source = "final text = '''first\nsecond''';\nreturn;";
        TestDocument document = dartDocument(source);
        TokenHierarchy<TestDocument> hierarchy = TokenHierarchy.get(document);
        int delimiterOffset = source.lastIndexOf("'''");

        assertEquals(DartTokenId.KEYWORD, tokenAt(hierarchy, source.indexOf("return")).id());

        document.remove(delimiterOffset, 3);
        String unterminated = document.getText(0, document.getLength());
        assertEquals(
                DartTokenId.ERROR,
                tokenAt(hierarchy, unterminated.indexOf("return")).id());

        document.insertString(delimiterOffset, "'''", null);
        String restored = document.getText(0, document.getLength());
        assertEquals(
                DartTokenId.KEYWORD,
                tokenAt(hierarchy, restored.indexOf("return")).id());
    }

    private static TestDocument dartDocument(String text) throws BadLocationException {
        TestDocument document = new TestDocument();
        document.putProperty(Language.class, DartTokenId.language());
        TokenHierarchy.get(document);
        document.insertString(0, text, null);
        return document;
    }

    private static LexedToken tokenAt(TokenHierarchy<?> hierarchy, int offset) {
        for (LexedToken token : lex(hierarchy)) {
            if (offset >= token.offset() && offset < token.offset() + token.text().length()) {
                return token;
            }
        }
        throw new AssertionError("No Dart token at offset " + offset);
    }

    private static List<LexedToken> lex(String text) {
        return lex(TokenHierarchy.create(text, DartTokenId.language()));
    }

    private static List<LexedToken> lex(TokenHierarchy<?> hierarchy) {
        TokenSequence<DartTokenId> sequence = hierarchy.tokenSequence(DartTokenId.language());
        assertNotNull(sequence, "missing Dart token sequence");
        List<LexedToken> result = new ArrayList<>();
        StringBuilder reconstructed = new StringBuilder();
        while (sequence.moveNext()) {
            String text = sequence.token().text().toString();
            result.add(new LexedToken(sequence.token().id(), text, sequence.offset()));
            reconstructed.append(text);
        }
        String inputText = inputText(hierarchy);
        String tokenText = reconstructed.toString();
        if (hierarchy.inputSource() instanceof Document) {
            assertTrue(
                    tokenText.equals(inputText) || tokenText.equals(inputText + '\n'),
                    () -> "Token text differs from document text: '" + tokenText + "'");
        } else {
            assertEquals(inputText, tokenText);
        }
        return result;
    }

    private static String inputText(TokenHierarchy<?> hierarchy) {
        Object input = hierarchy.inputSource();
        if (input instanceof Document document) {
            try {
                return document.getText(0, document.getLength());
            } catch (BadLocationException ex) {
                throw new AssertionError("Cannot read lexer test document", ex);
            }
        }
        return input.toString();
    }

    private static List<LexedToken> withoutWhitespace(List<LexedToken> tokens) {
        return tokens.stream()
                .filter(token -> token.id() != DartTokenId.WHITESPACE)
                .toList();
    }

    private static void assertContains(
            List<LexedToken> tokens,
            DartTokenId id,
            String text) {
        assertTrue(
                tokens.stream().anyMatch(token -> token.id() == id && token.text().equals(text)),
                () -> "Missing " + id + " token '" + text + "' in " + tokens);
    }

    private record LexedToken(DartTokenId id, String text, int offset) {
    }

    /**
     * Delegating document with explicit lock introspection for the standalone lexer API.
     * NetBeans normally gets this information from its editor document implementation.
     */
    public static final class TestDocument implements Document {
        private final PlainDocument delegate = new PlainDocument();

        public boolean isReadLocked() {
            return true;
        }

        public boolean isWriteLocked() {
            return true;
        }

        @Override
        public int getLength() {
            return delegate.getLength();
        }

        @Override
        public void addDocumentListener(DocumentListener listener) {
            delegate.addDocumentListener(listener);
        }

        @Override
        public void removeDocumentListener(DocumentListener listener) {
            delegate.removeDocumentListener(listener);
        }

        @Override
        public void addUndoableEditListener(UndoableEditListener listener) {
            delegate.addUndoableEditListener(listener);
        }

        @Override
        public void removeUndoableEditListener(UndoableEditListener listener) {
            delegate.removeUndoableEditListener(listener);
        }

        @Override
        public Object getProperty(Object key) {
            return delegate.getProperty(key);
        }

        @Override
        public void putProperty(Object key, Object value) {
            delegate.putProperty(key, value);
        }

        @Override
        public void remove(int offset, int length) throws BadLocationException {
            delegate.remove(offset, length);
        }

        @Override
        public void insertString(int offset, String text, AttributeSet attributes)
                throws BadLocationException {
            delegate.insertString(offset, text, attributes);
        }

        @Override
        public String getText(int offset, int length) throws BadLocationException {
            return delegate.getText(offset, length);
        }

        @Override
        public void getText(int offset, int length, Segment segment)
                throws BadLocationException {
            delegate.getText(offset, length, segment);
        }

        @Override
        public Position getStartPosition() {
            return delegate.getStartPosition();
        }

        @Override
        public Position getEndPosition() {
            return delegate.getEndPosition();
        }

        @Override
        public Position createPosition(int offset) throws BadLocationException {
            return delegate.createPosition(offset);
        }

        @Override
        public Element[] getRootElements() {
            return delegate.getRootElements();
        }

        @Override
        public Element getDefaultRootElement() {
            return delegate.getDefaultRootElement();
        }

        @Override
        public void render(Runnable runnable) {
            delegate.render(runnable);
        }
    }
}
