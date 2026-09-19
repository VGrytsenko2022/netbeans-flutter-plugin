package io.github.vgrytsenko2022.designer.move;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DartDirectiveScannerTest {
    private final DartDirectiveScanner scanner = new DartDirectiveScanner();

    @Test
    void scansAllConditionalUrisAndPartFormsWhileIgnoringLexicalNoise() {
        String source = """
                // import 'ignored_line.dart';
                /* export 'ignored_block.dart'; /* part 'nested.dart'; */ */
                final ordinary = "import 'ignored_string.dart';";
                final interpolated = "outside ${"export 'ignored_nested.dart';"}";
                final triple = '''part 'ignored_triple.dart';''';

                import 'base.dart'
                    if (dart.library.io) 'io.dart'
                    if (dart.library.html) r'web.dart'
                    deferred as platform show value;
                export 'api.dart'
                    if (dart.library.js_interop) 'api_web.dart'
                    hide internal;
                part 'screen.g.dart';
                part of 'screen.dart';
                part of example.library;
                """;

        DartDirectiveScanResult.Parsed parsed = assertInstanceOf(
                DartDirectiveScanResult.Parsed.class,
                scanner.scan(snapshot("lib/screen.dart", source)));

        assertAll(
                () -> assertEquals(7, parsed.references().size()),
                () -> assertEquals(
                        List.of(
                                "base.dart",
                                "io.dart",
                                "web.dart",
                                "api.dart",
                                "api_web.dart",
                                "screen.g.dart",
                                "screen.dart"),
                        parsed.references().stream()
                                .map(DartDirectiveReference::uri)
                                .toList()),
                () -> assertEquals(
                        List.of(false, true, true, false, true, false, false),
                        parsed.references().stream()
                                .map(DartDirectiveReference::conditional)
                                .toList()),
                () -> assertEquals(
                        List.of(
                                DartDirectiveKind.IMPORT,
                                DartDirectiveKind.IMPORT,
                                DartDirectiveKind.IMPORT,
                                DartDirectiveKind.EXPORT,
                                DartDirectiveKind.EXPORT,
                                DartDirectiveKind.PART,
                                DartDirectiveKind.PART_OF),
                        parsed.references().stream()
                                .map(DartDirectiveReference::kind)
                                .toList()));
    }

    @ParameterizedTest
    @MethodSource("unsupportedUriLiterals")
    void rejectsUriLiteralsWhoseValueWouldRequireEvaluation(String source) {
        DartDirectiveScanResult.Rejected rejected = assertInstanceOf(
                DartDirectiveScanResult.Rejected.class,
                scanner.scan(snapshot("lib/source.dart", source)));

        assertEquals(
                DartMoveDependencyDiagnostic.Code.UNSUPPORTED_URI_LITERAL,
                rejected.diagnostic().code());
    }

    private static Stream<Arguments> unsupportedUriLiterals() {
        return Stream.of(
                Arguments.of("import '''package:http/http.dart''';"),
                Arguments.of("import 'package:http\\/http.dart';"),
                Arguments.of("import 'package:$name/http.dart';"));
    }

    @Test
    void rejectsInvalidUtf8AndUnterminatedLexemes() {
        DartDirectiveScanResult.Rejected invalidUtf8 = assertInstanceOf(
                DartDirectiveScanResult.Rejected.class,
                scanner.scan(new DartMoveSourceSnapshot(
                        "lib/source.dart", new byte[]{(byte) 0xc3, (byte) 0x28})));
        DartDirectiveScanResult.Rejected comment = assertInstanceOf(
                DartDirectiveScanResult.Rejected.class,
                scanner.scan(snapshot("lib/source.dart", "/* never closed")));
        DartDirectiveScanResult.Rejected string = assertInstanceOf(
                DartDirectiveScanResult.Rejected.class,
                scanner.scan(snapshot("lib/source.dart", "final x = 'never closed")));

        assertAll(
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.INVALID_UTF8,
                        invalidUtf8.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.MALFORMED_DART_LEXEME,
                        comment.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.MALFORMED_DART_LEXEME,
                        string.diagnostic().code()));
    }

    @Test
    void rejectsMalformedConditionalDirectiveInsteadOfMissingItsBranch() {
        DartDirectiveScanResult.Rejected rejected = assertInstanceOf(
                DartDirectiveScanResult.Rejected.class,
                scanner.scan(snapshot(
                        "lib/source.dart",
                        "import 'base.dart' if (dart.library.io);")));

        assertAll(
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.MALFORMED_DIRECTIVE,
                        rejected.diagnostic().code()),
                () -> assertTrue(rejected.diagnostic().message()
                        .contains("requires a URI literal")));
    }

    @Test
    void enforcesDirectiveAndSourceBounds() {
        DartMoveDependencyLimits limits = new DartMoveDependencyLimits(
                8, 32, 64, 1, 16, 8);
        DartDirectiveScanner bounded = new DartDirectiveScanner(limits);

        DartDirectiveScanResult.Rejected tooMany = assertInstanceOf(
                DartDirectiveScanResult.Rejected.class,
                bounded.scan(snapshot(
                        "lib/source.dart", "import 'a'; export 'b';")));
        DartDirectiveScanResult.Rejected tooLarge = assertInstanceOf(
                DartDirectiveScanResult.Rejected.class,
                bounded.scan(snapshot(
                        "lib/source.dart", "x".repeat(33))));

        assertAll(
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.TOO_MANY_DIRECTIVES,
                        tooMany.diagnostic().code()),
                () -> assertEquals(
                        DartMoveDependencyDiagnostic.Code.SOURCE_TOO_LARGE,
                        tooLarge.diagnostic().code()));
    }

    private static DartMoveSourceSnapshot snapshot(String path, String source) {
        return new DartMoveSourceSnapshot(
                path, source.getBytes(StandardCharsets.UTF_8));
    }
}
