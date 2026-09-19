package io.github.vgrytsenko2022.dart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DartIgnoreForFileMaskerTest {
    @Test
    void masksOnlyActiveLineCommentDirectivesAtEqualLength() {
        String source = """
                final ordinary = '// ignore_for_file: invalid_assignment';
                final raw = r"// ignore_for_file: invalid_assignment";
                final triple = '''
                // ignore_for_file: invalid_assignment
                ''';
                /* // ignore_for_file: invalid_assignment
                   /* nested // ignore_for_file: invalid_assignment */
                */
                // ignore_for_file : invalid_assignment, undefined_identifier
                void main() {}
                """;

        DartIgnoreForFileMasker.Result result =
                DartIgnoreForFileMasker.mask(source);

        assertEquals(1, result.directivesMasked());
        assertEquals(source.length(), result.content().length());
        assertTrue(result.content().contains(
                "'// ignore_for_file: invalid_assignment'"));
        assertTrue(result.content().contains(
                "r\"// ignore_for_file: invalid_assignment\""));
        assertTrue(result.content().contains(
                "/* // ignore_for_file: invalid_assignment"));
        int active = source.lastIndexOf("ignore_for_file");
        assertEquals(" ".repeat("ignore_for_file".length()),
                result.content().substring(
                        active, active + "ignore_for_file".length()));
    }

    @Test
    void scansInterpolationCodeButNotTripleStringText() {
        String source = """
                final value = """ + "\"\"\"" + """
                literal // ignore_for_file: invalid_assignment
                ${(() {
                  // ignore_for_file: invalid_assignment
                  return 1;
                })()}
                """ + "\"\"\"" + ";\n";

        DartIgnoreForFileMasker.Result result =
                DartIgnoreForFileMasker.mask(source);

        assertEquals(1, result.directivesMasked());
        assertEquals(source.length(), result.content().length());
        assertTrue(result.content().contains(
                "literal // ignore_for_file: invalid_assignment"));
        int active = source.lastIndexOf("ignore_for_file");
        assertEquals(" ".repeat("ignore_for_file".length()),
                result.content().substring(
                        active, active + "ignore_for_file".length()));
    }

    @Test
    void preservesOffsetsBeyondSixtyFourKiB() {
        String prefix = "final value = '" + "x".repeat(70_000) + "';\n";
        String directive = "// ignore_for_file: invalid_assignment\n";
        String source = prefix + directive + "void main() {}\n";

        DartIgnoreForFileMasker.Result result =
                DartIgnoreForFileMasker.mask(source);

        assertEquals(1, result.directivesMasked());
        assertEquals(source.length(), result.content().length());
        assertEquals(prefix, result.content().substring(0, prefix.length()));
        assertFalse(result.content().substring(prefix.length())
                .startsWith("// ignore_for_file"));
        int active = prefix.length() + "// ".length();
        assertEquals(" ".repeat("ignore_for_file".length()),
                result.content().substring(
                        active, active + "ignore_for_file".length()));
    }
}
