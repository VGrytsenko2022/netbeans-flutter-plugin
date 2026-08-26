package dev.flutter.netbeans.designer.source;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.flutter.netbeans.designer.model.DartSourceDescriptor;
import dev.flutter.netbeans.designer.model.ManagedRegion;
import dev.flutter.netbeans.designer.model.ManagedRegions;
import dev.flutter.netbeans.designer.model.WidgetClassKind;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DartSourceIntegrityScannerTest {

    private static final String IMPORTS_PAYLOAD =
            "import 'package:flutter/material.dart';\n";
    private static final String BUILD_PAYLOAD = "  @override\n"
            + "  Widget build(BuildContext context) {\n"
            + "    return const SizedBox();\n"
            + "  }\n";

    @Test
    void verifiesGoldenPayloadHashesClassAndExactSourceBaseline() {
        String source = source("\n", "StatelessWidget");
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);

        DartSourceIntegrityResult result = scan(bytes, descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS));

        assertTrue(result.onDiskDeclaredMatch(), () -> result.diagnostics().toString());
        assertEquals(DartSourceIntegrityStatus.ON_DISK_DECLARED_MATCH, result.status());
        assertEquals(List.of("imports", "build"), result.regions().stream()
                .map(DartManagedRegionSnapshot::id)
                .toList());
        DartDesignerSuperclassOccurrence superclass = result
                .superclassOccurrence().orElseThrow();
        int superclassUtf16 = source.indexOf("StatelessWidget");
        int superclassByte = indexOf(bytes,
                "StatelessWidget".getBytes(StandardCharsets.UTF_8));
        assertEquals("HomePage", superclass.className());
        assertEquals("StatelessWidget", superclass.symbolName());
        assertEquals(superclassUtf16, superclass.startUtf16());
        assertEquals(superclassUtf16 + "StatelessWidget".length(),
                superclass.endUtf16());
        assertEquals(superclassByte, superclass.startByte());
        assertEquals(superclassByte + "StatelessWidget".length(),
                superclass.endByte());
        assertTrue(superclass.belongsTo(result.original().orElseThrow()));
        assertEquals(hash(IMPORTS_PAYLOAD), result.region("imports").orElseThrow()
                .normalizedSha256());
        assertEquals(hash(BUILD_PAYLOAD), result.region("build").orElseThrow()
                .normalizedSha256());
        assertArrayEquals(bytes, result.original().orElseThrow().copyBytes());
    }

    @Test
    void normalizesCrLfAndCrToTheSameManagedRegionHashes() {
        DartSourceDescriptor descriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS);

        DartSourceIntegrityResult crlf = scan(
                source("\r\n", "StatelessWidget").getBytes(StandardCharsets.UTF_8),
                descriptor);
        DartSourceIntegrityResult cr = scan(
                source("\r", "StatelessWidget").getBytes(StandardCharsets.UTF_8),
                descriptor);

        assertTrue(crlf.onDiskDeclaredMatch(), () -> crlf.diagnostics().toString());
        assertTrue(cr.onDiskDeclaredMatch(), () -> cr.diagnostics().toString());
        assertEquals(crlf.regions().stream().map(DartManagedRegionSnapshot::normalizedSha256)
                .toList(), cr.regions().stream()
                .map(DartManagedRegionSnapshot::normalizedSha256).toList());
    }

    @Test
    void reportsTheExactRegionWhenAStoredHashNoLongerMatches() {
        DartSourceDescriptor descriptor = descriptor(
                "0".repeat(64), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS);

        DartSourceIntegrityResult result = scan(
                source("\n", "StatelessWidget").getBytes(StandardCharsets.UTF_8),
                descriptor);

        assertFalse(result.onDiskDeclaredMatch());
        assertEquals(DartSourceIntegrityStatus.CONFLICT, result.status());
        DartSourceIntegrityDiagnostic mismatch = result.diagnostics().stream()
                .filter(issue -> issue.code()
                == DartSourceIntegrityDiagnosticCode.REGION_HASH_MISMATCH)
                .findFirst().orElseThrow();
        assertEquals("/source/managedRegions/imports/sha256", mismatch.path());
        assertEquals(Optional.of("imports"), mismatch.regionId());
        assertTrue(mismatch.message().contains(hash(IMPORTS_PAYLOAD)));
    }

    @Test
    void ignoresMarkerAndClassLookalikesInsideStringsCommentsAndInterpolation() {
        String prelude = "const sample = '''\n"
                + "// <netbeans-flutter-designer region=\"imports\">\n"
                + "class HomePage extends StatefulWidget {}\n"
                + "${(() {\n"
                + "  // <netbeans-flutter-designer region=\"build\">\n"
                + "  return \"// </netbeans-flutter-designer>\";\n"
                + "})()}\n"
                + "// </netbeans-flutter-designer>\n"
                + "''';\n"
                + "/* class HomePage extends StatefulWidget { }\n"
                + " * // <netbeans-flutter-designer region=\"build\">\n"
                + " * // </netbeans-flutter-designer>\n"
                + " */\n";
        String real = source("\n", "StatelessWidget");

        DartSourceIntegrityResult result = scan(
                (prelude + real).getBytes(StandardCharsets.UTF_8),
                descriptor(hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD),
                        WidgetClassKind.STATELESS));

        assertTrue(result.onDiskDeclaredMatch(), () -> result.diagnostics().toString());
        assertEquals(2, result.regions().size());
    }

    @ParameterizedTest
    @MethodSource("invalidMarkerTopologies")
    void rejectsEveryAmbiguousMarkerTopology(
            String managedSource,
            DartSourceIntegrityDiagnosticCode expectedCode) {
        String source = managedSource
                + "class HomePage extends StatelessWidget {}\n";

        DartSourceIntegrityResult result = scan(
                source.getBytes(StandardCharsets.UTF_8),
                descriptor(hash("generated();\n"), hash("generated();\n"),
                        WidgetClassKind.STATELESS));

        assertFalse(result.onDiskDeclaredMatch());
        assertTrue(result.diagnostics().stream()
                .anyMatch(issue -> issue.code() == expectedCode),
                () -> "Expected " + expectedCode + " in " + result.diagnostics());
    }

    @Test
    void distinguishesMissingDuplicateAndWrongKindDesignerClasses() {
        DartSourceDescriptor descriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS);
        String regions = source("\n", "StatelessWidget");

        DartSourceIntegrityResult missing = scan(
                regions.replace("HomePage", "AnotherPage")
                        .getBytes(StandardCharsets.UTF_8), descriptor);
        DartSourceIntegrityResult duplicate = scan(
                (regions + "class HomePage extends StatelessWidget {}\n")
                        .getBytes(StandardCharsets.UTF_8), descriptor);
        DartSourceIntegrityResult wrongKind = scan(
                source("\n", "StatefulWidget").getBytes(StandardCharsets.UTF_8),
                descriptor);

        assertHasCode(missing, DartSourceIntegrityDiagnosticCode.CLASS_MISSING);
        assertHasCode(duplicate, DartSourceIntegrityDiagnosticCode.CLASS_DUPLICATE);
        assertHasCode(wrongKind, DartSourceIntegrityDiagnosticCode.CLASS_KIND_MISMATCH);
        assertTrue(missing.superclassOccurrence().isEmpty());
        assertTrue(duplicate.superclassOccurrence().isEmpty());
        assertTrue(wrongKind.superclassOccurrence().isEmpty());
    }

    @Test
    void requiresTopLevelUnqualifiedRootAndDirectStatelessRegionScopes() {
        DartSourceDescriptor descriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS);
        String nestedImports = "void configure() {\n  " + open("imports")
                + IMPORTS_PAYLOAD
                + "  " + DartSourceIntegrityScanner.CLOSE_MARKER + "\n}\n"
                + statelessClassWithBuild("HomePage");
        String buildInWrongClass = region("imports", IMPORTS_PAYLOAD)
                + "class HomePage extends StatelessWidget {}\n"
                + statelessClassWithBuild("OtherPage");
        String nestedRoot = region("imports", IMPORTS_PAYLOAD)
                + "void configure() {\n"
                + statelessClassWithBuild("HomePage")
                + "}\n";
        String qualifiedBase = source("\n", "widgets.StatelessWidget");

        assertHasCode(scan(nestedImports.getBytes(StandardCharsets.UTF_8), descriptor),
                DartSourceIntegrityDiagnosticCode.REGION_SCOPE_MISMATCH);
        assertHasCode(scan(buildInWrongClass.getBytes(StandardCharsets.UTF_8), descriptor),
                DartSourceIntegrityDiagnosticCode.REGION_SCOPE_MISMATCH);
        assertHasCode(scan(nestedRoot.getBytes(StandardCharsets.UTF_8), descriptor),
                DartSourceIntegrityDiagnosticCode.CLASS_SCOPE_MISMATCH);
        DartSourceIntegrityResult qualified = scan(
                qualifiedBase.getBytes(StandardCharsets.UTF_8), descriptor);
        assertHasCode(qualified,
                DartSourceIntegrityDiagnosticCode.QUALIFIED_WIDGET_BASE_UNSUPPORTED);
        assertEquals(DartSourceIntegrityStatus.UNSUPPORTED, qualified.status());
        assertTrue(qualified.superclassOccurrence().isEmpty());
    }

    @Test
    void rejectsManagedRegionsNestedInParenthesesOrBrackets() {
        String nestedImportsPayload = "  42,\n";
        String nestedImports = "final values = <int>[\n"
                + "  " + region("imports", nestedImportsPayload)
                + "];\n"
                + statelessClassWithBuild("HomePage");
        DartSourceDescriptor nestedImportsDescriptor = descriptor(
                hash(nestedImportsPayload), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS);

        String nestedBuildPayload = "    builder: (context) => const SizedBox(),\n";
        String nestedBuild = region("imports", IMPORTS_PAYLOAD)
                + "class HomePage extends StatelessWidget {\n"
                + "  final child = Builder(\n"
                + "    " + open("build")
                + nestedBuildPayload
                + "    " + DartSourceIntegrityScanner.CLOSE_MARKER + "\n"
                + "  );\n"
                + "  @override\n"
                + "  Widget build(BuildContext context) => const SizedBox();\n"
                + "}\n";
        DartSourceDescriptor nestedBuildDescriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(nestedBuildPayload), WidgetClassKind.STATELESS);

        assertHasCode(scan(nestedImports.getBytes(StandardCharsets.UTF_8),
                nestedImportsDescriptor),
                DartSourceIntegrityDiagnosticCode.REGION_SCOPE_MISMATCH);
        assertHasCode(scan(nestedBuild.getBytes(StandardCharsets.UTF_8),
                nestedBuildDescriptor),
                DartSourceIntegrityDiagnosticCode.REGION_SCOPE_MISMATCH);
    }

    @Test
    void rejectsGenericBoundsThatOnlyLookLikeTheClassSuperclass() {
        DartSourceDescriptor descriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS);
        String spoofed = source("\n", "StatelessWidget").replace(
                "class HomePage extends StatelessWidget",
                "class HomePage<T extends StatelessWidget>");
        String validGeneric = source("\n", "StatelessWidget").replace(
                "class HomePage extends StatelessWidget",
                "class HomePage<T extends Object> extends StatelessWidget");

        DartSourceIntegrityResult spoofedResult = scan(
                spoofed.getBytes(StandardCharsets.UTF_8), descriptor);
        assertHasCode(spoofedResult,
                DartSourceIntegrityDiagnosticCode.CLASS_KIND_MISMATCH);
        assertTrue(spoofedResult.superclassOccurrence().isEmpty());
        DartSourceIntegrityResult accepted = scan(
                validGeneric.getBytes(StandardCharsets.UTF_8), descriptor);
        assertTrue(accepted.onDiskDeclaredMatch(), () -> accepted.diagnostics().toString());
    }

    @Test
    void rejectsLocalTypeDeclarationsThatShadowTheFlutterWidgetBase() {
        DartSourceDescriptor descriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS);
        for (String declaration : List.of(
                "class StatelessWidget {}\n",
                "typedef StatelessWidget = Object;\n",
                "mixin StatelessWidget {}\n",
                "mixin class StatelessWidget {}\n",
                "enum StatelessWidget { value }\n",
                "extension type StatelessWidget(Object value) {}\n")) {
            DartSourceIntegrityResult result = scan(
                    (declaration + source("\n", "StatelessWidget"))
                            .getBytes(StandardCharsets.UTF_8),
                    descriptor);

            assertHasCode(result,
                    DartSourceIntegrityDiagnosticCode.LOCAL_WIDGET_BASE_SHADOWED);
            assertEquals(DartSourceIntegrityStatus.UNSUPPORTED, result.status());
            assertTrue(result.superclassOccurrence().isEmpty());
        }
    }

    @Test
    void keepsStatefulBindingExplicitlyUnsupportedInThisReadOnlySlice() {
        DartSourceDescriptor descriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD), WidgetClassKind.STATEFUL);

        DartSourceIntegrityResult result = scan(
                source("\n", "StatefulWidget").getBytes(StandardCharsets.UTF_8),
                descriptor);

        assertHasCode(result,
                DartSourceIntegrityDiagnosticCode.STATEFUL_SOURCE_BINDING_UNSUPPORTED);
        assertEquals(DartSourceIntegrityStatus.UNSUPPORTED, result.status());
        assertTrue(result.original().isPresent());
        assertTrue(result.superclassOccurrence().isEmpty());
        assertEquals(2, result.regions().size(),
                "hash facts remain available for a concrete unsupported result");
    }

    @Test
    void preservesUnsupportedClassificationWhenDiagnosticDetailsAreTruncated() {
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner(
                new DartSourceIntegrityLimits(4_096, 8, 1));
        DartSourceDescriptor descriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD), WidgetClassKind.STATEFUL);

        DartSourceIntegrityResult result = scanner.scan(
                "class HomePage extends StatefulWidget {}\n"
                        .getBytes(StandardCharsets.UTF_8),
                descriptor);

        assertEquals(1, result.diagnostics().size());
        assertEquals(DartSourceIntegrityStatus.UNSUPPORTED, result.status());
        assertEquals(DartSourceIntegrityDiagnosticCode.STATEFUL_SOURCE_BINDING_UNSUPPORTED,
                result.primaryDiagnostic().orElseThrow().code());
    }

    @Test
    void selectsAnUnsupportedPrimaryDiagnosticAheadOfAnEarlierHashConflict() {
        DartSourceDescriptor descriptor = descriptor(
                "0".repeat(64), hash(BUILD_PAYLOAD), WidgetClassKind.STATEFUL);

        DartSourceIntegrityResult result = scan(
                source("\n", "StatefulWidget").getBytes(StandardCharsets.UTF_8),
                descriptor);

        assertHasCode(result, DartSourceIntegrityDiagnosticCode.REGION_HASH_MISMATCH);
        assertHasCode(result,
                DartSourceIntegrityDiagnosticCode.STATEFUL_SOURCE_BINDING_UNSUPPORTED);
        assertEquals(DartSourceIntegrityStatus.UNSUPPORTED, result.status());
        assertEquals(DartSourceIntegrityDiagnosticCode.STATEFUL_SOURCE_BINDING_UNSUPPORTED,
                result.primaryDiagnostic().orElseThrow().code());
    }

    @Test
    void ignoresDartShebangAndRejectsOrdinaryStringsThatCrossPhysicalLines() {
        DartSourceDescriptor descriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS);
        String shebangOnlyClass = "#!/usr/bin/env class HomePage extends StatelessWidget\n"
                + region("imports", IMPORTS_PAYLOAD)
                + "class OtherPage extends StatelessWidget {\n"
                + "  " + region("build", BUILD_PAYLOAD)
                + "}\n";
        String invalidString = source("\n", "StatelessWidget")
                + "const invalid = 'first line\nsecond line';\n";

        assertHasCode(scan(shebangOnlyClass.getBytes(StandardCharsets.UTF_8), descriptor),
                DartSourceIntegrityDiagnosticCode.CLASS_MISSING);
        assertHasCode(scan(invalidString.getBytes(StandardCharsets.UTF_8), descriptor),
                DartSourceIntegrityDiagnosticCode.UNTERMINATED_STRING);
    }

    @Test
    void acceptsUtf8BomButRejectsMalformedUtf8WithoutLosingTheBoundedBaseline() {
        byte[] source = source("\n", "StatelessWidget").getBytes(StandardCharsets.UTF_8);
        byte[] bomSource = new byte[source.length + 3];
        bomSource[0] = (byte) 0xEF;
        bomSource[1] = (byte) 0xBB;
        bomSource[2] = (byte) 0xBF;
        System.arraycopy(source, 0, bomSource, 3, source.length);
        DartSourceDescriptor descriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS);

        DartSourceIntegrityResult bom = scan(bomSource, descriptor);
        DartSourceIntegrityResult malformed = scan(
                new byte[]{(byte) 0xC3, (byte) 0x28}, descriptor);

        assertTrue(bom.onDiskDeclaredMatch(), () -> bom.diagnostics().toString());
        assertHasCode(malformed, DartSourceIntegrityDiagnosticCode.INVALID_UTF8);
        assertTrue(malformed.superclassOccurrence().isEmpty());
        assertArrayEquals(new byte[]{(byte) 0xC3, (byte) 0x28},
                malformed.original().orElseThrow().copyBytes());
    }

    @Test
    void mapsScannerOwnedSuperclassAcrossBomCrLfAndNonBmpPrefix() {
        String decoded = "// user-owned 😀 Привіт\r\n"
                + source("\r\n", "StatelessWidget");
        byte[] content = decoded.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = new byte[content.length + 3];
        bytes[0] = (byte) 0xEF;
        bytes[1] = (byte) 0xBB;
        bytes[2] = (byte) 0xBF;
        System.arraycopy(content, 0, bytes, 3, content.length);

        DartSourceIntegrityResult result = scan(bytes, descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD),
                WidgetClassKind.STATELESS));

        assertTrue(result.onDiskDeclaredMatch(), () -> result.diagnostics().toString());
        DartDesignerSuperclassOccurrence occurrence = result
                .superclassOccurrence().orElseThrow();
        int expectedUtf16 = decoded.indexOf("StatelessWidget");
        int expectedByte = indexOf(bytes,
                "StatelessWidget".getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedUtf16, occurrence.startUtf16());
        assertEquals(expectedByte, occurrence.startByte());
        assertEquals("StatelessWidget".length(), occurrence.lengthUtf16());
        assertEquals("StatelessWidget".getBytes(StandardCharsets.UTF_8).length,
                occurrence.lengthBytes());
    }

    @Test
    void rejectsDetachedOrMismatchedSuperclassCoordinates() {
        byte[] bytes = source("\n", "StatelessWidget")
                .getBytes(StandardCharsets.UTF_8);
        DartSourceDescriptor descriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD),
                WidgetClassKind.STATELESS);
        DartSourceIntegrityResult first = scan(bytes, descriptor);
        DartSourceIntegrityResult second = scan(bytes, descriptor);
        DartDesignerSuperclassOccurrence occurrence = first
                .superclassOccurrence().orElseThrow();

        assertThrows(IllegalArgumentException.class,
                () -> new DartSourceIntegrityResult(
                        second.original(),
                        second.regions(),
                        List.of(),
                        Optional.of(occurrence)));
        assertThrows(IllegalArgumentException.class,
                () -> new DartDesignerSuperclassOccurrence(
                        first.original().orElseThrow(),
                        occurrence.className(),
                        occurrence.startUtf16() + 1,
                        occurrence.endUtf16(),
                        occurrence.startByte(),
                        occurrence.endByte()));
    }

    @Test
    void reportsOversizedSourceWithoutCreatingAnotherExactByteSnapshot() {
        DartSourceIntegrityScanner scanner = new DartSourceIntegrityScanner(
                new DartSourceIntegrityLimits(16, 8, 8));

        DartSourceIntegrityResult result = scanner.scan(
                new byte[17], descriptor("0".repeat(64), "0".repeat(64),
                        WidgetClassKind.STATELESS));

        assertFalse(result.onDiskDeclaredMatch());
        assertTrue(result.original().isEmpty());
        assertEquals(DartSourceIntegrityStatus.UNAVAILABLE, result.status());
        assertHasCode(result, DartSourceIntegrityDiagnosticCode.SOURCE_TOO_LARGE);
    }

    @Test
    void exposesUtf8ByteOffsetsAndDefensiveCopiesForUnicodePayloads() {
        String imports = "// Привіт before payload\n" + IMPORTS_PAYLOAD;
        String source = source(imports, BUILD_PAYLOAD, "\n", "StatelessWidget");
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        DartSourceIntegrityResult result = scan(bytes, descriptor(
                hash(imports), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS));
        DartManagedRegionSnapshot importsRegion = result.region("imports").orElseThrow();
        byte[] expectedPayload = imports.getBytes(StandardCharsets.UTF_8);

        assertTrue(result.onDiskDeclaredMatch(), () -> result.diagnostics().toString());
        assertEquals(indexOf(bytes, expectedPayload), importsRegion.payloadStartByte());
        assertEquals(expectedPayload.length, importsRegion.payloadLengthBytes());
        byte[] copy = result.original().orElseThrow().copyBytes();
        copy[0] ^= 0x7F;
        assertArrayEquals(bytes, result.original().orElseThrow().copyBytes());
    }

    @Test
    void normalizationAlwaysProducesExactlyOneTrailingLfIncludingEmptyPayload() {
        String emptyHash = hash("");

        assertEquals(emptyHash, hash("\n\n"));
        assertEquals(emptyHash, hash("\r\n\r"));
        assertEquals("\n", DartManagedRegionHashing.normalize("\r\n\r"));
        assertEquals("text\n", DartManagedRegionHashing.normalize("text\r\n\n"));
        assertNotEquals(hash("é"), hash("e\u0301"),
                "hashing must not apply Unicode normalization");
        assertThrows(IllegalArgumentException.class,
                () -> hash("broken \uD800 scalar"));
    }

    @Test
    void rejectsUnterminatedLexicalStructuresInsteadOfTrustingEarlierMarkers() {
        String valid = source("\n", "StatelessWidget");

        DartSourceIntegrityResult string = scan(
                (valid + "const broken = '''").getBytes(StandardCharsets.UTF_8),
                descriptor(hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD),
                        WidgetClassKind.STATELESS));
        DartSourceIntegrityResult comment = scan(
                (valid + "/* broken").getBytes(StandardCharsets.UTF_8),
                descriptor(hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD),
                        WidgetClassKind.STATELESS));

        assertHasCode(string, DartSourceIntegrityDiagnosticCode.UNTERMINATED_STRING);
        assertHasCode(comment,
                DartSourceIntegrityDiagnosticCode.UNTERMINATED_BLOCK_COMMENT);
    }

    @Test
    void boundsNestedStringInterpolationBeforeTheJavaStackCanOverflow() {
        String expression = "0";
        for (int nesting = 0; nesting < 512; nesting++) {
            expression = "\"${" + expression + "}\"";
        }
        String source = "final nested = " + expression + ";\n"
                + source("\n", "StatelessWidget");

        DartSourceIntegrityResult result = scan(
                source.getBytes(StandardCharsets.UTF_8),
                descriptor(hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD),
                        WidgetClassKind.STATELESS));

        assertHasCode(result,
                DartSourceIntegrityDiagnosticCode.LEXICAL_NESTING_TOO_DEEP);
    }

    @Test
    void rejectsUnmatchedAndUnclosedStructuralDelimiters() {
        DartSourceDescriptor descriptor = descriptor(
                hash(IMPORTS_PAYLOAD), hash(BUILD_PAYLOAD), WidgetClassKind.STATELESS);

        assertHasCode(scan(("}\n" + source("\n", "StatelessWidget"))
                .getBytes(StandardCharsets.UTF_8), descriptor),
                DartSourceIntegrityDiagnosticCode.UNMATCHED_DELIMITER);
        assertHasCode(scan((source("\n", "StatelessWidget") + "{\n")
                .getBytes(StandardCharsets.UTF_8), descriptor),
                DartSourceIntegrityDiagnosticCode.UNCLOSED_DELIMITER);
        assertHasCode(scan(("([)]\n" + source("\n", "StatelessWidget"))
                .getBytes(StandardCharsets.UTF_8), descriptor),
                DartSourceIntegrityDiagnosticCode.UNMATCHED_DELIMITER);
    }

    @Test
    void usesAHeapPracticalDefaultSourceLimit() {
        assertEquals(2 * 1024 * 1024,
                DartSourceIntegrityLimits.defaults().maxSourceBytes());
    }

    private static Stream<Arguments> invalidMarkerTopologies() {
        String openImports = open("imports");
        String openBuild = open("build");
        String close = DartSourceIntegrityScanner.CLOSE_MARKER + "\n";
        String body = "generated();\n";
        return Stream.of(
                Arguments.of("", DartSourceIntegrityDiagnosticCode.MISSING_REGION),
                Arguments.of("// <netbeans-flutter-designer region='imports'>\n"
                        + body + close,
                        DartSourceIntegrityDiagnosticCode.MALFORMED_MARKER),
                Arguments.of(openImports + openBuild + body + close + close,
                        DartSourceIntegrityDiagnosticCode.NESTED_MARKER),
                Arguments.of(region("imports", body) + region("imports", body)
                        + region("build", body),
                        DartSourceIntegrityDiagnosticCode.DUPLICATE_REGION),
                Arguments.of(region("extra", body) + region("imports", body)
                        + region("build", body),
                        DartSourceIntegrityDiagnosticCode.UNKNOWN_REGION),
                Arguments.of(region("build", body) + region("imports", body),
                        DartSourceIntegrityDiagnosticCode.REORDERED_REGIONS),
                Arguments.of(close + region("imports", body) + region("build", body),
                        DartSourceIntegrityDiagnosticCode.UNMATCHED_CLOSE_MARKER),
                Arguments.of(openImports + body,
                        DartSourceIntegrityDiagnosticCode.UNCLOSED_MARKER));
    }

    private static DartSourceIntegrityResult scan(
            byte[] bytes,
            DartSourceDescriptor descriptor) {
        return new DartSourceIntegrityScanner().scan(bytes, descriptor);
    }

    private static DartSourceDescriptor descriptor(
            String importsHash,
            String buildHash,
            WidgetClassKind kind) {
        return new DartSourceDescriptor(
                "home_page.dart",
                "HomePage",
                kind,
                Optional.empty(),
                new ManagedRegions(
                        new ManagedRegion(importsHash),
                        new ManagedRegion(buildHash)));
    }

    private static String source(String separator, String baseClass) {
        return source(IMPORTS_PAYLOAD, BUILD_PAYLOAD, separator, baseClass);
    }

    private static String source(
            String importsPayload,
            String buildPayload,
            String separator,
            String baseClass) {
        return open("imports").replace("\n", separator)
                + withSeparator(importsPayload, separator)
                + (DartSourceIntegrityScanner.CLOSE_MARKER + "\n").replace("\n", separator)
                + separator
                + "class HomePage extends " + baseClass + " {" + separator
                + ("  " + open("build")).replace("\n", separator)
                + withSeparator(buildPayload, separator)
                + ("  " + DartSourceIntegrityScanner.CLOSE_MARKER + "\n")
                        .replace("\n", separator)
                + "}" + separator;
    }

    private static String statelessClassWithBuild(String className) {
        return "class " + className + " extends StatelessWidget {\n"
                + "  " + open("build")
                + BUILD_PAYLOAD
                + "  " + DartSourceIntegrityScanner.CLOSE_MARKER + "\n"
                + "}\n";
    }

    private static String withSeparator(String value, String separator) {
        return value.replace("\n", separator);
    }

    private static String region(String id, String payload) {
        return open(id) + payload + DartSourceIntegrityScanner.CLOSE_MARKER + "\n";
    }

    private static String open(String id) {
        return DartSourceIntegrityScanner.OPEN_PREFIX + id
                + DartSourceIntegrityScanner.OPEN_SUFFIX + "\n";
    }

    private static String hash(String value) {
        return DartManagedRegionHashing.normalizedSha256(value);
    }

    private static void assertHasCode(
            DartSourceIntegrityResult result,
            DartSourceIntegrityDiagnosticCode code) {
        assertFalse(result.onDiskDeclaredMatch());
        assertTrue(result.diagnostics().stream().anyMatch(issue -> issue.code() == code),
                () -> "Expected " + code + " in " + result.diagnostics());
    }

    private static int indexOf(byte[] source, byte[] target) {
        outer:
        for (int offset = 0; offset <= source.length - target.length; offset++) {
            for (int index = 0; index < target.length; index++) {
                if (source[offset + index] != target[index]) {
                    continue outer;
                }
            }
            return offset;
        }
        return -1;
    }
}
