package dev.flutter.netbeans.plugin.designer.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import dev.flutter.netbeans.designer.source.DartManagedRegionHashing;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityResult;
import dev.flutter.netbeans.designer.source.DartSourceIntegrityScanner;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Keeps the core on-disk scanner and NetBeans guarded reader lexical contract aligned. */
class DartSourceIntegrityGuardParityTest {

    private static final String IMPORTS = "import 'package:flutter/material.dart';\n";
    private static final String BUILD = "  @override\n"
            + "  Widget build(BuildContext context) => const SizedBox();\n";

    @ParameterizedTest
    @MethodSource("validLexicalVariants")
    void bothReadersIgnoreTheSameLookalikesAndFindOnlyTheRealRegions(String source) {
        DartGuardedSectionMarkers.ParseResult guarded =
                DartGuardedSectionMarkers.parse(source.toCharArray());
        DartSourceIntegrityResult integrity = new DartSourceIntegrityScanner().scan(
                source.getBytes(StandardCharsets.UTF_8), descriptor());

        assertTrue(guarded.valid());
        assertEquals(List.of("imports", "build"), guarded.regions().stream()
                .map(DartGuardedSectionMarkers.Region::id)
                .toList());
        assertTrue(integrity.onDiskDeclaredMatch(),
                () -> integrity.diagnostics().toString());
    }

    @ParameterizedTest
    @MethodSource("ambiguousStructuralMarkers")
    void bothReadersFailClosedForAmbiguousStructuralMarkers(String source) {
        DartGuardedSectionMarkers.ParseResult guarded =
                DartGuardedSectionMarkers.parse(source.toCharArray());
        DartSourceIntegrityResult integrity = new DartSourceIntegrityScanner().scan(
                source.getBytes(StandardCharsets.UTF_8), descriptor());

        assertFalse(guarded.valid());
        assertFalse(integrity.onDiskDeclaredMatch());
        assertTrue(integrity.diagnostics().stream().anyMatch(issue -> switch (issue.code()) {
            case MALFORMED_MARKER,
                    NESTED_MARKER,
                    DUPLICATE_REGION,
                    UNMATCHED_CLOSE_MARKER,
                    UNCLOSED_MARKER -> true;
            default -> false;
        }), () -> integrity.diagnostics().toString());
    }

    private static Stream<String> validLexicalVariants() {
        return Stream.of(
                validSource("", "\n"),
                validSource("\uFEFF", "\r\n"),
                validSource("const fake = r'''\n"
                        + "// <netbeans-flutter-designer region=\"imports\">\n"
                        + "${'// </netbeans-flutter-designer>'}\n"
                        + "// </netbeans-flutter-designer>\n"
                        + "''';\n", "\n"),
                validSource("/* nested comment\n"
                        + "  /* // <netbeans-flutter-designer region=\"build\"> */\n"
                        + "  // </netbeans-flutter-designer>\n"
                        + "*/\n", "\n"));
    }

    private static Stream<String> ambiguousStructuralMarkers() {
        String tail = "class HomePage extends StatelessWidget {}\n";
        return Stream.of(
                "// <netbeans-flutter-designer region='imports'>\n"
                        + IMPORTS + close() + tail,
                open("imports") + open("build") + BUILD + close() + close() + tail,
                region("imports", IMPORTS) + region("imports", IMPORTS)
                        + region("build", BUILD) + tail,
                close() + region("imports", IMPORTS) + region("build", BUILD) + tail,
                open("imports") + IMPORTS + tail);
    }

    private static String validSource(String prelude, String separator) {
        return withSeparator(prelude, separator)
                + withSeparator(region("imports", IMPORTS), separator)
                + "class HomePage extends StatelessWidget {" + separator
                + "  " + withSeparator(open("build"), separator)
                + withSeparator(BUILD, separator)
                + "  " + withSeparator(close(), separator)
                + "}" + separator;
    }

    private static DartSourceDescriptor descriptor() {
        return new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                WidgetClassKind.STATELESS,
                Optional.empty(),
                new ManagedRegions(
                        new ManagedRegion(DartManagedRegionHashing.normalizedSha256(IMPORTS)),
                        new ManagedRegion(DartManagedRegionHashing.normalizedSha256(BUILD))));
    }

    private static String region(String id, String body) {
        return open(id) + body + close();
    }

    private static String open(String id) {
        return DartSourceIntegrityScanner.OPEN_PREFIX + id
                + DartSourceIntegrityScanner.OPEN_SUFFIX + "\n";
    }

    private static String close() {
        return DartSourceIntegrityScanner.CLOSE_MARKER + "\n";
    }

    private static String withSeparator(String value, String separator) {
        return value.replace("\n", separator);
    }
}
