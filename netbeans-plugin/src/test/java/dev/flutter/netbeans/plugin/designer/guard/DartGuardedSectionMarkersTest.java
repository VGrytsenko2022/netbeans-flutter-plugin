package dev.flutter.netbeans.plugin.designer.guard;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DartGuardedSectionMarkersTest {

    @Test
    void masksMarkersWithoutChangingOffsetsAndRestoresLfSourceExactly() {
        String source = "import 'package:flutter/material.dart';\n"
                + "// <netbeans-flutter-designer region=\"screen.build\">\n"
                + "Widget build(BuildContext context) {\n"
                + "  return const Text('Привіт');\n"
                + "}\n"
                + "// </netbeans-flutter-designer>\n"
                + "void manualHelper() {}\n";

        DartGuardedSectionMarkers.ParseResult parsed = parse(source);

        assertTrue(parsed.valid());
        assertEquals(List.of("screen.build"), parsed.regions().stream()
                .map(DartGuardedSectionMarkers.Region::id)
                .toList());
        assertEquals(source.length(), parsed.content().length);
        assertFalse(new String(parsed.content()).contains("netbeans-flutter-designer"));
        assertTrue(new String(parsed.content()).contains("return const Text('Привіт');"));
        assertArrayEquals(source.toCharArray(), DartGuardedSectionMarkers.writeRegions(
                parsed.regions(), parsed.content()));
    }

    @Test
    void preservesBomUnicodeAndMixedLineSeparatorsOnDirectRoundTrip() {
        String source = "\uFEFFmanual();\r\n"
                + "// <netbeans-flutter-designer region=\"build-1\">\r\n"
                + "const Text('Łódź');\n"
                + "// </netbeans-flutter-designer>\r"
                + "tail();\r";

        DartGuardedSectionMarkers.ParseResult parsed = parse(source);

        assertTrue(parsed.valid());
        assertArrayEquals(source.toCharArray(), DartGuardedSectionMarkers.writeRegions(
                parsed.regions(), parsed.content()));
    }

    @Test
    void recognizesAndRestoresAFirstLineMarkerImmediatelyAfterUtf8Bom() {
        String source = "\uFEFF// <netbeans-flutter-designer region=\"imports\">\r\n"
                + "import 'package:flutter/widgets.dart';\r\n"
                + "// </netbeans-flutter-designer>\r\n";

        DartGuardedSectionMarkers.ParseResult parsed = parse(source);

        assertTrue(parsed.valid());
        assertEquals(List.of("imports"), parsed.regions().stream()
                .map(DartGuardedSectionMarkers.Region::id).toList());
        assertArrayEquals(source.toCharArray(), DartGuardedSectionMarkers.writeRegions(
                parsed.regions(), parsed.content()));
    }

    @Test
    void roundTripsIndentedGoldenBuildMarkersWithCrLf() {
        String source = "class HomePage extends StatelessWidget {\r\n"
                + "  // <netbeans-flutter-designer region=\"build\">\r\n"
                + "  @override\r\n"
                + "  Widget build(BuildContext context) {\r\n"
                + "    return const Text('Home');\r\n"
                + "  }\r\n"
                + "  // </netbeans-flutter-designer>\r\n"
                + "}\r\n";

        DartGuardedSectionMarkers.ParseResult parsed = parse(source);

        assertTrue(parsed.valid());
        assertEquals("build", parsed.regions().getFirst().id());
        String masked = new String(parsed.content());
        assertTrue(masked.contains("  " + " ".repeat(
                "// <netbeans-flutter-designer region=\"build\">".length()) + "\r\n"));
        assertArrayEquals(source.toCharArray(), DartGuardedSectionMarkers.writeRegions(
                parsed.regions(), parsed.content()));
    }

    @Test
    void supportsMultipleUniqueNonNestedRegions() {
        String source = region("imports", "import 'package:flutter/material.dart';", "\n")
                + "manual();\n"
                + region("screen.body", "const Text('body');", "\n");

        DartGuardedSectionMarkers.ParseResult parsed = parse(source);

        assertTrue(parsed.valid());
        assertEquals(List.of("imports", "screen.body"), parsed.regions().stream()
                .map(DartGuardedSectionMarkers.Region::id)
                .toList());
        assertArrayEquals(source.toCharArray(), DartGuardedSectionMarkers.writeRegions(
                parsed.regions(), parsed.content()));
    }

    @Test
    void ignoresDesignerTokenInsideNormalDartCode() {
        String source = "const marker = 'netbeans-flutter-designer';\n"
                + "const closing = '// </netbeans-flutter-designer>';\n"
                + "const escaped = \"quote: \\\" // <netbeans-flutter-designer region=\\\"fake\\\">\";\n"
                + "// Documentation mentions netbeans-flutter-designer support.\n";

        DartGuardedSectionMarkers.ParseResult parsed = parse(source);

        assertTrue(parsed.valid());
        assertTrue(parsed.regions().isEmpty());
        assertArrayEquals(source.toCharArray(), parsed.content());
    }

    @ParameterizedTest
    @MethodSource("lexicallyNonStructuralMarkerSources")
    void ignoresExactMarkerLinesOutsideNormalDartLineComments(String source) {
        DartGuardedSectionMarkers.ParseResult parsed = parse(source);

        assertTrue(parsed.valid());
        assertTrue(parsed.regions().isEmpty());
        assertArrayEquals(source.toCharArray(), parsed.content());
    }

    @Test
    void ignoresFakeTripleStringMarkersButStillFindsFollowingRealRegion() {
        String source = "const sample = '''\n"
                + "// <netbeans-flutter-designer region=\"screen.build\">\n"
                + "${'interpolation with // </netbeans-flutter-designer>'}\n"
                + "// </netbeans-flutter-designer>\n"
                + "''';\n"
                + region("screen.build", "generated();", "\n");

        DartGuardedSectionMarkers.ParseResult parsed = parse(source);

        assertTrue(parsed.valid());
        assertEquals(List.of("screen.build"), parsed.regions().stream()
                .map(DartGuardedSectionMarkers.Region::id)
                .toList());
        assertArrayEquals(source.toCharArray(), DartGuardedSectionMarkers.writeRegions(
                parsed.regions(), parsed.content()));
    }

    @ParameterizedTest
    @MethodSource("ambiguousOrMalformedSources")
    void createsNoRegionForMalformedNestedOrDuplicateMarkers(String source) {
        DartGuardedSectionMarkers.ParseResult parsed = parse(source);

        assertFalse(parsed.valid());
        assertTrue(parsed.regions().isEmpty());
        assertArrayEquals(source.toCharArray(), parsed.content());
    }

    @Test
    void writerAddsCanonicalMarkerLinesWhenGeneratedTextHasNoPlaceholders() {
        String generated = "Widget build(BuildContext context) {\r\n"
                + "  return const Text('generated');\r\n"
                + "}";
        var region = new DartGuardedSectionMarkers.Region("screen.build", 0, generated.length() - 1);

        char[] written = DartGuardedSectionMarkers.writeRegions(
                List.of(region), generated.toCharArray());
        String expected = "// <netbeans-flutter-designer region=\"screen.build\">\r\n"
                + generated
                + "\r\n// </netbeans-flutter-designer>";

        assertEquals(expected, new String(written));
        DartGuardedSectionMarkers.ParseResult reparsed = DartGuardedSectionMarkers.parse(written);
        assertTrue(reparsed.valid());
        assertEquals("screen.build", reparsed.regions().getFirst().id());
    }

    private static Stream<String> ambiguousOrMalformedSources() {
        return Stream.of(
                "// </netbeans-flutter-designer>\n",
                "// <netbeans-flutter-designer region=\"open\">\ncode();\n",
                "// <netbeans-flutter-designer region=\"outer\">\n"
                        + "// <netbeans-flutter-designer region=\"inner\">\n"
                        + "code();\n"
                        + "// </netbeans-flutter-designer>\n"
                        + "// </netbeans-flutter-designer>\n",
                region("same", "first();", "\n") + region("same", "second();", "\n"),
                "// <netbeans-flutter-designer region='wrong-quotes'>\n"
                        + "code();\n// </netbeans-flutter-designer>\n",
                "// <netbeans-flutter-designer region=\"trailing\"> \n"
                        + "code();\n// </netbeans-flutter-designer>\n");
    }

    private static Stream<String> lexicallyNonStructuralMarkerSources() {
        return Stream.of(
                "const single = '// <netbeans-flutter-designer region=\"fake\">';\n"
                        + "const close = '// </netbeans-flutter-designer>';\n",
                "const doubleQuoted = \"// <netbeans-flutter-designer region=\\\"fake\\\">\";\n"
                        + "const escaped = \"\\\\\"// </netbeans-flutter-designer>\";\n",
                "const multiline = '''\n"
                        + "// <netbeans-flutter-designer region=\"fake\">\n"
                        + "${value ?? '// </netbeans-flutter-designer>'}\n"
                        + "// </netbeans-flutter-designer>\n"
                        + "''';\n",
                "const multiline = \"\"\"\n"
                        + "// <netbeans-flutter-designer region=\"fake\">\n"
                        + "escaped \\\" quote and $value\n"
                        + "// </netbeans-flutter-designer>\n"
                        + "\"\"\";\n",
                "const rawSingle = r'// <netbeans-flutter-designer region=\"fake\">';\n"
                        + "const rawDouble = r\"// </netbeans-flutter-designer>\";\n",
                "const rawTriple = r'''\n"
                        + "// <netbeans-flutter-designer region=\"fake\">\n"
                        + "${notInterpolation}\n"
                        + "// </netbeans-flutter-designer>\n"
                        + "''';\n",
                "const interpolated = '''${(() {\n"
                        + "  // <netbeans-flutter-designer region=\"fake\">\n"
                        + "  generated();\n"
                        + "  // </netbeans-flutter-designer>\n"
                        + "  return 1;\n"
                        + "})()}''';\n",
                "/* outer block\n"
                        + "  // <netbeans-flutter-designer region=\"fake\">\n"
                        + "  /* nested // </netbeans-flutter-designer> */\n"
                        + "  // </netbeans-flutter-designer>\n"
                        + "*/\n",
                "/** documentation block\n"
                        + " * // <netbeans-flutter-designer region=\"fake\">\n"
                        + " * // </netbeans-flutter-designer>\n"
                        + " */\n");
    }

    private static DartGuardedSectionMarkers.ParseResult parse(String source) {
        return DartGuardedSectionMarkers.parse(source.toCharArray());
    }

    private static String region(String id, String body, String separator) {
        return "// <netbeans-flutter-designer region=\"" + id + "\">" + separator
                + body + separator
                + "// </netbeans-flutter-designer>" + separator;
    }
}
