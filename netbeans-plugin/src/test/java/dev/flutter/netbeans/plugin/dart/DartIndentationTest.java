package dev.flutter.netbeans.plugin.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.text.BadLocationException;
import org.junit.jupiter.api.Test;
import org.netbeans.editor.BaseDocument;
import org.netbeans.api.lexer.Language;

class DartIndentationTest {
    @Test
    void indentsAfterOpeningBraceAndAlignsClosingBrace() throws Exception {
        String source = "void main() {\n\n}\n";

        assertEquals(2, indentAt(source, 1, 1));
        assertEquals(0, indentAt(source, 2, 2));
    }

    @Test
    void tracksNestedBlocksListsAndCalls() throws Exception {
        String source = """
                void main() {
                  final values = build([
                    1,
                  ]);
                }
                """;

        assertEquals(2, indentAt(source, 1, 1));
        assertEquals(6, indentAt(source, 2, 2));
        assertEquals(2, indentAt(source, 3, 3));
        assertEquals(0, indentAt(source, 4, 4));
    }

    @Test
    void ignoresDelimitersInStringsAndComments() throws Exception {
        String source = """
                void main() {
                  final text = r'{[( not code';
                  // { [ (
                  /* nested { /* [ */ ( */
                  print(text);
                }
                """;

        assertEquals(2, indentAt(source, 4, 4));
        assertEquals(0, indentAt(source, 5, 5));
    }

    @Test
    void preservesInteriorLinesOfTripleStringsAndBlockComments() throws Exception {
        String source = """
                void main() {
                  final text = '''first
                    keep { this exact text
                  last''';
                  /* comment
                       keep comment layout
                   */
                }
                """;

        assertEquals(DartIndentation.KEEP, indentAt(source, 2, 2));
        assertEquals(DartIndentation.KEEP, indentAt(source, 5, 5));
    }

    @Test
    void interpolationDelimitersDoNotLeakIntoFollowingCode() throws Exception {
        String source = """
                void main() {
                  final value = '${{'items': [1, 2]}}';
                  print(value);
                }
                """;

        assertEquals(2, indentAt(source, 2, 2));
        assertEquals(0, indentAt(source, 3, 3));
    }

    @Test
    void mismatchedClosingDelimitersNeverProduceNegativeIndent() throws Exception {
        String source = ")] }\nvalue;\n";

        assertEquals(0, indentAt(source, 0, 0));
        assertEquals(0, indentAt(source, 1, 1));
    }

    @Test
    void leavesUnrelatedBlankLinesWithoutTrailingWhitespace() throws Exception {
        String source = "void main() {\n\n\n}\n";
        int secondBlankStart = lineStart(source, 2);
        var decisions = DartIndentation.plan(
                document(source),
                lineStart(source, 1),
                secondBlankStart,
                lineStart(source, 1),
                2);

        assertEquals(2, decisions.get(0).indent());
        assertEquals(DartIndentation.KEEP, decisions.get(1).indent());
    }

    @Test
    void expandsOnlyARealEmptyCodeBlock() throws Exception {
        assertTrue(DartIndentation.shouldExpandBreak(document("void f() {}"), 10));
        assertFalse(DartIndentation.shouldExpandBreak(document("final s = '{}';"), 12));
        assertFalse(DartIndentation.shouldExpandBreak(document("final s = '${value}';"), 13));
        assertFalse(DartIndentation.shouldExpandBreak(document("/* {} */"), 4));
    }

    @Test
    void recognizesOnlyLeadingCodeClosingBracesForReindent() throws Exception {
        String leading = "  }";
        String inline = "work(); }";
        String string = "  '${value}'";

        assertTrue(DartIndentation.isCodeSeparatorAt(document(leading), 2, '}'));
        assertTrue(DartIndentation.isFirstNonWhitespaceOnLine(document(leading), 2));
        assertFalse(DartIndentation.isFirstNonWhitespaceOnLine(document(inline), 8));
        assertFalse(DartIndentation.isCodeSeparatorAt(
                document(string), string.length() - 2, '}'));
    }

    @Test
    void handlesCrLfLineStarts() throws Exception {
        String source = "void main() {\r\n\r\n}\r\n";

        assertEquals(2, indentAt(source, 1, 1));
        assertEquals(0, indentAt(source, 2, 2));
    }

    @Test
    void usesTheIncrementalDocumentHierarchyAfterAnEdit() throws Exception {
        String original = "void main() {\n\n}\n";
        BaseDocument document = document(original);
        var hierarchy = org.netbeans.api.lexer.TokenHierarchy.get(document);
        int insertion = "void main() {\n".length();
        document.insertString(insertion, "  if (true) {\n", null);
        int blankLine = insertion + "  if (true) {\n".length();

        assertTrue(hierarchy == org.netbeans.api.lexer.TokenHierarchy.get(document));
        assertEquals(4, DartIndentation.plan(
                document, blankLine, blankLine, blankLine, 2).get(0).indent());
    }

    @Test
    void boundsDensePrefixScanAndUsesTheMeaningfulPreviousLineFallback()
            throws Exception {
        StringBuilder source = new StringBuilder("{\n");
        for (int index = 0; index < DartIndentation.MAX_PREFIX_SCAN_TOKENS + 64; index++) {
            source.append("        work();\n");
        }
        source.append("        build([\n");
        int blankLine = source.length();
        source.append('\n');

        assertTrue(blankLine < DartIndentation.MAX_PREFIX_SCAN_CHARACTERS,
                "the token budget, not the character budget, must select the fallback");
        assertEquals(12, DartIndentation.plan(
                document(source.toString()), blankLine, blankLine, blankLine, 2)
                .get(0)
                .indent());
    }

    @Test
    void boundsLongPrefixScanByCharacterBudget() throws Exception {
        StringBuilder source = new StringBuilder("{\n        ");
        source.append("x".repeat(DartIndentation.MAX_PREFIX_SCAN_CHARACTERS));
        source.append(";\n        build([\n");
        int blankLine = source.length();
        source.append('\n');

        assertTrue(blankLine > DartIndentation.MAX_PREFIX_SCAN_CHARACTERS);
        assertEquals(12, DartIndentation.plan(
                document(source.toString()), blankLine, blankLine, blankLine, 2)
                .get(0)
                .indent());
    }

    private static int indentAt(String source, int line, int caretLine) throws Exception {
        BaseDocument document = document(source);
        String actualSource = document.getText(0, document.getLength());
        int start = lineStart(actualSource, line);
        int caret = lineStart(actualSource, caretLine);
        return DartIndentation.plan(
                document, start, start, caret, 2).get(0).indent();
    }

    private static int lineStart(String source, int line) {
        int offset = 0;
        for (int current = 0; current < line; current++) {
            offset = source.indexOf('\n', offset) + 1;
        }
        return offset;
    }

    private static BaseDocument document(String source) throws BadLocationException {
        BaseDocument document = new BaseDocument(false, DartTokenId.MIME_TYPE);
        document.putProperty(Language.class, DartTokenId.language());
        document.insertString(0, source, null);
        return document;
    }
}
